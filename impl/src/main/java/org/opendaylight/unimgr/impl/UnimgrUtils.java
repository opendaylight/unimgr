/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.impl;

import java.lang.reflect.Field;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Uni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnimgrUtils {

    private static final Logger LOG = LoggerFactory.getLogger(UnimgrUtils.class);

    public static final Optional<Node> readNode(DataBroker dataBroker,
            InstanceIdentifier<?> genericNode) {
        ReadTransaction read = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<Node> nodeIid = genericNode.firstIdentifierOf(Node.class);
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

    public static OvsdbBridgeAugmentation createOvsdbBridgeAugmentation(Uni uni) throws Exception {
        OvsdbNodeRef ovsdbNodeRef = uni.getOvsdbNodeRef();
        if (ovsdbNodeRef != null && ovsdbNodeRef.getValue() != null) {
            UUID bridgeUuid = UUID.randomUUID();
            OvsdbBridgeAugmentation ovsdbBridge = new OvsdbBridgeAugmentationBuilder()
                        .setBridgeName(new OvsdbBridgeName(UnimgrConstants.DEFAULT_BRIDGE_NAME))
                        .setManagedBy(ovsdbNodeRef)
                        .setBridgeUuid(new Uuid(bridgeUuid.toString()))
                        .build();
            return ovsdbBridge;
        } else {
            throw new Exception("Ovsdb Node Reference does not exist !");
        }
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

    public static ConnectionInfo getConnectionInfo(DataBroker dataBroker, NodeId ovsdbNodeId) {
        InstanceIdentifier<Node> nodeIid = UnimgrMapper.getOvsdbNodeIID(ovsdbNodeId);
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
                UnimgrConstants.OVSDB_PROTOCOL_MAP.inverse();
        protocolList.add(new ProtocolEntryBuilder().
                setProtocol((Class<? extends OvsdbBridgeProtocolBase>) mapper.get("OpenFlow13")).build());
        return protocolList;
    }

    public static void createOvsdbNode(DataBroker dataBroker, NodeId ovsdbNodeId, Uni uni) {
        InstanceIdentifier<Node> ovsdbNodeIid = UnimgrMapper
                .getOvsdbNodeIID(uni.getIpAddress());
        try {
            NodeKey ovsdbNodeKey = new NodeKey(ovsdbNodeId);
            Node nodeData = new NodeBuilder().setNodeId(ovsdbNodeId)
                    .setKey(ovsdbNodeKey)
                    .addAugmentation(OvsdbNodeAugmentation.class, UnimgrUtils.createOvsdbNodeAugmentation(uni))
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

    public static Node createOvsdbNode(DataBroker dataBroker, UniAugmentation uni) {
        List<Node> ovsdbNodes = getOvsdbNodes(dataBroker);
        if (!ovsdbNodes.isEmpty()) {
            for (Node ovsdbNode: ovsdbNodes) {
                OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode
                        .getAugmentation(OvsdbNodeAugmentation.class);
                if (ovsdbNodeAugmentation.getConnectionInfo()
                                         .getRemoteIp().getIpv4Address()
                                         .equals(uni.getIpAddress().getIpv4Address())) {
                    LOG.info("Found ovsdb node");
                    return ovsdbNode;
                }
            }
        }
        return null;
    }

    public static Node findOvsdbNode(DataBroker dataBroker, UniAugmentation uni) {
        List<Node> ovsdbNodes = getOvsdbNodes(dataBroker);
        if (!ovsdbNodes.isEmpty()) {
            for (Node ovsdbNode: ovsdbNodes) {
                OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode
                        .getAugmentation(OvsdbNodeAugmentation.class);
                if (ovsdbNodeAugmentation.getConnectionInfo()
                                         .getRemoteIp().getIpv4Address()
                                         .equals(uni.getIpAddress().getIpv4Address())) {
                    LOG.info("Found ovsdb node");
                    return ovsdbNode;
                }
            }
        }
        return null;
    }

    public static void updateUniNode(LogicalDatastoreType dataStore,
                               InstanceIdentifier<?> uniKey,
                               UniAugmentation uni,
                               Node ovsdbNode,
                               DataBroker dataBroker) {
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        UniAugmentationBuilder updatedUniBuilder = new UniAugmentationBuilder(uni);
        Optional<Node> optionalNode = UnimgrUtils.readNode(dataBroker, uniKey);
        if (optionalNode.isPresent()) {
            Node node = optionalNode.get();
            NodeBuilder nodeBuilder = new NodeBuilder();
            nodeBuilder.setKey(node.getKey());
            nodeBuilder.setNodeId(node.getNodeId());
            nodeBuilder.addAugmentation(UniAugmentation.class, updatedUniBuilder.build());
            transaction.put(dataStore, uniKey.firstIdentifierOf(Node.class), nodeBuilder.build());
        }
    }

    @Deprecated
    public static void createBridgeNode(DataBroker dataBroker, NodeId ovsdbNodeId, Uni uni, String bridgeName) {
        LOG.info("Creating a bridge on node {}", ovsdbNodeId);
        InstanceIdentifier<Node> ovsdbNodeIid = UnimgrMapper
                .getOvsdbNodeIID(uni.getIpAddress());
        ConnectionInfo connectionInfo = UnimgrUtils.getConnectionInfo(dataBroker, ovsdbNodeId);
        if (connectionInfo != null) {
            NodeBuilder bridgeNodeBuilder = new NodeBuilder();
            InstanceIdentifier<Node> bridgeIid = UnimgrMapper
                    .getOvsdbBridgeNodeIID(ovsdbNodeId, bridgeName);
            NodeId bridgeNodeId = new NodeId(ovsdbNodeId
                    + UnimgrConstants.DEFAULT_BRIDGE_NODE_ID_SUFFIX + bridgeName);
            bridgeNodeBuilder.setNodeId(bridgeNodeId);
            OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
            ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(
                    bridgeName));
            ovsdbBridgeAugmentationBuilder.setProtocolEntry(UnimgrUtils
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

    public static NodeId createOvsdbNodeId(IpAddress ipAddress) {
        String nodeId = UnimgrConstants.OVSDB_PREFIX
                + ipAddress.getIpv4Address().getValue().toString()
                + ":"
                + UnimgrConstants.OVSDB_PORT;
        return new NodeId(nodeId);
    }

    public static List<Node> getOvsdbNodes(DataBroker dataBroker) {
        List<Node> ovsdbNodes = new ArrayList<>();
        InstanceIdentifier<Topology> topologyInstanceIdentifier = UnimgrMapper.createTopologyIid();
        Topology topology = UnimgrUtils.read(dataBroker, LogicalDatastoreType.OPERATIONAL, topologyInstanceIdentifier);
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
        InstanceIdentifier<Topology> topologyInstanceIdentifier = UnimgrMapper.createTopologyIid();
        Topology topology = UnimgrUtils.read(dataBroker, LogicalDatastoreType.OPERATIONAL, topologyInstanceIdentifier);
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

    public static List<Link> getEvcLinks(DataBroker dataBroker, IpAddress ipAddress) {
        List<Link> evcLinks = new ArrayList<>();
        InstanceIdentifier<Topology> topologyInstanceIdentifier = UnimgrMapper.createTopologyIid();
        Topology topology = UnimgrUtils.read(dataBroker, LogicalDatastoreType.OPERATIONAL, topologyInstanceIdentifier);
        if (topology != null && topology.getNode() != null) {
            for (Link link : topology.getLink()) {
                EvcAugmentation evcAugmentation = link.getAugmentation(EvcAugmentation.class);
                if (evcAugmentation != null) {
                    evcLinks.add(link);
                }
            }
        }
        return evcLinks;
    }

    public static void createBridgeNode(DataBroker dataBroker, Node ovsdbNode, UniAugmentation uni, String bridgeName) {
        LOG.info("Creating a bridge on node {}", ovsdbNode.getNodeId());
        InstanceIdentifier<Node> ovsdbNodeIid = UnimgrMapper
                .getOvsdbNodeIID(uni.getIpAddress());
        ConnectionInfo connectionInfo = UnimgrUtils.getConnectionInfo(dataBroker, ovsdbNode.getNodeId());
        if (connectionInfo != null) {
            NodeBuilder bridgeNodeBuilder = new NodeBuilder();
            InstanceIdentifier<Node> bridgeIid = UnimgrMapper
                    .getOvsdbBridgeNodeIID(ovsdbNode.getNodeId(), bridgeName);
            NodeId bridgeNodeId = new NodeId(ovsdbNode.getNodeId()
                    + UnimgrConstants.DEFAULT_BRIDGE_NODE_ID_SUFFIX + bridgeName);
            bridgeNodeBuilder.setNodeId(bridgeNodeId);
            OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
            ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(
                    bridgeName));
            ovsdbBridgeAugmentationBuilder.setProtocolEntry(UnimgrUtils
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
            LOG.error("The OVSDB node is not connected {}", ovsdbNode.getNodeId());
        }
    }

    public static void createTerminationPointNode(DataBroker dataBroker, Uni uni,
            Node bridgeNode, String bridgeName, String portName, String type) {
        InstanceIdentifier<TerminationPoint> tpIid = UnimgrMapper
                .createTerminationPointInstanceIdentifier(bridgeNode, portName);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder =
                new OvsdbTerminationPointAugmentationBuilder();
        tpAugmentationBuilder.setName(portName);
        if (type != null) {
            tpAugmentationBuilder.setInterfaceType(UnimgrConstants.OVSDB_INTERFACE_TYPE_MAP.get(type));
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
        InstanceIdentifier<TerminationPoint> tpIid = UnimgrMapper
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
        tpAugmentationBuilder.setInterfaceType(UnimgrConstants.OVSDB_INTERFACE_TYPE_MAP.get("gre"));
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
