/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.impl;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.utils.UniUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntriesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Queues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QueuesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosOtherConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosOtherConfigKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesOtherConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesOtherConfigKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class UnimgrMapper {

    /**
     * Create an OVSDB Bridge node Instance Identifier by constructing
     * a nodeId manually. This function is different from
     * getOvsdbBridgeNodeIid
     * @param ovsdbNode The OVSDB node that has the OvsdbNodeAugmentation
     * @param bridgeName The bridge name appended to the URI.
     * @return An Instance Identifier of a bridge
     */
    public static InstanceIdentifier<Node> createOvsdbBridgeNodeIid(Node ovsdbNode,
                                                                    String bridgeName) {
        String bridgeNodeName = ovsdbNode.getNodeId().getValue()
                            + UnimgrConstants.DEFAULT_BRIDGE_NODE_ID_SUFFIX
                            + bridgeName;
        NodeId bridgeNodeId = new NodeId(bridgeNodeName);
        InstanceIdentifier<Node> bridgeNodePath = InstanceIdentifier
                                                      .create(NetworkTopology.class)
                                                      .child(Topology.class,
                                                              new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                                                      .child(Node.class, new NodeKey(bridgeNodeId));
        return bridgeNodePath;
    }

    /**
     * Generates an Instance Identifier by using a Link ID. The Link Id
     * is appended as a link key.
     * @param id The LinkId of a given Link. Similiar to a nodeId.
     * @return An Instance Identifier for a given Link
     */
    public static InstanceIdentifier<Link> getEvcLinkIid(LinkId id) {
        InstanceIdentifier<Link> linkPath = InstanceIdentifier
                                                .create(NetworkTopology.class)
                                                .child(Topology.class,
                                                        new TopologyKey(UnimgrConstants.EVC_TOPOLOGY_ID))
                                                .child(Link.class,
                                                        new LinkKey(id));
        return linkPath;
    }

    /**
     * Generates an Instance Identifier for the unimgr:evc URI.
     * @return An Instance Identifier for the EVC topology: unimgr:evc
     */
    public static InstanceIdentifier<Topology> getEvcTopologyIid() {
        InstanceIdentifier<Topology> topoPath = InstanceIdentifier
                                                    .create(NetworkTopology.class)
                                                    .child(Topology.class,
                                                            new TopologyKey(UnimgrConstants.EVC_TOPOLOGY_ID));
        return topoPath;
    }

    /**
     * Generates an Instance Identifier for the EVC nodes.
     * @return An Instance Identifier for the EVC nodes.
     */
    public static InstanceIdentifier<Node> getEvcTopologyNodeIid() {
        InstanceIdentifier<Node> nodePath = InstanceIdentifier
                                                .create(NetworkTopology.class)
                                                .child(Topology.class,
                                                        new TopologyKey(UnimgrConstants.EVC_TOPOLOGY_ID))
                                                .child(Node.class);
        return nodePath;
    }

    /**
     * Generates an Instance Identifier for an OvsdbBridgeNode by retrieving the Iid
     * via the OvsdbNodeAugmentation's BridgeRef.
     * the same as createOvsdbBridgeNodeIid.
     * @param ovsdbNode the ovsdb node
     * @return An Instance Identifier for a bridge associated with an OVSDB node.
     */
    public static InstanceIdentifier<Node> getOvsdbBridgeNodeIid(Node ovsdbNode) {
        OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
        InstanceIdentifier<Node> nodePath = ovsdbNodeAugmentation
                                                .getManagedNodeEntry()
                                                .iterator()
                                                .next()
                                                .getBridgeRef()
                                                .getValue()
                                                .firstIdentifierOf(Node.class);
        return nodePath;
    }

    /**
     * Generates an Instance Identifier for a specific OVSDB node
     * by using its IP address.
     * @param ipAddress The IP address of the OVSDB node. This should be the remote IP
     * @return An Instance Identifier for a specific OVSDB node
     */
    public static InstanceIdentifier<Node> getOvsdbNodeIid(IpAddress ipAddress) {
        String nodeId = UnimgrConstants.OVSDB_PREFIX
                            + ipAddress.getIpv4Address().getValue().toString()
                            + ":"
                            + UnimgrConstants.OVSDB_PORT;
        InstanceIdentifier<Node> nodePath = InstanceIdentifier
                                                .create(NetworkTopology.class)
                                                .child(Topology.class,
                                                        new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                                                .child(Node.class,
                                                        new NodeKey(new NodeId(nodeId)));
        return nodePath;
    }

    /**
     * Generates an Instance Identifier for a specific OVSDB node by
     * using the node Id.
     * @param nodeId The node ID of a specific OVSDB node.
     * @return An Instance Identifier for a specific OVSDB node.
     */
    public static InstanceIdentifier<Node> getOvsdbNodeIid(NodeId nodeId) {
        InstanceIdentifier<Node> nodePath = InstanceIdentifier
                                                .create(NetworkTopology.class)
                                                .child(Topology.class,
                                                        new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                                                .child(Node.class,
                                                        new NodeKey(nodeId));
        return nodePath;
    }

    /**
     * Generates an Instance Identifier for the OVSDB topology ovsdb:1.
     * @return An Instance Identifier for the OVSDB topology ovsdb:1
     */
    public static InstanceIdentifier<Topology> getOvsdbTopologyIid() {
        InstanceIdentifier<Topology> topoPath = InstanceIdentifier
                                                    .create(NetworkTopology.class)
                                                    .child(Topology.class,
                                                            new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID));
        return topoPath;
    }

    /**
     * Generates an Instance Identifier for a Termination Point by using
     * the Bridge Node and the Port Name.
     * @param bridgeNode The bridge where the port resides.
     * @param portName The name of the port, example: eth0
     * @return instance identifier
     */
    public static InstanceIdentifier<TerminationPoint> getTerminationPointIid(
                                                           Node bridgeNode,
                                                           String portName) {
        InstanceIdentifier<TerminationPoint> terminationPointPath =
                                                 InstanceIdentifier
                                                     .create(NetworkTopology.class)
                                                     .child(Topology.class,
                                                             new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                                                     .child(Node.class, bridgeNode.getKey())
                                                     .child(TerminationPoint.class,
                                                             new TerminationPointKey(new TpId(portName)));
        return terminationPointPath;
    }

    /**
     * Generates an Instance Identifier for a Termination Point
     * by using the bridge node and the Termination Point ID.
     * @param bridgeNode The bridge Node to where the TP resides.
     * @param tpId The termination point ID
     * @return An Instance Identifier for a specific TP
     */
    public static InstanceIdentifier<TerminationPoint> getTerminationPointIid(
                                                           Node bridgeNode,
                                                           TpId tpId) {
        InstanceIdentifier<TerminationPoint> terminationPointPath =
                                                 InstanceIdentifier
                                                     .create(NetworkTopology.class)
                                                     .child(Topology.class,
                                                             new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                                                     .child(Node.class,
                                                             bridgeNode.getKey())
                                                     .child(TerminationPoint.class,
                                                             new TerminationPointKey(tpId));
        return terminationPointPath;
    }

    /**
     * Generates an Instance Identifier for a UNI by querying the datastore.
     * Query will ask the Operational store by default.
     * @param dataBroker the data broker
     * @param ip The IP of the UNI
     * @return An Instance Identifier of a UNI by using its IP address.
     */
    public static InstanceIdentifier<Node> getUniIid(DataBroker dataBroker,
                                                     IpAddress ip) {
        List<Node> uniNodes = UniUtils.getUniNodes(dataBroker,
                                                      LogicalDatastoreType.OPERATIONAL);
        for (Node node : uniNodes) {
            UniAugmentation uniAugmentation = node.getAugmentation(UniAugmentation.class);
            if (uniAugmentation.getIpAddress().equals(ip)) {
                InstanceIdentifier<Node> uniNode = InstanceIdentifier
                                                       .create(NetworkTopology.class)
                                                       .child(Topology.class,
                                                               new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID))
                                                       .child(Node.class,
                                                               new NodeKey(node.getKey()));
                return uniNode;
            }
        }
        return null;
    }

    /**
     * Generates an Instance Identifier for a UNI by querying the datastore
     * with the IP address of the UNI.
     * @param dataBroker the data broker
     * @param ip The IP of the UNI
     * @param store The store where the query should be sent
     * @return An Instance Identifier of a UNI by using its IP address.
     */
    public static InstanceIdentifier<Node> getUniIid(DataBroker dataBroker,
                                                     IpAddress ip,
                                                     LogicalDatastoreType store) {
        List<Node> uniNodes = UniUtils.getUniNodes(dataBroker,
                                                      store);
        for (Node node : uniNodes) {
            UniAugmentation uniAugmentation = node.getAugmentation(UniAugmentation.class);
            if (uniAugmentation.getIpAddress().equals(ip)) {
                InstanceIdentifier<Node> uniNode = InstanceIdentifier
                                                       .create(NetworkTopology.class)
                                                       .child(Topology.class,
                                                               new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID))
                                                       .child(Node.class,
                                                               new NodeKey(node.getKey()));
                return uniNode;
            }
        }
        return null;
    }

    /**
     * Generates an Instance Identifier for the UNI topology: unimgr:uni.
     * @return An Instance Identifier for the UNI topology
     */
    public static InstanceIdentifier<Topology> getUniTopologyIid() {
        InstanceIdentifier<Topology> topoPath = InstanceIdentifier
                                                   .create(NetworkTopology.class)
                                                   .child(Topology.class,
                                                           new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID));
        return topoPath;
    }

    /**
     * Generates an Instance Identifier for UNI nodes topology.
     * @return An Instance Identifier for the UNI nodes topology.
     */
    public static InstanceIdentifier<Node> getUniTopologyNodeIid() {
        InstanceIdentifier<Node> nodePath = InstanceIdentifier
                                                .create(NetworkTopology.class)
                                                .child(Topology.class,
                                                        new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID))
                                                .child(Node.class);
        return nodePath;
    }

    /**
     * Generates an Instance Identifier for a specific UNI node by
     * using the node Id.
     * @param nodeId The node ID of a specific UNI node.
     * @return An Instance Identifier for a specific UNI node.
     */
    public static InstanceIdentifier<Node> getUniNodeIid(NodeId nodeId) {
        InstanceIdentifier<Node> nodePath = InstanceIdentifier
                                            .create(NetworkTopology.class)
                                            .child(Topology.class,
                                                    new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID))
                                            .child(Node.class,
                                                    new NodeKey(nodeId));
        return nodePath;
    }

    /**
     * Generates an Instance Identifier for an OVSDB QoS queue list entry.
     * @param ovsdbNodeId the desired node id
     * @param qosEntryKey the key of the desired QoS entry
     * @param queueNumber the key of the desired queue entry
     * @return instance identifier
     */
    public static InstanceIdentifier<QueueList> getOvsdbQueueListIid(NodeId ovsdbNodeId,
            QosEntriesKey qosEntryKey,
            Long queueNumber) {
        InstanceIdentifier<QueueList> queueIid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(ovsdbNodeId))
                .augmentation(OvsdbNodeAugmentation.class)
                .child(QosEntries.class, qosEntryKey)
                .child(QueueList.class, new QueueListKey(queueNumber));
        return queueIid;
    }

    /**
     * Generates an Instance Identifier for an OVSDB QoS other config entry.
     * @param ovsdbNodeId the desired node id
     * @param qosEntryKey the key of the desired QoS entry
     * @return instance identifier
     */
    public static InstanceIdentifier<QosOtherConfig> getQosOtherConfigIid(NodeId ovsdbNodeId,
            QosEntriesKey qosEntryKey) {
        InstanceIdentifier<QosOtherConfig> qosOtherConfigIid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(ovsdbNodeId))
                .augmentation(OvsdbNodeAugmentation.class)
                .child(QosEntries.class, qosEntryKey)
                .child(QosOtherConfig.class, new QosOtherConfigKey(UnimgrConstants.QOS_MAX_RATE));
        return qosOtherConfigIid;
    }

    /**
     * Generates an Instance Identifier for an OVSDB queue other config entry.
     * @param ovsdbNodeId the desired node id
     * @param queuesKey the key of the desired queue entry
     * @return instance identifier
     */
    public static InstanceIdentifier<QueuesOtherConfig> getQueuesOtherConfigIid(NodeId ovsdbNodeId,
            QueuesKey queuesKey) {
        InstanceIdentifier<QueuesOtherConfig> queuesOtherConfig = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(ovsdbNodeId))
                .augmentation(OvsdbNodeAugmentation.class)
                .child(Queues.class, queuesKey)
                .child(QueuesOtherConfig.class, new QueuesOtherConfigKey(UnimgrConstants.QOS_MAX_RATE));
        return queuesOtherConfig;
    }

    /**
     * Generates an Instance Identifier for an OVSDB QoS entries list.
     * @param ovsdbNode the desired node
     * @param qosEntryKey the key of the desired QoS entry
     * @return instance identifier
     */
    public static InstanceIdentifier<QosEntries> getOvsdbQoSEntriesIid(Node ovsdbNode, QosEntriesKey qosEntryKey) {
        InstanceIdentifier<QosEntries> qosEntriesIid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(ovsdbNode.getNodeId()))
                .augmentation(OvsdbNodeAugmentation.class)
                .child(QosEntries.class, qosEntryKey);
        return qosEntriesIid;
    }

    /**
     * Generates an Instance Identifier for an OVSDB QoS queue list entry.
     * @param ovsdbNode the desired node
     * @param queuesKey the key of the desired queue list
     * @return instance identifier
     */
    public static InstanceIdentifier<Queues> getOvsdbQueuesIid(Node ovsdbNode, QueuesKey queuesKey) {
        InstanceIdentifier<Queues> queuesIid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(ovsdbNode.getNodeId()))
                .augmentation(OvsdbNodeAugmentation.class)
                .child(Queues.class, queuesKey);
        return queuesIid;
    }
}
