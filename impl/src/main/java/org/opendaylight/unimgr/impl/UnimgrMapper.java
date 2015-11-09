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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
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

    public static InstanceIdentifier<Link> getEvcLinkIid(LinkId id) {
        InstanceIdentifier<Link> linkPath = InstanceIdentifier
                                                .create(NetworkTopology.class)
                                                .child(Topology.class,
                                                        new TopologyKey(UnimgrConstants.EVC_TOPOLOGY_ID))
                                                .child(Link.class,
                                                        new LinkKey(id));
        return linkPath;
    }

    public static InstanceIdentifier<Topology> getEvcTopologyIid() {
        InstanceIdentifier<Topology> topoPath = InstanceIdentifier
                                                    .create(NetworkTopology.class)
                                                    .child(Topology.class,
                                                            new TopologyKey(UnimgrConstants.EVC_TOPOLOGY_ID));
        return topoPath;
    }

    public static InstanceIdentifier<Node> getEvcTopologyNodeIid() {
        InstanceIdentifier<Node> nodePath = InstanceIdentifier
                                                .create(NetworkTopology.class)
                                                .child(Topology.class,
                                                        new TopologyKey(UnimgrConstants.EVC_TOPOLOGY_ID))
                                                .child(Node.class);
        return nodePath;
    }

    public static InstanceIdentifier<Node> getOvsdbBridgeNodeIid(Node bridgeNode) {
        OvsdbNodeAugmentation ovsdbNodeAugmentation = bridgeNode.getAugmentation(OvsdbNodeAugmentation.class);
        InstanceIdentifier<Node> nodePath = ovsdbNodeAugmentation
                                                .getManagedNodeEntry()
                                                .iterator()
                                                .next()
                                                .getBridgeRef()
                                                .getValue()
                                                .firstIdentifierOf(Node.class);
        return nodePath;
    }

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

    public static InstanceIdentifier<Node> getOvsdbNodeIid(NodeId nodeId) {
        InstanceIdentifier<Node> nodePath = InstanceIdentifier
                                                .create(NetworkTopology.class)
                                                .child(Topology.class,
                                                        new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                                                .child(Node.class,
                                                        new NodeKey(nodeId));
        return nodePath;
    }

    public static InstanceIdentifier<Topology> getOvsdbTopologyIid() {
        InstanceIdentifier<Topology> topoPath = InstanceIdentifier
                                                    .create(NetworkTopology.class)
                                                    .child(Topology.class,
                                                            new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID));
        return topoPath;
    }

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

    public static InstanceIdentifier<Node> getUniIid(DataBroker dataBroker,
                                                     IpAddress ip) {
        List<Node> uniNodes = UnimgrUtils.getUniNodes(dataBroker,
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

    public static InstanceIdentifier<Node> getUniIid(DataBroker dataBroker,
                                                     IpAddress ip,
                                                     LogicalDatastoreType store) {
        List<Node> uniNodes = UnimgrUtils.getUniNodes(dataBroker,
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

    public static InstanceIdentifier<Topology> getUniTopologyIid() {
        InstanceIdentifier<Topology> topoPath = InstanceIdentifier
                                                   .create(NetworkTopology.class)
                                                   .child(Topology.class,
                                                           new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID));
        return topoPath;
    }

    public static InstanceIdentifier<Node> getUniTopologyNodeIid() {
        InstanceIdentifier<Node> nodePath = InstanceIdentifier
                                                .create(NetworkTopology.class)
                                                .child(Topology.class,
                                                        new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID))
                                                .child(Node.class);
        return nodePath;
    }

}
