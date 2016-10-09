/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.ip.unis.IpUni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.ip.unis.ip.uni.subnets.Subnet;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.Ipvc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.ipvc.unis.Uni;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.OdlArputilService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.etree.rev160614.EtreeInterface.EtreeInterfaceType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpvcListener extends UnimgrDataTreeChangeListener<Ipvc> {
    private static final Logger Log = LoggerFactory.getLogger(IpvcListener.class);
    private ListenerRegistration<IpvcListener> ipvcListenerRegistration;
    private final NotificationPublishService notificationPublishService;
    private final OdlArputilService arpUtilService;


    public IpvcListener(final DataBroker dataBroker, final NotificationPublishService notPublishService, final OdlArputilService arputilService) {
        super(dataBroker);
        this.notificationPublishService = notPublishService;
        this.arpUtilService = arputilService;
        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<Ipvc> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                    MefUtils.getIpvcInstanceIdentifier());
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
            Ipvc data = newDataObject.getRootNode().getDataAfter();
            String instanceName = data.getIpvcId().getValue();
            final String vpnName =  NetvirtVpnUtils.getUUidFromString(instanceName);

            Log.info("Adding vpn instance: " + instanceName);
            NetvirtVpnUtils.createVpnInstance(dataBroker, vpnName);

            // Create elan interfaces
            for (Uni uni : data.getUnis().getUni()) {
                createInterfaces(vpnName, uni);
            }       
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
                        createInterfaces(original, uni);
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

    private void createInterfaces(Ipvc ipvc, Uni uni) {
        String instanceName = ipvc.getIpvcId().getValue();
        createInterfaces(instanceName, uni);
    }

    private void createInterfaces(String vpnName, Uni uniInService) {
        String uniId = uniInService.getUniId().getValue();
        String ipUniId = uniInService.getIpUniId().getValue();


        org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni
        uni = IpvcUniUtils.getUni(dataBroker, uniId);
        if (uni == null) {
            Log.error("Couldn't find uni {} for ipvc-uni", uniId);
            throw new UnsupportedOperationException();
        }
        IpUni ipUni =  IpvcUniUtils.getIpUni(dataBroker, uniId, ipUniId);        
        Integer vlan =  (ipUni.getVlan()) != null ? ipUni.getVlan().getValue():null;

        Log.info("Adding/updating elan instance: " + uniId);
        String elanName = NetvirtVpnUtils.getElanNameForVpnPort(uniId);
        Log.info("Adding elan instance: " + elanName);
        NetvirtUtils.updateElanInstance(dataBroker, elanName);

        String interfaceName = NetvirtVpnUtils.getInterfaceNameForVlan(ipUniId, vlan);
        Log.info("Adding trunk interface: " + interfaceName);
        IpvcUniUtils.addUni(dataBroker, uniInService, interfaceName, vlan);

        Log.info("Adding elan interface: " + interfaceName);
        NetvirtUtils.createInterface(dataBroker, elanName, interfaceName, EtreeInterfaceType.Root, false);    

        Log.info("Adding vpn interface: " + interfaceName);
        NetvirtVpnUtils.createUpdateVpnInterface(dataBroker, vpnName, interfaceName, ipUni.getIpAddress(), uni.getMacAddress(), true, null);
        NetvirtVpnUtils.createVpnPortFixedIp(dataBroker, vpnName, interfaceName, ipUni.getIpAddress(), uni.getMacAddress());

        Log.info("Adding connected network for interface : " + interfaceName);
        NetvirtVpnUtils.addDirectSubnetToVpn(dataBroker, notificationPublishService, vpnName, elanName, ipUni.getIpAddress(), interfaceName);

        if (ipUni.getSubnets() != null) {
            for ( Subnet subnet : ipUni.getSubnets().getSubnet() ) {
                MacAddress gwMacAddress = NetvirtVpnUtils.resolveGwMac(dataBroker, arpUtilService, vpnName, ipUni.getIpAddress(), subnet.getGateway(), interfaceName); // trunk
                if (gwMacAddress == null) {
                    continue;
                }
                NetvirtVpnUtils.createUpdateVpnInterface(dataBroker, vpnName, interfaceName, subnet.getSubnet(), gwMacAddress, false, ipUni.getIpAddress());
            }  
        }
    }

    private void removeElanInterface(String instanceName, Uni uni) {
        String uniId = uni.getIpUniId().getValue();
        String interfaceName = uniId;
        Log.info("Removing elan interface: " + uniId);
        NetvirtUtils.deleteElanInterface(dataBroker, instanceName, interfaceName);
    }
}
