/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.subnets.Subnet;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.ip.unis.IpUni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.IpvcVpn;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.MefServices;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.MefService;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.IpvcChoice;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.Ipvc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.ipvc.VpnElans;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.ipvc.unis.Uni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.Identifier45;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.OdlArputilService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class SubnetListener extends UnimgrDataTreeChangeListener<Subnet> {
    private static final Logger Log = LoggerFactory.getLogger(SubnetListener.class);
    private ListenerRegistration<SubnetListener> subnetListenerRegistration;
    private final NotificationPublishService notificationPublishService;
    private final OdlArputilService arpUtilService;
    private final IUniPortManager uniPortManager;

    public SubnetListener(final DataBroker dataBroker, final NotificationPublishService notPublishService,
            final OdlArputilService arputilService, final UniPortManager uniPortManager) {
        super(dataBroker);
        this.notificationPublishService = notPublishService;
        this.arpUtilService = arputilService;
        this.uniPortManager = uniPortManager;

        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<Subnet> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                    MefInterfaceUtils.getSubnetsListInstanceIdentifier());
            subnetListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
            Log.info("IpvcDataTreeChangeListener created and registered");
        } catch (final Exception e) {
            Log.error("Ipvc DataChange listener registration failed !", e);
            throw new IllegalStateException("Ipvc registration Listener failed.", e);
        }
    }

    @Override
    public void close() throws Exception {
        subnetListenerRegistration.close();
    }

    @Override
    public void add(DataTreeModification<Subnet> newDataObject) {
        if (newDataObject.getRootPath() != null && newDataObject.getRootNode() != null) {
            Log.info("subnet {} created", newDataObject.getRootNode().getIdentifier());
        }

        createNetwork(newDataObject);
    }

    private void createNetwork(DataTreeModification<Subnet> newDataObject) {
        Subnet newSubnet = newDataObject.getRootNode().getDataAfter();
        Identifier45 nwUniId = newSubnet.getUniId();
        Identifier45 nwIpUniId = newSubnet.getIpUniId();

        InstanceIdentifier<Ipvc> ipvcId = findService(nwUniId, nwIpUniId);
        if (ipvcId == null) {
            Log.info("Subnet Uni {} IpUNI {} is not assosiated to service", nwUniId, nwIpUniId);
            return;
        }
        IpvcVpn ipvcVpn = MefServicesUtils.getOperIpvcVpn(dataBroker, ipvcId);
        if (ipvcVpn == null || ipvcVpn.getVpnElans() == null) {
            Log.error("Subnet Uni {} IpUNI {} is not operational", nwUniId, nwIpUniId);
            return;
        }

        String vpnId = ipvcVpn.getVpnId();
        synchronized (vpnId.intern()) {
            if (newSubnet.getGateway() == null) {
                checkCreateDirectNetwork(newSubnet, ipvcVpn);
            } else {
                createNonDirectNetwork(newSubnet, ipvcVpn);
            }
        }
    }

    @Override
    public void remove(DataTreeModification<Subnet> removedDataObject) {
        if (removedDataObject.getRootPath() != null && removedDataObject.getRootNode() != null) {
            Log.info("subnet {} deleted", removedDataObject.getRootNode().getIdentifier());
        }
    }

    @Override
    public void update(DataTreeModification<Subnet> modifiedDataObject) {
        if (modifiedDataObject.getRootPath() != null && modifiedDataObject.getRootNode() != null) {
            Log.info("subnet {} updated", modifiedDataObject.getRootNode().getIdentifier());
            Log.info("process as delete / create");
        }
    }

    private void createNonDirectNetwork(Subnet newSubnet, IpvcVpn ipvcVpn) {
        if (newSubnet.getGateway() == null)
            return;
        
        Identifier45 nwUniId = newSubnet.getUniId();
        Identifier45 nwIpUniId = newSubnet.getIpUniId();
        List<VpnElans> vpnElans = ipvcVpn.getVpnElans();
        
        
        
        MacAddress gwMacAddress = NetvirtVpnUtils.resolveGwMac(dataBroker, arpUtilService, vpnName,
                ipUni.getIpAddress(), newSubnet.getGateway(), interfaceName); // trunk
        
        if (ipUni.getSubnets() != null) {
            for (Subnet subnet : ipUni.getSubnets().getSubnet()) {
                MacAddress gwMacAddress = NetvirtVpnUtils.resolveGwMac(dataBroker, arpUtilService, vpnName,
                        ipUni.getIpAddress(), subnet.getGateway(), interfaceName); // trunk
                if (gwMacAddress == null) {
                    continue;
                }
                NetvirtVpnUtils.createUpdateVpnInterface(dataBroker, vpnName, interfaceName, subnet.getSubnet(),
                        gwMacAddress, false, ipUni.getIpAddress());
            }
        }

    }

   

    private void checkCreateDirectNetwork(Subnet newSubnet, IpvcVpn ipvcVpn) {
        if (newSubnet.getGateway() != null)
            return;
        Identifier45 nwUniId = newSubnet.getUniId();
        Identifier45 nwIpUniId = newSubnet.getIpUniId();
        List<VpnElans> vpnElans = ipvcVpn.getVpnElans();
        for (VpnElans vpnElan : vpnElans) {
            if (vpnElan.getUniId().equals(newSubnet.getUniId())
                    && (vpnElan.getIpUniId().equals(newSubnet.getIpUniId()))) {
                if (vpnElan.getSubnets() != null && vpnElan.getSubnets().contains(newSubnet)) {
                    Log.info("Network {} already created under UNI {} IpUni {}", newSubnet, nwUniId, nwIpUniId);
                    return;
                }
                Log.info("Creation directly connected network for vpn {} port {}", ipvcVpn.getVpnId(),
                        vpnElan.getElanPort());
                NetvirtVpnUtils.addDirectSubnetToVpn(dataBroker, notificationPublishService, ipvcVpn.getVpnId(),
                        vpnElan.getElanId(), newSubnet.getSubnet(), vpnElan.getElanPort());
            }
        }
    }
    
    private String findNetwork (Subnet newSubnet, IpvcVpn ipvcVpn) {
        List<VpnElans> vpnElans = ipvcVpn.getVpnElans();
        String subnetStr = NetvirtVpnUtils.ipPrefixToString(newSubnet.getSubnet())
        if (vpnElans == null) {
            return null;
        }
        for (VpnElans vpnElan : vpnElans) {
            if (vpnElan.getUniId().equals(newSubnet.getUniId())
                    && (vpnElan.getIpUniId().equals(newSubnet.getIpUniId()))) {
                if (vpnElan.getSubnets() != null && vpnElan.getSubnets().contains(newSubnet.getSubnet())) {
                    Log.info("Network {} already created under UNI {} IpUni {}", newSubnet, nwUniId, nwIpUniId);
                    return;
                }
            }
        }
    }
    
    
    
    private InstanceIdentifier<Ipvc> findService(Identifier45 uniId, Identifier45 ipUniId) {
        InstanceIdentifier<MefServices> path = MefServicesUtils.getMefServicesInstanceIdentifier();
        Optional<MefServices> mefServices = MdsalUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION, path);
        for (MefService service : mefServices.get().getMefService()) {
            if (service.getMefServiceChoice() instanceof IpvcChoice) {
                Ipvc ipvc = ((IpvcChoice) service.getMefServiceChoice()).getIpvc();
                if (ipvc.getUnis() == null || ipvc.getUnis().getUni() == null) {
                    continue;
                }
                List<Uni> unis = ipvc.getUnis().getUni();
                for (Uni uni : unis) {
                    if (uni.getUniId().equals(uniId) && uni.getIpUniId().equals(ipUniId)) {
                        return MefServicesUtils.getIpvcsInstanceIdentifier(service.getSvcId());
                    }
                }
            }
        }
        return null;
    }

}
