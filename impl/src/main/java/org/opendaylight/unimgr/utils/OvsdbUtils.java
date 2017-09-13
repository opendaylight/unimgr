/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.utils;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Uni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbUtils {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbUtils.class);

    private OvsdbUtils() {
        throw new AssertionError("Instantiating utility class.");
    }

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
            ovsdbBridgeAugmentationBuilder.setProtocolEntry(OvsdbUtils.createMdsalProtocols());
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
            final Optional<Node> optionalOvsdbNode = MdsalUtils.readNode(dataBroker,
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
                final OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder =
                        new OvsdbBridgeAugmentationBuilder();
                ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(bridgeName));
                ovsdbBridgeAugmentationBuilder.setProtocolEntry(OvsdbUtils.createMdsalProtocols());
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
        final List<ControllerEntry> controllerEntries = new ArrayList<>();
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
            final Uuid qosUuid = getQosUuid(dataBroker, source);
            //tpAugmentationBuilder.setQos(getQosUuid(dataBroker, source));
            LOG.info("Updating Qos {} to termination point {}", qosUuid , bridgeName);
        }
        final TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
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
        final List<ProtocolEntry> protocolList = new ArrayList<>();
        final ImmutableBiMap<String, Class<? extends OvsdbBridgeProtocolBase>> mapper =
                SouthboundConstants.OVSDB_PROTOCOL_MAP.inverse();
        protocolList.add(new ProtocolEntryBuilder().setProtocol(
                mapper.get("OpenFlow13")).build());
        return protocolList;
    }

    /**
     * Creates a Bridge Augmentation by using a UNI.
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
                            OvsdbUtils.createOvsdbNodeAugmentation(uni))
                    .build();
            // Submit the node to the datastore
            final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            transaction.put(LogicalDatastoreType.CONFIGURATION, ovsdbNodeIid, nodeData);
            transaction.submit();
            LOG.info("Created and submitted a new OVSDB node {}", nodeData.getNodeId());
        } catch (final Exception e) {
            LOG.error("Exception while creating OvsdbNodeAugmentation, Uni is null. Node Id: {}", ovsdbNodeId, e);
        }
    }

    /**
     * Creates and submit an OvsdbNode by using the Data contained in the UniAugmentation.
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
                            OvsdbUtils.createOvsdbNodeAugmentation(uni))
                    .build();
            // Submit the node to the datastore
            final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            transaction.put(LogicalDatastoreType.CONFIGURATION, ovsdbNodeIid, nodeData);
            transaction.submit();
            LOG.info("Created and submitted a new OVSDB node {}", nodeData.getNodeId());
            return nodeData;
        } catch (final Exception e) {
            LOG.error("Exception while creating OvsdbNodeAugmentation, Uni is null. Node Id: {}", ovsdbNodeId, e);
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

    /**
     * Create and build an OvsdbNodeAugmentation.
     * @param uni the UNI data
     * @param remotePort port number
     * @return OvsdbNodeAugmentation
     */
    public static OvsdbNodeAugmentation createOvsdbNodeAugmentation(UniAugmentation uni,
            PortNumber remotePort) {
        final ConnectionInfo connectionInfos = new ConnectionInfoBuilder()
                .setRemoteIp(uni.getIpAddress())
                .setRemotePort(remotePort)
                .build();
        final OvsdbNodeAugmentation ovsdbNode = new OvsdbNodeAugmentationBuilder()
                .setConnectionInfo(connectionInfos)
                .setQosEntries(createQosEntries(uni))
                .setQueues(createQueues(uni))
                .build();
        return ovsdbNode;
    }

    /**
     * Create and write QoS forn an OVSDB node, copying from UNI entry.
     * @param dataBroker the data broker
     * @param uni the UNI to copy data from
     * @return null
     */
    public static Node createQoSForOvsdbNode(DataBroker dataBroker, UniAugmentation uni) {
        final Optional<Node> optionalNode = findOvsdbNode(dataBroker, uni);
        if (optionalNode.isPresent()) {
            final NodeId ovsdbNodeId = optionalNode.get().getNodeId();
            final InstanceIdentifier<OvsdbNodeAugmentation> ovsdbNodeAugmentationIid = UnimgrMapper
                    .getOvsdbNodeIid(ovsdbNodeId)
                    .augmentation(OvsdbNodeAugmentation.class);
            final OvsdbNodeAugmentation ovsdbNodeAugmentation = createOvsdbNodeAugmentation(uni,
                    getRemotePort(dataBroker, uni));
            final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            transaction.delete(LogicalDatastoreType.CONFIGURATION, ovsdbNodeAugmentationIid);
            transaction.put(LogicalDatastoreType.CONFIGURATION, ovsdbNodeAugmentationIid, ovsdbNodeAugmentation, true);
            final CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
            try {
                Thread.sleep(UnimgrConstants.OVSDB_UPDATE_TIMEOUT);
            } catch (final InterruptedException e) {
                LOG.warn("Interrupted while waiting after OVSDB node augmentation {}", ovsdbNodeId, e);
            }
            try {
                future.checkedGet();
                LOG.trace("Update qos and queues to ovsdb for node {} {}", ovsdbNodeId, ovsdbNodeAugmentationIid);
            } catch (final TransactionCommitFailedException e) {
                LOG.warn("Failed to put {} ", ovsdbNodeAugmentationIid, e);
            }
            updateQosEntries(dataBroker, uni);
        }
        return null;
    }

    private static PortNumber getRemotePort(DataBroker dataBroker, UniAugmentation uni) {
        PortNumber remotePort = null;
        final Optional<Node> optionalNode = findOvsdbNode(dataBroker, uni);

        if (optionalNode.isPresent()) {
            remotePort = optionalNode.get()
                    .getAugmentation(OvsdbNodeAugmentation.class)
                    .getConnectionInfo().getRemotePort();
        }
        return remotePort;
    }

    private static List<QosEntries> createQosEntries(Uni uni) {
        // Configure queue for best-effort dscp and max rate
        final List<QosOtherConfig> otherConfig = new ArrayList<>();
        QosOtherConfig qosOtherConfig = new QosOtherConfigBuilder()
                .setKey(new QosOtherConfigKey(UnimgrConstants.QOS_DSCP_ATTRIBUTE))
                .setOtherConfigKey(UnimgrConstants.QOS_DSCP_ATTRIBUTE)
                .setOtherConfigValue(UnimgrConstants.QOS_DSCP_ATTRIBUTE_VALUE)
                .build();
        otherConfig.add(qosOtherConfig);

        qosOtherConfig = new QosOtherConfigBuilder()
                .setKey(new QosOtherConfigKey(UnimgrConstants.QOS_MAX_RATE))
                .setOtherConfigKey(UnimgrConstants.QOS_MAX_RATE)
                .setOtherConfigValue(UniUtils.getSpeed(uni.getSpeed().getSpeed()))
                .build();
        otherConfig.add(qosOtherConfig);

        final Uuid qosUuid = new Uuid(UUID.randomUUID().toString());
        final QosEntries qosEntry = new QosEntriesBuilder()
                .setKey(new QosEntriesKey(new Uri(UnimgrConstants.QOS_PREFIX + qosUuid.getValue())))
                .setQosId(new Uri(UnimgrConstants.QOS_PREFIX + qosUuid.getValue()))
                .setQosOtherConfig(otherConfig)
                .setQosType(SouthboundMapper.createQosType(SouthboundConstants.QOS_LINUX_HTB))
                .build();

        final List<QosEntries> qosEntries = new ArrayList<>();
        qosEntries.add(qosEntry);
        return qosEntries;
    }

    private static List<Queues> createQueues(Uni uni) {
        final List<QueuesOtherConfig> otherConfig = new ArrayList<>();
        QueuesOtherConfig queuesOtherConfig = new QueuesOtherConfigBuilder()
                .setKey(new QueuesOtherConfigKey(UnimgrConstants.QOS_DSCP_ATTRIBUTE))
                .setQueueOtherConfigKey(UnimgrConstants.QOS_DSCP_ATTRIBUTE)
                .setQueueOtherConfigValue(UnimgrConstants.QOS_DSCP_ATTRIBUTE_VALUE)
                .build();
        otherConfig.add(queuesOtherConfig);

        queuesOtherConfig = new QueuesOtherConfigBuilder()
                .setKey(new QueuesOtherConfigKey(UnimgrConstants.QOS_MAX_RATE))
                .setQueueOtherConfigKey(UnimgrConstants.QOS_MAX_RATE)
                .setQueueOtherConfigValue(UniUtils.getSpeed(uni.getSpeed().getSpeed()))
                .build();
        otherConfig.add(queuesOtherConfig);

        // Configure dscp value for best-effort
        final Uuid queueUuid = new Uuid(UUID.randomUUID().toString());
        final Queues queues = new QueuesBuilder()
                .setDscp(Short.parseShort(UnimgrConstants.QOS_DSCP_ATTRIBUTE_VALUE))
                .setKey(new QueuesKey(new Uri(UnimgrConstants.QUEUE_PREFIX + queueUuid.getValue())))
                .setQueueId(new Uri(UnimgrConstants.QUEUE_PREFIX + queueUuid.getValue()))
                .setQueuesOtherConfig(otherConfig)
                .build();

        final List<Queues> queuesList = new ArrayList<>();
        queuesList.add(queues);
        return queuesList;
    }

    private static void updateQosEntries(DataBroker dataBroker, UniAugmentation uni) {
        final Optional<Node> optionalNode = findOvsdbNode(dataBroker, uni);
        if (optionalNode.isPresent()) {
            final NodeId ovsdbNodeId = optionalNode.get().getNodeId();
            final Long queueNumber = 0L;
            final List<QosEntries> qosList = optionalNode.get()
                    .getAugmentation(OvsdbNodeAugmentation.class)
                    .getQosEntries();
            LOG.trace("QOS entries list {} for node {}", qosList, ovsdbNodeId);
            QosEntriesKey qosEntryKey = null;
            for (final QosEntries qosEntry : qosList) {
                qosEntryKey = qosEntry.getKey();
            }
            final InstanceIdentifier<QueueList> queueIid = UnimgrMapper
                    .getOvsdbQueueListIid(ovsdbNodeId, qosEntryKey, queueNumber);

            Uuid queueUuid = null;
            final List<Queues> queuesList = optionalNode.get()
                    .getAugmentation(OvsdbNodeAugmentation.class).getQueues();
            for (final Queues queue : queuesList) {
                queueUuid = queue.getQueueUuid();
            }
            final QueueList queueList = new QueueListBuilder()
                    .setKey(new QueueListKey(queueNumber))
                    .setQueueNumber(queueNumber)
                    //.setQueueUuid(queueUuid)
                    .build();

            final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            transaction.delete(LogicalDatastoreType.CONFIGURATION, queueIid);
            transaction.put(LogicalDatastoreType.CONFIGURATION, queueIid, queueList, true);
            final CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
            try {
                future.checkedGet();
                LOG.info("Update qos-entries to ovsdb for node {} {}", ovsdbNodeId, queueIid);
            } catch (final TransactionCommitFailedException e) {
                LOG.warn("Failed to put {} ", queueIid, e);
            }
        }
    }

    /**
     * Write a new max rate into an EVC's objects.
     * @param dataBroker the data broker
     * @param sourceUniAugmentation source UNI
     * @param destinationUniAugmentation destination UNI
     * @param evc EVC link
     */
    public static void updateMaxRate(DataBroker dataBroker,
            UniAugmentation sourceUniAugmentation,
            UniAugmentation destinationUniAugmentation,
            EvcAugmentation evc) {
        Optional<Node> optionalNode;
        if (UniUtils.getSpeed(sourceUniAugmentation.getSpeed().getSpeed())
                .equals(UniUtils.getSpeed(evc.getIngressBw().getSpeed()))) {
            LOG.info("Source UNI speed matches EVC ingress BW");
        } else {
            // update Uni's ovsdbNodeRef qos-entries and queues for max-rate to match EVC ingress BW
            optionalNode = findOvsdbNode(dataBroker, sourceUniAugmentation);
            if (optionalNode.isPresent()) {
                updateQosMaxRate(dataBroker, optionalNode, evc);
                updateQueuesMaxRate(dataBroker, optionalNode, evc);
            }
        }

        if (UniUtils.getSpeed(destinationUniAugmentation.getSpeed().getSpeed())
                .equals(UniUtils.getSpeed(evc.getIngressBw().getSpeed()))) {
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
        final NodeId ovsdbNodeId = optionalOvsdbNode.get().getNodeId();
        final List<QosEntries> qosList = optionalOvsdbNode.get()
                .getAugmentation(OvsdbNodeAugmentation.class)
                .getQosEntries();
        LOG.trace("QOS entries list {} for node {}", qosList, ovsdbNodeId);
        QosEntriesKey qosEntryKey = null;
        for (final QosEntries qosEntry : qosList) {
            qosEntryKey = qosEntry.getKey();
        }
        final InstanceIdentifier<QosOtherConfig> qosOtherConfigIid = UnimgrMapper
                .getQosOtherConfigIid(ovsdbNodeId, qosEntryKey);
        final QosOtherConfig qOtherConfig = new QosOtherConfigBuilder()
                .setKey(new QosOtherConfigKey(UnimgrConstants.QOS_MAX_RATE))
                .setOtherConfigKey(UnimgrConstants.QOS_MAX_RATE)
                .setOtherConfigValue(UniUtils.getSpeed(evc.getIngressBw().getSpeed()))
                .build();
        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION, qosOtherConfigIid, qOtherConfig, true);
        final CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        try {
            future.checkedGet();
            LOG.info("Update qos-entries max-rate to ovsdb for node {} {}", ovsdbNodeId, qosOtherConfigIid);
        } catch (final TransactionCommitFailedException e) {
            LOG.warn("Failed to put {}", qosOtherConfigIid, e);
        }
    }

    private static void updateQueuesMaxRate(DataBroker dataBroker,
            Optional<Node> optionalOvsdbNode,
            EvcAugmentation evc) {
        final NodeId ovsdbNodeId = optionalOvsdbNode.get().getNodeId();
        final List<Queues> queues = optionalOvsdbNode.get()
                .getAugmentation(OvsdbNodeAugmentation.class)
                .getQueues();
        QueuesKey queuesKey = null;
        for (final Queues queue: queues) {
            queuesKey = queue.getKey();
        }
        final InstanceIdentifier<QueuesOtherConfig> queuesOtherConfigIid = UnimgrMapper
                .getQueuesOtherConfigIid(ovsdbNodeId, queuesKey);
        final QueuesOtherConfig queuesOtherConfig = new QueuesOtherConfigBuilder()
                .setKey(new QueuesOtherConfigKey(UnimgrConstants.QOS_MAX_RATE))
                .setQueueOtherConfigKey(UnimgrConstants.QOS_MAX_RATE)
                .setQueueOtherConfigValue(UniUtils.getSpeed(evc.getIngressBw().getSpeed()))
                .build();
        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION, queuesOtherConfigIid, queuesOtherConfig, true);
        final CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        try {
            future.checkedGet();
            LOG.info("Update queues max-rate to ovsdb for node {} {}", ovsdbNodeId, queuesOtherConfigIid);
        } catch (final TransactionCommitFailedException e) {
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
     * Creates a built OvsdbTerminationAugmentation with data.
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
     * Creates and Submit a termination point Node to the configuration DateStore.
     * @param dataBroker The instance of the data broker to create transactions
     * @param uni The UNI's data
     * @param bridgeNode The Bridge node
     * @param bridgeName The Bridge name (example: br0)
     * @param portName The Port name (example: eth0)
     * @param type The type of termination (example: gre) Refer to OVSDB_INTERFACE_TYPE_MAP
     *     to review the list of available Interface Types.
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
            final Uuid qosUuid = getQosUuid(dataBroker, uni);
            //tpAugmentationBuilder.setQos(getQosUuid(dataBroker, uni));
            LOG.info("Updating Qos {} to termination point {}", qosUuid , bridgeName);
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

    private static Uuid getQosUuid(DataBroker dataBroker, Uni uni) {
        Uuid qosUuid = null;
        final Optional<Node> optionalNode = UniUtils.findUniNode(dataBroker, uni.getIpAddress());

        if (optionalNode.isPresent()) {
            final UniAugmentation uniAugmentation = optionalNode.get()
                    .getAugmentation(UniAugmentation.class);
            final Optional<Node> ovsdbNode = findOvsdbNode(dataBroker, uniAugmentation);
            if (ovsdbNode.isPresent()) {
                final List<QosEntries> qosEntries = ovsdbNode.get()
                        .getAugmentation(OvsdbNodeAugmentation.class)
                        .getQosEntries();
                for (final QosEntries qosEntry : qosEntries) {
                    qosUuid = qosEntry.getQosUuid();
                }
            }
        }
        return qosUuid;
    }

    /**
     * Deletes a generic node.
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
    public static CheckedFuture<Void, TransactionCommitFailedException> deleteTerminationPoint(DataBroker dataBroker,
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
        return transaction.submit();
    }


    /**
     * Extract a data object by using its instance indentifier and it's class type.
     * @param changes Data Change object
     * @param klazz Class type
     * @return The extracted DataObject as an Object casted as the class type
     */
    public static <T extends DataObject> Map<InstanceIdentifier<T>,T> extract(
            Map<InstanceIdentifier<?>, DataObject> changes, Class<T> klazz) {
        final Map<InstanceIdentifier<T>,T> result = new HashMap<>();
        if (changes != null && changes.entrySet() != null) {
            for (final Entry<InstanceIdentifier<?>, DataObject> created : changes.entrySet()) {
                if (klazz.isInstance(created.getValue())) {
                    @SuppressWarnings("unchecked")
                    final T value = (T) created.getValue();
                    final Class<?> type = created.getKey().getTargetType();
                    if (type.equals(klazz)) {
                        @SuppressWarnings("unchecked") // Actually checked above
                        final InstanceIdentifier<T> iid = (InstanceIdentifier<T>) created.getKey();
                        result.put(iid, value);
                    }
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
     * Retrieves the connection information from an Ovsdb Connection by
     * using the Ovsdb Node Id.
     * @param dataBroker The dataBroker instance to create transactions
     * @param ovsdbNodeId The NodeId of the OVSDB node
     * @return The ConnectionInfo object
     */
    public static ConnectionInfo getConnectionInfo(DataBroker dataBroker,
            NodeId ovsdbNodeId) {
        final InstanceIdentifier<Node> nodeIid = UnimgrMapper.getOvsdbNodeIid(ovsdbNodeId);
        final Optional<Node> node = MdsalUtils.readNode(dataBroker,
                LogicalDatastoreType.OPERATIONAL,
                nodeIid);
        if (node.isPresent()) {
            final Node ovsdbNode = node.get();
            final OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode
                    .getAugmentation(OvsdbNodeAugmentation.class);
            return ovsdbNodeAugmentation.getConnectionInfo();
        } else {
            return null;
        }
    }

    /**
     * Retrieve the Local IP of the controller.
     * @return The LocalIp object of the Controller
     */
    public static IpAddress getLocalIp() {
        String ip;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
            final Ipv4Address ipv4 = new Ipv4Address(ip);
            return new IpAddress(ipv4);
        } catch (final UnknownHostException e) {
            LOG.info("Unable to retrieve controller's ip address, using loopback. {}", e);
        }
        return new IpAddress(UnimgrConstants.LOCAL_IP);
    }

    /**
     * Retrieve a list of Ovsdb Nodes from the Operational DataStore.
     * @param dataBroker The dataBroker instance to create transactions
     * @return The Ovsdb Node retrieved from the Operational DataStore
     */
    public static List<Node> getOvsdbNodes(DataBroker dataBroker) {
        final List<Node> ovsdbNodes = new ArrayList<>();
        final InstanceIdentifier<Topology> ovsdbTopoIdentifier = UnimgrMapper.getOvsdbTopologyIid();
        Topology topology = MdsalUtils.read(dataBroker,
                LogicalDatastoreType.OPERATIONAL,
                ovsdbTopoIdentifier);
        if (topology != null && topology.getNode() != null) {
            for (final Node node : topology.getNode()) {
                final OvsdbNodeAugmentation ovsdbNodeAugmentation = node.getAugmentation(OvsdbNodeAugmentation.class);
                if (ovsdbNodeAugmentation != null) {
                    ovsdbNodes.add(node);
                }
            }
        } else {
            topology = MdsalUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION, ovsdbTopoIdentifier);
            if (topology != null && topology.getNode() != null) {
                for (final Node node : topology.getNode()) {
                    final OvsdbNodeAugmentation ovsdbNodeAugmentation =
                            node.getAugmentation(OvsdbNodeAugmentation.class);
                    if (ovsdbNodeAugmentation != null) {
                        ovsdbNodes.add(node);
                    }
                }
            }
        }
        return ovsdbNodes;
    }
}
