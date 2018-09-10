/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortNumberUni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * @author marek.ryznar@amartus.com
 */
public class OpenFlowTopologyTestUtils {

    public static void createOpenFlowNodes(List<Node> nodeList, DataBroker dataBroker) {
        NodesBuilder nodesBuilder = new NodesBuilder();
        nodesBuilder.setNode(nodeList);
        Nodes nodes = nodesBuilder.build();
        InstanceIdentifier<Nodes> nodesIId = InstanceIdentifier.builder(Nodes.class).build();
        DataStoreTestUtils.write(nodes,nodesIId,dataBroker);
    }

    public static org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node createOpenFlowNode(String oFName, List<NodeConnector> nodeConnectorList) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder nodeBuilder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder();
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId nodeId =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(oFName);
        nodeBuilder.setId(nodeId);
        nodeBuilder.withKey(new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey(nodeId));
        nodeBuilder.setNodeConnector(nodeConnectorList);
        nodeBuilder.addAugmentation(FlowCapableNode.class,createFlowCapableNode());
        return nodeBuilder.build();
    }

    public static NodeConnector createNodeConnector(String ofBridgeName, Long portNumber, String ovsdbPortName) {
        NodeConnectorBuilder nodeConnectorBuilder = new NodeConnectorBuilder();
        String ofPortName = ofBridgeName + ":" + portNumber.toString();
        NodeConnectorId nodeConnectorId = new NodeConnectorId(ofPortName);
        nodeConnectorBuilder.setId(nodeConnectorId);
        nodeConnectorBuilder.withKey(new NodeConnectorKey(nodeConnectorId));
        nodeConnectorBuilder.addAugmentation(FlowCapableNodeConnector.class,createFlowCapableNodeConnector(ovsdbPortName,portNumber));
        return nodeConnectorBuilder.build();
    }

    private static FlowCapableNodeConnector createFlowCapableNodeConnector(String ovsdbName, Long portNumber) {
        FlowCapableNodeConnectorBuilder flowCapableNodeConnectorBuilder = new FlowCapableNodeConnectorBuilder();
        flowCapableNodeConnectorBuilder.setName(ovsdbName);
        flowCapableNodeConnectorBuilder.setPortNumber(new PortNumberUni(portNumber));
        return flowCapableNodeConnectorBuilder.build();
    }

    public static FlowCapableNode createFlowCapableNode() {
        FlowCapableNodeBuilder flowCapableNodeBuilder = new FlowCapableNodeBuilder();

        flowCapableNodeBuilder.setTable(createOfTable());
        return flowCapableNodeBuilder.build();
    }

    private static List<Table> createOfTable() {
        List<Table> tables = new ArrayList<>();
        TableBuilder tableBuilder = new TableBuilder();
        tableBuilder.setId(Short.valueOf("0"));
        List<Flow> flows = new ArrayList<>();
        tableBuilder.setFlow(flows);
        tables.add(tableBuilder.build());
        return tables;
    }

    public static InstanceIdentifier<NodeConnector> getNodeConnectorInstanceIdentifier(String ofBridgeName, String nodeConnectorId) {
        return InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey(
                                new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(ofBridgeName)))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId(nodeConnectorId)))
                .build();
    }
}
