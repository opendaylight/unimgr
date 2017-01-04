/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.VpnInstances;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.VpnInterfaces;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.af.config.VpnTargetsBuilder;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.af.config.vpntargets.VpnTarget;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.instances.VpnInstance;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.instances.VpnInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.instances.VpnInstanceKey;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.instances.vpn.instance.Ipv4FamilyBuilder;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.interfaces.VpnInterface;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.interfaces.VpnInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.interfaces.VpnInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.OdlArputilService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.SendArpRequestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.SendArpRequestInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.interfaces.InterfaceAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.interfaces.InterfaceAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.ElanInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.instances.ElanInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.instances.ElanInstanceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.Adjacencies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.AdjacenciesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.adjacency.list.Adjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.adjacency.list.AdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.adjacency.list.AdjacencyKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.NetworkMaps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.NeutronVpnPortipPortData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.SubnetAddedToVpnBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.SubnetDeletedFromVpnBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.Subnetmaps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.networkmaps.NetworkMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.networkmaps.NetworkMapBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.networkmaps.NetworkMapKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.neutron.vpn.portip.port.data.VpnPortipToPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.neutron.vpn.portip.port.data.VpnPortipToPortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.neutron.vpn.portip.port.data.VpnPortipToPortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.subnetmaps.Subnetmap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.subnetmaps.SubnetmapBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.subnetmaps.SubnetmapKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.IpVersionV4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.SubnetKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class NetvirtVpnUtils {
    private static final Logger logger = LoggerFactory.getLogger(NetvirtVpnUtils.class);
    private final static String ELAN_PREFIX = "elan.";
    private final static String IP_ADDR_SUFFIX = "/32";
    private final static String IP_MUSK_SEPARATOR = "/";

    public static void createVpnInstance(DataBroker dataBroker, String instanceName) {
        WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);
        createVpnInstance(instanceName, tx);
        MdsalUtils.commitTransaction(tx);
    }

    public static void createVpnInstance(String instanceName, WriteTransaction tx) {
        VpnInstanceBuilder builder = new VpnInstanceBuilder();
        builder.setVpnInstanceName(instanceName);
        Ipv4FamilyBuilder ipv4FamilyBuilder = new Ipv4FamilyBuilder();
        VpnTargetsBuilder vpnTargetsB = new VpnTargetsBuilder();
        vpnTargetsB.setVpnTarget(new ArrayList<VpnTarget>());
        ipv4FamilyBuilder.setVpnTargets(vpnTargetsB.build());

        // WA till netvirt will allow creation of VPN without RD
        UUID vpnId = UUID.fromString(instanceName);
        String rd = String.valueOf(Math.abs(vpnId.getLeastSignificantBits()));
        ipv4FamilyBuilder.setRouteDistinguisher(rd);
        builder.setIpv4Family(ipv4FamilyBuilder.build());

        tx.put(LogicalDatastoreType.CONFIGURATION, getVpnInstanceInstanceIdentifier(instanceName), builder.build());
    }

    public static void removeVpnInstance(String instanceName, WriteTransaction tx) {
        tx.delete(LogicalDatastoreType.CONFIGURATION, getVpnInstanceInstanceIdentifier(instanceName));
    }

    private static InstanceIdentifier<VpnInstance> getVpnInstanceInstanceIdentifier(String instanceName) {
        return InstanceIdentifier.builder(VpnInstances.class).child(VpnInstance.class, new VpnInstanceKey(instanceName))
                .build();
    }

    public static void createUpdateVpnInterface(DataBroker dataBroker, String vpnName, String interfaceName,
            String ifAddr, String macAddress, boolean primary, String gwIpAddress, String directSubnetId) {
        WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);
        createUpdateVpnInterface(vpnName, interfaceName, ifAddr, macAddress, primary, gwIpAddress, directSubnetId, tx);
        MdsalUtils.commitTransaction(tx);
    }

    public static void createUpdateVpnInterface(String vpnName, String interfaceName, IpPrefix ifPrefix,
            String macAddress, boolean primary, IpPrefix gwIpAddress, String directSubnetId, WriteTransaction tx) {
        synchronized (interfaceName.intern()) {
            String ipAddress = null;
            String nextHopIp = null;
            if (primary) {
                ipAddress = getAddressFromSubnet(ipPrefixToString(ifPrefix));
            } else {
                ipAddress = ipPrefixToString(ifPrefix);
                nextHopIp = getIpAddressFromPrefix(ipPrefixToString(gwIpAddress));
            }
            createUpdateVpnInterface(vpnName, interfaceName, ipAddress, macAddress, primary, nextHopIp, directSubnetId,
                    tx);
        }
    }

    public static void createUpdateVpnInterface(String vpnName, String interfaceName, String ipAddress,
            String macAddress, boolean primary, String nextHopIp, String subnetId, WriteTransaction tx) {
        synchronized (interfaceName.intern()) {
            Adjacencies adjancencies = buildInterfaceAdjacency(ipAddress, macAddress, primary, nextHopIp, subnetId);
            VpnInterfaceBuilder einterfaceBuilder = createVpnInterface(vpnName, interfaceName, adjancencies);

            tx.merge(LogicalDatastoreType.CONFIGURATION, getVpnInterfaceInstanceIdentifier(interfaceName),
                    einterfaceBuilder.build());
        }
    }

    private static VpnInterfaceBuilder createVpnInterface(String instanceName, String interfaceName,
            Adjacencies adjacencies) {
        VpnInterfaceBuilder einterfaceBuilder = new VpnInterfaceBuilder();
        einterfaceBuilder.setVpnInstanceName(instanceName);
        einterfaceBuilder.setName(interfaceName);
        einterfaceBuilder.addAugmentation(Adjacencies.class, adjacencies);
        return einterfaceBuilder;
    }

    private static Adjacencies buildInterfaceAdjacency(String ipAddress, String macAddress, boolean primary,
            String nextHopIp, String subnetId) {
        AdjacenciesBuilder builder = new AdjacenciesBuilder();
        List<Adjacency> list = new ArrayList<>();

        AdjacencyBuilder aBuilder = new AdjacencyBuilder();
        aBuilder.setIpAddress(ipAddress);
        if (macAddress != null) {
            aBuilder.setMacAddress(macAddress);
        }
        aBuilder.setPrimaryAdjacency(primary);
        if (subnetId != null) {
            aBuilder.setSubnetId(new Uuid(subnetId));
        }
        if (nextHopIp != null) {
            aBuilder.setNextHopIpList(Arrays.asList(nextHopIp));
        }
        list.add(aBuilder.build());

        builder.setAdjacency(list);
        return builder.build();
    }

    public static void removeVpnInterface(String interfaceName, WriteTransaction tx) {
        synchronized (interfaceName.intern()) {
            tx.delete(LogicalDatastoreType.CONFIGURATION, getVpnInterfaceInstanceIdentifier(interfaceName));
        }
    }

    public static void removeVpnInterfaceAdjacencies(DataBroker dataBroker, String vpnName, String interfaceName) {
        InstanceIdentifier<VpnInterface> identifier = getVpnInterfaceInstanceIdentifier(interfaceName);
        InstanceIdentifier<Adjacencies> path = identifier.augmentation(Adjacencies.class);
        Optional<Adjacencies> adjacencies = MdsalUtils.read(dataBroker, LogicalDatastoreType.OPERATIONAL, path);
        List<Adjacency> adjacenciesList = adjacencies.isPresent() && adjacencies.get().getAdjacency() != null
                ? adjacencies.get().getAdjacency() : Collections.emptyList();
        adjacenciesList.forEach(a -> {
            String ipStr = getIpAddressFromPrefix(a.getIpAddress());
            InstanceIdentifier<VpnPortipToPort> id = getVpnPortipToPortIdentifier(vpnName, ipStr);
            MdsalUtils.syncDelete(dataBroker, LogicalDatastoreType.OPERATIONAL, id);
        });

        AdjacenciesBuilder builder = new AdjacenciesBuilder();
        List<Adjacency> list = new ArrayList<>();
        builder.setAdjacency(list);
        VpnInterfaceBuilder einterfaceBuilder = createVpnInterface(vpnName, interfaceName, builder.build());

        MdsalUtils.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION,
                getVpnInterfaceInstanceIdentifier(interfaceName), einterfaceBuilder.build());

    }

    public static void removeVpnInterfaceAdjacency(DataBroker dataBroker, String interfaceName, IpPrefix ifPrefix) {
        WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);
        String ipAddress = ipPrefixToString(ifPrefix);
        removeVpnInterfaceAdjacency(interfaceName, ipAddress, tx);
        MdsalUtils.commitTransaction(tx);
    }

    public static void removeVpnInterfaceAdjacency(DataBroker dataBroker, String interfaceName, IpAddress ifAddress) {
        WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);
        String ifAddressStr = getAddressFromSubnet(ipAddressToString(ifAddress));
        removeVpnInterfaceAdjacency(interfaceName, ifAddressStr, tx);
        MdsalUtils.commitTransaction(tx);
    }

    private static void removeVpnInterfaceAdjacency(String interfaceName, String ipAddress, WriteTransaction tx) {
        synchronized (interfaceName.intern()) {

            InstanceIdentifier<Adjacency> adjacencyIdentifier = InstanceIdentifier.builder(VpnInterfaces.class)
                    .child(VpnInterface.class, new VpnInterfaceKey(interfaceName)).augmentation(Adjacencies.class)
                    .child(Adjacency.class, new AdjacencyKey(ipAddress)).build();

            tx.delete(LogicalDatastoreType.CONFIGURATION, adjacencyIdentifier);
        }
    }

    private static InstanceIdentifier<VpnInterface> getVpnInterfaceInstanceIdentifier(String interfaceName) {
        return InstanceIdentifier.builder(VpnInterfaces.class)
                .child(VpnInterface.class, new VpnInterfaceKey(interfaceName)).build();
    }

    public static void createVpnPortFixedIp(DataBroker dataBroker, String vpnName, String portName, IpPrefix ipAddress,
            MacAddress macAddress) {
        String fixedIpPrefix = ipPrefixToString(ipAddress);
        String fixedIp = getIpAddressFromPrefix(fixedIpPrefix);

        WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);
        createVpnPortFixedIp(vpnName, portName, fixedIp, macAddress, tx);
        MdsalUtils.commitTransaction(tx);
    }

    public static void createVpnPortFixedIp(String vpnName, String portName, IpPrefix ipAddress, MacAddress macAddress,
            WriteTransaction tx) {
        String fixedIpPrefix = ipPrefixToString(ipAddress);
        String fixedIp = getIpAddressFromPrefix(fixedIpPrefix);
        createVpnPortFixedIp(vpnName, portName, fixedIp, macAddress, tx);
    }

    private static void createVpnPortFixedIp(String vpnName, String portName, String fixedIp, MacAddress macAddress,
            WriteTransaction tx) {
        synchronized ((vpnName + fixedIp).intern()) {
            InstanceIdentifier<VpnPortipToPort> id = getVpnPortipToPortIdentifier(vpnName, fixedIp);
            VpnPortipToPortBuilder builder = new VpnPortipToPortBuilder()
                    .setKey(new VpnPortipToPortKey(fixedIp, vpnName)).setVpnName(vpnName).setPortFixedip(fixedIp)
                    .setPortName(portName).setMacAddress(macAddress.getValue()).setSubnetIp(true);
            tx.put(LogicalDatastoreType.OPERATIONAL, id, builder.build());
            logger.debug(
                    "Interface to fixedIp added: {}, vpn {}, interface {}, mac {} added to " + "VpnPortipToPort DS",
                    fixedIp, vpnName, portName, macAddress);
        }
    }

    public static VpnPortipToPort getVpnPortFixedIp(DataBroker dataBroker, String vpnName, String fixedIp) {
        InstanceIdentifier<VpnPortipToPort> id = getVpnPortipToPortIdentifier(vpnName, fixedIp);
        Optional<VpnPortipToPort> opt = MdsalUtils.read(dataBroker, LogicalDatastoreType.OPERATIONAL, id);
        return opt != null && opt.isPresent() ? opt.get() : null;
    }

    public static void removeVpnPortFixedIp(String vpnName, IpPrefix ipAddress, WriteTransaction tx) {
        String fixedIpPrefix = ipPrefixToString(ipAddress);
        String fixedIp = getIpAddressFromPrefix(fixedIpPrefix);
        InstanceIdentifier<VpnPortipToPort> id = getVpnPortipToPortIdentifier(vpnName, fixedIp);
        tx.delete(LogicalDatastoreType.OPERATIONAL, id);
    }

    public static void registerDirectSubnetForVpn(DataBroker dataBroker, Uuid subnetName, IpAddress gwIpAddress) {
        final SubnetKey subnetkey = new SubnetKey(subnetName);

        final InstanceIdentifier<Subnet> subnetidentifier = InstanceIdentifier.create(Neutron.class)
                .child(Subnets.class).child(Subnet.class, subnetkey);

        SubnetBuilder subnetBuilder = new SubnetBuilder();
        subnetBuilder.setIpVersion(IpVersionV4.class);
        subnetBuilder.setGatewayIp(gwIpAddress);
        subnetBuilder.setKey(subnetkey);
        MdsalUtils.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION, subnetidentifier, subnetBuilder.build());
    }

    public static void unregisterDirectSubnetForVpn(DataBroker dataBroker, Uuid subnetName) {
        final SubnetKey subnetkey = new SubnetKey(subnetName);
        final InstanceIdentifier<Subnet> subnetidentifier = InstanceIdentifier.create(Neutron.class)
                .child(Subnets.class).child(Subnet.class, subnetkey);

        MdsalUtils.syncDelete(dataBroker, LogicalDatastoreType.CONFIGURATION, subnetidentifier);
    }

    public static void addDirectSubnetToVpn(DataBroker dataBroker,
            final NotificationPublishService notificationPublishService, String vpnName, String subnetName,
            IpPrefix subnetIpPrefix, String interfaceName, int waitForElan) {
        InstanceIdentifier<ElanInstance> elanIdentifierId = NetvirtUtils.getElanInstanceInstanceIdentifier(subnetName);

        @SuppressWarnings("resource") // AutoCloseable
        DataWaitListener<ElanInstance> elanTagWaiter = new DataWaitListener<>(
                dataBroker, elanIdentifierId, 10, LogicalDatastoreType.CONFIGURATION, el -> el.getElanTag());
        if ( !elanTagWaiter.waitForData()) {
            logger.error("Trying to add invalid elan {} to vpn {}", subnetName, vpnName);
            return;
        }

        Uuid subnetId = new Uuid(subnetName);
        logger.info("Adding subnet {} {} to elan map", subnetId, subnetId);
        createSubnetToNetworkMapping(dataBroker, subnetId, subnetId);

        String subnetIp = getSubnetFromPrefix(ipPrefixToString(subnetIpPrefix));
        logger.info("Adding subnet {} {} to vpn {}", subnetName, subnetIp, vpnName);
        updateSubnetNode(dataBroker, new Uuid(vpnName), subnetId, subnetIp);

        logger.info("Adding port {} to subnet {}", interfaceName, subnetName);
        updateSubnetmapNodeWithPorts(dataBroker, subnetId, new Uuid(interfaceName), null);

        Optional<ElanInstance> elanInstance = MdsalUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                elanIdentifierId);
        Long elanTag = elanInstance.get().getElanTag();

        logger.info("Publish subnet {}", subnetName);
        publishSubnetAddNotification(notificationPublishService, subnetId, subnetIp, vpnName, elanTag);
        logger.info("Finished Working on subnet {}", subnetName);
    }

    public static void removeDirectSubnetFromVpn(DataBroker dataBroker,
            final NotificationPublishService notificationPublishService, String vpnName, String subnetName,
            String interfaceName) {
        InstanceIdentifier<ElanInstance> elanIdentifierId = InstanceIdentifier.builder(ElanInstances.class)
                .child(ElanInstance.class, new ElanInstanceKey(subnetName)).build();
        Optional<ElanInstance> elanInstance = MdsalUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                elanIdentifierId);
        if (!elanInstance.isPresent()) {
            logger.error("Trying to add invalid elan {} to vpn {}", subnetName, vpnName);
            return;
        }
        Long elanTag = elanInstance.get().getElanTag() != null ? elanInstance.get().getElanTag()
                : elanInstance.get().getSegmentationId();
        Uuid subnetId = new Uuid(subnetName);

        logger.info("Publish subnet remove {}", subnetName);
        publishSubnetRemoveNotification(notificationPublishService, subnetId, vpnName, elanTag);

        logger.info("Removing port {} from subnet {}", interfaceName, subnetName);
        updateSubnetmapNodeWithPorts(dataBroker, subnetId, null, new Uuid(interfaceName));

        logger.info("Removing subnet {} from vpn {}", subnetName, vpnName);
        removeSubnetNode(dataBroker, new Uuid(vpnName));

        logger.info("Removing subnet {} to elan map", subnetId);
        removeSubnetToNetworkMapping(dataBroker, subnetId);

        logger.info("Finished Working on subnet {}", subnetName);
    }

    private static void createSubnetToNetworkMapping(DataBroker dataBroker, Uuid subnetId, Uuid networkId) {
        InstanceIdentifier<NetworkMap> networkMapIdentifier = getNetworkMapIdentifier(networkId);
        Optional<NetworkMap> optionalNetworkMap = MdsalUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                networkMapIdentifier);
        NetworkMapBuilder nwMapBuilder = null;
        if (optionalNetworkMap.isPresent()) {
            nwMapBuilder = new NetworkMapBuilder(optionalNetworkMap.get());
        } else {
            nwMapBuilder = new NetworkMapBuilder().setKey(new NetworkMapKey(networkId)).setNetworkId(networkId);
            logger.debug("Adding a new network node in NetworkMaps DS for network {}", networkId.getValue());
        }
        List<Uuid> subnetIdList = nwMapBuilder.getSubnetIdList();
        if (subnetIdList == null) {
            subnetIdList = new ArrayList<>();
        }
        subnetIdList.add(subnetId);
        nwMapBuilder.setSubnetIdList(subnetIdList);
        MDSALUtil.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION, networkMapIdentifier, nwMapBuilder.build());
        logger.debug("Created subnet-network mapping for subnet {} network {}", subnetId.getValue(),
                networkId.getValue());
    }

    private static void removeSubnetToNetworkMapping(DataBroker dataBroker, Uuid networkId) {
        InstanceIdentifier<NetworkMap> networkMapIdentifier = getNetworkMapIdentifier(networkId);
        MDSALUtil.syncDelete(dataBroker, LogicalDatastoreType.CONFIGURATION, networkMapIdentifier);
        logger.debug("Deleted subnet-network mapping for  network {}", networkId.getValue());
    }

    protected static void updateSubnetNode(DataBroker dataBroker, Uuid vpnId, Uuid subnetId, String subnetIp) {
        Subnetmap subnetmap = null;
        SubnetmapBuilder builder = null;
        InstanceIdentifier<Subnetmap> id = InstanceIdentifier.builder(Subnetmaps.class)
                .child(Subnetmap.class, new SubnetmapKey(subnetId)).build();

        synchronized (subnetId.getValue().intern()) {
            Optional<Subnetmap> sn = MdsalUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION, id);
            if (sn.isPresent()) {
                builder = new SubnetmapBuilder(sn.get());
                logger.debug("updating existing subnetmap node for subnet ID {}", subnetId.getValue());
            } else {
                builder = new SubnetmapBuilder().setKey(new SubnetmapKey(subnetId)).setId(subnetId);
                logger.debug("creating new subnetmap node for subnet ID {}", subnetId.getValue());
            }

            builder.setSubnetIp(subnetIp);
            builder.setNetworkId(subnetId);
            builder.setVpnId(vpnId);

            subnetmap = builder.build();
            logger.debug("Creating/Updating subnetMap node: {} ", subnetId.getValue());
            MDSALUtil.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION, id, subnetmap);
        }
    }

    protected static void removeSubnetNode(DataBroker dataBroker, Uuid subnetId) {
        InstanceIdentifier<Subnetmap> id = InstanceIdentifier.builder(Subnetmaps.class)
                .child(Subnetmap.class, new SubnetmapKey(subnetId)).build();

        synchronized (subnetId.getValue().intern()) {
            logger.debug("Deleting subnetMap node: {} ", subnetId.getValue());
            MDSALUtil.syncDelete(dataBroker, LogicalDatastoreType.CONFIGURATION, id);
        }
    }

    private static void updateSubnetmapNodeWithPorts(DataBroker dataBroker, Uuid subnetId, Uuid portIdToAdd,
            Uuid portIdToRemove) {
        Subnetmap subnetmap = null;
        InstanceIdentifier<Subnetmap> id = InstanceIdentifier.builder(Subnetmaps.class)
                .child(Subnetmap.class, new SubnetmapKey(subnetId)).build();
        synchronized (subnetId.getValue().intern()) {
            Optional<Subnetmap> sn = MdsalUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION, id);
            if (sn.isPresent()) {
                SubnetmapBuilder builder = new SubnetmapBuilder(sn.get());
                if (null != portIdToAdd) {
                    List<Uuid> portList = builder.getPortList();
                    if (null == portList) {
                        portList = new ArrayList<>();
                    }
                    if (portIdToAdd != null) {
                        portList.add(portIdToAdd);
                        logger.debug("Updating subnetmap node {} with port {}", subnetId.getValue(),
                                portIdToAdd.getValue());

                    }
                    if (portIdToRemove != null) {
                        portList.remove(portIdToRemove);
                        logger.debug("Updating subnetmap node {} removing port {}", subnetId.getValue(),
                                portIdToRemove.getValue());

                    }
                    builder.setPortList(portList);
                }
                subnetmap = builder.build();
                MDSALUtil.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION, id, subnetmap);
            } else {
                logger.error("Trying to update non-existing subnetmap node {} ", subnetId.getValue());
            }
        }
    }

    private static InstanceIdentifier<NetworkMap> getNetworkMapIdentifier(Uuid networkId) {
        InstanceIdentifier<NetworkMap> id = InstanceIdentifier.builder(NetworkMaps.class)
                .child(NetworkMap.class, new NetworkMapKey(networkId)).build();
        return id;
    }

    private static InstanceIdentifier<VpnPortipToPort> getVpnPortipToPortIdentifier(String vpnName, String fixedIp) {
        InstanceIdentifier<VpnPortipToPort> id = InstanceIdentifier.builder(NeutronVpnPortipPortData.class)
                .child(VpnPortipToPort.class, new VpnPortipToPortKey(fixedIp, vpnName)).build();
        return id;
    }

    public static InstanceIdentifier<VpnPortipToPort> getVpnPortipToPortIdentifier() {
        return InstanceIdentifier.builder(NeutronVpnPortipPortData.class).child(VpnPortipToPort.class).build();
    }

    private static void publishSubnetAddNotification(final NotificationPublishService notificationPublishService,
            Uuid subnetId, String subnetIp, String vpnName, Long elanTag) {
        SubnetAddedToVpnBuilder builder = new SubnetAddedToVpnBuilder();

        logger.info("publish notification called for network creation");

        builder.setSubnetId(subnetId);
        builder.setSubnetIp(subnetIp);
        builder.setVpnName(vpnName);
        builder.setExternalVpn(true);
        builder.setElanTag(elanTag);

        try {
            notificationPublishService.putNotification(builder.build());
        } catch (InterruptedException e) {
            logger.error("Fail to publish notification {}", builder, e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private static void publishSubnetRemoveNotification(final NotificationPublishService notificationPublishService,
            Uuid subnetId, String vpnName, Long elanTag) {
        SubnetDeletedFromVpnBuilder builder = new SubnetDeletedFromVpnBuilder();

        logger.info("publish notification called for network deletion");

        builder.setSubnetId(subnetId);
        builder.setVpnName(vpnName);
        builder.setExternalVpn(true);
        builder.setElanTag(elanTag);

        try {
            notificationPublishService.putNotification(builder.build());
        } catch (InterruptedException e) {
            logger.error("Fail to publish notification {}", builder, e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public static void sendArpRequest(OdlArputilService arpUtilService, IpAddress srcIpAddress, IpAddress dstIpAddress,
            String interf) {
        try {
            List<InterfaceAddress> interfaceAddresses = new ArrayList<>();
            interfaceAddresses
                    .add(new InterfaceAddressBuilder().setInterface(interf).setIpAddress(srcIpAddress).build());

            SendArpRequestInput sendArpRequestInput = new SendArpRequestInputBuilder().setIpaddress(dstIpAddress)
                    .setInterfaceAddress(interfaceAddresses).build();
            arpUtilService.sendArpRequest(sendArpRequestInput);
        } catch (Exception e) {
            logger.error("Failed to send ARP request to IP {} from interfaces {}",
                    dstIpAddress.getIpv4Address().getValue(), interf, e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public static String getElanNameForVpnPort(String uniId, String ipUniId) {
        return getUUidFromString(ELAN_PREFIX + uniId + ipUniId);
    }

    public static String getIpAddressFromPrefix(String prefix) {
        return prefix.split(IP_MUSK_SEPARATOR)[0];
    }

    private static String getMaskFromPrefix(String prefix) {
        return prefix.split(IP_MUSK_SEPARATOR)[1];
    }

    public static String getSubnetFromPrefix(String prefix) {
        SubnetInfo subnet = new SubnetUtils(prefix).getInfo();
        return subnet.getNetworkAddress() + IP_MUSK_SEPARATOR + getMaskFromPrefix(prefix);
    }

    public static String getSubnetFromPrefix(IpPrefix prefix) {
        String prefixStr = ipPrefixToString(prefix);
        return getSubnetFromPrefix(prefixStr);
    }

    private static String getAddressFromSubnet(String prefix) {
        String myAddress = getIpAddressFromPrefix(prefix);
        return myAddress + IP_ADDR_SUFFIX;
    }

    public static String getUUidFromString(String key) {
        return java.util.UUID.nameUUIDFromBytes(key.getBytes()).toString();
    }

    public static String ipPrefixToString(IpPrefix ipAddress) {
        if (ipAddress.getIpv4Prefix() != null) {
            return ipAddress.getIpv4Prefix().getValue();
        }

        return ipAddress.getIpv6Prefix().getValue();
    }

    public static String ipAddressToString(IpAddress ipAddress) {
        if (ipAddress.getIpv4Address() != null) {
            return ipAddress.getIpv4Address().getValue();
        }

        return ipAddress.getIpv6Address().getValue();
    }
}
