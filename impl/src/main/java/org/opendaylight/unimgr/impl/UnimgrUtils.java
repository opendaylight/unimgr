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
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.QosTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntriesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Queues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QueuesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QueuesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosOtherConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosOtherConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosOtherConfigKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesOtherConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesOtherConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesOtherConfigKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsKey;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed100M;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10G;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10M;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed1G;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.uni.Speed;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.Augmentation;
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

    /**
     * Creates and submit a Bridge Node to the Configuration Data Store.
     * @param dataBroker The Data Broker Instance to create a transaction
     * @param ovsdbNode The OVSDB node
     * @param uni The UNI linked to the OVSDB node
     * @param bridgeName The bridge name (example: br0)
     */
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

    /**
     * Creates and submit a Bridge Node to the Configuration Data Store.
     * @param dataBroker The Data Broker Instance to create a transaction
     * @param ovsdbNodeIid The OVSDB node Instance Identifier
     * @param uni The UNI linked to the OVSDB node
     * @param bridgeName The bridge name (example: br0)
     */
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

    /**
     * Creates a List of Controller Entry to be used when adding controllers
     * to a Bridge.
     * @param targetString The URI in string format of the Controller Entry
     * @return A List of Controller Entry to be used when adding controllers
     */
    public static List<ControllerEntry> createControllerEntries(String targetString) {
        List<ControllerEntry> controllerEntries = new ArrayList<ControllerEntry>();
        ControllerEntryBuilder controllerEntryBuilder = new ControllerEntryBuilder();
        controllerEntryBuilder.setTarget(new Uri(targetString));
        controllerEntries.add(controllerEntryBuilder.build());
        return controllerEntries;
    }

    /**
     * Creates a submit a GRE tunnel to the Configuration DataStore.
     * @param dataBroker An instance of the Data Broker to create a transaction
     * @param source The source UNI
     * @param destination The destination UNI
     * @param bridgeNode The bridge Node
     * @param bridgeName The bridge name (example br0)
     * @param portName The Port Name (example: eth0)
     */
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

    /**
     * Utility function used to create a protocol entry when creating a bridge node.
     * @return A List of protocol entry
     */
    public static List<ProtocolEntry> createMdsalProtocols() {
        List<ProtocolEntry> protocolList = new ArrayList<ProtocolEntry>();
        ImmutableBiMap<String, Class<? extends OvsdbBridgeProtocolBase>> mapper =
                SouthboundConstants.OVSDB_PROTOCOL_MAP.inverse();
        protocolList.add(new ProtocolEntryBuilder().
                setProtocol((Class<? extends OvsdbBridgeProtocolBase>) mapper.get("OpenFlow13")).build());
        return protocolList;
    }

    /**
     * Creates a Bridge Augmentation by using a UNI
     * @param uni Contains data used to create the augmentation
     * @return A Built OvsdbBridgeAugmentation with data.
     * @throws Exception if the Ovsdb Node Reference cannot be found.
     */
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

    /**
     * Creates a submit an OvsdbNode to the Configuration DataStore.
     * @param dataBroker The instance of the Data Broker to create transactions.
     * @param ovsdbNodeId The Ovsdb Node Id to use on creation
     * @param uni The UNI's data
     */
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

    /**
     * Creates and submit an OvsdbNode by using the Data contained in the UniAugmentation
     * @param dataBroker The instance of the DataBroker to create transactions
     * @param uni The UNI's data
     * @return The instance of the Node
     */
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

    /**
     * Creates and Build the data for an OvsdbNodeAugmentation.
     * @param uni The UNI"s data
     * @return The built OsvdbNodeAugmentation
     */
    public static OvsdbNodeAugmentation createOvsdbNodeAugmentation(Uni uni) {
        ConnectionInfo connectionInfos = new ConnectionInfoBuilder()
                                                .setRemoteIp(uni.getIpAddress())
                                                .setRemotePort(new PortNumber(UnimgrConstants.OVSDB_PORT))
                                                .build();
        OvsdbNodeAugmentation ovsdbNode = new OvsdbNodeAugmentationBuilder()
                                                .setConnectionInfo(connectionInfos)
                                                .build();
        return ovsdbNode;
    }

    public static OvsdbNodeAugmentation createOvsdbNodeAugmentation(UniAugmentation uni,
            PortNumber remotePort) {
        ConnectionInfo connectionInfos = new ConnectionInfoBuilder()
                .setRemoteIp(uni.getIpAddress())
                .setRemotePort(remotePort)
                .build();
        OvsdbNodeAugmentation ovsdbNode = new OvsdbNodeAugmentationBuilder()
                .setConnectionInfo(connectionInfos)
                .setQosEntries(createQosEntries(uni))
                .setQueues(createQueues(uni))
                .build();
        return ovsdbNode;
    }

    public static Node createQoSForOvsdbNode (DataBroker dataBroker, UniAugmentation uni) {
        NodeId ovsdbNodeId = new NodeId(createOvsdbNodeId(uni.getIpAddress()));
        try {
            InstanceIdentifier<Node> ovsdbNodeIid = UnimgrMapper.getOvsdbNodeIid(ovsdbNodeId);
            NodeKey ovsdbNodeKey = new NodeKey(ovsdbNodeId);
            Node nodeData = new NodeBuilder()
                                    .setNodeId(ovsdbNodeId)
                                    .setKey(ovsdbNodeKey)
                                    .addAugmentation(OvsdbNodeAugmentation.class, UnimgrUtils
                                            .createOvsdbNodeAugmentation(uni
                                                    , getRemotePort(dataBroker, uni)))
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

    private static PortNumber getRemotePort(DataBroker dataBroker, UniAugmentation uni) {
        PortNumber remotePort = null;
        Optional<Node> optionalNode = findOvsdbNode(dataBroker, uni);

        if (optionalNode.isPresent()) {
            remotePort = optionalNode.get()
                    .getAugmentation(OvsdbNodeAugmentation.class)
                    .getConnectionInfo().getRemotePort();
        }
//
//        NodeId ovsdbNodeId = new NodeId(createOvsdbNodeId(uni.getIpAddress()));
//        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
//        InstanceIdentifier<Node> ovsdbNodeIid = UnimgrMapper.getOvsdbNodeIid(ovsdbNodeId);
//        Optional<Node> optionalDataObject;
//        CheckedFuture<Optional<Node>, ReadFailedException> future = transaction
//                .read(LogicalDatastoreType.OPERATIONAL, ovsdbNodeIid);
//        try {
//            optionalDataObject = future.checkedGet();
//            if (optionalDataObject.isPresent()) {
//                remotePort = optionalDataObject
//                        .get().getAugmentation(OvsdbNodeAugmentation.class)
//                        .getConnectionInfo().getRemotePort();
//            } else {
//                LOG.debug("{}: Failed to read {}", Thread.currentThread().getStackTrace()[1], ovsdbNodeIid);
//            }
//        } catch (ReadFailedException e) {
//            LOG.warn("Failed to read {} ", ovsdbNodeIid, e);
//        }
//        transaction.close();
        return remotePort;
    }

    private static List<QosEntries> createQosEntries(Uni uni) {
        // Configure queue for best-effort dscp and max rate
        List<QosOtherConfig> otherConfig = new ArrayList<>();
        QosOtherConfig qOtherConfig = new QosOtherConfigBuilder()
                .setKey(new QosOtherConfigKey(UnimgrConstants.QOS_DSCP_ATTRIBUTE))
                .setOtherConfigKey(UnimgrConstants.QOS_DSCP_ATTRIBUTE)
                .setOtherConfigValue(UnimgrConstants.QOS_DSCP_ATTRIBUTE_VALUE)
                .build();
        otherConfig.add(qOtherConfig);

        qOtherConfig = new QosOtherConfigBuilder()
                .setKey(new QosOtherConfigKey(UnimgrConstants.QOS_MAX_RATE))
                .setOtherConfigKey(UnimgrConstants.QOS_MAX_RATE)
                .setOtherConfigValue(getSpeed(uni.getSpeed()))
                .build();
        otherConfig.add(qOtherConfig);

        Uuid qosUuid = new Uuid(UUID.randomUUID().toString());
        QosEntries qosEntry = new QosEntriesBuilder()
                .setKey(new QosEntriesKey(new Uri(UnimgrConstants.QOS_PREFIX + qosUuid.getValue())))
                .setQosId(new Uri(UnimgrConstants.QOS_PREFIX + qosUuid.getValue()))
                .setQosOtherConfig(otherConfig)
                .setQosType(SouthboundMapper.createQosType(SouthboundConstants.QOS_LINUX_HTB))
                .build();

        List<QosEntries> qosEntries = new ArrayList<>();
        qosEntries.add(qosEntry);
        return qosEntries;
    }

    private static List<Queues> createQueues(Uni uni) {
        List<QueuesOtherConfig> otherConfig = new ArrayList<>();
        QueuesOtherConfig queuesOtherConfig = new QueuesOtherConfigBuilder()
                .setKey(new QueuesOtherConfigKey(UnimgrConstants.QOS_DSCP_ATTRIBUTE))
                .setQueueOtherConfigKey(UnimgrConstants.QOS_DSCP_ATTRIBUTE)
                .setQueueOtherConfigValue(UnimgrConstants.QOS_DSCP_ATTRIBUTE_VALUE)
                .build();
        otherConfig.add(queuesOtherConfig);

        queuesOtherConfig = new QueuesOtherConfigBuilder()
                .setKey(new QueuesOtherConfigKey(UnimgrConstants.QOS_MAX_RATE))
                .setQueueOtherConfigKey(UnimgrConstants.QOS_MAX_RATE)
                .setQueueOtherConfigValue(getSpeed(uni.getSpeed()))
                .build();
        otherConfig.add(queuesOtherConfig);

        // Configure dscp value for best-effort
        Uuid queueUuid = new Uuid(UUID.randomUUID().toString());
        Queues queues = new QueuesBuilder()
                .setDscp(Short.parseShort(UnimgrConstants.QOS_DSCP_ATTRIBUTE_VALUE))
                .setKey(new QueuesKey(new Uri(UnimgrConstants.QUEUE_PREFIX + queueUuid.getValue())))
                .setQueueId(new Uri(UnimgrConstants.QUEUE_PREFIX + queueUuid.getValue()))
                .setQueuesOtherConfig(otherConfig)
                .build();

        List<Queues> queuesList = new ArrayList<>();
        queuesList.add(queues);
        return queuesList;
    }

    private static String getSpeed(Speed speedObject) {
        String speed = null;
        if (speedObject.getSpeed() instanceof Speed10M) {
            speed = "10000000";
        }
        else if (speedObject.getSpeed() instanceof Speed100M) {
            speed = "100000000";
        }
        else if (speedObject.getSpeed() instanceof Speed1G) {
            speed = "1000000000";
        }
        else if (speedObject.getSpeed() instanceof Speed10G) {
            speed = "10000000000";
        }
        return speed;
    }

    /**
     * Creates an OVSDB node Id with an IP Address.
     * @param ipAddress The IP address of the UNI (therefo the OVSDB node)
     * @return A NodeId for a Specific Ovsdb Node Id
     */
    public static NodeId createOvsdbNodeId(IpAddress ipAddress) {
        String nodeId = UnimgrConstants.OVSDB_PREFIX
                        + ipAddress.getIpv4Address().getValue().toString()
                        + ":"
                        + UnimgrConstants.OVSDB_PORT;
        return new NodeId(nodeId);
    }

    /**
     * Creates a built OvsdbTerminationAugmentation with data
     * @param uni The UNI's data
     * @return A Built OvsdbTerminationPointAugmentation with data
     */
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

    /**
     * Creates and submit an evc by using the Data contained in the EvcAugmentation
     * @param dataBroker The instance of the DataBroker to create transactions
     * @param evc The EVC's data
     * @return true if evc created
     */
    public static boolean createEvc(DataBroker dataBroker, EvcAugmentation evc) {
         return false;
    }

    /**
     * Creates and submit an UNI Node by using the Data contained in the UniAugmentation
     * @param dataBroker The instance of the DataBroker to create transactions
     * @param uni The UNI's data
     * @return true if uni created
     */
    public static boolean createUniNode(DataBroker dataBroker, UniAugmentation uni) {
        NodeId uniNodeId = new NodeId(createUniNodeId(uni.getIpAddress()));
        boolean result = false;
        try {
            InstanceIdentifier<Node> uniNodeIid = UnimgrMapper.getUniNodeIid(uniNodeId);
            NodeKey uniNodeKey = new NodeKey(uniNodeId);
            Node nodeData = new NodeBuilder()
                                    .setNodeId(uniNodeId)
                                    .setKey(uniNodeKey)
                                    .addAugmentation(UniAugmentation.class, uni)
                                    .build();
            WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            transaction.put(LogicalDatastoreType.CONFIGURATION, uniNodeIid, nodeData);
            CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
            future.checkedGet();
            result = true;
            LOG.info("Created and submitted a new Uni node {}", nodeData.getNodeId());
        } catch (Exception e) {
            LOG.error("Exception while creating Uni Node" + "Uni Node Id: {}", uniNodeId);
        }
        return result;
    }

    /**
     * Creates an UNI node Id with an IP Address.
     * @param ipAddress The IP address of the UNI
     * @return A NodeId for a Specific UNI Node Id
     */
    public static NodeId createUniNodeId(IpAddress ipAddress) {
        return new NodeId(UnimgrConstants.UNI_PREFIX + ipAddress.getIpv4Address().getValue().toString());
    }

    /**
     * Creates and Submit a termination point Node to the configuration DateStore.
     * @param dataBroker The instance of the data broker to create transactions
     * @param uni The UNI's data
     * @param bridgeNode The Bridge node
     * @param bridgeName The Bridge name (example: br0)
     * @param portName The Port name (example: eth0)
     * @param type The type of termination (example: gre) Refer to OVSDB_INTERFACE_TYPE_MAP
     * to review the list of available Interface Types.
     */
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

    /**
     * Creates and Submit a termination point Node without specifying its interface type.
     * @param dataBroker The instance of the data broker to create transactions
     * @param uni The UNI's data
     * @param bridgeNode The Bridge node
     * @param bridgeName The Bridge name (example: br0)
     * @param portName The Port name (example: eth0)
     */
    public static void createTerminationPointNode(DataBroker dataBroker,
                                                  Uni uni,
                                                  Node bridgeNode,
                                                  String bridgeName,
                                                  String portName) {
        InstanceIdentifier<TerminationPoint> tpIid = UnimgrMapper
                                                        .getTerminationPointIid(bridgeNode,
                                                                                portName);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder =
                                                     new OvsdbTerminationPointAugmentationBuilder();
        tpAugmentationBuilder.setName(portName);
        tpAugmentationBuilder.setInterfaceType(null);
        if (uni.getSpeed() != null) {
            // TO-DO get the qos UUID from operational datastore
            // [1] – Create Qos and Queue individually: done
            // [2] – Query Qos and Queue operational: TO-DO
            // [3] – update Qos and termination point with queue and qos uuids respectively: TO-DO
//            tpAugmentationBuilder.setQos(QOS_UUID);
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

    /**
     * Deletes a generic node
     * @param dataBroker The instance of the data broker to create transactions
     * @param store The DataStore where the delete
     * @param path The path to delete
     * @return An instance of a generic Data Object
     */
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

    /**
     * Deletes a termination Point from the configuration data store.
     * @param dataBroker The instance of the data broker to create transactions
     * @param terminationPoint The Termination Point of the OVSDB bridge
     * @param ovsdbNode The ovsdb Node
     * @return A checked Future
     */
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

    /**
     * Generic function to delete a node on a specific dataStore
     * @param dataBroker The instance of the data broker to create transactions.
     * @param genericNode The instance identifier of a generic node
     * @param store The dataStore where to send and submit the delete call.
     */
    public static boolean deleteNode(DataBroker dataBroker,
                                  InstanceIdentifier<?> genericNode,
                                  LogicalDatastoreType store) {
        LOG.info("Received a request to delete node {}", genericNode);
        boolean result = false;
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.delete(store, genericNode);
        try {
            transaction.submit().checkedGet();
            result = true;
        } catch (TransactionCommitFailedException e) {
            LOG.error("Unable to remove node with Iid {} from store {}.", genericNode, store);
        }
        return result;
    }

    /**
     * Extract a data object by using its instance indentifier and it's class type.
     * @param changes Data Change object
     * @param klazz Class type
     * @return The extracted DataObject as an Object casted as the class type
     */
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

    /**
     * Extract original data from the data store.
     * @param changes The dataChange object
     * @param klazz The class type
     * @return The DataObject casted as a Class type
     */
    public static <T extends DataObject> Map<InstanceIdentifier<T>,T> extractOriginal(
            AsyncDataChangeEvent<InstanceIdentifier<?>,DataObject> changes,Class<T> klazz) {
        return extract(changes.getOriginalData(),klazz);
    }

    /**
     * Extracts the removed nodes
     * @param changes he dataChange object
     * @param klazz The class type
     * @return A set to removed nodes as DataObject casted as the class type
     */
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

    /**
     * Search the Operational Datastore for a specific OvsdbNode.
     * @param dataBroker The dataBroker instance to create transactions
     * @param uni The UNI's data
     * @return The Optional OvsdbNode
     */
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

    /**
     * Search the Operation DataStore for a specific UNI
     * @param dataBroker The dataBroker instance to create transactions
     * @param ipAddress The IP address of the UNI
     * @return An Optional UNI Node
     */
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

    /**
     * Retrieves the connection information from an Ovsdb Connection by
     * using the Ovsdb Node Id
     * @param dataBroker The dataBroker instance to create transactions
     * @param ovsdbNodeId The NodeId of the OVSDB node
     * @return The ConnectionInfo object
     */
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

    /**
     * Retrieve the list of links in the Operational DataStore
     * @param dataBroker The dataBroker instance to create transactions
     * @return A list of Links retrieved from the Operational DataStore
     */
    public static List<Link> getEvcLinks(DataBroker dataBroker) {
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

    /**
     * Retrieve the Local IP of the controller
     * @return The LocalIp object of the Controller
     */
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

    /**
     * Retrieve a list of Ovsdb Nodes from the Operational DataStore
     * @param dataBroker The dataBroker instance to create transactions
     * @return The Ovsdb Node retrieved from the Operational DataStore
     */
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

    /**
     * Retrieve a list of Uni Nodes from the Configuration DataStore
     * @param dataBroker The dataBroker instance to create transactions
     * @return A list of Uni Nodes from the Config dataStore
     */
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

    /**
     * Retrieve a list of Uni Nodes on a specific DataStore
     * @param dataBroker The dataBroker instance to create transactions
     * @param store The store to which to send the read request
     * @return A List of UNI Nodes.
     */
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

    /**
     * Retrieve a list of Unis on a specific DataStore
     * @param dataBroker instance to create transactions
     * @param store to which send the read request
     * @return A List of Unis.
     */
    public static List<UniAugmentation> getUnis(DataBroker dataBroker,
                                         LogicalDatastoreType store) {
        List<UniAugmentation> unis = new ArrayList<>();
        InstanceIdentifier<Topology> topologyInstanceIdentifier = UnimgrMapper.getUniTopologyIid();
        Topology topology = read(dataBroker,
                                 store,
                                 topologyInstanceIdentifier);
        if (topology != null && topology.getNode() != null) {
            for (Node node : topology.getNode()) {
                UniAugmentation uniAugmentation = node.getAugmentation(UniAugmentation.class);
                if (uniAugmentation != null) {
                    unis.add(uniAugmentation);
                }
            }
        }
        return unis;
    }

    /**
     * Retrieve a list of Unis on a specific DataStore
     * @param dataBroker instance to create transactions
     * @param store to which send the read request
     * @param ipAddress of the required Uni
     * @return uni.
     */
    public static UniAugmentation getUni(DataBroker dataBroker,
                                         LogicalDatastoreType store, IpAddress ipAddress) {
        InstanceIdentifier<Topology> topologyInstanceIdentifier = UnimgrMapper.getUniTopologyIid();
        Topology topology = read(dataBroker,
                                 store,
                                 topologyInstanceIdentifier);
        if (topology != null && topology.getNode() != null) {
            for (Node node : topology.getNode()) {
                UniAugmentation uniAugmentation = node.getAugmentation(UniAugmentation.class);
                if (uniAugmentation != null && uniAugmentation.getIpAddress().getIpv4Address().getValue().equals(ipAddress.getIpv4Address().getValue())) {
                    return uniAugmentation;
                }
            }
        }
        return null;
    }

    /**
     * Read a specific datastore type and return a DataObject as a casted
     * class type Object.
     * @param dataBroker The dataBroker instance to create transactions
     * @param store The store type to query
     * @param path The generic path to query
     * @return The DataObject as a casted Object
     */
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

    /**
     * Read a specific node from the Operational Data store by default.
     * @param dataBroker The dataBroker instance to create transactions
     * @param genericNode The Instance Identifier of the Node
     * @return The Optional Node instance
     */
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

    /**
     * Read a specific Link from a specific datastore
     * @param dataBroker The dataBroker instance to create transactions
     * @param store The datastore type.
     * @param genericNode The Instance Identifier of the Link
     * @return An Optional Link instance
     */
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

    /**
     * Read a specific node from a specific data store type.
     * @param dataBroker The dataBroker instance to create transactions
     * @param store The data store type
     * @param genericNode The Instance Identifier of a specific Node
     * @return An Optional Node instance
     */
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

    /**
     * Updates a specific Uni Node on a specific DataStore type
     * @param dataStore The datastore type
     * @param uniKey The UNI key
     * @param uni The Uni's data
     * @param ovsdbNode The Ovsdb Node
     * @param dataBroker The dataBroker instance to create transactions
     */
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

    /**
     * Update a specific UNI node on a specific datastore type
     * @param dataStore The datastore type
     * @param uniKey The UNI key
     * @param uni The Uni's data
     * @param ovsdbNodeIid The Ovsdb Node Instance Identifier
     * @param dataBroker The dataBroker instance to create transactions
     */
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

    /**
     * Updates a specific EVC into a specific DataStore type
     * @param dataStore The datastore type
     * @param evcKey The EVC key
     * @param evcAugmentation The EVC's data
     * @param sourceUniIid The Source Uni Instance Identifier
     * @param destinationUniIid The destination Uni Instance Identifier
     * @param dataBroker The dataBroker instance to create transactions
     */
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
                linkBuilder.setDestination(link.getDestination());
                linkBuilder.setSource(link.getSource());
                linkBuilder.addAugmentation(EvcAugmentation.class, updatedEvcBuilder.build());
                transaction.put(dataStore, evcKey.firstIdentifierOf(Link.class), linkBuilder.build());
                transaction.submit();
            }
        } else {
            LOG.info("Invalid instance identifiers for sourceUni and destUni.");
        }
    }
}
