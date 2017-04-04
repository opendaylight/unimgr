/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.Subnets;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class SubnetListener extends UnimgrDataTreeChangeListener<Subnet> implements ISubnetManager {
    private static final Logger Log = LoggerFactory.getLogger(SubnetListener.class);
    private ListenerRegistration<SubnetListener> subnetListenerRegistration;
    private final IGwMacListener gwMacListener;

    public SubnetListener(final DataBroker dataBroker, final IGwMacListener gwMacListener) {
        super(dataBroker);
        this.gwMacListener = gwMacListener;
        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<Subnet> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                    MefInterfaceUtils.getSubnetsInstanceIdentifier());
            subnetListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
            Log.info("SubnetListener created and registered");
        } catch (final Exception e) {
            Log.error("SubnetListener listener registration failed !", e);
            throw new IllegalStateException("SubnetListener registration Listener failed.", e);
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

    @Override
    public void remove(DataTreeModification<Subnet> removedDataObject) {
        if (removedDataObject.getRootPath() != null && removedDataObject.getRootNode() != null) {
            Log.info("subnet {} deleted", removedDataObject.getRootNode().getIdentifier());
        }
        removeNetwork(removedDataObject);
    }

    @Override
    public void update(DataTreeModification<Subnet> modifiedDataObject) {
        if (modifiedDataObject.getRootPath() != null && modifiedDataObject.getRootNode() != null) {
            Log.info("subnet {} updated", modifiedDataObject.getRootNode().getIdentifier());
            Log.info("process as delete / create");
            removeNetwork(modifiedDataObject);
            createNetwork(modifiedDataObject);
        }
    }

    @Override
    public void assignIpUniNetworks(Identifier45 uniId, Identifier45 ipUniId, InstanceIdentifier<Ipvc> ipvcId) {
        InstanceIdentifier<Subnets> id = MefInterfaceUtils.getSubnetListInstanceIdentifier();
        Optional<Subnets> allList = MdsalUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION, id);
        Subnets allSubnets = allList.isPresent() ? allList.get() : null;
        List<Subnet> allSubnet = allSubnets != null && allSubnets.getSubnet() != null ? allSubnets.getSubnet()
                : Collections.emptyList();
        List<Subnet> ipUniSubnets = allSubnet.stream()
                .filter(s -> s.getUniId().equals(uniId) && s.getIpUniId().equals(ipUniId)).collect(Collectors.toList());
        // recreate networks on restart
        ipUniSubnets.forEach(s -> createNetwork(s, uniId, ipUniId));
    }

    @Override
    public void unAssignIpUniNetworks(Identifier45 uniId, Identifier45 ipUniId, InstanceIdentifier<Ipvc> ipvcId) {
        InstanceIdentifier<Subnets> id = MefInterfaceUtils.getSubnetListInstanceIdentifier();
        Optional<Subnets> allList = MdsalUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION, id);
        Subnets allSubnets = allList.isPresent() ? allList.get() : null;
        List<Subnet> allSubnet = allSubnets != null && allSubnets.getSubnet() != null ? allSubnets.getSubnet()
                : Collections.emptyList();
        List<Subnet> ipUniSubnets = allSubnet.stream()
                .filter(s -> s.getUniId().equals(uniId) && s.getIpUniId().equals(ipUniId)).collect(Collectors.toList());
        ipUniSubnets.forEach(s -> removeNetwork(s, uniId, ipUniId, ipvcId));
    }

    private void createNetwork(DataTreeModification<Subnet> newDataObject) {
        Subnet newSubnet = newDataObject.getRootNode().getDataAfter();

        Identifier45 nwUniId = newSubnet.getUniId();
        Identifier45 nwIpUniId = newSubnet.getIpUniId();

        createNetwork(newSubnet, nwUniId, nwIpUniId);
    }

    private void createNetwork(Subnet newSubnet, Identifier45 nwUniId, Identifier45 nwIpUniId) {
        String subnetStr = NetvirtVpnUtils.ipPrefixToString(newSubnet.getSubnet());

        synchronized (subnetStr.intern()) {
            InstanceIdentifier<Ipvc> ipvcId = findService(nwUniId, nwIpUniId, getMefServices());
            if (ipvcId == null) {
                Log.info("Subnet Uni {} IpUNI {} is not assosiated to service", nwUniId, nwIpUniId);
                return;
            }
            IpvcVpn ipvcVpn = MefServicesUtils.getOperIpvcVpn(dataBroker, ipvcId);
            if (ipvcVpn == null || ipvcVpn.getVpnElans() == null) {
                Log.error("Subnet Uni {} IpUNI {} is not operational", nwUniId, nwIpUniId);
                return;
            }
            VpnElans vpnElan = MefServicesUtils.findVpnForNetwork(newSubnet, ipvcVpn);
            if (vpnElan == null) {
                Log.error("Subnet Uni {} IpUNI {} for network {} is not operational", nwUniId, nwIpUniId, subnetStr);
                return;
            }
            if (MefServicesUtils.findNetwork(newSubnet, vpnElan) != null) {
                Log.info("Network {} exists already", subnetStr);
                return;
            }

            if (newSubnet.getGateway() == null) {
                Log.trace("Direct subnet removed {}", newSubnet.getSubnet());
            } else {
                createNonDirectNetwork(newSubnet, ipvcVpn, ipvcId, vpnElan);
            }

            MefServicesUtils.addOperIpvcVpnElan(dataBroker, ipvcId, ipvcVpn.getVpnId(), nwUniId, nwIpUniId,
                    vpnElan.getElanId(), vpnElan.getElanPort(), Collections.singletonList(subnetStr));
        }

    }

    private void createNonDirectNetwork(Subnet newSubnet, IpvcVpn ipvcVpn, InstanceIdentifier<Ipvc> ipvcId,
            VpnElans vpnElan) {
        if (newSubnet.getGateway() == null) {
            return;
        }

        Identifier45 nwUniId = newSubnet.getUniId();
        Identifier45 nwIpUniId = newSubnet.getIpUniId();
        String subnetStr = NetvirtVpnUtils.ipPrefixToString(newSubnet.getSubnet());

        IpUni ipUni = MefInterfaceUtils.getIpUni(dataBroker, nwUniId, nwIpUniId, LogicalDatastoreType.CONFIGURATION);
        if (ipUni == null) {
            Log.error("Uni {} IpUni {}  for network {} is not operational", nwUniId, nwIpUniId, subnetStr);
            return;
        }

        String srcTpAddressStr = NetvirtVpnUtils
                .getIpAddressFromPrefix(NetvirtVpnUtils.ipPrefixToString(ipUni.getIpAddress()));
        IpAddress srcIpAddress = new IpAddress(srcTpAddressStr.toCharArray());
        String subnet = NetvirtVpnUtils.ipPrefixToString(newSubnet.getSubnet());
        gwMacListener.resolveGwMac(ipvcVpn.getVpnId(), vpnElan.getElanPort(), srcIpAddress, newSubnet.getGateway(),
                subnet);
    }

    private void removeNetwork(DataTreeModification<Subnet> deletedDataObject) {
        Subnet deletedSubnet = deletedDataObject.getRootNode().getDataBefore();
        Identifier45 dlUniId = deletedSubnet.getUniId();
        Identifier45 dlIpUniId = deletedSubnet.getIpUniId();
        InstanceIdentifier<Ipvc> ipvcId = findServiceOper(dlUniId, dlIpUniId, getMefServicesOper());
        if (ipvcId == null) {
            Log.info("Subnet Uni {} IpUNI {} for deleted network is not assosiated to service", dlUniId, dlIpUniId);
            return;
        }
        removeNetwork(deletedSubnet, dlUniId, dlIpUniId, ipvcId);
    }

    private void removeNetwork(Subnet dlSubnet, Identifier45 dlUniId, Identifier45 dlIpUniId,
            InstanceIdentifier<Ipvc> ipvcId) {
        String subnetStr = NetvirtVpnUtils.ipPrefixToString(dlSubnet.getSubnet());
        synchronized (subnetStr.intern()) {

            IpvcVpn ipvcVpn = MefServicesUtils.getOperIpvcVpn(dataBroker, ipvcId);
            if (ipvcVpn == null || ipvcVpn.getVpnElans() == null) {
                Log.error("Subnet Uni {} IpUNI {} is not operational", dlUniId, dlIpUniId);
                return;
            }
            VpnElans vpnElan = MefServicesUtils.findVpnForNetwork(dlSubnet, ipvcVpn);
            if (vpnElan == null) {
                Log.error("Trying to remove non-operational network {}", subnetStr);
                return;
            }
            if (MefServicesUtils.findNetwork(dlSubnet, vpnElan) == null) {
                Log.error("Trying to remove non-operational network {}", subnetStr);
                return;
            }

            if (dlSubnet.getGateway() == null) {
                Log.trace("Direct subnet removed {}", dlSubnet.getSubnet());
            } else {
                removeNonDirectNetwork(dlSubnet, ipvcVpn, ipvcId);
            }

            MefServicesUtils.removeOperIpvcSubnet(dataBroker, ipvcId, ipvcVpn.getVpnId(), dlUniId, dlIpUniId,
                    vpnElan.getElanId(), vpnElan.getElanPort(), subnetStr);
        }
    }

    private void removeNonDirectNetwork(Subnet deletedSubnet, IpvcVpn ipvcVpn, InstanceIdentifier<Ipvc> ipvcId) {
        if (deletedSubnet.getGateway() == null) {
            return;
        }

        Identifier45 nwUniId = deletedSubnet.getUniId();
        Identifier45 nwIpUniId = deletedSubnet.getIpUniId();
        String subnetStr = NetvirtVpnUtils.ipPrefixToString(deletedSubnet.getSubnet());
        VpnElans vpnElan = MefServicesUtils.findVpnForNetwork(deletedSubnet, ipvcVpn);
        if (vpnElan == null) {
            Log.error("Network {} has not been created as required, nothing to remove", subnetStr);
            return;
        }

        IpUni ipUni = MefInterfaceUtils.getIpUni(dataBroker, nwUniId, nwIpUniId, LogicalDatastoreType.CONFIGURATION);
        if (ipUni == null) {
            Log.error("Uni {} IpUni {}  for network {} is not operational", nwUniId, nwIpUniId, subnetStr);
            return;
        }

        String srcTpAddressStr = NetvirtVpnUtils
                .getIpAddressFromPrefix(NetvirtVpnUtils.ipPrefixToString(ipUni.getIpAddress()));
        IpAddress srcIpAddress = new IpAddress(srcTpAddressStr.toCharArray());
        String subnet = NetvirtVpnUtils.ipPrefixToString(deletedSubnet.getSubnet());
        gwMacListener.unResolveGwMac(ipvcVpn.getVpnId(), vpnElan.getElanPort(), srcIpAddress,
                deletedSubnet.getGateway(), subnet);

        NetvirtVpnUtils.removeVpnInterfaceAdjacency(dataBroker, vpnElan.getElanPort(), deletedSubnet.getSubnet());
        NetvirtVpnUtils.removeVpnInterfaceAdjacency(dataBroker, vpnElan.getElanPort(), deletedSubnet.getGateway());
    }

    Optional<MefServices> getMefServices() {
        InstanceIdentifier<MefServices> path = MefServicesUtils.getMefServicesInstanceIdentifier();
        return MdsalUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION, path);
    }

    Optional<MefServices> getMefServicesOper() {
        InstanceIdentifier<MefServices> path = MefServicesUtils.getMefServicesInstanceIdentifier();
        return MdsalUtils.read(dataBroker, LogicalDatastoreType.OPERATIONAL, path);
    }

    private InstanceIdentifier<Ipvc> findService(Identifier45 uniId, Identifier45 ipUniId,
            Optional<MefServices> mefServices) {
        if (!mefServices.isPresent() || mefServices.get() == null) {
            Log.info("Uni {} IpUni {} is not assosiated with service", uniId, ipUniId);
            return null;
        }
        for (MefService service : mefServices.get().getMefService()) {
            if (service.getMefServiceChoice() instanceof IpvcChoice) {
                Ipvc ipvc = ((IpvcChoice) service.getMefServiceChoice()).getIpvc();
                if (ipvc.getUnis() == null || ipvc.getUnis().getUni() == null) {
                    continue;
                }
                List<Uni> unis = ipvc.getUnis().getUni();
                for (Uni uni : unis) {
                    if (uni.getUniId().equals(uniId) && uni.getIpUniId().equals(ipUniId)) {
                        Log.info("Find service {} for uni {} ipuni {}", service.getSvcId(), uniId, ipUniId);
                        return MefServicesUtils.getIpvcInstanceIdentifier(service.getSvcId());
                    }
                }
            }
        }
        Log.info("Uni {} IpUni {} is not assosiated with service", uniId, ipUniId);
        return null;
    }

    private InstanceIdentifier<Ipvc> findServiceOper(Identifier45 uniId, Identifier45 ipUniId,
            Optional<MefServices> mefServices) {
        if (!mefServices.isPresent() || mefServices.get() == null) {
            Log.info("Uni {} IpUni {} is not assosiated with service", uniId, ipUniId);
            return null;
        }
        for (MefService service : mefServices.get().getMefService()) {
            if (service.getMefServiceChoice() instanceof IpvcChoice) {
                Ipvc ipvc = ((IpvcChoice) service.getMefServiceChoice()).getIpvc();
                IpvcVpn ipvcVpn = ipvc.getAugmentation(IpvcVpn.class);
                if (ipvcVpn == null || ipvcVpn.getVpnElans() == null) {
                    Log.info("Uni {} IpUni {} is not assosiated with service", uniId, ipUniId);
                    return null;
                }
                List<VpnElans> vpnElans = ipvcVpn.getVpnElans();
                for (VpnElans vpnElan : vpnElans) {
                    if (vpnElan.getUniId().equals(uniId) && vpnElan.getIpUniId().equals(ipUniId)) {
                        Log.info("Find service {} for uni {} ipuni {}", service.getSvcId(), uniId, ipUniId);
                        return MefServicesUtils.getIpvcInstanceIdentifier(service.getSvcId());
                    }
                }
            }
        }
        Log.info("Uni {} IpUni {} is not assosiated with service", uniId, ipUniId);
        return null;
    }
}
