/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.subnets.ip.uni.Subnet;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.subnets.ip.uni.SubnetBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.ip.unis.IpUni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.IpvcVpn;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.IpvcVpnBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.Ipvc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.ipvc.VpnElans;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.ipvc.VpnElansBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.ipvc.vpn.elans.ElanPorts;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.ipvc.vpn.elans.ElanPortsBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.ipvc.unis.Uni;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.OdlArputilService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.etree.rev160614.EtreeInterface.EtreeInterfaceType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class IpvcListener extends UnimgrDataTreeChangeListener<Ipvc> {
    private static final Logger Log = LoggerFactory.getLogger(IpvcListener.class);
    private final IUniPortManager uniPortManager;
    private ListenerRegistration<IpvcListener> ipvcListenerRegistration;
    private final NotificationPublishService notificationPublishService;
    private final OdlArputilService arpUtilService;

    public IpvcListener(final DataBroker dataBroker, final UniPortManager uniPortManager,
            final NotificationPublishService notPublishService, final OdlArputilService arputilService) {
        super(dataBroker);
        this.uniPortManager = uniPortManager;
        this.notificationPublishService = notPublishService;
        this.arpUtilService = arputilService;
        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<Ipvc> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                    MefServicesUtils.getIpvcsInstanceIdentifier());
            ipvcListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
            Log.info("IpvcDataTreeChangeListener created and registered");
        } catch (final Exception e) {
            Log.error("Ipvc DataChange listener registration failed !", e);
            throw new IllegalStateException("Ipvc registration Listener failed.", e);
        }
    }

    @Override
    public void close() throws Exception {
        ipvcListenerRegistration.close();
    }

    @Override
    public void add(DataTreeModification<Ipvc> newDataObject) {
        if (newDataObject.getRootPath() != null && newDataObject.getRootNode() != null) {
            Log.info("ipvc {} created", newDataObject.getRootNode().getIdentifier());
            addIpvc(newDataObject);
        }
    }

    @Override
    public void remove(DataTreeModification<Ipvc> removedDataObject) {
        if (removedDataObject.getRootPath() != null && removedDataObject.getRootNode() != null) {
            Log.info("ipvc {} deleted", removedDataObject.getRootNode().getIdentifier());
            removeIpvc(removedDataObject);
        }
    }

    @Override
    public void update(DataTreeModification<Ipvc> modifiedDataObject) {
        if (modifiedDataObject.getRootPath() != null && modifiedDataObject.getRootNode() != null) {
            Log.info("ipvc {} updated", modifiedDataObject.getRootNode().getIdentifier());
            updateIpvc(modifiedDataObject);
        }
    }

    private void addIpvc(DataTreeModification<Ipvc> newDataObject) {
        try {
            WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);
            Ipvc data = newDataObject.getRootNode().getDataAfter();
            String instanceName = data.getIpvcId().getValue();
            final String vpnName = NetvirtVpnUtils.getUUidFromString(instanceName);
            InstanceIdentifier<Ipvc> ipvcId = newDataObject.getRootPath().getRootIdentifier();
            synchronized (instanceName.intern()) {
                Log.info("Adding vpn instance: " + instanceName);
                NetvirtVpnUtils.createVpnInstance(vpnName, tx);
                Log.info("Number of UNI's: " + data.getUnis().getUni().size());

                // Create elan interfaces
                for (Uni uni : data.getUnis().getUni()) {
                    createInterfaces(vpnName, uni, ipvcId, tx);
                }
            }
            MdsalUtils.commitTransaction(tx);
        } catch (final Exception e) {
            Log.error("Add ipvc failed !", e);
        }
    }

    private void updateIpvc(DataTreeModification<Ipvc> modifiedDataObject) {
        try {
            Ipvc original = modifiedDataObject.getRootNode().getDataBefore();
            Ipvc update = modifiedDataObject.getRootNode().getDataAfter();

            String instanceName = original.getIpvcId().getValue();

            Log.info("Updating elan instance: " + instanceName);

            List<Uni> originalUni = original.getUnis().getUni();
            List<Uni> updateUni = update.getUnis().getUni();
            if (updateUni != null && !updateUni.isEmpty()) {
                List<Uni> existingClonedUni = new ArrayList<>();
                if (originalUni != null && !originalUni.isEmpty()) {
                    existingClonedUni.addAll(0, originalUni);
                    originalUni.removeAll(updateUni);
                    updateUni.removeAll(existingClonedUni);
                    // removing the Uni which are not presented in the updated
                    // List
                    for (Uni uni : originalUni) {
                        removeElanInterface(instanceName, uni);
                    }
                }

                // Adding the new Uni which are presented in the updated List
                if (updateUni.size() > 0) {
                    for (Uni uni : updateUni) {
                        // createInterfaces(original, uni);
                    }
                }
            } else if (originalUni != null && !originalUni.isEmpty()) {
                for (Uni uni : originalUni) {
                    removeElanInterface(instanceName, uni);
                }
            }
        } catch (final Exception e) {
            Log.error("Update ipvc failed !", e);
        }
    }

    private void removeIpvc(DataTreeModification<Ipvc> removedDataObject) {
        try {
            Ipvc data = removedDataObject.getRootNode().getDataBefore();
            String instanceName = data.getIpvcId().getValue();

            for (Uni uni : data.getUnis().getUni()) {
                removeElanInterface(instanceName, uni);
            }

            Log.info("Removing elan instance: " + instanceName);
            NetvirtUtils.deleteElanInstance(dataBroker, instanceName);
        } catch (final Exception e) {
            Log.error("Remove ipvc failed !", e);
        }
    }

    private void createInterfaces(String vpnName, Uni uniInService, InstanceIdentifier<Ipvc> ipvcId,
            WriteTransaction tx) {
        String uniId = uniInService.getUniId().getValue();
        String ipUniId = uniInService.getIpUniId().getValue();
        org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni uni = MefInterfaceUtils
                .getUni(dataBroker, uniId, LogicalDatastoreType.OPERATIONAL);
        if (uni == null) {
            String errorMessage = String.format("Couldn't find uni %s for ipvc", uniId);
            Log.error(errorMessage);
            throw new UnsupportedOperationException(errorMessage);
        }
        IpUni ipUni = MefInterfaceUtils.getIpUni(dataBroker, uniId, ipUniId, LogicalDatastoreType.CONFIGURATION);
        if (ipUni == null) {
            String errorMessage = String.format("Couldn't find ipuni %s for uni %s", ipUniId, uniId);
            Log.error(errorMessage);
            throw new UnsupportedOperationException(errorMessage);
        }

        Long vlan = (ipUni.getVlan()) != null ? Long.valueOf(ipUni.getVlan().getValue()) : null;
        String elanName = NetvirtVpnUtils.getElanNameForVpnPort(uniId);
        String interfaceName = createElanInterface(vpnName, ipvcId, uniId, elanName, vlan, tx);
        createVpnInterface(vpnName, uni, ipUni, interfaceName, tx);
        setOperIpvcVpnElan(ipvcId, vpnName, elanName, Collections.singletonList(interfaceName), tx);

        createDirectSubnet(uni, ipUni);

        // Log.info("Adding connected network for interface : " +
        // interfaceName);
        // NetvirtVpnUtils.addDirectSubnetToVpn(dataBroker,
        // notificationPublishService, vpnName, elanName,
        // ipUni.getIpAddress(), interfaceName);
        /*
         * if (ipUni.getSubnets() != null) { for (Subnet subnet :
         * ipUni.getSubnets().getSubnet()) { MacAddress gwMacAddress =
         * NetvirtVpnUtils.resolveGwMac(dataBroker, arpUtilService, vpnName,
         * ipUni.getIpAddress(), subnet.getGateway(), interfaceName); // trunk
         * if (gwMacAddress == null) { continue; }
         * NetvirtVpnUtils.createUpdateVpnInterface(dataBroker, vpnName,
         * interfaceName, subnet.getSubnet(), gwMacAddress, false,
         * ipUni.getIpAddress()); } }
         */
    }

    private void createDirectSubnet(
            org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni uni,
            IpUni ipUni) {
        IpPrefix uniIpPrefix = ipUni.getIpAddress();
        String subnetIp = NetvirtVpnUtils.getSubnetFromPrefix(uniIpPrefix);
        IpPrefix subnetPrefix = new IpPrefix(new Ipv4Prefix(subnetIp));
        InstanceIdentifier<Subnet> path = MefInterfaceUtils.getSubnetInstanceIdentifier(uni.getUniId(),
                ipUni.getIpUniId(), subnetPrefix);
        SubnetBuilder subnet = new SubnetBuilder();
        subnet.setSubnet(subnetPrefix);
        MdsalUtils.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION, path, subnet.build());
    }

    private String createElanInterface(String vpnName, InstanceIdentifier<Ipvc> ipvcId, String uniId, String elanName,
            Long vlan, WriteTransaction tx) {
        Log.info("Adding elan instance: " + elanName);
        NetvirtUtils.updateElanInstance(elanName, tx);

        // TODO - UUID ?!
        Log.info("Added trunk interface for uni {} vlan: {}", uniId, vlan);

        if (vlan != null) {
            uniPortManager.addCeVlan(uniId, vlan);
        }
        String interfaceName = uniPortManager.getUniVlanInterface(uniId, vlan);
        if (interfaceName == null) {
            String errorMessage = String.format("Couldn't create  uni %s vlan interface %s", uniId, vlan);
            Log.error(errorMessage);
            throw new UnsupportedOperationException(errorMessage);
        }

        Log.info("Adding elan interface: " + interfaceName);
        NetvirtUtils.createElanInterface(elanName, interfaceName, EtreeInterfaceType.Root, false, tx);
        return interfaceName;
    }

    private void createVpnInterface(String vpnName,
            org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni uni,
            IpUni ipUni, String interfaceName, WriteTransaction tx) {

        Log.info("Adding vpn interface: " + interfaceName);

        NetvirtVpnUtils.createUpdateVpnInterface(vpnName, interfaceName, ipUni.getIpAddress(), uni.getMacAddress(),
                true, null, tx);
        NetvirtVpnUtils.createVpnPortFixedIp(vpnName, interfaceName, ipUni.getIpAddress(), uni.getMacAddress(), tx);
        Log.info("Finished working on vpn instance: " + vpnName);
    }

    private void removeElanInterface(String instanceName, Uni uni) {
        String uniId = uni.getIpUniId().getValue();
        String interfaceName = uniId;
        Log.info("Removing elan interface: " + uniId);
        NetvirtUtils.deleteElanInterface(dataBroker, instanceName, interfaceName);
    }

    private IpvcVpn getOperIpvcVpn(InstanceIdentifier<Ipvc> identifier) {
        InstanceIdentifier<IpvcVpn> path = identifier.augmentation(IpvcVpn.class);
        Optional<IpvcVpn> ipvcVpn = MdsalUtils.read(dataBroker, LogicalDatastoreType.OPERATIONAL, path);
        if (ipvcVpn.isPresent()) {
            return ipvcVpn.get();
        } else {
            return null;
        }
    }

    private void setOperIpvcVpnElan(InstanceIdentifier<Ipvc> identifier, String vpnId, String elanId,
            List<String> elanPortsStr, WriteTransaction tx) {
        IpvcVpn ipvcVpnCur = getOperIpvcVpn(identifier);
        InstanceIdentifier<IpvcVpn> path = identifier.augmentation(IpvcVpn.class);

        IpvcVpnBuilder ipvcVpnBuilder = null;
        if (ipvcVpnCur != null) {
            ipvcVpnBuilder = new IpvcVpnBuilder(ipvcVpnCur);
        } else {
            ipvcVpnBuilder = new IpvcVpnBuilder();

        }
        ipvcVpnBuilder.setVpnId(vpnId);

        List<VpnElans> vpnElansEx = (ipvcVpnBuilder.getVpnElans() != null) ? ipvcVpnBuilder.getVpnElans()
                : new ArrayList<>();
        VpnElansBuilder vpnElans = new VpnElansBuilder();
        List<ElanPorts> elanPorts = elanPortsStr.stream().map(port -> {
            ElanPortsBuilder elanPortB = new ElanPortsBuilder();
            return elanPortB.setPortId(port).build();
        }).collect(Collectors.toList());

        vpnElans.setElanId(elanId);
        vpnElans.setElanPorts(elanPorts);
        vpnElansEx.add(vpnElans.build());
        ipvcVpnBuilder.setVpnElans(vpnElansEx);

        tx.merge(LogicalDatastoreType.OPERATIONAL, path, ipvcVpnBuilder.build(), true);
    }
}
