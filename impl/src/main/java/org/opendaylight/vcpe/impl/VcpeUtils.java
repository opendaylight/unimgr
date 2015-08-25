/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.vcpe.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes.VlanMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vcpe.rev150622.Unis;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vcpe.rev150622.unis.Uni;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VcpeUtils {

    private static final Logger LOG = LoggerFactory.getLogger(VcpeUtils.class);

    public static final Optional<Node> readNode(DataBroker dataBroker,
            InstanceIdentifier<Node> nodeIid) {
        ReadTransaction read = dataBroker.newReadOnlyTransaction();
        CheckedFuture<Optional<Node>, ReadFailedException> nodeFuture = read
                .read(LogicalDatastoreType.OPERATIONAL, nodeIid);
        Optional<Node> nodeOptional;
        try {
            nodeOptional = nodeFuture.get();
            return nodeOptional;
        } catch (InterruptedException e) {
            return Optional.absent();
        } catch (ExecutionException e) {
            return Optional.absent();
        }
    }

    public static final Optional<Uni> readUniNode(DataBroker dataBroker,
            InstanceIdentifier<Uni> nodeIid) {
        ReadTransaction read = dataBroker.newReadOnlyTransaction();
        CheckedFuture<Optional<Uni>, ReadFailedException> nodeFuture = read
                .read(LogicalDatastoreType.OPERATIONAL, nodeIid);
        Optional<Uni> nodeOptional;
        try {
            nodeOptional = nodeFuture.get();
            return nodeOptional;
        } catch (InterruptedException e) {
            return Optional.absent();
        } catch (ExecutionException e) {
            return Optional.absent();
        }
    }

    // This might not scale up.
    public static final Unis readUnisFromStore(DataBroker dataBroker,
            LogicalDatastoreType storetype) {
        ReadOnlyTransaction read = dataBroker.newReadOnlyTransaction();
        Optional<Unis> dataObject = null;
        try {
            dataObject = read.read(storetype,
                    VcpeMapper.getUnisIid()).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error retrieving the UNIs from the Configuration tree.");
        }
        if ((dataObject != null) && (dataObject.get() != null)) {
            read.close();
            return dataObject.get();
        } else {
            read.close();
            return null;
        }
    }

    public static void copyUniToDataStore(DataBroker dataBroker, Uni uni,
            LogicalDatastoreType dataStoreType) {
        WriteTransaction write = dataBroker.newWriteOnlyTransaction();
        write.put(dataStoreType, VcpeMapper.getUniIid(uni), uni);
        write.submit();
    }

    public static OvsdbBridgeAugmentation createOvsdbBridgeAugmentation(Uni uni) {
        NodeId ovsdbNodeId = uni.getOvsdbNodeId();
        InstanceIdentifier<Node> ovsdbNodeIid;
        if (ovsdbNodeId == null || ovsdbNodeId.getValue().isEmpty()) {
            ovsdbNodeIid = VcpeMapper.getOvsdbNodeIID(uni.getIpAddress());
        } else {
            ovsdbNodeIid = VcpeMapper.getOvsdbNodeIID(ovsdbNodeId);
        }
        OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(ovsdbNodeIid);
        UUID bridgeUuid = UUID.randomUUID();
        OvsdbBridgeAugmentation ovsdbBridge = new OvsdbBridgeAugmentationBuilder()
                    .setBridgeName(new OvsdbBridgeName(VcpeConstants.DEFAULT_BRIDGE_NAME))
                    .setManagedBy(ovsdbNodeRef)
                    .setBridgeUuid(new Uuid(bridgeUuid.toString()))
                    .build();
        return ovsdbBridge;
    }

    public static OvsdbNodeAugmentation createOvsdbNodeAugmentation(Uni uni) {
        ConnectionInfo connectionInfos = new ConnectionInfoBuilder()
                .setRemoteIp(uni.getIpAddress())
                .setRemotePort(new PortNumber(VcpeConstants.OVSDB_PORT))
                .build();
        OvsdbNodeAugmentation ovsdbNode = new OvsdbNodeAugmentationBuilder()
                .setConnectionInfo(connectionInfos).build();
        return ovsdbNode;
    }

    public static OvsdbTerminationPointAugmentation createOvsdbTerminationPointAugmentation(Uni uni) {
        // we will use nodeId to set interface port id
        VlanId vlanID = new VlanId(1);
        OvsdbTerminationPointAugmentation terminationPoint = new OvsdbTerminationPointAugmentationBuilder()
                                                                     .setName(VcpeConstants.DEFAULT_INTERNAL_IFACE)
                                                                     .setVlanTag(vlanID)
                                                                     .setVlanMode(VlanMode.Access)
                                                                     .build();
        return terminationPoint;
    }

    public static IpAddress getLocalIp() {
        String ip;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
            Ipv4Address ipv4 = new Ipv4Address(ip);
            IpAddress ipAddress = new IpAddress(ipv4);
            return ipAddress;
        } catch (UnknownHostException e) {
            LOG.info("Unable to retrieve controller's ip address, using loopback.");
        }
        return new IpAddress(VcpeConstants.LOCAL_IP);
    }

    public static ConnectionInfo getConnectionInfo(DataBroker dataBroker, NodeId ovsdbNodeId) {
        InstanceIdentifier<Node> nodeIid = VcpeMapper.getOvsdbNodeIID(ovsdbNodeId);
        Optional<Node> node = readNode(dataBroker, nodeIid);
        if (node.isPresent()) {
            Node ovsdbNode = node.get();
            OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
            ConnectionInfo connectionInfo = ovsdbNodeAugmentation.getConnectionInfo();
            return connectionInfo;
        } else {
            return null;
        }
    }

    public static List<ControllerEntry> createControllerEntries(String targetString) {
        List<ControllerEntry> controllerEntries = new ArrayList<ControllerEntry>();
        ControllerEntryBuilder controllerEntryBuilder = new ControllerEntryBuilder();
        controllerEntryBuilder.setTarget(new Uri(targetString));
        controllerEntries.add(controllerEntryBuilder.build());
        return controllerEntries;
    }

    public static List<ProtocolEntry> createMdsalProtocols() {
        List<ProtocolEntry> protocolList = new ArrayList<ProtocolEntry>();
        ImmutableBiMap<String, Class<? extends OvsdbBridgeProtocolBase>> mapper =
                VcpeConstants.OVSDB_PROTOCOL_MAP.inverse();
        protocolList.add(new ProtocolEntryBuilder().
                setProtocol((Class<? extends OvsdbBridgeProtocolBase>) mapper.get("OpenFlow13")).build());
        return protocolList;
    }

    public static void createOvsdbNode(DataBroker dataBroker, NodeId ovsdbNodeId, Uni uni) {
        InstanceIdentifier<Node> ovsdbNodeIid = VcpeMapper
                .getOvsdbNodeIID(uni.getIpAddress());
        try {
            NodeKey ovsdbNodeKey = new NodeKey(ovsdbNodeId);
            Node nodeData = new NodeBuilder().setNodeId(ovsdbNodeId)
                    .setKey(ovsdbNodeKey)
                    .addAugmentation(OvsdbNodeAugmentation.class, VcpeUtils.createOvsdbNodeAugmentation(uni))
                    .build();
            // Submit the node to the datastore
            WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            transaction.put(LogicalDatastoreType.CONFIGURATION, ovsdbNodeIid,
                    nodeData);
            transaction.submit();
            LOG.info("Created and submitted a new OVSDB node {}",
                    nodeData.getNodeId());
        } catch (Exception e) {
            LOG.error("Exception while creating OvsdbNodeAugmentation, "
                    + "Uni is null. Node Id: {}", ovsdbNodeId);
        }
    }

    public static void createBridgeNode(DataBroker dataBroker, NodeId ovsdbNodeId, Uni uni, String bridgeName) {
        LOG.info("Creating a bridge on node {}", ovsdbNodeId);
        InstanceIdentifier<Node> ovsdbNodeIid = VcpeMapper
                .getOvsdbNodeIID(uni.getIpAddress());
        ConnectionInfo connectionInfo = VcpeUtils.getConnectionInfo(dataBroker, ovsdbNodeId);
        if (connectionInfo != null) {
            NodeBuilder bridgeNodeBuilder = new NodeBuilder();
            InstanceIdentifier<Node> bridgeIid = VcpeMapper
                    .getOvsdbBridgeNodeIID(ovsdbNodeId, bridgeName);
            NodeId bridgeNodeId = new NodeId(ovsdbNodeId
                    + VcpeConstants.DEFAULT_BRIDGE_NODE_ID_SUFFIX + bridgeName);
            bridgeNodeBuilder.setNodeId(bridgeNodeId);
            OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
            // String target = VcpeUtils.getLocalIp().toString();
            // ovsdbBridgeAugmentationBuilder.setControllerEntry(VcpeUtils.createControllerEntries(target));
            ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(
                    bridgeName));
            ovsdbBridgeAugmentationBuilder.setProtocolEntry(VcpeUtils
                    .createMdsalProtocols());
            OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(ovsdbNodeIid);
            ovsdbBridgeAugmentationBuilder.setManagedBy(ovsdbNodeRef);
            bridgeNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class,
                    ovsdbBridgeAugmentationBuilder.build());
            WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            transaction.put(LogicalDatastoreType.CONFIGURATION, bridgeIid,
                    bridgeNodeBuilder.build());
            transaction.submit();
        } else {
            LOG.error("The OVSDB node is not connected {}", ovsdbNodeId);
        }
    }

    public static void createTerminationPointNode(DataBroker dataBroker, Uni uni,
            Node bridgeNode, String bridgeName, String portName, String type) {
        InstanceIdentifier<TerminationPoint> tpIid = VcpeMapper
                .createTerminationPointInstanceIdentifier(bridgeNode, portName);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder =
                new OvsdbTerminationPointAugmentationBuilder();
        tpAugmentationBuilder.setName(portName);
        if (type != null) {
            tpAugmentationBuilder.setInterfaceType(VcpeConstants.OVSDB_INTERFACE_TYPE_MAP.get(type));
        }
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION,tpIid,tpBuilder.build());
        transaction.submit();
    }

    public static void createGreTunnel(DataBroker dataBroker, Uni source, Uni destination,
            Node bridgeNode, String bridgeName, String portName) {
        InstanceIdentifier<TerminationPoint> tpIid = VcpeMapper
                .createTerminationPointInstanceIdentifier(bridgeNode, portName);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder =
                new OvsdbTerminationPointAugmentationBuilder();
        tpAugmentationBuilder.setName(portName);
        ArrayList<Options> options = Lists.newArrayList();
        OptionsKey optionKey = new OptionsKey("remote_ip");
        Options destinationIp = new OptionsBuilder()
                                        .setOption(destination.getIpAddress().getIpv4Address().getValue())
                                        .setKey(optionKey)
                                        .setValue(destination.getIpAddress().getIpv4Address().getValue())
                                        .build();
        options.add(destinationIp);
        tpAugmentationBuilder.setOptions(options);
        tpAugmentationBuilder.setInterfaceType(VcpeConstants.OVSDB_INTERFACE_TYPE_MAP.get("gre"));
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION,tpIid,tpBuilder.build());
        transaction.submit();
    }

    public static <T extends DataObject> Set<InstanceIdentifier<T>> extractRemoved(
            AsyncDataChangeEvent<InstanceIdentifier<?>,DataObject> changes,Class<T> klazz) {
        Set<InstanceIdentifier<T>> result = new HashSet<InstanceIdentifier<T>>();
        if (changes != null && changes.getRemovedPaths() != null) {
            for (InstanceIdentifier<?> iid : changes.getRemovedPaths()) {
                if (iid.getTargetType().equals(klazz)) {
                    @SuppressWarnings("unchecked") // Actually checked above
                    InstanceIdentifier<T> iidn = (InstanceIdentifier<T>)iid;
                    result.add(iidn);
                }
            }
        }
        return result;
    }

    public static <T extends DataObject> Map<InstanceIdentifier<T>,T> extractOriginal(
            AsyncDataChangeEvent<InstanceIdentifier<?>,DataObject> changes,Class<T> klazz) {
        return extract(changes.getOriginalData(),klazz);
    }

    public static <T extends DataObject> Map<InstanceIdentifier<T>,T> extract(
            Map<InstanceIdentifier<?>, DataObject> changes, Class<T> klazz) {
        Map<InstanceIdentifier<T>,T> result = new HashMap<InstanceIdentifier<T>,T>();
        if (changes != null && changes.entrySet() != null) {
            for (Entry<InstanceIdentifier<?>, DataObject> created : changes.entrySet()) {
                if (klazz.isInstance(created.getValue())) {
                    @SuppressWarnings("unchecked")
                    T value = (T) created.getValue();
                    Class<?> type = created.getKey().getTargetType();
                    if (type.equals(klazz)) {
                        @SuppressWarnings("unchecked") // Actually checked above
                        InstanceIdentifier<T> iid = (InstanceIdentifier<T>) created.getKey();
                        result.put(iid, value);
                    }
                }
            }
        }
        return result;
    }
}
