/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.subnets.Subnet;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.subnets.SubnetBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.ip.unis.IpUni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.IpvcVpn;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.Ipvc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.ipvc.VpnElans;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.ipvc.unis.Uni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.ipvc.unis.UniKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.Identifier45;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.RetailSvcIdType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.BridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.OdlInterfaceRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.etree.rev160614.EtreeInterface.EtreeInterfaceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.vpn.instance.op.data.VpnInstanceOpDataEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.vpn.instance.op.data.vpn.instance.op.data.entry.VpnToDpnList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.vpn.instance.op.data.vpn.instance.op.data.entry.vpn.to.dpn.list.VpnInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.vpn.instance.to.vpn.id.VpnInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class IpvcListener extends UnimgrDataTreeChangeListener<Ipvc> implements IUniAwareService {
    private static final Logger Log = LoggerFactory.getLogger(IpvcListener.class);
    private final IUniPortManager uniPortManager;
    private final ISubnetManager subnetManager;
    private final UniQosManager uniQosManager;
    private ListenerRegistration<IpvcListener> ipvcListenerRegistration;
    @SuppressWarnings("unused")
    private final UniAwareListener uniAwareListener;
    private final OdlInterfaceRpcService odlInterfaceRpcService;
    private final SouthboundUtils southBoundUtils;
    private final org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils mdsalUtils;
    private final NotificationPublishService notificationPublishService;

    private static final String LOCAL_IP = "local_ip";

    // TODO: make it as service
    private final ConcurrentHashMap<String, BigInteger> portToDpn;

    public IpvcListener(final DataBroker dataBroker, final IUniPortManager uniPortManager,
            final ISubnetManager subnetManager, final UniQosManager uniQosManager,
            final OdlInterfaceRpcService odlInterfaceRpcService, final NotificationPublishService notPublishService) {
        super(dataBroker);
        this.uniPortManager = uniPortManager;
        this.subnetManager = subnetManager;
        this.uniQosManager = uniQosManager;
        this.uniAwareListener = new UniAwareListener(dataBroker, this);
        this.odlInterfaceRpcService = odlInterfaceRpcService;
        this.mdsalUtils = new org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils(dataBroker);
        this.southBoundUtils = new SouthboundUtils(mdsalUtils);
        this.portToDpn = new ConcurrentHashMap<>();
        this.notificationPublishService = notPublishService;

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

    @Override
    public void connectUni(String uniId) {
        List<RetailSvcIdType> allIpvcs = MefServicesUtils.getAllIpvcsServiceIds(dataBroker);
        allIpvcs = allIpvcs != null ? allIpvcs : Collections.emptyList();

        for (RetailSvcIdType ipvcSerId : allIpvcs) {
            InstanceIdentifier<Ipvc> ipvcId = MefServicesUtils.getIpvcInstanceIdentifier(ipvcSerId);
            Ipvc ipvc = MefServicesUtils.getIpvc(dataBroker, ipvcId);
            if (ipvc == null) {
                Log.error("Inconsistent data for svcId {}", ipvcSerId);
                continue;
            }
            List<Uni> toConnect = new ArrayList<>();

            List<Uni> unis = ipvc.getUnis() != null ? ipvc.getUnis().getUni() : null;
            unis = unis != null ? unis : Collections.emptyList();
            for (Uni uni : unis) {
                if (uni.getUniId().getValue().equals(uniId)) {
                    Log.info("Connecting Uni {} to svc id {}", uniId, ipvcSerId);
                    toConnect.add(uni);
                    break;
                }
            }

            IpvcVpn operIpvcVpn = MefServicesUtils.getOperIpvcVpn(dataBroker, ipvcId);
            if (operIpvcVpn == null) {
                String instanceName = ipvc.getIpvcId().getValue();
                final String vpnName = NetvirtVpnUtils.getUUidFromString(instanceName);
                createVpnInstance(vpnName, ipvcId);
                operIpvcVpn = MefServicesUtils.getOperIpvcVpn(dataBroker, ipvcId);
                if (operIpvcVpn == null) {
                    Log.error("Ipvc {} hasn't been created as required, Nothing to reconnect", ipvcSerId);
                    return;
                }
            }
            String vpnName = operIpvcVpn.getVpnId();
            String rd = waitForRd(vpnName);
            createUnis(vpnName, ipvcId, toConnect, rd);
        }
    }

    @Override
    public void disconnectUni(String uniId) {

        List<RetailSvcIdType> allIpvcs = MefServicesUtils.getAllIpvcsServiceIds(dataBroker);
        allIpvcs = allIpvcs != null ? allIpvcs : Collections.emptyList();

        for (RetailSvcIdType ipvcSerId : allIpvcs) {
            InstanceIdentifier<Ipvc> ipvcId = MefServicesUtils.getIpvcInstanceIdentifier(ipvcSerId);
            Ipvc ipvc = MefServicesUtils.getIpvc(dataBroker, ipvcId);
            if (ipvc == null) {
                Log.error("Inconsistent data for svcId {}", ipvcSerId);
                continue;
            }
            List<Uni> toRemove = new ArrayList<>();

            List<Uni> unis = ipvc.getUnis() != null ? ipvc.getUnis().getUni() : null;
            unis = unis != null ? unis : Collections.emptyList();
            for (Uni uni : unis) {
                if (uni.getUniId().getValue().equals(uniId)) {
                    Log.info("Disconnecting Uni {} from svc id {}", uniId, ipvcSerId);
                    toRemove.add(uni);
                    break;
                }
            }

            IpvcVpn operIpvcVpn = MefServicesUtils.getOperIpvcVpn(dataBroker, ipvcId);
            if (operIpvcVpn == null) {
                Log.error("Ipvc {} hasn't been created as required, Nothing to disconnect", ipvcSerId);
                return;
            }

            removeUnis(ipvcId, operIpvcVpn, toRemove);
        }

    }

    private void addIpvc(DataTreeModification<Ipvc> newDataObject) {
        try {
            Ipvc ipvc = newDataObject.getRootNode().getDataAfter();
            String instanceName = ipvc.getIpvcId().getValue();
            final String vpnName = NetvirtVpnUtils.getUUidFromString(instanceName);
            InstanceIdentifier<Ipvc> ipvcId = newDataObject.getRootPath().getRootIdentifier();
            createVpnInstance(vpnName, ipvcId);
            String rd = waitForRd(vpnName);

            updateOperationalDataStoreWithVrfId(ipvcId, vpnName, rd);

            List<Uni> unis = new ArrayList<>();
            if (ipvc.getUnis() != null && ipvc.getUnis() != null) {
                unis = ipvc.getUnis().getUni();
            }
            Log.info("Number of UNI's: " + unis.size());

            createUnis(vpnName, ipvcId, unis, rd);
        } catch (final Exception e) {
            Log.error("Add ipvc failed !", e);
        }
    }

    private void updateOperationalDataStoreWithVrfId(InstanceIdentifier<Ipvc> ipvcId, String vpnName, String vrfId) {
        WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);
        MefServicesUtils.addOperIpvcVpnElan(ipvcId, vpnName, vrfId, tx);
        MdsalUtils.commitTransaction(tx);
    }

    private void createVpnInstance(final String vpnName, InstanceIdentifier<Ipvc> ipvcId) {
        synchronized (vpnName.intern()) {
            WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);
            Log.info("Adding vpn instance: " + vpnName);
            NetvirtVpnUtils.createVpnInstance(vpnName, tx);
            MefServicesUtils.addOperIpvcVpnElan(ipvcId, vpnName, tx);
            MdsalUtils.commitTransaction(tx);
        }
    }

    private String waitForRd(final String vpnName) {
        InstanceIdentifier<VpnInstance> vpnId = NetvirtVpnUtils.getVpnInstanceToVpnIdIdentifier(vpnName);
        @SuppressWarnings("resource")
        DataWaitListener<VpnInstance> vpnInstanceWaiter = new DataWaitListener<VpnInstance>(dataBroker, vpnId, 5,
                LogicalDatastoreType.CONFIGURATION, vpn -> vpn.getVrfId());
        if (!vpnInstanceWaiter.waitForData()) {
            String errorMessage = String.format("Fail to wait for vrfId for vpn %s", vpnName);
            Log.error(errorMessage);
            throw new UnsupportedOperationException(errorMessage);
        }
        String rd = (String) vpnInstanceWaiter.getData();
        return rd;
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
            removeUnis(ipvcId, operIpvcVpn, ipvc.getUnis().getUni());
            NetvirtUtils.safeSleep();

            String vpnId = operIpvcVpn.getVpnId();
            synchronized (vpnId.intern()) {
                WriteTransaction txvpn = MdsalUtils.createTransaction(dataBroker);
                NetvirtVpnUtils.removeVpnInstance(operIpvcVpn.getVpnId(), txvpn);
                MefServicesUtils.removeOperIpvcVpn(ipvcId, txvpn);
                MdsalUtils.commitTransaction(txvpn);
            }
        } catch (final Exception e) {
            Log.error("Remove ipvc failed !", e);
        }
    }

    private void removeUnis(InstanceIdentifier<Ipvc> ipvcId, IpvcVpn operIpvcVpn, List<Uni> uniToRemove) {
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
            removeInterfaces(ipvcId, operIpvcVpn, uni, ipUni);
        }
        updateQos(uniToRemove);
    }

    private void createUnis(String vpnName, InstanceIdentifier<Ipvc> ipvcId, List<Uni> uniToCreate, String rd) {
        // Create elan/vpn interfaces
        for (Uni uni : uniToCreate) {
            createInterfaces(vpnName, uni, ipvcId, rd);
        }

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
            String rd = waitForRd(vpnName);
            List<Uni> originalUni = origIpvc.getUnis() != null && origIpvc.getUnis().getUni() != null
                    ? origIpvc.getUnis().getUni() : Collections.emptyList();
            List<UniKey> originalUniIds = originalUni.stream().map(u -> u.getKey()).collect(Collectors.toList());
            List<Uni> updateUni = updateIpvc.getUnis() != null && updateIpvc.getUnis().getUni() != null
                    ? updateIpvc.getUnis().getUni() : Collections.emptyList();
            List<UniKey> updateUniIds = updateUni.stream().map(u -> u.getKey()).collect(Collectors.toList());

            List<Uni> uniToRemove = new ArrayList<>(originalUni);
            uniToRemove.removeIf(u -> updateUniIds.contains(u.getKey()));
            removeUnis(ipvcId, operIpvcVpn, uniToRemove);

            List<Uni> uniToCreate = new ArrayList<>(updateUni);
            uniToCreate.removeIf(u -> originalUniIds.contains(u.getKey()));
            createUnis(vpnName, ipvcId, uniToCreate, rd);

            List<Uni> uniToUpdate = new ArrayList<>(updateUni);
            uniToUpdate.removeIf(u -> !originalUniIds.contains(u.getKey()));
            updateUnis(uniToUpdate);

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
        String elanName = NetvirtVpnUtils.getElanNameForVpnPort(uniId, ipUniId);

        synchronized (vpnName.intern()) {
            Long vlan = ipUni.getVlan() != null ? Long.valueOf(ipUni.getVlan().getValue()) : null;

            interfaceName = createElanInterface(vpnName, ipvcId, uniId, elanName, vlan, ipUni.getSegmentationId());

            String portMacAddress = uni.getMacAddress().getValue();
            NetvirtVpnUtils.registerDirectSubnetForVpn(dataBroker, vpnName, new Uuid(elanName), ipUni.getIpAddress(),
                    interfaceName, portMacAddress);

            uniQosManager.mapUniPortBandwidthLimits(uniId, interfaceName, uniInService.getIngressBwProfile());
            createVpnInterface(vpnName, uni, ipUni, interfaceName, elanName, tx);
            NetvirtVpnUtils.createVpnPortFixedIp(dataBroker, vpnName, interfaceName, ipUni.getIpAddress(),
                    uni.getMacAddress(), tx);
            MefServicesUtils.addOperIpvcVpnElan(ipvcId, vpnName, uniInService.getUniId(), uniInService.getIpUniId(),
                    elanName, interfaceName, null, tx);

            if (uniInService.isPortSecurityEnabled() && uniInService.getSecurityGroups() != null && !uniInService.getSecurityGroups().isEmpty()) {
                NetvirtUtils.addAclToInterface(interfaceName, uniInService.getSecurityGroups(), tx);
            }

            MdsalUtils.commitTransaction(tx);
        }
    }

    private String createElanInterface(String vpnName, InstanceIdentifier<Ipvc> ipvcId, String uniId, String elanName,
            Long vlan, Long segmentationId) {
        Log.info("Adding elan instance: " + elanName);
        WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);
        NetvirtUtils.updateElanInstance(elanName, tx, segmentationId);

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
        MdsalUtils.commitTransaction(tx);
        return interfaceName;
    }

    private void createVpnInterface(String vpnName,
            org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni uni,
            IpUni ipUni, String interfaceName, String elanName, WriteTransaction tx) {

        Log.info("Adding vpn interface: " + interfaceName);
        BigInteger dpId = getPortDpId(interfaceName);
        IpAddress nextHop = getNodeIP(dpId);
        NetvirtVpnUtils.createUpdateVpnInterface(vpnName, interfaceName, nextHop, ipUni.getIpAddress(),
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

    private void removeInterfaces(InstanceIdentifier<Ipvc> ipvcId, IpvcVpn ipvcVpn, Uni uniInService, IpUni ipUni) {
        String uniId = uniInService.getUniId().getValue();
        String vpnName = ipvcVpn.getVpnId();
        VpnElans vpnElans = MefServicesUtils.findVpnElanForNetwork(new Identifier45(uniId), ipUni.getIpUniId(),
                ipvcVpn);
        if (vpnElans == null) {
            Log.error("Trying to remove non-operational vpn/elan for Uni {} Ip-UNi {}", uniId, ipUni.getIpUniId());
            return;
        }

        synchronized (vpnName.intern()) {
            NetvirtVpnUtils.unregisterDirectSubnetForVpn(dataBroker, vpnName, new Uuid(vpnElans.getElanId()),
                    vpnElans.getElanPort());
            uniQosManager.unMapUniPortBandwidthLimits(uniId, vpnElans.getElanPort());
            NetvirtVpnUtils.removeLearnedVpnVipToPort(dataBroker, vpnName, vpnElans.getElanPort());
            removeVpnInterface(vpnName, vpnElans, uniId, ipUni);
        }
        waitForInterfaceDpnClean(vpnName, ipvcVpn.getVrfId(), vpnElans.getElanPort());

        synchronized (vpnName.intern()) {
            removeElan(vpnElans, uniId, ipUni);
            MefServicesUtils.removeOperIpvcElan(dataBroker, ipvcId, ipvcVpn.getVpnId(), uniInService.getUniId(),
                    uniInService.getIpUniId(), vpnElans.getElanId(), vpnElans.getElanPort());
        }

    }

    private void waitForInterfaceDpnClean(String vpnName, String rd, String interfaceName) {
        InstanceIdentifier<VpnInstanceOpDataEntry> vpnId = NetvirtVpnUtils.getVpnInstanceOpDataIdentifier(rd);
        DataWaitGetter<VpnInstanceOpDataEntry> getInterfByName = (vpn) -> {
            if (vpn.getVpnToDpnList() == null) {
                return null;
            }
            for (VpnToDpnList is : vpn.getVpnToDpnList()) {
                if (is.getVpnInterfaces() == null) {
                    continue;
                }
                for (VpnInterfaces i : is.getVpnInterfaces()) {
                    if (i.getInterfaceName().equals(interfaceName)) {
                        Log.info("Waiting for deletion vpn interface from vpn to dpn list vpn : {} interface: {}",
                                vpnName, interfaceName);
                        return interfaceName;
                    }
                }
            }
            Log.info("Deleted vpn interface from vpn to dpn list vpn : {} interface: {}", vpnName, interfaceName);
            return null;
        };

        int retryCount = 2;
        Optional<VpnInstanceOpDataEntry> vpnOper = MdsalUtils.read(dataBroker, LogicalDatastoreType.OPERATIONAL, vpnId);
        if (vpnOper.isPresent() && vpnOper.get().getVpnToDpnList() != null) {
            for (VpnToDpnList vpnList : vpnOper.get().getVpnToDpnList()) {
                if (vpnList.getVpnInterfaces() != null) {
                    retryCount = retryCount + 2 * vpnList.getVpnInterfaces().size();
                }
            }
        }

        @SuppressWarnings("resource")
        DataWaitListener<VpnInstanceOpDataEntry> vpnInstanceWaiter = new DataWaitListener<>(dataBroker, vpnId,
                retryCount, LogicalDatastoreType.OPERATIONAL, getInterfByName);
        if (!vpnInstanceWaiter.waitForClean()) {
            String errorMessage = String.format("Fail to wait for vpn to dpn list clean-up vpn : %s interface: %s",
                    vpnName, interfaceName);
            Log.error(errorMessage);
            throw new UnsupportedOperationException(errorMessage);
        }
    }

    private void removeElan(VpnElans vpnElans, String uniId, IpUni ipUni) {
        WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);

        Long vlan = ipUni.getVlan() != null ? Long.valueOf(ipUni.getVlan().getValue()) : 0;
        Log.info("Removing trunk interface for uni {} vlan: {}", uniId, vlan);
        uniPortManager.removeCeVlan(uniId, vlan);

        String elanName = vpnElans.getElanId();
        String interfaceName = vpnElans.getElanPort();

        Log.info("Removing elan instance {} and interface {}: ", elanName, interfaceName);
        NetvirtUtils.deleteElanInterface(interfaceName, tx);
        NetvirtUtils.deleteElanInstance(elanName, tx);
        MdsalUtils.commitTransaction(tx);
    }

    private void removeVpnInterface(String vpnName, VpnElans vpnElans, String uniId, IpUni ipUni, WriteTransaction tx) {
        String interfaceName = vpnElans.getElanPort();
        NetvirtVpnUtils.removeVpnInterface(interfaceName, tx);
        NetvirtVpnUtils.removeVpnPortFixedIp(vpnName, ipUni.getIpAddress(), tx);
    }

    private void removeVpnInterface(String vpnName, VpnElans vpnElans, String uniId, IpUni ipUni) {
        Log.info("Removing vpn interface: " + vpnElans.getElanPort());
        WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);
        removeVpnInterface(vpnName, vpnElans, uniId, ipUni, tx);
        MdsalUtils.commitTransaction(tx);
        Log.info("Finished working on vpn interface: " + vpnElans.getElanPort());
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

    private void updateUnis(List<Uni> uniToUpdate) {
        uniToUpdate.forEach(u -> uniQosManager.updateUni(u.getUniId(), u.getIngressBwProfile()));
        updateQos(uniToUpdate);
    }

    private IpAddress getNodeIP(BigInteger dpId) {
        Node node = getPortsNode(dpId);
        String localIp = southBoundUtils.getOpenvswitchOtherConfig(node, LOCAL_IP);
        if (localIp == null) {
            throw new UnsupportedOperationException(
                    "missing local_ip key in ovsdb:openvswitch-other-configs in operational"
                            + " network-topology for node: " + node.getNodeId().getValue());
        }

        return new IpAddress(localIp.toCharArray());
    }

    @SuppressWarnings("unchecked")
    private Node getPortsNode(BigInteger dpnId) {
        InstanceIdentifier<BridgeRefEntry> bridgeRefInfoPath = InstanceIdentifier.create(BridgeRefInfo.class)
                .child(BridgeRefEntry.class, new BridgeRefEntryKey(dpnId));
        BridgeRefEntry bridgeRefEntry = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, bridgeRefInfoPath);
        if (bridgeRefEntry == null) {
            throw new UnsupportedOperationException("no bridge ref entry found for dpnId: " + dpnId);
        }

        InstanceIdentifier<Node> nodeId = ((InstanceIdentifier<OvsdbBridgeAugmentation>) bridgeRefEntry
                .getBridgeReference().getValue()).firstIdentifierOf(Node.class);
        Node node = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, nodeId);

        if (node == null) {
            throw new UnsupportedOperationException("missing node for dpnId: " + dpnId);
        }
        return node;

    }

    private BigInteger getPortDpId(String logicalPortId) {
        BigInteger dpId = BigInteger.ZERO;
        if (portToDpn.containsKey(logicalPortId)) {
            dpId = portToDpn.get(logicalPortId);
        } else {
            dpId = NetvirtUtils.getDpnForInterface(odlInterfaceRpcService, logicalPortId);
            portToDpn.put(logicalPortId, dpId);
        }
        return dpId;
    }
}
