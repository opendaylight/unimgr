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

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.subnets.Subnet;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.subnets.SubnetBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.ip.unis.IpUni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.IpvcVpn;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.Ipvc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.ipvc.VpnElans;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.ipvc.unis.Uni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.Identifier45;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.etree.rev160614.EtreeInterface.EtreeInterfaceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.vpn.instance.op.data.VpnInstanceOpDataEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.vpn.instance.to.vpn.id.VpnInstance;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpvcListener extends UnimgrDataTreeChangeListener<Ipvc> {
    private static final Logger Log = LoggerFactory.getLogger(IpvcListener.class);
    private final IUniPortManager uniPortManager;
    private final ISubnetManager subnetManager;
    private final UniQosManager uniQosManager;
    private ListenerRegistration<IpvcListener> ipvcListenerRegistration;

    public IpvcListener(final DataBroker dataBroker, final IUniPortManager uniPortManager,
            final ISubnetManager subnetManager, final UniQosManager uniQosManager) {
        super(dataBroker);
        this.uniPortManager = uniPortManager;
        this.subnetManager = subnetManager;
        this.uniQosManager = uniQosManager;
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
            Ipvc ipvc = newDataObject.getRootNode().getDataAfter();
            String instanceName = ipvc.getIpvcId().getValue();
            final String vpnName = NetvirtVpnUtils.getUUidFromString(instanceName);
            InstanceIdentifier<Ipvc> ipvcId = newDataObject.getRootPath().getRootIdentifier();
            List<Uni> unis = new ArrayList<>();
            String rd = null;
            synchronized (vpnName.intern()) {
                WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);
                Log.info("Adding vpn instance: " + instanceName);
                NetvirtVpnUtils.createVpnInstance(vpnName, tx);
                MefServicesUtils.addOperIpvcVpnElan(ipvcId, vpnName, tx);
                MdsalUtils.commitTransaction(tx);

                InstanceIdentifier<VpnInstance> vpnId = NetvirtVpnUtils.getVpnInstanceToVpnIdIdentifier(vpnName);
                DataWaitListener<VpnInstance> vpnInstanceWaiter = new DataWaitListener<>(dataBroker, vpnId, 10,
                        LogicalDatastoreType.CONFIGURATION, vpn -> vpn.getVrfId());
                if (!vpnInstanceWaiter.waitForData()) {
                    String errorMessage = String.format("Fail to wait for vrfId for vpn %s", vpnName);
                    Log.error(errorMessage);
                    throw new UnsupportedOperationException(errorMessage);
                }
                rd = (String) vpnInstanceWaiter.getData();
            }

            if (ipvc.getUnis() != null && ipvc.getUnis() != null) {
                unis = ipvc.getUnis().getUni();
            }
            Log.info("Number of UNI's: " + unis.size());

            // Create elan/vpn interfaces
            for (Uni uni : unis) {
                createInterfaces(vpnName, uni, ipvcId, rd);
            }

            createUnis(ipvcId, unis);
        } catch (final Exception e) {
            Log.error("Add ipvc failed !", e);
        }
    }

    private void removeIpvc(DataTreeModification<Ipvc> removedDataObject) {
        try {
            Ipvc ipvc = removedDataObject.getRootNode().getDataBefore();
            InstanceIdentifier<Ipvc> ipvcId = removedDataObject.getRootPath().getRootIdentifier();
            IpvcVpn operIpvcVpn = MefServicesUtils.getOperIpvcVpn(dataBroker, ipvcId);
            if (operIpvcVpn == null) {
                Log.error("Ipvc {} hasn't been created as required", ipvc.getIpvcId());
                return;
            }
            String vpnName = operIpvcVpn.getVpnId();

            synchronized (vpnName.intern()) {
                // remove elan/vpn interfaces
                // must be in different transactios
                WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);
                removeUnis(ipvcId, operIpvcVpn, ipvc.getUnis().getUni(), tx);
                MdsalUtils.commitTransaction(tx);
                // Let to work for listeners
                // TODO : change to listener
                NetvirtUtils.safeSleep();

                WriteTransaction txvpn = MdsalUtils.createTransaction(dataBroker);
                NetvirtVpnUtils.removeVpnInstance(operIpvcVpn.getVpnId(), txvpn);
                MefServicesUtils.removeOperIpvcVpn(ipvcId, txvpn);
                MdsalUtils.commitTransaction(txvpn);
            }
        } catch (final Exception e) {
            Log.error("Remove ipvc failed !", e);
        }
    }

    private void removeUnis(InstanceIdentifier<Ipvc> ipvcId, IpvcVpn operIpvcVpn, List<Uni> uniToRemove,
            WriteTransaction tx) {
        if (uniToRemove == null) {
            Log.trace("No UNI's to remove");
        }
        for (Uni uni : uniToRemove) {
            Identifier45 uniId = uni.getUniId();
            Identifier45 ipUniId = uni.getIpUniId();
            IpUni ipUni = MefInterfaceUtils.getIpUni(dataBroker, uniId, ipUniId, LogicalDatastoreType.CONFIGURATION);
            if (ipUni == null) {
                String errorMessage = String.format("Couldn't find ipuni %s for uni %s", ipUniId, uniId);
                Log.error(errorMessage);
                throw new UnsupportedOperationException(errorMessage);
            }

            removeDirectSubnet(uni, ipUni);
            subnetManager.unAssignIpUniNetworks(uni.getUniId(), ipUni.getIpUniId(), ipvcId);
            removeInterfaces(ipvcId, operIpvcVpn, uni, ipUni, tx);
        }
        updateQos(uniToRemove);
    }

    private void createUnis(InstanceIdentifier<Ipvc> ipvcId, List<Uni> uniToCreate) {
        for (Uni uni : uniToCreate) {
            IpUni ipUni = MefInterfaceUtils.getIpUni(dataBroker, uni.getUniId(), uni.getIpUniId(),
                    LogicalDatastoreType.CONFIGURATION);
            createDirectSubnet(uni, ipUni);
            subnetManager.assignIpUniNetworks(uni.getUniId(), ipUni.getIpUniId(), ipvcId);
        }
        updateQos(uniToCreate);
    }

    private void updateIpvc(DataTreeModification<Ipvc> modifiedDataObject) {
        try {
            Ipvc origIpvc = modifiedDataObject.getRootNode().getDataBefore();
            Ipvc updateIpvc = modifiedDataObject.getRootNode().getDataAfter();
            InstanceIdentifier<Ipvc> ipvcId = modifiedDataObject.getRootPath().getRootIdentifier();
            IpvcVpn operIpvcVpn = MefServicesUtils.getOperIpvcVpn(dataBroker, ipvcId);
            if (operIpvcVpn == null) {
                Log.error("Ipvc {} hasn't been created as required", origIpvc.getIpvcId());
                return;
            }
            String vpnName = operIpvcVpn.getVpnId();
            InstanceIdentifier<VpnInstance> vpnId = NetvirtVpnUtils.getVpnInstanceToVpnIdIdentifier(vpnName);
            @SuppressWarnings("resource")
            DataWaitListener<VpnInstance> vpnInstanceWaiter = new DataWaitListener<>(dataBroker, vpnId, 10,
                    LogicalDatastoreType.CONFIGURATION, vpn -> vpn.getVrfId());
            if (!vpnInstanceWaiter.waitForData()) {
                String errorMessage = String.format("Fail to wait for vrfId for vpn %s", vpnName);
                Log.error(errorMessage);
                throw new UnsupportedOperationException(errorMessage);
            }
            String rd = (String) vpnInstanceWaiter.getData();

            List<Uni> originalUni = origIpvc.getUnis() != null && origIpvc.getUnis().getUni() != null
                    ? origIpvc.getUnis().getUni() : Collections.emptyList();
            List<Uni> updateUni = updateIpvc.getUnis() != null && updateIpvc.getUnis().getUni() != null
                    ? updateIpvc.getUnis().getUni() : Collections.emptyList();

            synchronized (vpnName.intern()) {
                WriteTransaction txRemove = MdsalUtils.createTransaction(dataBroker);
                List<Uni> uniToRemove = new ArrayList<>(originalUni);
                uniToRemove.removeAll(updateUni);
                removeUnis(ipvcId, operIpvcVpn, uniToRemove, txRemove);
                MdsalUtils.commitTransaction(txRemove);
            }
            List<Uni> uniToCreate = new ArrayList<>(updateUni);
            uniToCreate.removeAll(originalUni);

            for (Uni uni : uniToCreate) {
                createInterfaces(vpnName, uni, ipvcId, rd);
            }
            createUnis(ipvcId, uniToCreate);

        } catch (final Exception e) {
            Log.error("Update ipvc failed !", e);
        }
    }

    private void createInterfaces(String vpnName, Uni uniInService, InstanceIdentifier<Ipvc> ipvcId, String rd) {

        WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);
        String uniId = uniInService.getUniId().getValue();
        String ipUniId = uniInService.getIpUniId().getValue();
        org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni uni = MefInterfaceUtils
                .getUni(dataBroker, uniId, LogicalDatastoreType.OPERATIONAL);
        if (uni == null) {
            String errorMessage = String.format("Couldn't find uni %s for ipvc", uniId);
            Log.error(errorMessage);
            throw new UnsupportedOperationException(errorMessage);
        }
        IpUni ipUni = MefInterfaceUtils.getIpUni(dataBroker, uniInService.getUniId(), uniInService.getIpUniId(),
                LogicalDatastoreType.CONFIGURATION);
        if (ipUni == null) {
            String errorMessage = String.format("Couldn't find ipuni %s for uni %s", ipUniId, uniId);
            Log.error(errorMessage);
            throw new UnsupportedOperationException(errorMessage);
        }

        String interfaceName = null;
        synchronized (vpnName.intern()) {
            Long vlan = ipUni.getVlan() != null ? Long.valueOf(ipUni.getVlan().getValue()) : null;
            String elanName = NetvirtVpnUtils.getElanNameForVpnPort(uniId, ipUniId);

            String srcIpAddressStr = NetvirtVpnUtils
                    .getIpAddressFromPrefix(NetvirtVpnUtils.ipPrefixToString(ipUni.getIpAddress()));
            IpAddress ipAddress = new IpAddress(srcIpAddressStr.toCharArray());

            interfaceName = createElanInterface(vpnName, ipvcId, uniId, elanName, vlan, ipAddress, tx,
                    ipUni.getSegmentationId());
            uniQosManager.mapUniPortBandwidthLimits(uniId, interfaceName, uniInService.getIngressBwProfile());
            createVpnInterface(vpnName, uni, ipUni, interfaceName, elanName, tx);
            MefServicesUtils.addOperIpvcVpnElan(ipvcId, vpnName, uniInService.getUniId(), uniInService.getIpUniId(),
                    elanName, interfaceName, null, tx);
            MdsalUtils.commitTransaction(tx);
        }

        InstanceIdentifier<VpnInstanceOpDataEntry> vpnId = NetvirtVpnUtils.getVpnInstanceOpDataIdentifier(rd);
        @SuppressWarnings("resource")
        DataWaitListener<VpnInstanceOpDataEntry> vpnInstanceWaiter = new DataWaitListener<>(dataBroker, vpnId, 10,
                LogicalDatastoreType.OPERATIONAL, vpn -> vpn.getVpnToDpnList());
        if (!vpnInstanceWaiter.waitForData()) {
            String errorMessage = String.format("Fail to wait for vpn to dpn list %s", vpnName);
            Log.error(errorMessage);
            throw new UnsupportedOperationException(errorMessage);
        }

        NetvirtVpnUtils.createVpnPortFixedIp(dataBroker, vpnName, interfaceName, ipUni.getIpAddress(),
                uni.getMacAddress());
    }

    private String createElanInterface(String vpnName, InstanceIdentifier<Ipvc> ipvcId, String uniId, String elanName,
            Long vlan, IpAddress ipAddress, WriteTransaction tx, Long segmentationId) {
        Log.info("Adding elan instance: " + elanName);
        NetvirtUtils.updateElanInstance(elanName, tx, segmentationId);
        NetvirtVpnUtils.registerDirectSubnetForVpn(dataBroker, new Uuid(elanName), ipAddress);

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
            IpUni ipUni, String interfaceName, String elanName, WriteTransaction tx) {

        Log.info("Adding vpn interface: " + interfaceName);

        NetvirtVpnUtils.createUpdateVpnInterface(vpnName, interfaceName, ipUni.getIpAddress(),
                uni.getMacAddress().getValue(), true, null, elanName, tx);

        Log.info("Finished working on vpn instance {} interface () ", vpnName, interfaceName);
    }

    private void createDirectSubnet(Uni uni, IpUni ipUni) {
        IpPrefix uniIpPrefix = ipUni.getIpAddress();
        String subnetIp = NetvirtVpnUtils.getSubnetFromPrefix(uniIpPrefix);
        IpPrefix subnetPrefix = new IpPrefix(new Ipv4Prefix(subnetIp));
        InstanceIdentifier<Subnet> path = MefInterfaceUtils.getSubnetInstanceIdentifier(uni.getUniId(),
                ipUni.getIpUniId(), subnetPrefix);
        SubnetBuilder subnet = new SubnetBuilder();
        subnet.setUniId(uni.getUniId());
        subnet.setIpUniId(ipUni.getIpUniId());
        subnet.setSubnet(subnetPrefix);
        MdsalUtils.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION, path, subnet.build());
    }

    private void removeInterfaces(InstanceIdentifier<Ipvc> ipvcId, IpvcVpn ipvcVpn, Uni uniInService, IpUni ipUni,
            WriteTransaction tx) {
        String uniId = uniInService.getUniId().getValue();
        org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni uni = MefInterfaceUtils
                .getUni(dataBroker, uniId, LogicalDatastoreType.OPERATIONAL);
        if (uni == null) {
            String errorMessage = String.format("Couldn't find uni %s for ipvc", uniId);
            Log.error(errorMessage);
            throw new UnsupportedOperationException(errorMessage);
        }

        String vpnName = ipvcVpn.getVpnId();
        VpnElans vpnElans = MefServicesUtils.findVpnElanForNetwork(new Identifier45(uniId), ipUni.getIpUniId(),
                ipvcVpn);
        if (vpnElans == null) {
            Log.error("Trying to remome non-operational vpn/elan for Uni {} Ip-UNi {}", uniId, ipUni.getIpUniId());
        }

        NetvirtVpnUtils.removeVpnInterfaceAdjacencies(dataBroker, vpnName, vpnElans.getElanPort());
        // TODO : change to listener
        NetvirtUtils.safeSleep();
        uniQosManager.unMapUniPortBandwidthLimits(uniId, vpnElans.getElanPort());
        removeElan(vpnElans, uniId, ipUni, tx);
        // record Uni bw limits
        removeVpnInterface(vpnName, vpnElans, uniId, ipUni, tx);
        MefServicesUtils.removeOperIpvcElan(dataBroker, ipvcId, ipvcVpn.getVpnId(), uniInService.getUniId(),
                uniInService.getIpUniId(), vpnElans.getElanId(), vpnElans.getElanPort());
    }

    private void removeElan(VpnElans vpnElans, String uniId, IpUni ipUni, WriteTransaction tx) {
        Long vlan = ipUni.getVlan() != null ? Long.valueOf(ipUni.getVlan().getValue()) : 0;
        Log.info("Removing trunk interface for uni {} vlan: {}", uniId, vlan);
        uniPortManager.removeCeVlan(uniId, vlan);

        String elanName = vpnElans.getElanId();
        String interfaceName = vpnElans.getElanPort();

        Log.info("Removing elan instance {} and interface {}: ", elanName, interfaceName);
        NetvirtVpnUtils.unregisterDirectSubnetForVpn(dataBroker, new Uuid(elanName));
        NetvirtUtils.deleteElanInterface(interfaceName, tx);
        NetvirtUtils.deleteElanInstance(elanName, tx);
    }

    private void removeVpnInterface(String vpnName, VpnElans vpnElans, String uniId, IpUni ipUni, WriteTransaction tx) {
        String interfaceName = vpnElans.getElanPort();
        Log.info("Removing vpn interface: " + interfaceName);
        NetvirtVpnUtils.removeVpnInterface(interfaceName, tx);
        NetvirtVpnUtils.removeVpnPortFixedIp(vpnName, ipUni.getIpAddress(), tx);
        Log.info("Finished working on vpn instance: " + vpnName);
    }

    private void removeDirectSubnet(Uni uni, IpUni ipUni) {
        IpPrefix uniIpPrefix = ipUni.getIpAddress();
        String subnetIp = NetvirtVpnUtils.getSubnetFromPrefix(uniIpPrefix);
        IpPrefix subnetPrefix = new IpPrefix(new Ipv4Prefix(subnetIp));
        InstanceIdentifier<Subnet> path = MefInterfaceUtils.getSubnetInstanceIdentifier(uni.getUniId(),
                ipUni.getIpUniId(), subnetPrefix);
        MdsalUtils.delete(dataBroker, LogicalDatastoreType.CONFIGURATION, path);
    }

    private void updateQos(List<Uni> uniToUpdate) {
        uniToUpdate.forEach(u -> uniQosManager.setUniBandwidthLimits(u.getUniId()));
    }
}
