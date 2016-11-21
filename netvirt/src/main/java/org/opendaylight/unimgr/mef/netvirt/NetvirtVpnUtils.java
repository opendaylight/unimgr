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
import java.util.List;
import java.util.UUID;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.VpnInstances;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.VpnInterfaces;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.af.config.VpnTargetsBuilder;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.instances.VpnInstance;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.instances.VpnInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.instances.VpnInstanceKey;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.instances.vpn.instance.Ipv4FamilyBuilder;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.interfaces.VpnInterface;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.interfaces.VpnInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.interfaces.VpnInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.NetworkMaps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.NeutronVpnPortipPortData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.SubnetAddedToVpnBuilder;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;

public class NetvirtVpnUtils {
    private static final Logger logger = LoggerFactory.getLogger(NetvirtVpnUtils.class);
    private final static String ELAN_PREFIX = "elan.";
    private final static String TRUNK_SUFFIX = "-trunk";
    private final static String VLAN_SEPARATOR = ".";
    private final static String IP_ADDR_SUFFIX = "/32";
    private final static String IP_MUSK_SEPARATOR = "/";
    private final static int MaxRetries = 10;

    public static void createVpnInstance(DataBroker dataBroker, String instanceName) {
        VpnInstanceBuilder builder = new VpnInstanceBuilder();
        builder.setVpnInstanceName(instanceName);
        Ipv4FamilyBuilder ipv4FamilyBuilder = new Ipv4FamilyBuilder();
        ipv4FamilyBuilder.setVpnTargets(new VpnTargetsBuilder().build());
        // WA till netvirt will allow creation of VPN without RD
        UUID vpnId = UUID.fromString(instanceName);
        String rd = String.valueOf(Math.abs(vpnId.getLeastSignificantBits()));
        ipv4FamilyBuilder.setRouteDistinguisher(rd);
        builder.setIpv4Family(ipv4FamilyBuilder.build());

        MdsalUtils.syncUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION,
                getVpnInstanceInstanceIdentifier(instanceName), builder.build());
    }

    public static void createUpdateVpnInterface(DataBroker dataBroker, String vpnName, String interfaceName,
            IpPrefix ifPrefix, MacAddress macAddress, boolean primary, IpPrefix gwIpAddress) {
        synchronized (interfaceName.intern()) {
            String ipAddress = null;
            String nextHopIp = null;
            if (primary) {
                ipAddress = getPrefixFromSubnet(MefUtils.ipPrefixToString(ifPrefix));
            } else {
                ipAddress = MefUtils.ipPrefixToString(ifPrefix);
                nextHopIp = getIpAddressFromPrefix(MefUtils.ipPrefixToString(gwIpAddress));
            }

            Adjacencies adjancencies = buildInterfaceAdjacency(ipAddress, macAddress, primary, nextHopIp);
            VpnInterfaceBuilder einterfaceBuilder = createVpnInterface(vpnName, interfaceName, adjancencies);

            MdsalUtils.syncUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION,
                    getVpnInterfaceInstanceIdentifier(interfaceName), einterfaceBuilder.build());
        }
    }

    private static Adjacencies buildInterfaceAdjacency(String ipAddress, MacAddress macAddress, boolean primary,
            String nextHopIp) {
        AdjacenciesBuilder builder = new AdjacenciesBuilder();
        List<Adjacency> list = new ArrayList<>();

        AdjacencyBuilder aBuilder = new AdjacencyBuilder();
        aBuilder.setIpAddress(ipAddress);
        if (macAddress != null) {
            aBuilder.setMacAddress(macAddress.getValue());
        }
        aBuilder.setPrimaryAdjacency(primary);
        if (nextHopIp != null) {
            aBuilder.setNextHopIpList(Arrays.asList(nextHopIp));
        }
        list.add(aBuilder.build());

        builder.setAdjacency(list);
        return builder.build();
    }

    private static VpnInterfaceBuilder createVpnInterface(String instanceName, String interfaceName,
            Adjacencies adjacencies) {
        VpnInterfaceBuilder einterfaceBuilder = new VpnInterfaceBuilder();
        einterfaceBuilder.setVpnInstanceName(instanceName);
        einterfaceBuilder.setName(interfaceName);
        einterfaceBuilder.addAugmentation(Adjacencies.class, adjacencies);
        return einterfaceBuilder;
    }

    private static InstanceIdentifier<VpnInstance> getVpnInstanceInstanceIdentifier(String instanceName) {
        return InstanceIdentifier.builder(VpnInstances.class).child(VpnInstance.class, new VpnInstanceKey(instanceName))
                .build();
    }

    private static InstanceIdentifier<VpnInterface> getVpnInterfaceInstanceIdentifier(String interfaceName) {
        return InstanceIdentifier.builder(VpnInterfaces.class)
                .child(VpnInterface.class, new VpnInterfaceKey(interfaceName)).build();
    }

    public static void createVpnPortFixedIp(DataBroker dataBroker, String vpnName, String portName, IpPrefix ipAddress,
            MacAddress macAddress) {
        String fixedIpPrefix = MefUtils.ipPrefixToString(ipAddress);
        String fixedIp = getIpAddressFromPrefix(fixedIpPrefix);
        createVpnPortFixedIp(dataBroker, vpnName, portName, fixedIp, macAddress);
    }

    public static void createVpnPortFixedIp(DataBroker dataBroker, String vpnName, String portName, IpAddress ipAddress,
            MacAddress macAddress) {
        String fixedIp = MefUtils.ipAddressToString(ipAddress);
        createVpnPortFixedIp(dataBroker, vpnName, portName, fixedIp, macAddress);
    }

    public static void createVpnPortFixedIp(DataBroker dataBroker, String vpnName, String portName, String fixedIp,
            MacAddress macAddress) {
        synchronized ((vpnName + fixedIp).intern()) {
            InstanceIdentifier<VpnPortipToPort> id = buildVpnPortipToPortIdentifier(vpnName, fixedIp);
            VpnPortipToPortBuilder builder = new VpnPortipToPortBuilder()
                    .setKey(new VpnPortipToPortKey(fixedIp, vpnName)).setVpnName(vpnName).setPortFixedip(fixedIp)
                    .setPortName(portName).setMacAddress(macAddress.getValue()).setSubnetIp(true).setConfig(true)
                    .setLearnt(false);
            MDSALUtil.syncWrite(dataBroker, LogicalDatastoreType.OPERATIONAL, id, builder.build());
            logger.debug(
                    "Interface to fixedIp added: {}, vpn {}, interface {}, mac {} added to " + "VpnPortipToPort DS",
                    fixedIp, vpnName, portName, macAddress);
        }
    }

    static InstanceIdentifier<NetworkMap> buildNetworkMapIdentifier(Uuid networkId) {
        InstanceIdentifier<NetworkMap> id = InstanceIdentifier.builder(NetworkMaps.class)
                .child(NetworkMap.class, new NetworkMapKey(networkId)).build();
        return id;
    }

    private static void createSubnetToNetworkMapping(DataBroker dataBroker, Uuid subnetId, Uuid networkId) {
        InstanceIdentifier<NetworkMap> networkMapIdentifier = buildNetworkMapIdentifier(networkId);
        Optional<NetworkMap> optionalNetworkMap = MDSALUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION,
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

    private static InstanceIdentifier<VpnPortipToPort> buildVpnPortipToPortIdentifier(String vpnName, String fixedIp) {
        InstanceIdentifier<VpnPortipToPort> id = InstanceIdentifier.builder(NeutronVpnPortipPortData.class)
                .child(VpnPortipToPort.class, new VpnPortipToPortKey(fixedIp, vpnName)).build();
        return id;
    }

    public static void addDirectSubnetToVpn(DataBroker dataBroker,
            final NotificationPublishService notificationPublishService, String vpnName, String subnetName,
            IpPrefix subnetIpPrefix, String interfaceName) {
        InstanceIdentifier<ElanInstance> elanIdentifierId = InstanceIdentifier.builder(ElanInstances.class)
                .child(ElanInstance.class, new ElanInstanceKey(subnetName)).build();
        Optional<ElanInstance> elanInstance = MDSALUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                elanIdentifierId);
        if (!elanInstance.isPresent()) {
            logger.error("Trying to add invalid elan {} to vpn {}", subnetName, vpnName);
            return;
        }
        Long elanTag = elanInstance.get().getElanTag();

        Uuid subnetId = new Uuid(subnetName);
        logger.info("Adding subnet {} {} to elan map", subnetId);
        createSubnetToNetworkMapping(dataBroker, subnetId, subnetId);

        String subnetIp = getSubnetFromPrefix(MefUtils.ipPrefixToString(subnetIpPrefix));
        logger.info("Adding subnet {} {} to vpn {}", subnetName, subnetIp, vpnName);
        updateSubnetNode(dataBroker, new Uuid(vpnName), subnetId, subnetIp);

        logger.info("Adding port {} to subnet {}", interfaceName, subnetName);
        updateSubnetmapNodeWithPorts(dataBroker, subnetId, new Uuid(interfaceName));

        logger.info("Publish subnet {}", subnetName);
        publishSubnetAddNotification(notificationPublishService, subnetId, subnetIp, vpnName, elanTag);
        logger.info("Finished Working on subnet {}", subnetName);

    }

    protected static void updateSubnetNode(DataBroker dataBroker, Uuid vpnId, Uuid subnetId, String subnetIp) {
        Subnetmap subnetmap = null;
        SubnetmapBuilder builder = null;
        InstanceIdentifier<Subnetmap> id = InstanceIdentifier.builder(Subnetmaps.class)
                .child(Subnetmap.class, new SubnetmapKey(subnetId)).build();

        synchronized (subnetId.getValue().intern()) {
            Optional<Subnetmap> sn = MDSALUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION, id);
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

    private static void updateSubnetmapNodeWithPorts(DataBroker dataBroker, Uuid subnetId, Uuid portId) {
        Subnetmap subnetmap = null;
        InstanceIdentifier<Subnetmap> id = InstanceIdentifier.builder(Subnetmaps.class)
                .child(Subnetmap.class, new SubnetmapKey(subnetId)).build();
        synchronized (subnetId.getValue().intern()) {
            Optional<Subnetmap> sn = MDSALUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION, id);
            if (sn.isPresent()) {
                SubnetmapBuilder builder = new SubnetmapBuilder(sn.get());
                if (null != portId) {
                    List<Uuid> portList = builder.getPortList();
                    if (null == portList) {
                        portList = new ArrayList<>();
                    }
                    portList.add(portId);
                    builder.setPortList(portList);
                    logger.debug("Updating subnetmap node {} with port {}", subnetId.getValue(), portId.getValue());
                }
                subnetmap = builder.build();
                MDSALUtil.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION, id, subnetmap);
            } else {
                logger.error("Trying to update non-existing subnetmap node {} ", subnetId.getValue());
            }
        }
    }

    private static void publishSubnetAddNotification(final NotificationPublishService notificationPublishService,
            Uuid subnetId, String subnetIp, String vpnName, Long elanTag) {
        SubnetAddedToVpnBuilder builder = new SubnetAddedToVpnBuilder();

        logger.info("publish notification called");

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

    private static String getIpAddressFromPrefix(String prefix) {
        return prefix.split(IP_MUSK_SEPARATOR)[0];
    }

    private static String getMaskFromPrefix(String prefix) {
        return prefix.split(IP_MUSK_SEPARATOR)[1];
    }

    private static String getSubnetFromPrefix(String prefix) {
        SubnetInfo subnet = new SubnetUtils(prefix).getInfo();
        return subnet.getNetworkAddress() + IP_MUSK_SEPARATOR + getMaskFromPrefix(prefix);
    }

    private static String getPrefixFromSubnet(String prefix) {
        String myAddress = getIpAddressFromPrefix(prefix);
        return myAddress + IP_ADDR_SUFFIX;
    }

    public static String getElanNameForVpnPort(String portName) {
        return getUUidFromString(ELAN_PREFIX + portName);
    }

    public static String getInterfaceNameForVlan(String interfaceName, Integer vlan) {
        final StringBuilder s = new StringBuilder();
        s.append(interfaceName);
        if (vlan != null) {
            s.append(VLAN_SEPARATOR).append(vlan);
        }
        s.append(TRUNK_SUFFIX);
        return getUUidFromString(s.toString());
    }

    public static String getUUidFromString(String key) {
        return java.util.UUID.nameUUIDFromBytes(key.getBytes()).toString();
    }

    public static MacAddress resolveGwMac(DataBroker dataBroker, OdlArputilService arpUtilService, String vpnName,
            IpPrefix srcIpPrefix, IpAddress dstIpAddress, String interf) {

        String srcTpAddressStr = getIpAddressFromPrefix(MefUtils.ipPrefixToString(srcIpPrefix));
        IpAddress srcIpAddress = new IpAddress(srcTpAddressStr.toCharArray());

        if (srcIpAddress == null || dstIpAddress == null) {
            logger.error("Can't send ARP to srcIp {} dstIp {}", srcIpAddress, dstIpAddress);
            throw new RuntimeException("Can't send ARP for dstIp " + dstIpAddress);
        }

        MacAddress macAddress = null;
        int retries = MaxRetries;
        while (retries > 0 && macAddress == null) {
            logger.info("Sending ARP request to dstIp {} take {}", dstIpAddress, MaxRetries - retries + 1);
            sendArpRequest(arpUtilService, srcIpAddress, dstIpAddress, interf);
            macAddress = waitForArpReplyProcessing(dataBroker, vpnName, dstIpAddress, MaxRetries);
            retries--;
        }
        return macAddress;
    }

    private static void sendArpRequest(OdlArputilService arpUtilService, IpAddress srcIpAddress, IpAddress dstIpAddress,
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

    public static MacAddress waitForArpReplyProcessing(DataBroker dataBroker, String vpnName, IpAddress dstIpAddress,
            int retries) {
        while (retries > 0) {
            logger.info("Waiting for ARP reply from dstIp {} take {}", dstIpAddress, MaxRetries - retries + 1);
            InstanceIdentifier<VpnPortipToPort> optionalPortIpId = buildVpnPortipToPortIdentifier(vpnName,
                    MefUtils.ipAddressToString(dstIpAddress));
            Optional<VpnPortipToPort> optionalPortIp = MdsalUtils.read(dataBroker, LogicalDatastoreType.OPERATIONAL,
                    optionalPortIpId);

            if (optionalPortIp.isPresent()) {
                return new MacAddress(optionalPortIp.get().getMacAddress());
            } else {
                sleep();
            }
            retries--;
        }
        return null;
    }

    private static void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }
}
