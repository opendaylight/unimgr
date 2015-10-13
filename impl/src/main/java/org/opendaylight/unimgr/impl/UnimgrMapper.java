/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnimgrMapper {

    private static final Logger LOG = LoggerFactory.getLogger(UnimgrMapper.class);

    public static InstanceIdentifier<Node> getOvsdbNodeIID(NodeId nodeId) {
        InstanceIdentifier<Node> nodePath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class,new NodeKey(nodeId));
        return nodePath;
    }

    public static InstanceIdentifier<Node> getOvsdbBridgeNodeIID(NodeId ovsdbNode, String bridgeName) {
        NodeId bridgeNodeId = new NodeId(ovsdbNode + UnimgrConstants.DEFAULT_BRIDGE_NODE_ID_SUFFIX + bridgeName);
        InstanceIdentifier<Node> nodePath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class,new NodeKey(bridgeNodeId));
        return nodePath;
    }

    public static InstanceIdentifier<Node> getOvsdbNodeIID(IpAddress ipAddress) {
        String nodeId = UnimgrConstants.OVSDB_PREFIX
                + ipAddress.getIpv4Address().getValue().toString()
                + ":"
                + UnimgrConstants.OVSDB_PORT;
        InstanceIdentifier<Node> nodePath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class,new NodeKey(new NodeId(nodeId)));
        return nodePath;
    }

    public static NodeId createOvsdbNodeId(IpAddress ipAddress) {
        String nodeId = UnimgrConstants.OVSDB_PREFIX
                + ipAddress.getIpv4Address().getValue().toString()
                + ":"
                + UnimgrConstants.OVSDB_PORT;
        return new NodeId(nodeId);
    }

    public static InstanceIdentifier<Node> getOvsdbTopologyIdentifier() {
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class,
                        new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class);
        return path;
    }

    public static InstanceIdentifier<TerminationPoint> createTerminationPointInstanceIdentifier(
            Node bridgeNode, String portName) {
        InstanceIdentifier<TerminationPoint> terminationPointPath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class,bridgeNode.getKey())
                .child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));

        LOG.debug("Termination point InstanceIdentifier generated : {}",terminationPointPath);
        return terminationPointPath;
    }

    public static InstanceIdentifier<Node> createUniIid() {
        InstanceIdentifier<Node> iid = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID))
                .child(Node.class);
        return iid;
    }

    public static InstanceIdentifier<Node> createEvcIid() {
        InstanceIdentifier<Node> iid = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.EVC_TOPOLOGY_ID))
                .child(Node.class);
        return iid;
    }

    public static InstanceIdentifier<Topology> createTopologyIid() {
        InstanceIdentifier<Topology> iid = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID));
        return iid;
    }

    public static List<Node> getOvsdbNodes(DataBroker dataBroker, IpAddress ipAddress) {
        List<Node> ovsdbNodes = new ArrayList<>();
        InstanceIdentifier<Topology> topologyInstanceIdentifier = createTopologyIid();
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

    public static List<Node> getUniNodes(DataBroker dataBroker, IpAddress ipAddress) {
        List<Node> uniNodes = new ArrayList<>();
        InstanceIdentifier<Topology> topologyInstanceIdentifier = createTopologyIid();
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
        InstanceIdentifier<Topology> topologyInstanceIdentifier = createTopologyIid();
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

    public static InstanceIdentifier<Link> getEvcLinkIID(LinkId id) {
        InstanceIdentifier<Link> linkPath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.EVC_TOPOLOGY_ID))
                .child(Link.class,new LinkKey(id));
        return linkPath;
    }

    public static OvsdbNodeRef createOvsdbNodeRef(IpAddress ipAddress) {
        // TODO Auto-generated method stub
        return null;
    }
}
