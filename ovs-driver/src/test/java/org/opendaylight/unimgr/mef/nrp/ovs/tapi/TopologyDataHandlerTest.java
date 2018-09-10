/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.tapi;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.unimgr.mef.nrp.api.TopologyManager;
import org.opendaylight.unimgr.mef.nrp.ovs.FlowTopologyTestUtils;
import org.opendaylight.unimgr.mef.nrp.ovs.OvsdbTopologyTestUtils;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

/**
 * @author marek.ryznar@amartus.com
 */
@Ignore
public class TopologyDataHandlerTest extends AbstractConcurrentDataBrokerTest {

    private TopologyDataHandler topologyDataHandler;
    private DataBroker dataBroker;
    private TopologyDataHandlerTestUtils helper;
    private static final String ovs_nep_prefix = "ovs-node:";
    private static final String sip_prefix = "sip:";
    private static final String bridgeName = "br1";
    private static final String expectedNep1 = "br1:br1-eth1";
    private static final String expectedNep2 = "br1:br1-eth2";
    private static String tp1Name = "br1-eth1";
    private static int initialBridgePortCount = 3;

    @Before
    public void setUp() {
        //given
        dataBroker = getDataBroker();
        helper = new TopologyDataHandlerTestUtils(dataBroker);
        TopologyManager topologyManager = Mockito.mock(TopologyManager.class);
        Mockito.when(topologyManager.getSystemTopologyId()).thenReturn(TapiConstants.PRESTO_SYSTEM_TOPO);

        //helper.createOvsdbTopology();
        OvsdbTopologyTestUtils.createOvsdbTopology(dataBroker);
        helper.createOpenFlowNodes();
        FlowTopologyTestUtils.createFlowTopology(dataBroker,getLinkList());
        helper.createPrestoSystemTopology();

        topologyDataHandler = new TopologyDataHandler(dataBroker, topologyManager);
        topologyDataHandler.init();
    }

    @Test
    public void testBridgeAddition() {
        //when
        helper.createTestBridge();

        //then
        Node ovsNode = helper.readOvsNode();
        assertNotNull(ovsNode);
        checkNeps(ovsNode,expectedNep1,expectedNep2);
        checkSips(helper.readSips(),expectedNep1,expectedNep2);
    }

    @Test
    public void testPortAddition() {
        //given
        String newPortName = "br1-eth4";
        Long ofPortNumber = 4L;
        helper.createTestBridge();
        Node ovsNode = helper.readOvsNode();
        assertEquals(initialBridgePortCount,ovsNode.getOwnedNodeEdgePoint().size());

        //when
        helper.addPort(bridgeName,newPortName,ofPortNumber);

        //then
        ovsNode = helper.readOvsNode();
        assertEquals(initialBridgePortCount+1,ovsNode.getOwnedNodeEdgePoint().size());
        checkNeps(ovsNode,"br1:"+newPortName,expectedNep1,expectedNep2);
        checkSips(helper.readSips(),"br1:"+newPortName,expectedNep1,expectedNep2);
    }

    @Test
    public void testBridgeRemoval() {
        //given
        helper.createTestBridge();
        Node ovsNode = helper.readOvsNode();
        assertEquals(initialBridgePortCount,ovsNode.getOwnedNodeEdgePoint().size());

        //when
        helper.deleteTestBridge();

        //then
        ovsNode = helper.readOvsNode();
        assertEquals(0,ovsNode.getOwnedNodeEdgePoint().size());
    }

    @Test
    public void testPortRemoval() {
        //given
        String fullPortNameToRemove = bridgeName+tp1Name;
        //helper.createTestBridge();
        helper.createTestBridge();
        Node ovsNode = helper.readOvsNode();
        assertEquals(initialBridgePortCount,ovsNode.getOwnedNodeEdgePoint().size());

        //when
        helper.deletePort(tp1Name);

        //then
        ovsNode = helper.readOvsNode();
        assertEquals(initialBridgePortCount-1,ovsNode.getOwnedNodeEdgePoint().size());
        assertFalse(checkNep.apply(ovsNode,fullPortNameToRemove));
        assertFalse(checkSip.apply(helper.readSips(), fullPortNameToRemove));
    }

    private BiFunction<Node, String, Boolean> checkNep = (node,nepName) ->
            node.getOwnedNodeEdgePoint().stream()
                    .filter(ownedNep -> ownedNep.getUuid().getValue().equals(ovs_nep_prefix + nepName))
                    .flatMap(ownedNep -> ownedNep.getMappedServiceInterfacePoint().stream())
                    .anyMatch(sipRef ->
                        sipRef.getServiceInterfacePointId().equals(new Uuid(sip_prefix + ovs_nep_prefix + nepName))
                    );

    private void checkNeps(Node node,String ... neps) {
        Arrays.stream(neps)
                .forEach(nep -> assertTrue(checkNep.apply(node,nep)));
    }

    private BiFunction<List<ServiceInterfacePoint>, String, Boolean> checkSip =
            (sips, nep) -> sips.stream()
                .anyMatch(sip -> sip.getUuid().getValue().equals(sip_prefix + ovs_nep_prefix + nep));

    private void checkSips(List<ServiceInterfacePoint> sips, String ... neps) {
        Arrays.stream(neps)
                .forEach(nep -> assertTrue(checkSip.apply(sips,nep)));
    }

    private List<Link> getLinkList() {
        List<Link> linkList = new ArrayList<>();

        //For testing purposes only - can't be find anywhere else in DataStore
        String of1NodeName = "1";
        String of2NodeName = "2";
        Long of2PortNumber = 1L;
        Long of1PortNumber = 3L;

        //openflow:1:3 -> <- openflow:2:1
        linkList.add(FlowTopologyTestUtils.createLink(of1NodeName,of1PortNumber,of2NodeName,of2PortNumber));
        linkList.add(FlowTopologyTestUtils.createLink(of2NodeName,of2PortNumber,of1NodeName,of1PortNumber));

        return linkList;
    }



}
