/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.tapi;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.unimgr.mef.nrp.impl.NrpInitializer;
import org.opendaylight.unimgr.mef.nrp.ovs.DataStoreTestUtils;
import org.opendaylight.unimgr.mef.nrp.ovs.OpenFlowTopologyTestUtils;
import org.opendaylight.unimgr.mef.nrp.ovs.OvsdbTopologyTestUtils;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Context;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.Context1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortNumberUni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

/**
 * @author marek.ryznar@amartus.com
 */
public class TopologyDataHandlerTestUtils {
    private static String bridgeName = "br1";
    private static String ofBridgeName = "openflow:1";
    private static String tp1Name = "br1-eth1";
    private static String tp2Name = "br1-eth2";
    private static String tp3Name = "br1-eth3";
    private static Long tp1OFport = 1L;
    private static Long tp2OFport = 2L;
    private static Long tp3OFport = 3L;

    private static final String prestoNrpTopoId = "mef:presto-nrp-topology-system";
    private static final String ovsNodeId = "ovs-node";

    private final DataBroker dataBroker;

    protected TopologyDataHandlerTestUtils(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    /**
     * Creates ovsdb bridge "br1" with 3 ports:
     *  1. tp1 - nep
     *  2. tp2 - nep
     *  3. tp3 - not nep, becouse it is connected to other switch
     */
    public void createTestBridge() {
        List<TerminationPoint> tps = new LinkedList<>();

        tps.add(OvsdbTopologyTestUtils.createTerminationPoint(tp1Name,tp1OFport));
        tps.add(OvsdbTopologyTestUtils.createTerminationPoint(tp2Name,tp2OFport));
        tps.add(OvsdbTopologyTestUtils.createTerminationPoint(tp3Name,tp3OFport));

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node node = OvsdbTopologyTestUtils.createBridge(bridgeName,tps);
        InstanceIdentifier instanceIdentifier = OvsdbTopologyTestUtils.getNodeInstanceIdentifier(node.getNodeId());
        DataStoreTestUtils.write(node,instanceIdentifier,dataBroker);
    }

    protected void deleteTestBridge() {
        InstanceIdentifier instanceIdentifier = OvsdbTopologyTestUtils.getNodeInstanceIdentifier(new NodeId(bridgeName));
        DataStoreTestUtils.delete(instanceIdentifier,dataBroker);
    }

    protected void deletePort(String port) {
        InstanceIdentifier<TerminationPoint> tpIid = OvsdbTopologyTestUtils.getPortInstanceIdentifier(bridgeName,port);
        DataStoreTestUtils.delete(tpIid,dataBroker);
    }

    protected void addPort(String bridgeName, String portName, Long ofNumber) {
        String bridgeId = bridgeName;
        //openflow init
        NodeConnector nodeConnector = OpenFlowTopologyTestUtils.createNodeConnector(ofBridgeName,ofNumber,portName);
        InstanceIdentifier<NodeConnector> nodeConnectorInstanceIdentifier =
                OpenFlowTopologyTestUtils.getNodeConnectorInstanceIdentifier(ofBridgeName,ofBridgeName+":"+ofNumber.toString());
        DataStoreTestUtils.write(nodeConnector,nodeConnectorInstanceIdentifier,dataBroker);
        //ovsdb init
        TerminationPoint terminationPoint = OvsdbTopologyTestUtils.createTerminationPoint(portName,ofNumber);
        InstanceIdentifier<TerminationPoint> tpIid = OvsdbTopologyTestUtils.getPortInstanceIdentifier(bridgeId,portName);
        DataStoreTestUtils.write(terminationPoint,tpIid,dataBroker);
    }

    /**
     * Creates OpenFlow Nodes with one Node ("openflow:1" openflow equivalent of ovsdb's "br1"), which consist of 3 NodeConnectors:
     *  1. id:"openflow:1:1", name: "br1-eth1", portNumber: "1"
     *  2. id:"openflow:1:2", name: "br1-eth2", portNumber: "2"
     *  3. id:"openflow:1:3", name: "br1-eth3", portNumber: "3"
     */
    protected void createOpenFlowNodes() {
        NodesBuilder nodesBuilder = new NodesBuilder();
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodeList = new ArrayList<>();
        nodeList.add(createOpenFlowNode(ofBridgeName));
        nodesBuilder.setNode(nodeList);
        Nodes nodes = nodesBuilder.build();
        InstanceIdentifier<Nodes> nodesIId = InstanceIdentifier.builder(Nodes.class).build();

        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        transaction.put(LogicalDatastoreType.OPERATIONAL,nodesIId,nodes);
        transaction.submit();
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node createOpenFlowNode(String oFName) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder nodeBuilder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder();
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId nodeId =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(oFName);
        nodeBuilder.setId(nodeId);
        nodeBuilder.setKey(new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey(nodeId));
        List<NodeConnector> nodeConnectorList = new ArrayList<>();
        nodeConnectorList.add(createNodeConnector(oFName,tp1OFport,tp1Name));
        nodeConnectorList.add(createNodeConnector(oFName,tp2OFport,tp2Name));
        nodeConnectorList.add(createNodeConnector(oFName,tp3OFport,tp3Name));
        nodeBuilder.setNodeConnector(nodeConnectorList);
        return nodeBuilder.build();
    }

    private NodeConnector createNodeConnector(String ofBridgeName, Long portNumber, String ovsdbPortName) {
        NodeConnectorBuilder nodeConnectorBuilder = new NodeConnectorBuilder();
        String ofPortName = ofBridgeName + ":" + portNumber.toString();
        NodeConnectorId nodeConnectorId = new NodeConnectorId(ofPortName);
        nodeConnectorBuilder.setId(nodeConnectorId);
        nodeConnectorBuilder.setKey(new NodeConnectorKey(nodeConnectorId));
        nodeConnectorBuilder.addAugmentation(FlowCapableNodeConnector.class,createFlowCapableNodeConnector(ovsdbPortName,portNumber));
        return nodeConnectorBuilder.build();
    }

    private FlowCapableNodeConnector createFlowCapableNodeConnector(String ovsdbName, Long portNumber) {
        FlowCapableNodeConnectorBuilder flowCapableNodeConnectorBuilder = new FlowCapableNodeConnectorBuilder();
        flowCapableNodeConnectorBuilder.setName(ovsdbName);
        flowCapableNodeConnectorBuilder.setPortNumber(new PortNumberUni(portNumber));
        return flowCapableNodeConnectorBuilder.build();
    }

    protected void createPrestoSystemTopology() {
        NrpInitializer nrpInitializer = new NrpInitializer(dataBroker);
        try {
            nrpInitializer.init();
        } catch (Exception e) {
            fail("Could not initialize NRP topology.");
        }
    }

    protected org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.Node readOvsNode() {
        return DataStoreTestUtils.read(getNodeIid(),dataBroker);
    }

    protected List<org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.context.attrs.ServiceInterfacePoint> readSips() {
        ReadWriteTransaction readWriteTransaction = dataBroker.newReadWriteTransaction();
        try {
            Optional<Context> opt = readWriteTransaction.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Context.class)).checkedGet();
            if (opt.isPresent()) {
                return opt.get().getServiceInterfacePoint();
            } else {
                fail("There are no sips.");
            }
        } catch (ReadFailedException e) {
            fail(e.getMessage());
        }
        return null;
    }

    private static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.Node> getNodeIid() {
        return getTopoIid()
                .child(org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.Node.class,
                        new org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.NodeKey(new Uuid(ovsNodeId)));
    }

    private static InstanceIdentifier getTopoIid() {
        return InstanceIdentifier.create(Context.class)
                .augmentation(Context1.class)
                .child(org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.context.Topology.class,
                        new org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.context.TopologyKey(new Uuid(prestoNrpTopoId)));
    }
}