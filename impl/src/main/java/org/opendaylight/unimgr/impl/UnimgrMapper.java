/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.impl;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev150622.Evcs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev150622.Unis;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev150622.evcs.Evc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev150622.evcs.EvcKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev150622.unis.Uni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev150622.unis.UniKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnimgrMapper {

    private static final Logger LOG = LoggerFactory.getLogger(UnimgrMapper.class);

    public static InstanceIdentifier<Unis> getUnisIid() {
        return InstanceIdentifier.builder(Unis.class)
                .build();
    }

    public static InstanceIdentifier<Uni> getUniIid() {
        return InstanceIdentifier.builder(Unis.class)
                .child(Uni.class)
                .build();
    }

    public static InstanceIdentifier<Uni> getUniIid(String id) {
        return InstanceIdentifier.builder(Unis.class)
                .child(Uni.class, new UniKey(new NodeId(id)))
                .build();
    }

    public static InstanceIdentifier<Uni> getUniIid(UniKey uniKey) {
        return InstanceIdentifier.builder(Unis.class)
                .child(Uni.class, uniKey)
                .build();
    }

    public static InstanceIdentifier<Uni> getUniIid(NodeId uniNodeId) {
        return InstanceIdentifier.builder(Unis.class)
                .child(Uni.class, new UniKey(uniNodeId))
                .build();
    }

    public static InstanceIdentifier<Uni> getUniIid(Uni uni) {
        return InstanceIdentifier.builder(Unis.class)
                .child(Uni.class, uni.getKey())
                .build();
    }

    public static InstanceIdentifier<Evcs> getEvcsIid() {
        return InstanceIdentifier.builder(Evcs.class)
                .build();
    }

    public static InstanceIdentifier<Evc> getEvcIid() {
        return InstanceIdentifier.builder(Evcs.class)
                .child(Evc.class)
                .build();
    }

    public static InstanceIdentifier<Evc> getEvcIid(String id) {
        return InstanceIdentifier.builder(Evcs.class)
                .child(Evc.class, new EvcKey(new NodeId(id)))
                .build();
    }

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

    public static NodeId createNodeId(IpAddress ipAddress) {
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
}
