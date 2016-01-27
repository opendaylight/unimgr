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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosOtherConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosOtherConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosOtherConfigKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueListKey;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.Speed;
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
        final InstanceIdentifier<Node> ovsdbNodeIid = uni.getOvsdbNodeRef().getValue().firstIdentifierOf(Node.class);
        if (ovsdbNodeIid != null) {
            final NodeBuilder bridgeNodeBuilder = new NodeBuilder();
            final InstanceIdentifier<Node> bridgeIid = UnimgrMapper.createOvsdbBridgeNodeIid(ovsdbNode,
                                                                                       bridgeName);
            final NodeId bridgeNodeId = new NodeId(ovsdbNode.getNodeId()
                                           + UnimgrConstants.DEFAULT_BRIDGE_NODE_ID_SUFFIX
                                           + bridgeName);
            bridgeNodeBuilder.setNodeId(bridgeNodeId);
            final OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
            ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(bridgeName));
            ovsdbBridgeAugmentationBuilder.setProtocolEntry(UnimgrUtils.createMdsalProtocols());
            final OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(ovsdbNodeIid);
            ovsdbBridgeAugmentationBuilder.setManagedBy(ovsdbNodeRef);
            bridgeNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, ovsdbBridgeAugmentationBuilder.build());
            final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
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
            final NodeBuilder bridgeNodeBuilder = new NodeBuilder();
            final Optional<Node> optionalOvsdbNode = UnimgrUtils.readNode(dataBroker,
                                                                    LogicalDatastoreType.OPERATIONAL,
                                                                    ovsdbNodeIid);
            if (optionalOvsdbNode.isPresent()) {
                final Node ovsdbNode = optionalOvsdbNode.get();
                final InstanceIdentifier<Node> bridgeIid = UnimgrMapper.createOvsdbBridgeNodeIid(ovsdbNode,
                                                                                           bridgeName);
                final NodeId bridgeNodeId = new NodeId(ovsdbNode.getNodeId().getValue()
                                               + UnimgrConstants.DEFAULT_BRIDGE_NODE_ID_SUFFIX
                                               + bridgeName);
                bridgeNodeBuilder.setNodeId(bridgeNodeId);
                final OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
                ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(bridgeName));
                ovsdbBridgeAugmentationBuilder.setProtocolEntry(UnimgrUtils.createMdsalProtocols());
                final OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(ovsdbNodeIid);
                ovsdbBridgeAugmentationBuilder.setManagedBy(ovsdbNodeRef);
                bridgeNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class,
                                                  ovsdbBridgeAugmentationBuilder.build());
                final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
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
        final List<ControllerEntry> controllerEntries = new ArrayList<ControllerEntry>();
        final ControllerEntryBuilder controllerEntryBuilder = new ControllerEntryBuilder();
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
        final InstanceIdentifier<TerminationPoint> tpIid =
                                                 UnimgrMapper.getTerminationPointIid(bridgeNode,
                                                                                     portName);
        final OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder =
                                                     new OvsdbTerminationPointAugmentationBuilder();
        tpAugmentationBuilder.setName(portName);
        final ArrayList<Options> options = Lists.newArrayList();
        final OptionsKey optionKey = new OptionsKey("remote_ip");
        final Options destinationIp = new OptionsBuilder()
                                        .setOption(destination.getIpAddress().getIpv4Address().getValue())
                                        .setKey(optionKey).setValue(destination.getIpAddress().getIpv4Address().getValue())
                                        .build();
        options.add(destinationIp);
        tpAugmentationBuilder.setOptions(options);
        tpAugmentationBuilder.setInterfaceType(SouthboundConstants.OVSDB_INTERFACE_TYPE_MAP.get("gre"));
        if (source.getSpeed() != null) {
            Uuid qosUuid = getQosUuid(dataBroker, source);
            tpAugmentationBuilder.setQos(getQosUuid(dataBroker, source));
            LOG.info("Updating Qos {} to termination point {}", qosUuid , bridgeName);
        }
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
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
        final List<ProtocolEntry> protocolList = new ArrayList<ProtocolEntry>();
        final ImmutableBiMap<String, Class<? extends OvsdbBridgeProtocolBase>> mapper =
                SouthboundConstants.OVSDB_PROTOCOL_MAP.inverse();
        protocolList.add(new ProtocolEntryBuilder().
                setProtocol(mapper.get("OpenFlow13")).build());
        return protocolList;
    }

    /**
     * Creates a Bridge Augmentation by using a UNI
     * @param uni Contains data used to create the augmentation
     * @return A Built OvsdbBridgeAugmentation with data.
     * @throws Exception if the Ovsdb Node Reference cannot be found.
     */
    public static OvsdbBridgeAugmentation createOvsdbBridgeAugmentation(Uni uni) throws Exception {
        final OvsdbNodeRef ovsdbNodeRef = uni.getOvsdbNodeRef();
        if (ovsdbNodeRef != null && ovsdbNodeRef.getValue() != null) {
            final UUID bridgeUuid = UUID.randomUUID();
            final OvsdbBridgeAugmentation ovsdbBridge = new OvsdbBridgeAugmentationBuilder()
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
        final InstanceIdentifier<Node> ovsdbNodeIid = UnimgrMapper.getOvsdbNodeIid(uni.getIpAddress());
        try {
            final NodeKey ovsdbNodeKey = new NodeKey(ovsdbNodeId);
            final Node nodeData = new NodeBuilder()
                                    .setNodeId(ovsdbNodeId)
                                    .setKey(ovsdbNodeKey)
                                    .addAugmentation(OvsdbNodeAugmentation.class,
                                                     UnimgrUtils.createOvsdbNodeAugmentation(uni))
                                    .build();
            // Submit the node to the datastore
            final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            transaction.put(LogicalDatastoreType.CONFIGURATION, ovsdbNodeIid, nodeData);
            transaction.submit();
            LOG.info("Created and submitted a new OVSDB node {}", nodeData.getNodeId());
        } catch (final Exception e) {
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
        final NodeId ovsdbNodeId = new NodeId(createOvsdbNodeId(uni.getIpAddress()));
        try {
            final InstanceIdentifier<Node> ovsdbNodeIid = UnimgrMapper.getOvsdbNodeIid(ovsdbNodeId);
            final NodeKey ovsdbNodeKey = new NodeKey(ovsdbNodeId);
            final Node nodeData = new NodeBuilder()
                                    .setNodeId(ovsdbNodeId)
                                    .setKey(ovsdbNodeKey)
                                    .addAugmentation(OvsdbNodeAugmentation.class,
                                                     UnimgrUtils.createOvsdbNodeAugmentation(uni))
                                    .build();
            // Submit the node to the datastore
            final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            transaction.put(LogicalDatastoreType.CONFIGURATION, ovsdbNodeIid, nodeData);
            transaction.submit();
            LOG.info("Created and submitted a new OVSDB node {}", nodeData.getNodeId());
            return nodeData;
        } catch (final Exception e) {
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
        final ConnectionInfo connectionInfos = new ConnectionInfoBuilder()
                                                .setRemoteIp(uni.getIpAddress())
                                                .setRemotePort(new PortNumber(UnimgrConstants.OVSDB_PORT))
                                                .build();
        final OvsdbNodeAugmentation ovsdbNode = new OvsdbNodeAugmentationBuilder()
                                                .setConnectionInfo(connectionInfos).build();
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
        Optional<Node> optionalNode = findOvsdbNode(dataBroker, uni);
        if (optionalNode.isPresent()) {
            NodeId ovsdbNodeId = optionalNode.get().getNodeId();
            InstanceIdentifier<OvsdbNodeAugmentation> ovsdbNodeAugmentationIid = UnimgrMapper
                    .getOvsdbNodeIid(ovsdbNodeId)
                    .augmentation(OvsdbNodeAugmentation.class);
            OvsdbNodeAugmentation ovsdbNodeAugmentation = createOvsdbNodeAugmentation(uni,
                    getRemotePort(dataBroker, uni));
            WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            transaction.put(LogicalDatastoreType.CONFIGURATION, ovsdbNodeAugmentationIid, ovsdbNodeAugmentation, true);
            CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
            try {
                Thread.sleep(UnimgrConstants.OVSDB_UPDATE_TIMEOUT);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting after OVSDB node augmentation {} {}", ovsdbNodeId, e);
            }
            try {
                future.checkedGet();
                LOG.trace("Update qos and queues to ovsdb for node {} {}", ovsdbNodeId, ovsdbNodeAugmentationIid);
            } catch (TransactionCommitFailedException e) {
                LOG.warn("Failed to put {} ", ovsdbNodeAugmentationIid, e);
            }
            updateQosEntries(dataBroker, uni);
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
                .setOtherConfigValue(getSpeed(uni.getSpeed().getSpeed()))
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
                .setQueueOtherConfigValue(getSpeed(uni.getSpeed().getSpeed()))
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
        if (speedObject instanceof Speed10M) {
            // map to 1MB
            speed = "1000000";
        }
        else if (speedObject instanceof Speed100M) {
            // map to 2MB
            speed = "2000000";
        }
        else if (speedObject instanceof Speed1G) {
            // map to 3MB
            speed = "3000000";
        }
        else if (speedObject instanceof Speed10G) {
            // map to 4MB
            speed = "4000000";
        }
        return speed;
    }

    private static void updateQosEntries(DataBroker dataBroker, UniAugmentation uni) {
        Optional<Node> optionalNode = findOvsdbNode(dataBroker, uni);
        if (optionalNode.isPresent()) {
            NodeId ovsdbNodeId = optionalNode.get().getNodeId();
            Long queueNumber = 0L;
            List<QosEntries> qosList = optionalNode.get()
                    .getAugmentation(OvsdbNodeAugmentation.class)
                    .getQosEntries();
            LOG.trace("QOS entries list {} for node {}", qosList, ovsdbNodeId);
            QosEntriesKey qosEntryKey = null;
            for (QosEntries qosEntry : qosList) {
                qosEntryKey = qosEntry.getKey();
            }
            InstanceIdentifier<QueueList> queueIid = UnimgrMapper
                    .getOvsdbQueueListIid(ovsdbNodeId, qosEntryKey, queueNumber);

            Uuid queueUuid = null;
            List<Queues> queuesList = optionalNode.get()
                    .getAugmentation(OvsdbNodeAugmentation.class).getQueues();
            for (Queues queue : queuesList) {
                queueUuid = queue.getQueueUuid();
            }
            QueueList queueList = new QueueListBuilder()
                    .setKey(new QueueListKey(queueNumber))
                    .setQueueNumber(queueNumber)
                    .setQueueUuid(queueUuid)
                    .build();

            WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            transaction.put(LogicalDatastoreType.CONFIGURATION, queueIid, queueList, true);
            CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
            try {
                future.checkedGet();
                LOG.info("Update qos-entries to ovsdb for node {} {}", ovsdbNodeId, queueIid);
            } catch (TransactionCommitFailedException e) {
                LOG.warn("Failed to put {} ", queueIid, e);
            }
        }
    }

    public static void updateMaxRate (DataBroker dataBroker,
            UniAugmentation sourceUniAugmentation,
            UniAugmentation destinationUniAugmentation,
            EvcAugmentation evc) {
        Optional<Node> optionalNode;
        if (getSpeed(sourceUniAugmentation.getSpeed().getSpeed()).equals(getSpeed(evc.getIngressBw().getSpeed()))) {
            LOG.info("Source UNI speed matches EVC ingress BW");
        } else {
            // update Uni's ovsdbNodeRef qos-entries and queues for max-rate to match EVC ingress BW
            optionalNode = findOvsdbNode(dataBroker, sourceUniAugmentation);
            if (optionalNode.isPresent()) {
                updateQosMaxRate(dataBroker, optionalNode, evc);
                updateQueuesMaxRate(dataBroker, optionalNode, evc);
            }
        }

        if (getSpeed(destinationUniAugmentation.getSpeed().getSpeed()).equals(getSpeed(evc.getIngressBw().getSpeed()))) {
            LOG.info("Destination UNI speed matches EVC ingress BW");
        } else {
            // update Uni's ovsdbNodeRef qos-entries and queues for max-rate to match EVC ingress BW
            optionalNode = findOvsdbNode(dataBroker, destinationUniAugmentation);
            if (optionalNode.isPresent()) {
                updateQosMaxRate(dataBroker, optionalNode, evc);
                updateQueuesMaxRate(dataBroker, optionalNode, evc);
            }
        }
    }

    private static void updateQosMaxRate(DataBroker dataBroker,
            Optional<Node> optionalOvsdbNode,
            EvcAugmentation evc) {
        NodeId ovsdbNodeId = optionalOvsdbNode.get().getNodeId();
        List<QosEntries> qosList = optionalOvsdbNode.get()
                .getAugmentation(OvsdbNodeAugmentation.class)
                .getQosEntries();
        LOG.trace("QOS entries list {} for node {}", qosList, ovsdbNodeId);
        QosEntriesKey qosEntryKey = null;
        for (QosEntries qosEntry : qosList) {
            qosEntryKey = qosEntry.getKey();
        }
        InstanceIdentifier<QosOtherConfig> qosOtherConfigIid = UnimgrMapper
                .getQosOtherConfigIid(ovsdbNodeId, qosEntryKey);
        QosOtherConfig qOtherConfig = new QosOtherConfigBuilder()
                .setKey(new QosOtherConfigKey(UnimgrConstants.QOS_MAX_RATE))
                .setOtherConfigKey(UnimgrConstants.QOS_MAX_RATE)
                .setOtherConfigValue(getSpeed(evc.getIngressBw().getSpeed()))
                .build();
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION, qosOtherConfigIid, qOtherConfig, true);
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        try {
            future.checkedGet();
            LOG.info("Update qos-entries max-rate to ovsdb for node {} {}", ovsdbNodeId, qosOtherConfigIid);;
        } catch (TransactionCommitFailedException e) {
            LOG.warn("Failed to put {} ", qosOtherConfigIid, e);
        }
    }

    private static void updateQueuesMaxRate(DataBroker dataBroker,
            Optional<Node> optionalOvsdbNode,
            EvcAugmentation evc) {
        NodeId ovsdbNodeId = optionalOvsdbNode.get().getNodeId();
        List<Queues> queues = optionalOvsdbNode.get()
                .getAugmentation(OvsdbNodeAugmentation.class)
                .getQueues();
        QueuesKey queuesKey = null;
        for (Queues queue: queues) {
            queuesKey = queue.getKey();
        }
        InstanceIdentifier<QueuesOtherConfig> queuesOtherConfigIid = UnimgrMapper
                .getQueuesOtherConfigIid(ovsdbNodeId, queuesKey);
        QueuesOtherConfig queuesOtherConfig = new QueuesOtherConfigBuilder()
                .setKey(new QueuesOtherConfigKey(UnimgrConstants.QOS_MAX_RATE))
                .setQueueOtherConfigKey(UnimgrConstants.QOS_MAX_RATE)
                .setQueueOtherConfigValue(getSpeed(evc.getIngressBw().getSpeed()))
                .build();
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION, queuesOtherConfigIid, queuesOtherConfig, true);
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        try {
            future.checkedGet();
            LOG.info("Update queues max-rate to ovsdb for node {} {}", ovsdbNodeId, queuesOtherConfigIid);;
        } catch (TransactionCommitFailedException e) {
            LOG.warn("Failed to put {} ", queuesOtherConfigIid, e);
        }
    }

    /**
     * Creates an OVSDB node Id with an IP Address.
     * @param ipAddress The IP address of the UNI (therefo the OVSDB node)
     * @return A NodeId for a Specific Ovsdb Node Id
     */
    public static NodeId createOvsdbNodeId(IpAddress ipAddress) {
        final String nodeId = UnimgrConstants.OVSDB_PREFIX
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
        final VlanId vlanID = new VlanId(1);
        final OvsdbTerminationPointAugmentation terminationPoint = new OvsdbTerminationPointAugmentationBuilder()
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
        final NodeId uniNodeId = new NodeId(createUniNodeId(uni.getIpAddress()));
        boolean result = false;
        try {
            final InstanceIdentifier<Node> uniNodeIid = UnimgrMapper.getUniNodeIid(uniNodeId);
            final NodeKey uniNodeKey = new NodeKey(uniNodeId);
            final Node nodeData = new NodeBuilder()
                                    .setNodeId(uniNodeId)
                                    .setKey(uniNodeKey)
                                    .addAugmentation(UniAugmentation.class, uni)
                                    .build();
            final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            transaction.put(LogicalDatastoreType.CONFIGURATION, uniNodeIid, nodeData);
            final CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
            future.checkedGet();
            result = true;
            LOG.info("Created and submitted a new Uni node {}", nodeData.getNodeId());
        } catch (final Exception e) {
            LOG.error("Exception while creating Uni Node" + "Uni Node Id: {}", uniNodeId);
        }
        return result;
    }

    /**
     * Creates and submit an UNI Node by using the Data contained in the UniAugmentation
     * @param dataBroker The instance of the DataBroker to create transactions
     * @param uni The UNI's data
     * @return true if uni created
     */
    public static boolean createUniNode(DataBroker dataBroker, UniAugmentation uni, NodeId uniNodeId) {
        boolean result = false;
        try {
            final InstanceIdentifier<Node> uniNodeIid = UnimgrMapper.getUniNodeIid(uniNodeId);
            final NodeKey uniNodeKey = new NodeKey(uniNodeId);
            final Node nodeData = new NodeBuilder()
                                    .setNodeId(uniNodeId)
                                    .setKey(uniNodeKey)
                                    .addAugmentation(UniAugmentation.class, uni)
                                    .build();
            final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            transaction.put(LogicalDatastoreType.CONFIGURATION, uniNodeIid, nodeData);
            final CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
            future.checkedGet();
            result = true;
            LOG.info("Created and submitted a new Uni node {}", nodeData.getNodeId());
        } catch (final Exception e) {
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
        final InstanceIdentifier<TerminationPoint> tpIid = UnimgrMapper
                                                        .getTerminationPointIid(bridgeNode,
                                                                                portName);
        final OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder =
                                                     new OvsdbTerminationPointAugmentationBuilder();
        tpAugmentationBuilder.setName(portName);
        if (type != null) {
            tpAugmentationBuilder.setInterfaceType(SouthboundConstants.OVSDB_INTERFACE_TYPE_MAP.get(type));
        }
        final TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class,
                                  tpAugmentationBuilder.build());
        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
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
        final InstanceIdentifier<TerminationPoint> tpIid = UnimgrMapper
                                                        .getTerminationPointIid(bridgeNode,
                                                                                portName);
        final OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder =
                                                     new OvsdbTerminationPointAugmentationBuilder();
        tpAugmentationBuilder.setName(portName);
        tpAugmentationBuilder.setInterfaceType(null);
        if (uni.getSpeed() != null) {
            Uuid qosUuid = getQosUuid(dataBroker, uni);
            tpAugmentationBuilder.setQos(getQosUuid(dataBroker, uni));
            LOG.info("Updating Qos {} to termination point {}", qosUuid , bridgeName);
        }
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class,
                                  tpAugmentationBuilder.build());
        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION,
                        tpIid,
                        tpBuilder.build());
        transaction.submit();
    }

    private static Uuid getQosUuid(DataBroker dataBroker, Uni uni) {
        Uuid qosUuid = null;
        Optional<Node> optionalNode = findUniNode(dataBroker, uni.getIpAddress());

        if (optionalNode.isPresent()) {
            UniAugmentation uniAugmentation = optionalNode.get()
                    .getAugmentation(UniAugmentation.class);
            Optional<Node> ovsdbNode = findOvsdbNode(dataBroker, uniAugmentation);
            if (ovsdbNode.isPresent()) {
                List<QosEntries> qosEntries = ovsdbNode.get()
                        .getAugmentation(OvsdbNodeAugmentation.class)
                        .getQosEntries();
                for (QosEntries qosEntry : qosEntries) {
                    qosUuid = qosEntry.getQosUuid();
                }
            }
        }
        return qosUuid;
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
        final CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        try {
            future.checkedGet();
            result = true;
        } catch (final TransactionCommitFailedException e) {
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
        final InstanceIdentifier<TerminationPoint> terminationPointPath =
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
        final CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
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
        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.delete(store, genericNode);
        try {
            transaction.submit().checkedGet();
            result = true;
        } catch (final TransactionCommitFailedException e) {
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
        final Map<InstanceIdentifier<T>,T> result = new HashMap<InstanceIdentifier<T>,T>();
        if (changes != null && changes.entrySet() != null) {
            for (final Entry<InstanceIdentifier<?>, DataObject> created : changes.entrySet()) {
                if (klazz.isInstance(created.getValue())) {
                    @SuppressWarnings("unchecked")
                    final
                    T value = (T) created.getValue();
                    final Class<?> type = created.getKey().getTargetType();
                    if (type.equals(klazz)) {
                        @SuppressWarnings("unchecked") // Actually checked above
                        final
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
        final Set<InstanceIdentifier<T>> result = new HashSet<InstanceIdentifier<T>>();
        if (changes != null && changes.getRemovedPaths() != null) {
            for (final InstanceIdentifier<?> iid : changes.getRemovedPaths()) {
                if (iid.getTargetType().equals(klazz)) {
                    @SuppressWarnings("unchecked") // Actually checked above
                    final
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
        final List<Node> ovsdbNodes = getOvsdbNodes(dataBroker);
        Optional<Node> optionalOvsdb;
        if (!ovsdbNodes.isEmpty()) {
            for (final Node ovsdbNode : ovsdbNodes) {
                final OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode
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
        final List<Node> uniNodes = getUniNodes(dataBroker);
        if (!uniNodes.isEmpty()) {
            for (final Node uniNode : uniNodes) {
                final UniAugmentation uniAugmentation = uniNode.getAugmentation(UniAugmentation.class);
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
        final InstanceIdentifier<Node> nodeIid = UnimgrMapper.getOvsdbNodeIid(ovsdbNodeId);
        final Optional<Node> node = readNode(dataBroker,
                                       LogicalDatastoreType.OPERATIONAL,
                                       nodeIid);
        if (node.isPresent()) {
            final Node ovsdbNode = node.get();
            final OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode
                                                              .getAugmentation(OvsdbNodeAugmentation.class);
            final ConnectionInfo connectionInfo = ovsdbNodeAugmentation.getConnectionInfo();
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
        final List<Link> evcLinks = new ArrayList<>();
        final InstanceIdentifier<Topology> evcTopology = UnimgrMapper.getEvcTopologyIid();
        final Topology topology = UnimgrUtils.read(dataBroker,
                                             LogicalDatastoreType.OPERATIONAL,
                                             evcTopology);
        if (topology != null && topology.getLink() != null) {
            for (final Link link : topology.getLink()) {
                final EvcAugmentation evcAugmentation = link.getAugmentation(EvcAugmentation.class);
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
            final Ipv4Address ipv4 = new Ipv4Address(ip);
            final IpAddress ipAddress = new IpAddress(ipv4);
            return ipAddress;
        } catch (final UnknownHostException e) {
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
        final List<Node> ovsdbNodes = new ArrayList<>();
        final InstanceIdentifier<Topology> ovsdbTopoIdentifier = UnimgrMapper.getOvsdbTopologyIid();
        final Topology topology = UnimgrUtils.read(dataBroker,
                                             LogicalDatastoreType.OPERATIONAL,
                                             ovsdbTopoIdentifier);
        if (topology != null && topology.getNode() != null) {
            for (final Node node : topology.getNode()) {
                final OvsdbNodeAugmentation ovsdbNodeAugmentation = node.getAugmentation(OvsdbNodeAugmentation.class);
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
        final List<Node> uniNodes = new ArrayList<>();
        final InstanceIdentifier<Topology> topologyInstanceIdentifier = UnimgrMapper.getUniTopologyIid();
        final Topology topology = read(dataBroker,
                                 LogicalDatastoreType.CONFIGURATION,
                                 topologyInstanceIdentifier);
        if (topology != null && topology.getNode() != null) {
            for (final Node node : topology.getNode()) {
                final UniAugmentation uniAugmentation = node.getAugmentation(UniAugmentation.class);
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
        final List<Node> uniNodes = new ArrayList<>();
        final InstanceIdentifier<Topology> topologyInstanceIdentifier = UnimgrMapper.getUniTopologyIid();
        final Topology topology = read(dataBroker,
                                 store,
                                 topologyInstanceIdentifier);
        if (topology != null && topology.getNode() != null) {
            for (final Node node : topology.getNode()) {
                final UniAugmentation uniAugmentation = node.getAugmentation(UniAugmentation.class);
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
        final List<UniAugmentation> unis = new ArrayList<>();
        final InstanceIdentifier<Topology> topologyInstanceIdentifier = UnimgrMapper.getUniTopologyIid();
        final Topology topology = read(dataBroker,
                                 store,
                                 topologyInstanceIdentifier);
        if (topology != null && topology.getNode() != null) {
            for (final Node node : topology.getNode()) {
                final UniAugmentation uniAugmentation = node.getAugmentation(UniAugmentation.class);
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
        final InstanceIdentifier<Topology> topologyInstanceIdentifier = UnimgrMapper.getUniTopologyIid();
        final Topology topology = read(dataBroker,
                                 store,
                                 topologyInstanceIdentifier);
        if (topology != null && topology.getNode() != null) {
            for (final Node node : topology.getNode()) {
                final UniAugmentation uniAugmentation = node.getAugmentation(UniAugmentation.class);
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
        final CheckedFuture<Optional<D>, ReadFailedException> future = transaction.read(store, path);
        try {
            optionalDataObject = future.checkedGet();
            if (optionalDataObject.isPresent()) {
                result = optionalDataObject.get();
            } else {
                LOG.debug("{}: Failed to read {}",
                        Thread.currentThread().getStackTrace()[1], path);
            }
        } catch (final ReadFailedException e) {
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
        final ReadTransaction read = dataBroker.newReadOnlyTransaction();
        final InstanceIdentifier<Node> nodeIid = genericNode.firstIdentifierOf(Node.class);
        final CheckedFuture<Optional<Node>, ReadFailedException> nodeFuture =
                                                              read.read(LogicalDatastoreType.OPERATIONAL,
                                                                        nodeIid);
        try {
            return nodeFuture.checkedGet();
        } catch (final ReadFailedException e) {
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
        final ReadTransaction read = dataBroker.newReadOnlyTransaction();
        final InstanceIdentifier<Link> linkIid = genericNode.firstIdentifierOf(Link.class);
        final CheckedFuture<Optional<Link>, ReadFailedException> linkFuture = read.read(store, linkIid);
        try {
            return linkFuture.checkedGet();
        } catch (final ReadFailedException e) {
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
        final ReadTransaction read = dataBroker.newReadOnlyTransaction();
        final InstanceIdentifier<Node> nodeIid = genericNode.firstIdentifierOf(Node.class);
        final CheckedFuture<Optional<Node>, ReadFailedException> nodeFuture = read
                .read(store, nodeIid);
        try {
            return nodeFuture.checkedGet();
        } catch (final ReadFailedException e) {
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
     * @return true if uni is updated
     */
    public static boolean updateUniNode(LogicalDatastoreType dataStore,
                                     InstanceIdentifier<?> uniKey,
                                     UniAugmentation uni,
                                     Node ovsdbNode,
                                     DataBroker dataBroker) {
        final InstanceIdentifier<Node> ovsdbNodeIid = UnimgrMapper.getOvsdbNodeIid(ovsdbNode.getNodeId());
        final OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(ovsdbNodeIid);
        final UniAugmentationBuilder updatedUniBuilder = new UniAugmentationBuilder(uni);
        if (ovsdbNodeRef != null) {
            updatedUniBuilder.setOvsdbNodeRef(ovsdbNodeRef);
        }
        final Optional<Node> optionalNode = readNode(dataBroker,
                                               LogicalDatastoreType.CONFIGURATION,
                                               uniKey);
        if (optionalNode.isPresent()) {
            final Node node = optionalNode.get();
            final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            final NodeBuilder nodeBuilder = new NodeBuilder();
            nodeBuilder.setKey(node.getKey());
            nodeBuilder.setNodeId(node.getNodeId());
            nodeBuilder.addAugmentation(UniAugmentation.class, updatedUniBuilder.build());
            transaction.put(dataStore, uniKey.firstIdentifierOf(Node.class), nodeBuilder.build());
            transaction.submit();
            return true;
        }
        return false;
    }

    /**
     * Update a specific UNI node on a specific datastore type
     * @param dataStore The datastore type
     * @param uniKey The UNI key
     * @param uni The Uni's data
     * @param ovsdbNodeIid The Ovsdb Node Instance Identifier
     * @param dataBroker The dataBroker instance to create transactions
     * @return true if uni is updated
     */
    public static boolean updateUniNode(LogicalDatastoreType dataStore,
                                     InstanceIdentifier<?> uniKey,
                                     UniAugmentation uni,
                                     InstanceIdentifier<?> ovsdbNodeIid,
                                     DataBroker dataBroker) {
        final OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(ovsdbNodeIid);
        final UniAugmentationBuilder updatedUniBuilder = new UniAugmentationBuilder(uni);
        if (ovsdbNodeRef != null) {
            updatedUniBuilder.setOvsdbNodeRef(ovsdbNodeRef);
        }
        final Optional<Node> optionalNode = readNode(dataBroker,
                                               LogicalDatastoreType.CONFIGURATION,
                                               uniKey);
        if (optionalNode.isPresent()) {
            final Node node = optionalNode.get();
            final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            final NodeBuilder nodeBuilder = new NodeBuilder();
            nodeBuilder.setKey(node.getKey());
            nodeBuilder.setNodeId(node.getNodeId());
            nodeBuilder.addAugmentation(UniAugmentation.class, updatedUniBuilder.build());
            transaction.put(dataStore, uniKey.firstIdentifierOf(Node.class), nodeBuilder.build());
            transaction.submit();
            return true;
        }
        return false;
    }

    /**
     * Updates a specific EVC into a specific DataStore type
     *
     * @param dataStore
     *            The datastore type
     * @param evcKey
     *            The EVC key
     * @param evcAugmentation
     *            The EVC's data
     * @param sourceUniIid
     *            The Source Uni Instance Identifier
     * @param destinationUniIid
     *            The destination Uni Instance Identifier
     * @param dataBroker
     *            The dataBroker instance to create transactions
     * @return
     */
    public static boolean updateEvcNode(LogicalDatastoreType dataStore, InstanceIdentifier<?> evcKey,
            EvcAugmentation evcAugmentation, InstanceIdentifier<?> sourceUniIid,
            InstanceIdentifier<?> destinationUniIid, DataBroker dataBroker) {
        final EvcAugmentationBuilder updatedEvcBuilder = new EvcAugmentationBuilder(evcAugmentation);
        if (sourceUniIid != null && destinationUniIid != null) {
            final List<UniSource> sourceList = new ArrayList<UniSource>();
            final UniSourceKey sourceKey = evcAugmentation.getUniSource().iterator().next().getKey();
            final short sourceOrder = evcAugmentation.getUniSource().iterator().next().getOrder();
            final IpAddress sourceIp = evcAugmentation.getUniSource().iterator().next().getIpAddress();
            final UniSource uniSource = new UniSourceBuilder().setOrder(sourceOrder).setKey(sourceKey).setIpAddress(sourceIp)
                    .setUni(sourceUniIid).build();
            sourceList.add(uniSource);
            updatedEvcBuilder.setUniSource(sourceList);

            final List<UniDest> destinationList = new ArrayList<UniDest>();
            final UniDestKey destKey = evcAugmentation.getUniDest().iterator().next().getKey();
            final short destOrder = evcAugmentation.getUniDest().iterator().next().getOrder();
            final IpAddress destIp = evcAugmentation.getUniDest().iterator().next().getIpAddress();
            final UniDest uniDest = new UniDestBuilder().setIpAddress(destIp).setOrder(destOrder).setKey(destKey)
                    .setUni(destinationUniIid).build();
            destinationList.add(uniDest);
            updatedEvcBuilder.setUniDest(destinationList);
            final Optional<Link> optionalEvcLink = readLink(dataBroker, LogicalDatastoreType.CONFIGURATION, evcKey);
            if (optionalEvcLink.isPresent()) {
                final Link link = optionalEvcLink.get();
                final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
                final LinkBuilder linkBuilder = new LinkBuilder();
                linkBuilder.setKey(link.getKey());
                linkBuilder.setLinkId(link.getLinkId());
                linkBuilder.setDestination(link.getDestination());
                linkBuilder.setSource(link.getSource());
                linkBuilder.addAugmentation(EvcAugmentation.class, updatedEvcBuilder.build());
                transaction.put(dataStore, evcKey.firstIdentifierOf(Link.class), linkBuilder.build());
                transaction.submit();
                return true;
            } else {
                LOG.info("EvcLink is not present: " + optionalEvcLink.get().getKey());
            }
        } else {
            LOG.info("Invalid instance identifiers for sourceUni and destUni.");
        }
        return false;
    }
}