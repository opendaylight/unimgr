/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Evc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Uni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDestKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSourceKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;

public class UnimgrUtils {

    private static final Logger LOG = LoggerFactory.getLogger(UnimgrUtils.class);

    public static void createBridgeNode(DataBroker dataBroker,
                                        Node ovsdbNode,
                                        UniAugmentation uni,
                                        String bridgeName) {
        LOG.info("Creating a bridge on node {}", ovsdbNode.getNodeId().getValue());
        InstanceIdentifier<Node> ovsdbNodeIid = uni.getOvsdbNodeRef().getValue().firstIdentifierOf(Node.class);
        if (ovsdbNodeIid != null) {
            NodeBuilder bridgeNodeBuilder = new NodeBuilder();
            InstanceIdentifier<Node> bridgeIid = UnimgrMapper.createOvsdbBridgeNodeIid(ovsdbNode,
                                                                                       bridgeName);
            NodeId bridgeNodeId = new NodeId(ovsdbNode.getNodeId()
                                           + UnimgrConstants.DEFAULT_BRIDGE_NODE_ID_SUFFIX
                                           + bridgeName);
            bridgeNodeBuilder.setNodeId(bridgeNodeId);
            OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
            ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(bridgeName));
            ovsdbBridgeAugmentationBuilder.setProtocolEntry(UnimgrUtils.createMdsalProtocols());
            OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(ovsdbNodeIid);
            ovsdbBridgeAugmentationBuilder.setManagedBy(ovsdbNodeRef);
            bridgeNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, ovsdbBridgeAugmentationBuilder.build());
            WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            transaction.put(LogicalDatastoreType.CONFIGURATION, bridgeIid, bridgeNodeBuilder.build());
            transaction.submit();
        } else {
            LOG.info("OvsdbNodeRef is null");
        }
    }

    public static void createBridgeNode(DataBroker dataBroker,
                                        InstanceIdentifier<Node> ovsdbNodeIid,
                                        UniAugmentation uni,
                                        String bridgeName) {
        LOG.info("Creating a bridge on node {}", ovsdbNodeIid);
        if (ovsdbNodeIid != null) {
            NodeBuilder bridgeNodeBuilder = new NodeBuilder();
            Optional<Node> optionalOvsdbNode = UnimgrUtils.readNode(dataBroker,
                                                                    LogicalDatastoreType.OPERATIONAL,
                                                                    ovsdbNodeIid);
            if (optionalOvsdbNode.isPresent()) {
                Node ovsdbNode = optionalOvsdbNode.get();
                InstanceIdentifier<Node> bridgeIid = UnimgrMapper.createOvsdbBridgeNodeIid(ovsdbNode,
                                                                                           bridgeName);
                NodeId bridgeNodeId = new NodeId(ovsdbNode.getNodeId().getValue()
                                               + UnimgrConstants.DEFAULT_BRIDGE_NODE_ID_SUFFIX
                                               + bridgeName);
                bridgeNodeBuilder.setNodeId(bridgeNodeId);
                OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
                ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(bridgeName));
                ovsdbBridgeAugmentationBuilder.setProtocolEntry(UnimgrUtils.createMdsalProtocols());
                OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(ovsdbNodeIid);
                ovsdbBridgeAugmentationBuilder.setManagedBy(ovsdbNodeRef);
                bridgeNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class,
                                                  ovsdbBridgeAugmentationBuilder.build());
                WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
                transaction.put(LogicalDatastoreType.CONFIGURATION,
                                bridgeIid,
                                bridgeNodeBuilder.build());
                transaction.submit();
            }
        } else {
            LOG.info("OvsdbNodeRef is null");
        }
    }

    public static List<ControllerEntry> createControllerEntries(String targetString) {
        List<ControllerEntry> controllerEntries = new ArrayList<ControllerEntry>();
        ControllerEntryBuilder controllerEntryBuilder = new ControllerEntryBuilder();
        controllerEntryBuilder.setTarget(new Uri(targetString));
        controllerEntries.add(controllerEntryBuilder.build());
        return controllerEntries;
    }

    public static void createGreTunnel(DataBroker dataBroker,
                                       Uni source,
                                       Uni destination,
                                       Node bridgeNode,
                                       String bridgeName,
                                       String portName) {
        InstanceIdentifier<TerminationPoint> tpIid =
                                                 UnimgrMapper.getTerminationPointIid(bridgeNode,
                                                                                     portName);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder =
                                                     new OvsdbTerminationPointAugmentationBuilder();
        tpAugmentationBuilder.setName(portName);
        ArrayList<Options> options = Lists.newArrayList();
        OptionsKey optionKey = new OptionsKey("remote_ip");
        Options destinationIp = new OptionsBuilder()
                                        .setOption(destination.getIpAddress().getIpv4Address().getValue())
                                        .setKey(optionKey).setValue(destination.getIpAddress().getIpv4Address().getValue())
                                        .build();
        options.add(destinationIp);
        tpAugmentationBuilder.setOptions(options);
        tpAugmentationBuilder.setInterfaceType(SouthboundConstants.OVSDB_INTERFACE_TYPE_MAP.get("gre"));
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION,
                        tpIid,
                        tpBuilder.build());
        transaction.submit();
    }

    public static List<ProtocolEntry> createMdsalProtocols() {
        List<ProtocolEntry> protocolList = new ArrayList<ProtocolEntry>();
        ImmutableBiMap<String, Class<? extends OvsdbBridgeProtocolBase>> mapper =
                SouthboundConstants.OVSDB_PROTOCOL_MAP.inverse();
        protocolList.add(new ProtocolEntryBuilder().
                setProtocol((Class<? extends OvsdbBridgeProtocolBase>) mapper.get("OpenFlow13")).build());
        return protocolList;
    }

    public static OvsdbBridgeAugmentation createOvsdbBridgeAugmentation(Uni uni) throws Exception {
        OvsdbNodeRef ovsdbNodeRef = uni.getOvsdbNodeRef();
        if (ovsdbNodeRef != null && ovsdbNodeRef.getValue() != null) {
            UUID bridgeUuid = UUID.randomUUID();
            OvsdbBridgeAugmentation ovsdbBridge = new OvsdbBridgeAugmentationBuilder()
                                                        .setBridgeName(
                                                                new OvsdbBridgeName(UnimgrConstants.DEFAULT_BRIDGE_NAME))
                                                        .setManagedBy(ovsdbNodeRef)
                                                        .setBridgeUuid(
                                                                new Uuid(bridgeUuid.toString()))
                                                        .build();
            return ovsdbBridge;
        } else {
            throw new Exception("Ovsdb Node Reference does not exist !");
        }
    }

    public static void createOvsdbNode(DataBroker dataBroker,
                                       NodeId ovsdbNodeId,
                                       Uni uni) {
        InstanceIdentifier<Node> ovsdbNodeIid = UnimgrMapper.getOvsdbNodeIid(uni.getIpAddress());
        try {
            NodeKey ovsdbNodeKey = new NodeKey(ovsdbNodeId);
            Node nodeData = new NodeBuilder()
                                    .setNodeId(ovsdbNodeId)
                                    .setKey(ovsdbNodeKey)
                                    .addAugmentation(OvsdbNodeAugmentation.class,
                                                     UnimgrUtils.createOvsdbNodeAugmentation(uni))
                                    .build();
            // Submit the node to the datastore
            WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            transaction.put(LogicalDatastoreType.CONFIGURATION, ovsdbNodeIid, nodeData);
            transaction.submit();
            LOG.info("Created and submitted a new OVSDB node {}", nodeData.getNodeId());
        } catch (Exception e) {
            LOG.error("Exception while creating OvsdbNodeAugmentation, " + "Uni is null. Node Id: {}", ovsdbNodeId);
        }
    }

    public static Node createOvsdbNode(DataBroker dataBroker,
                                       UniAugmentation uni) {
        NodeId ovsdbNodeId = new NodeId(createOvsdbNodeId(uni.getIpAddress()));
        try {
            InstanceIdentifier<Node> ovsdbNodeIid = UnimgrMapper.getOvsdbNodeIid(ovsdbNodeId);
            NodeKey ovsdbNodeKey = new NodeKey(ovsdbNodeId);
            Node nodeData = new NodeBuilder()
                                    .setNodeId(ovsdbNodeId)
                                    .setKey(ovsdbNodeKey)
                                    .addAugmentation(OvsdbNodeAugmentation.class,
                                                     UnimgrUtils.createOvsdbNodeAugmentation(uni))
                                    .build();
            // Submit the node to the datastore
            WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            transaction.put(LogicalDatastoreType.CONFIGURATION, ovsdbNodeIid, nodeData);
            transaction.submit();
            LOG.info("Created and submitted a new OVSDB node {}", nodeData.getNodeId());
            return nodeData;
        } catch (Exception e) {
            LOG.error("Exception while creating OvsdbNodeAugmentation, " + "Uni is null. Node Id: {}", ovsdbNodeId);
        }
        return null;
    }

    public static OvsdbNodeAugmentation createOvsdbNodeAugmentation(Uni uni) {
        ConnectionInfo connectionInfos = new ConnectionInfoBuilder()
                                                .setRemoteIp(uni.getIpAddress())
                                                .setRemotePort(new PortNumber(UnimgrConstants.OVSDB_PORT))
                                                .build();
        OvsdbNodeAugmentation ovsdbNode = new OvsdbNodeAugmentationBuilder()
                                                .setConnectionInfo(connectionInfos).build();
        return ovsdbNode;
    }

    public static NodeId createOvsdbNodeId(IpAddress ipAddress) {
        String nodeId = UnimgrConstants.OVSDB_PREFIX
                        + ipAddress.getIpv4Address().getValue().toString()
                        + ":"
                        + UnimgrConstants.OVSDB_PORT;
        return new NodeId(nodeId);
    }

    public static OvsdbTerminationPointAugmentation createOvsdbTerminationPointAugmentation(Uni uni) {
        // we will use nodeId to set interface port id
        VlanId vlanID = new VlanId(1);
        OvsdbTerminationPointAugmentation terminationPoint = new OvsdbTerminationPointAugmentationBuilder()
                                                                     .setName(UnimgrConstants.DEFAULT_INTERNAL_IFACE)
                                                                     .setVlanTag(vlanID)
                                                                     .setVlanMode(VlanMode.Access)
                                                                     .build();
        return terminationPoint;
    }

    public static void createTerminationPointNode(DataBroker dataBroker,
                                                  Uni uni,
                                                  Node bridgeNode,
                                                  String bridgeName,
                                                  String portName,
                                                  String type) {
        InstanceIdentifier<TerminationPoint> tpIid = UnimgrMapper
                                                        .getTerminationPointIid(bridgeNode,
                                                                                portName);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder =
                                                     new OvsdbTerminationPointAugmentationBuilder();
        tpAugmentationBuilder.setName(portName);
        if (type != null) {
            tpAugmentationBuilder.setInterfaceType(SouthboundConstants.OVSDB_INTERFACE_TYPE_MAP.get(type));
        }
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class,
                                  tpAugmentationBuilder.build());
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION,
                        tpIid,
                        tpBuilder.build());
        transaction.submit();
    }

    public <D extends org.opendaylight.yangtools.yang.binding.DataObject> boolean delete(
            DataBroker dataBroker,
            final LogicalDatastoreType store,
            final InstanceIdentifier<D> path)  {
        boolean result = false;
        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.delete(store, path);
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        try {
            future.checkedGet();
            result = true;
        } catch (TransactionCommitFailedException e) {
            LOG.warn("Failed to delete {} ", path, e);
        }
        return result;
    }

    public static CheckedFuture<Void,
                                TransactionCommitFailedException>
                                deleteTerminationPoint(DataBroker dataBroker,
                                                       TerminationPoint terminationPoint,
                                                       Node ovsdbNode) {
        InstanceIdentifier<TerminationPoint> terminationPointPath =
                                                 InstanceIdentifier
                                                     .create(NetworkTopology.class)
                                                     .child(Topology.class,
                                                             new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                                                     .child(Node.class,
                                                            ovsdbNode.getKey())
                                                     .child(TerminationPoint.class,
                                                            terminationPoint.getKey());
        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.delete(LogicalDatastoreType.CONFIGURATION, terminationPointPath);
        transaction.delete(LogicalDatastoreType.OPERATIONAL, terminationPointPath);
        transaction.submit();
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        return future;
    }

    public static CheckedFuture<Void,
                                TransactionCommitFailedException>
                                deleteNode(DataBroker dataBroker,
                                           InstanceIdentifier<?> genericNode,
                                           LogicalDatastoreType store) {
        LOG.info("Received a request to delete node {}", genericNode);
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.delete(store, genericNode);
        return transaction.submit();
    }

    public static void deletePath(DataBroker dataBroker, LogicalDatastoreType dataStoreType, InstanceIdentifier<?> iid) {
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.delete(dataStoreType, iid);
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        try {
            future.checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.warn("Failed to delete iidNode {} {}", e.getMessage(), iid);
        }
    }

    public static void deletePath(DataBroker dataBroker, InstanceIdentifier<?> iid) {
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.delete(LogicalDatastoreType.OPERATIONAL, iid);
        transaction.delete(LogicalDatastoreType.CONFIGURATION, iid);
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        try {
            future.checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.warn("Failed to delete iidNode {} {}", e.getMessage(), iid);
        }
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

    public static <T extends DataObject> Map<InstanceIdentifier<T>,T> extractOriginal(
            AsyncDataChangeEvent<InstanceIdentifier<?>,DataObject> changes,Class<T> klazz) {
        return extract(changes.getOriginalData(),klazz);
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

    public static Optional<Node> findOvsdbNode(DataBroker dataBroker,
                                               UniAugmentation uni) {
        List<Node> ovsdbNodes = getOvsdbNodes(dataBroker);
        Optional<Node> optionalOvsdb;
        if (!ovsdbNodes.isEmpty()) {
            for (Node ovsdbNode : ovsdbNodes) {
                OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode
                                                                  .getAugmentation(OvsdbNodeAugmentation.class);
                if (ovsdbNodeAugmentation.getConnectionInfo()
                                         .getRemoteIp()
                                         .getIpv4Address()
                        .equals(uni.getIpAddress().getIpv4Address())) {
                    LOG.info("Found ovsdb node");
                    optionalOvsdb = Optional.of(ovsdbNode);
                    return optionalOvsdb;
                }
            }
        }
        return Optional.absent();
    }

    public static Optional<Node> findUniNode(DataBroker dataBroker,
                                             IpAddress ipAddress) {
        List<Node> uniNodes = getUniNodes(dataBroker);
        if (!uniNodes.isEmpty()) {
            for (Node uniNode : uniNodes) {
                UniAugmentation uniAugmentation = uniNode.getAugmentation(UniAugmentation.class);
                if (uniAugmentation.getIpAddress().equals(ipAddress)) {
                    LOG.info("Found Uni node");
                    return Optional.of(uniNode);
                }
            }
        }
        return Optional.absent();
    }

    public static ConnectionInfo getConnectionInfo(DataBroker dataBroker,
                                                   NodeId ovsdbNodeId) {
        InstanceIdentifier<Node> nodeIid = UnimgrMapper.getOvsdbNodeIid(ovsdbNodeId);
        Optional<Node> node = readNode(dataBroker,
                                       LogicalDatastoreType.OPERATIONAL,
                                       nodeIid);
        if (node.isPresent()) {
            Node ovsdbNode = node.get();
            OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode
                                                              .getAugmentation(OvsdbNodeAugmentation.class);
            ConnectionInfo connectionInfo = ovsdbNodeAugmentation.getConnectionInfo();
            return connectionInfo;
        } else {
            return null;
        }
    }

    public static List<Link> getEvcLinks(DataBroker dataBroker,
                                         IpAddress ipAddress) {
        List<Link> evcLinks = new ArrayList<>();
        InstanceIdentifier<Topology> evcTopology = UnimgrMapper.getEvcTopologyIid();
        Topology topology = UnimgrUtils.read(dataBroker,
                                             LogicalDatastoreType.OPERATIONAL,
                                             evcTopology);
        if (topology != null && topology.getLink() != null) {
            for (Link link : topology.getLink()) {
                EvcAugmentation evcAugmentation = link.getAugmentation(EvcAugmentation.class);
                if (evcAugmentation != null) {
                    evcLinks.add(link);
                }
            }
        }
        return evcLinks;
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
        return new IpAddress(UnimgrConstants.LOCAL_IP);
    }

    public static List<Node> getOvsdbNodes(DataBroker dataBroker) {
        List<Node> ovsdbNodes = new ArrayList<>();
        InstanceIdentifier<Topology> ovsdbTopoIdentifier = UnimgrMapper.getOvsdbTopologyIid();
        Topology topology = UnimgrUtils.read(dataBroker,
                                             LogicalDatastoreType.OPERATIONAL,
                                             ovsdbTopoIdentifier);
        if (topology != null && topology.getNode() != null) {
            for (Node node : topology.getNode()) {
                OvsdbNodeAugmentation ovsdbNodeAugmentation = node.getAugmentation(OvsdbNodeAugmentation.class);
                if (ovsdbNodeAugmentation != null) {
                    ovsdbNodes.add(node);
                }
            }
        }
        return ovsdbNodes;
    }

    public static List<Node> getUniNodes(DataBroker dataBroker) {
        List<Node> uniNodes = new ArrayList<>();
        InstanceIdentifier<Topology> topologyInstanceIdentifier = UnimgrMapper.getUniTopologyIid();
        Topology topology = read(dataBroker,
                                 LogicalDatastoreType.CONFIGURATION,
                                 topologyInstanceIdentifier);
        if (topology != null && topology.getNode() != null) {
            for (Node node : topology.getNode()) {
                UniAugmentation uniAugmentation = node.getAugmentation(UniAugmentation.class);
                if (uniAugmentation != null) {
                    uniNodes.add(node);
                }
            }
        }
        return uniNodes;
    }

    public static List<Node> getUniNodes(DataBroker dataBroker,
                                         LogicalDatastoreType store) {
        List<Node> uniNodes = new ArrayList<>();
        InstanceIdentifier<Topology> topologyInstanceIdentifier = UnimgrMapper.getUniTopologyIid();
        Topology topology = read(dataBroker,
                                 store,
                                 topologyInstanceIdentifier);
        if (topology != null && topology.getNode() != null) {
            for (Node node : topology.getNode()) {
                UniAugmentation uniAugmentation = node.getAugmentation(UniAugmentation.class);
                if (uniAugmentation != null) {
                    uniNodes.add(node);
                }
            }
        }
        return uniNodes;
    }

    public static <D extends org.opendaylight.yangtools.yang.binding.DataObject> D read(
            DataBroker dataBroker,
            final LogicalDatastoreType store,
            final InstanceIdentifier<D> path)  {
        D result = null;
        final ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        Optional<D> optionalDataObject;
        CheckedFuture<Optional<D>, ReadFailedException> future = transaction.read(store, path);
        try {
            optionalDataObject = future.checkedGet();
            if (optionalDataObject.isPresent()) {
                result = optionalDataObject.get();
            } else {
                LOG.debug("{}: Failed to read {}",
                        Thread.currentThread().getStackTrace()[1], path);
            }
        } catch (ReadFailedException e) {
            LOG.warn("Failed to read {} ", path, e);
        }
        transaction.close();
        return result;
    }

    @Deprecated
    public static final Optional<Node> readNode(DataBroker dataBroker,
                                                InstanceIdentifier<?> genericNode) {
        ReadTransaction read = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<Node> nodeIid = genericNode.firstIdentifierOf(Node.class);
        CheckedFuture<Optional<Node>, ReadFailedException> nodeFuture =
                                                              read.read(LogicalDatastoreType.OPERATIONAL,
                                                                        nodeIid);
        try {
            return nodeFuture.checkedGet();
        } catch (ReadFailedException e) {
            LOG.info("Unable to read node with Iid {}", nodeIid);
        }
        return Optional.absent();
    }

    public static final Optional<Link> readLink(DataBroker dataBroker,
                                                LogicalDatastoreType store,
                                                InstanceIdentifier<?> genericNode) {
        ReadTransaction read = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<Link> linkIid = genericNode.firstIdentifierOf(Link.class);
        CheckedFuture<Optional<Link>, ReadFailedException> linkFuture = read.read(store, linkIid);
        try {
            return linkFuture.checkedGet();
        } catch (ReadFailedException e) {
            LOG.info("Unable to read node with Iid {}", linkIid);
        }
        return Optional.absent();
    }

    public static final Optional<Node> readNode(DataBroker dataBroker,
                                                LogicalDatastoreType store,
                                                InstanceIdentifier<?> genericNode) {
        ReadTransaction read = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<Node> nodeIid = genericNode.firstIdentifierOf(Node.class);
        CheckedFuture<Optional<Node>, ReadFailedException> nodeFuture = read
                .read(store, nodeIid);
        try {
            return nodeFuture.checkedGet();
        } catch (ReadFailedException e) {
            LOG.info("Unable to read node with Iid {}", nodeIid);
        }
        return Optional.absent();
    }

    public static void updateUniNode(LogicalDatastoreType dataStore,
                                     InstanceIdentifier<?> uniKey,
                                     UniAugmentation uni,
                                     Node ovsdbNode,
                                     DataBroker dataBroker) {
        InstanceIdentifier<Node> ovsdbNodeIid = UnimgrMapper.getOvsdbNodeIid(ovsdbNode.getNodeId());
        OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(ovsdbNodeIid);
        UniAugmentationBuilder updatedUniBuilder = new UniAugmentationBuilder(uni);
        if (ovsdbNodeRef != null) {
            updatedUniBuilder.setOvsdbNodeRef(ovsdbNodeRef);
        }
        Optional<Node> optionalNode = readNode(dataBroker,
                                               LogicalDatastoreType.CONFIGURATION,
                                               uniKey);
        if (optionalNode.isPresent()) {
            Node node = optionalNode.get();
            WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            NodeBuilder nodeBuilder = new NodeBuilder();
            nodeBuilder.setKey(node.getKey());
            nodeBuilder.setNodeId(node.getNodeId());
            nodeBuilder.addAugmentation(UniAugmentation.class, updatedUniBuilder.build());
            transaction.put(dataStore, uniKey.firstIdentifierOf(Node.class), nodeBuilder.build());
            transaction.submit();
        }
    }

    public static void updateUniNode(LogicalDatastoreType dataStore,
                                     InstanceIdentifier<?> uniKey,
                                     UniAugmentation uni,
                                     InstanceIdentifier<?> ovsdbNodeIid,
                                     DataBroker dataBroker) {
        OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(ovsdbNodeIid);
        UniAugmentationBuilder updatedUniBuilder = new UniAugmentationBuilder(uni);
        if (ovsdbNodeRef != null) {
            updatedUniBuilder.setOvsdbNodeRef(ovsdbNodeRef);
        }
        Optional<Node> optionalNode = readNode(dataBroker,
                                               LogicalDatastoreType.CONFIGURATION,
                                               uniKey);
        if (optionalNode.isPresent()) {
            Node node = optionalNode.get();
            WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            NodeBuilder nodeBuilder = new NodeBuilder();
            nodeBuilder.setKey(node.getKey());
            nodeBuilder.setNodeId(node.getNodeId());
            nodeBuilder.addAugmentation(UniAugmentation.class, updatedUniBuilder.build());
            transaction.put(dataStore, uniKey.firstIdentifierOf(Node.class), nodeBuilder.build());
            transaction.submit();
        }
    }

    public static void updateEvcNode(LogicalDatastoreType dataStore,
                                     InstanceIdentifier<?> evcKey,
                                     EvcAugmentation evcAugmentation,
                                     InstanceIdentifier<?> sourceUniIid,
                                     InstanceIdentifier<?> destinationUniIid,
                                     DataBroker dataBroker) {
        EvcAugmentationBuilder updatedEvcBuilder = new EvcAugmentationBuilder(evcAugmentation);
        if (sourceUniIid != null && destinationUniIid != null) {
            List<UniSource> sourceList = new ArrayList<UniSource>();
            UniSourceKey sourceKey = evcAugmentation.getUniSource().iterator().next().getKey();
            short sourceOrder = evcAugmentation.getUniSource().iterator().next().getOrder();
            IpAddress sourceIp = evcAugmentation.getUniSource().iterator().next().getIpAddress();
            UniSource uniSource = new UniSourceBuilder()
                                          .setOrder(sourceOrder)
                                          .setKey(sourceKey)
                                          .setIpAddress(sourceIp)
                                          .setUni(sourceUniIid)
                                          .build();
            sourceList.add(uniSource);
            updatedEvcBuilder.setUniSource(sourceList);

            List<UniDest> destinationList = new ArrayList<UniDest>();
            UniDestKey destKey = evcAugmentation.getUniDest().iterator().next().getKey();
            short destOrder = evcAugmentation.getUniDest().iterator().next().getOrder();
            IpAddress destIp = evcAugmentation.getUniDest().iterator().next().getIpAddress();
            UniDest uniDest = new UniDestBuilder()
                                      .setIpAddress(destIp)
                                      .setOrder(destOrder)
                                      .setKey(destKey)
                                      .setUni(destinationUniIid)
                                      .build();
            destinationList.add(uniDest);
            updatedEvcBuilder.setUniDest(destinationList);
            Optional<Link> optionalEvcLink = readLink(dataBroker,
                                                      LogicalDatastoreType.CONFIGURATION,
                                                      evcKey);
            if (optionalEvcLink.isPresent()) {
                Link link = optionalEvcLink.get();
                WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
                LinkBuilder linkBuilder = new LinkBuilder();
                linkBuilder.setKey(link.getKey());
                linkBuilder.setLinkId(link.getLinkId());
                linkBuilder.addAugmentation(EvcAugmentation.class, updatedEvcBuilder.build());
                transaction.put(dataStore, evcKey.firstIdentifierOf(Link.class), linkBuilder.build());
                transaction.submit();
            }
        } else {
            LOG.info("Invalid instance identifiers for sourceUni and destUni.");
        }
    }
}
