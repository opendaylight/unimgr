/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.activator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.unimgr.mef.nrp.api.TopologyManager;
import org.opendaylight.unimgr.mef.nrp.common.ResourceNotAvailableException;
import org.opendaylight.unimgr.mef.nrp.common.TapiUtils;
import org.opendaylight.unimgr.mef.nrp.ovs.FlowTopologyTestUtils;
import org.opendaylight.unimgr.mef.nrp.ovs.OpenFlowTopologyTestUtils;
import org.opendaylight.unimgr.mef.nrp.ovs.OvsdbTopologyTestUtils;
import org.opendaylight.unimgr.mef.nrp.ovs.tapi.TopologyDataHandler;
import org.opendaylight.unimgr.mef.nrp.ovs.tapi.TopologyDataHandlerTestUtils;
import org.opendaylight.unimgr.mef.nrp.ovs.util.OpenFlowUtils;
import org.opendaylight.unimgr.mef.nrp.ovs.util.OvsdbUtils;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.common.types.rev180321.NaturalNumber;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.common.types.rev180321.PositiveInteger;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev180321.carrier.eth.connectivity.end.point.resource.CeVlanIdListAndUntag;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev180321.carrier.eth.connectivity.end.point.resource.IngressBwpFlow;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev180321.vlan.id.list.and.untag.VlanId;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.NrpConnectivityServiceEndPointAttrs;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.connectivity.service.end.point.attrs.NrpCarrierEthConnectivityEndPointResource;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ConnectivityServiceEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ServiceType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.end.point.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.OwnedNodeEdgePointRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * @author marek.ryznar@amartus.com
 */
@Ignore
public class OvsActivatorTest extends AbstractConcurrentDataBrokerTest {

    private DataBroker dataBroker;
    private OvsActivator ovsActivator;
    private static final String port1Name = "sip:ovs-node:s1:s1-eth1";
    private static final String port2Name = "sip:ovs-node:s5:s5-eth1";
    private static final String ofPort1Name = "openflow:1";
    private static final String ofPort2Name = "openflow:5";
    private static final Integer expectedVlanId = 200;
    private static final String serviceId = "serviceId";
    private static final String nodeId = "ovs-node";

    private static final String interswitchName = "interswitch-openflow";
    private static final String vlanName = "vlan1-openflow";
    private static final String dropName = "default-DROP";

    List<String> of1InterwitchPorts = Arrays.asList("openflow:1:3", "openflow:1:4", "openflow:1:5");
    List<String> of2InterwitchPorts = Arrays.asList("openflow:5:3", "openflow:5:4");

    @Before
    public void setUp() {
        //given
        dataBroker = getDataBroker();
        ovsActivator = new OvsActivator(dataBroker);
        OvsdbTopologyTestUtils.createOvsdbTopology(dataBroker);
        initTopologies();
        FlowTopologyTestUtils.createFlowTopology(dataBroker, getLinkList());

        TopologyDataHandlerTestUtils helper = new TopologyDataHandlerTestUtils(dataBroker);
        TopologyManager topologyManager = mock(TopologyManager.class);
        when(topologyManager.getSystemTopologyId()).thenReturn(TapiConstants.PRESTO_SYSTEM_TOPO);
        helper.createPrestoSystemTopology();
        new TopologyDataHandler(dataBroker, topologyManager).init();
    }

    @Test
    public void testActivate() {
        //given
        List<EndPoint> endPoints = prepareEndpoints();

        //when
        try {
            ovsActivator.activate(endPoints, serviceId, true, ServiceType.POINTTOPOINTCONNECTIVITY);
        } catch (ResourceNotAvailableException | InterruptedException | ExecutionException e) {
            fail(e.getMessage());
        }

        //then
        Nodes nodes = readOpenFLowTopology(dataBroker);
        Node odlNode = OvsdbUtils.getOdlNode(dataBroker);
        checkTable(nodes,activated);
        System.out.println("Before deactivation: "+ nodes.toString());

        //when
        try {
            ovsActivator.deactivate(endPoints, serviceId, ServiceType.POINTTOPOINTCONNECTIVITY);
        } catch (ResourceNotAvailableException | InterruptedException | ExecutionException e) {
            fail(e.getMessage());
        }
        nodes = readOpenFLowTopology(dataBroker);
        checkTable(nodes,deactivated);
        System.out.println("After deactivation: "+ nodes.toString());
    }

    BiConsumer<Table,List<String>> activated = (table,interswitchPorts) -> {
        List<Flow> flows = table.getFlow();
        int interswitchPortCount = interswitchPorts.size();
        //vlan & interwitch + 1 vlan + 1 drop
        int flowCount = interswitchPortCount * 2 + 2;
        assertEquals(flowCount,flows.size());
        List<Flow> interswitchFlows = flows.stream()
                .filter(flow -> flow.getId().getValue().contains(interswitchName))
                .collect(Collectors.toList());
        assertEquals(interswitchPortCount,interswitchFlows.size());

        List<Flow> vlanFlows = flows.stream()
                .filter(flow -> flow.getId().getValue().contains(vlanName))
                .collect(Collectors.toList());
        assertEquals(interswitchPortCount+1,vlanFlows.size());

        assertTrue(flows.stream().anyMatch(flow -> flow.getId().getValue().equals(dropName)));
    };

    BiConsumer<Table,List<String>> deactivated = (table,interswitchPorts) -> {
        List<Flow> flows = table.getFlow();
        boolean hasVlanFlows = flows.stream()
                .filter(flow -> flow.getId().getValue().contains(serviceId+"-"+vlanName))
                .anyMatch(this::hasVlanMatch);
        assertFalse(hasVlanFlows);
    };

    private boolean hasVlanMatch(Flow flow) {
        if (flow.getMatch().getVlanMatch().getVlanId().getVlanId().getValue().equals(expectedVlanId)) {
            return true;
        }
        return false;
    }

    private void checkTable(Nodes nodes, BiConsumer<Table,List<String>> checkTable) {
        nodes.getNode()
                .forEach(node -> {
                    try {
                        Table t = OpenFlowUtils.getTable(node);
                        if (node.key().getId().getValue().equals(ofPort1Name)) {
                            checkTable.accept(t,of1InterwitchPorts);
                        } else if (node.key().getId().getValue().equals(ofPort2Name)) {
                            checkTable.accept(t,of2InterwitchPorts);
                        }
                    } catch (ResourceNotAvailableException e) {
                        fail(e.getMessage());
                    }
                });
    }

    private List<EndPoint> prepareEndpoints() {
        List<EndPoint> endPoints = new ArrayList<>();
        endPoints.add(mockEndPoint(port1Name));
        endPoints.add(mockEndPoint(port2Name));
        return endPoints;
    }

    private EndPoint mockEndPoint(String portName) {
        ConnectivityServiceEndPoint connectivityServiceEndPoint = mock(ConnectivityServiceEndPoint.class);
        NrpConnectivityServiceEndPointAttrs attrs = mock(NrpConnectivityServiceEndPointAttrs.class);
        //UNI port mock
        ServiceInterfacePoint sipRef = TapiUtils.toSipRef(new Uuid(portName), ServiceInterfacePoint.class);
        when(connectivityServiceEndPoint.getServiceInterfacePoint())
                .thenReturn(sipRef);

        //Vlan Id mock
        VlanId vlanIdList = mock(VlanId.class);
        when(vlanIdList.getVlanId())
                .thenReturn(PositiveInteger.getDefaultInstance(expectedVlanId.toString()));

        List<VlanId> vlanIds = new ArrayList<>();
        vlanIds.add(vlanIdList);

        CeVlanIdListAndUntag ceVlanIdList = mock(CeVlanIdListAndUntag.class);
        when(ceVlanIdList.getVlanId())
                .thenReturn(vlanIds);

        IngressBwpFlow ingressBwpFlow = mock(IngressBwpFlow.class);
        when(ingressBwpFlow.getCir()).thenReturn(new NaturalNumber(4000000L));
        when(ingressBwpFlow.getEir()).thenReturn(new NaturalNumber(4000000L));

        NrpCarrierEthConnectivityEndPointResource nrpCgEthFrameFlowCpaAspec =
                mock(NrpCarrierEthConnectivityEndPointResource.class);

		when(nrpCgEthFrameFlowCpaAspec.getIngressBwpFlow())
                .thenReturn(ingressBwpFlow);

        when(nrpCgEthFrameFlowCpaAspec.getCeVlanIdListAndUntag())
        .thenReturn(ceVlanIdList);

        when(attrs.getNrpCarrierEthConnectivityEndPointResource())
                .thenReturn(nrpCgEthFrameFlowCpaAspec);

        EndPoint ep = new EndPoint(connectivityServiceEndPoint,attrs);
        ep.setNepRef(mock(OwnedNodeEdgePointRef.class));
        when(ep.getNepRef().getNodeId()).thenReturn(new Uuid(nodeId));
        return ep;
    }

    /**
     * Add 5 ovsdb bridges and suitable openflow nodes
     */
    private void initTopologies() {
        List<Node> bridges = new ArrayList<>();
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> ofNodes = new ArrayList<>();
        bridges.add(createBridge("s1",5));
        bridges.add(createBridge("s2",2));
        bridges.add(createBridge("s3",4));
        bridges.add(createBridge("s4",3));
        bridges.add(createBridge("s5",4));

        bridges.add(createBridge("odl", 0));

        bridges.forEach(node -> {
            OvsdbTopologyTestUtils.writeBridge(node,dataBroker);
            ofNodes.add(createOpenFlowNode(node));
        });

        OpenFlowTopologyTestUtils.createOpenFlowNodes(ofNodes,dataBroker);
    }

    private Node createBridge(String name, int portCount) {
        List<TerminationPoint> tps = new ArrayList<>();
        IntStream.range(1,portCount+1)
                .forEach(i -> tps.add(OvsdbTopologyTestUtils.createTerminationPoint(name+"-eth"+i,Long.valueOf(i))));
        return OvsdbTopologyTestUtils.createBridge(name, tps);
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node createOpenFlowNode(Node node) {
        String ovsdbName = node.key().getNodeId().getValue();
        String ofBridgeName = getOfName(ovsdbName);

        List<NodeConnector> nodeConnectors = new ArrayList<>();
        node.getTerminationPoint()
                .forEach(tp -> nodeConnectors.add(OpenFlowTopologyTestUtils.createNodeConnector(ofBridgeName, tp.augmentation(OvsdbTerminationPointAugmentation.class).getOfport(), ovsdbName+"-eth"+tp.augmentation(OvsdbTerminationPointAugmentation.class).getOfport())));

        return OpenFlowTopologyTestUtils.createOpenFlowNode(ofBridgeName,nodeConnectors);
    }

    private String getOfName(String ovsdbName) {
        String bridgeNumber = ovsdbName.substring(1,ovsdbName.length());
        return "openflow:"+bridgeNumber;
    }

    public static Nodes readOpenFLowTopology(DataBroker dataBroker) {
        InstanceIdentifier<Nodes> instanceIdentifier = InstanceIdentifier.builder(Nodes.class).build();
        return (Nodes) MdsalUtils.read(dataBroker,LogicalDatastoreType.CONFIGURATION,instanceIdentifier);
    }

    /**
     * @return List of links between ovswitches
     */
    private static List<Link> getLinkList() {
        List<Link> linkList = new ArrayList<>();

        //openflow nodes
        String of1 = "1";
        String of2 = "2";
        String of3 = "3";
        String of4 = "4";
        String of5 = "5";
        //ports
        Long p1 = 1L;
        Long p2 = 2L;
        Long p3 = 3L;
        Long p4 = 4L;
        Long p5 = 5L;

        //openflow:1
        linkList.add(FlowTopologyTestUtils.createLink(of1,p3,of2,p1));
        linkList.add(FlowTopologyTestUtils.createLink(of1,p5,of4,p1));
        linkList.add(FlowTopologyTestUtils.createLink(of1,p4,of3,p1));
        //openflow:2
        linkList.add(FlowTopologyTestUtils.createLink(of2,p2,of3,p2));
        linkList.add(FlowTopologyTestUtils.createLink(of2,p1,of1,p3));
        //openflow:3
        linkList.add(FlowTopologyTestUtils.createLink(of3,p4,of5,p3));
        linkList.add(FlowTopologyTestUtils.createLink(of3,p1,of1,p4));
        linkList.add(FlowTopologyTestUtils.createLink(of3,p3,of4,p2));
        linkList.add(FlowTopologyTestUtils.createLink(of3,p2,of2,p2));
        //openflow:4
        linkList.add(FlowTopologyTestUtils.createLink(of4,p3,of5,p4));
        linkList.add(FlowTopologyTestUtils.createLink(of4,p2,of3,p3));
        linkList.add(FlowTopologyTestUtils.createLink(of4,p1,of1,p5));
        //openflow:5
        linkList.add(FlowTopologyTestUtils.createLink(of5,p3,of3,p4));
        linkList.add(FlowTopologyTestUtils.createLink(of5,p4,of4,p3));

        return linkList;
    }
}
