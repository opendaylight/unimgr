/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.tapi;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.unimgr.mef.nrp.ovs.FlowTopologyTestUtils;
import org.opendaylight.unimgr.mef.nrp.ovs.OvsdbTopologyTestUtils;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.context.attrs.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

/**
 * @author marek.ryznar@amartus.com
 */
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

        //helper.createOvsdbTopology();
        OvsdbTopologyTestUtils.createOvsdbTopology(dataBroker);
        helper.createOpenFlowNodes();
        FlowTopologyTestUtils.createFlowTopology(dataBroker,getLinkList());
        helper.createPrestoSystemTopology();

        topologyDataHandler = new TopologyDataHandler(dataBroker);
        topologyDataHandler.init();
    }

    @Test
    public void testBridgeAddition() {
        //when
        helper.createTestBridge();

        //then
        Node ovsNode = getOvsNodeWithNeps(n -> n.getOwnedNodeEdgePoint() != null);

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
        getOvsNodeWithNeps(initialBridgePortCount);


        //when
        helper.addPort(bridgeName,newPortName,ofPortNumber);

        //then
        Node ovsNode = getOvsNodeWithNeps(initialBridgePortCount + 1);
        assertEquals(initialBridgePortCount+1,ovsNode.getOwnedNodeEdgePoint().size());
        checkNeps(ovsNode,"br1:"+newPortName,expectedNep1,expectedNep2);
        checkSips(helper.readSips(),"br1:"+newPortName,expectedNep1,expectedNep2);
    }

    @Test
    public void testBridgeRemoval() {
        //given
        helper.createTestBridge();
        getOvsNodeWithNeps(initialBridgePortCount);

        //when
        helper.deleteTestBridge();

        //then
        Node ovsNode = getOvsNodeWithNeps(0);
    }

    @Test
    public void testPortRemoval() {
        //given
        String fullPortNameToRemove = bridgeName+tp1Name;

        helper.createTestBridge();
        getOvsNodeWithNeps(initialBridgePortCount);

        //when
        helper.deletePort(tp1Name);

        //then
        Node ovsNode = getOvsNodeWithNeps(initialBridgePortCount-1);
        ovsNode = helper.readOvsNode();
        assertEquals(initialBridgePortCount-1,ovsNode.getOwnedNodeEdgePoint().size());
        assertFalse(checkNep.apply(ovsNode,fullPortNameToRemove));
        assertFalse(checkSip.apply(helper.readSips(), fullPortNameToRemove));
    }

    private Node getOvsNodeWithNeps(int expectedPorts) {
        Node node = getOvsNodeWithNeps(n -> n.getOwnedNodeEdgePoint() != null && n.getOwnedNodeEdgePoint().size() == expectedPorts);
        if(node == null) {
            fail("OVS node does not contain " + expectedPorts + " ports");
            throw new IllegalStateException("OVS node does not contain " + expectedPorts + " ports");
        }
        return node;
    }

    private Node getOvsNodeWithNeps(Predicate<Node> nodePredicate) {
        for(int i = 0; i < 5; ++i) {
            Node ovsNode = helper.readOvsNode();
            if(nodePredicate.test(ovsNode)) {
                return ovsNode;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException _e) { }
        }
        return null;
    }


    private BiFunction<Node, String, Boolean> checkNep = (node,nepName) ->
            node.getOwnedNodeEdgePoint().stream()
                    .anyMatch(ownedNep -> ownedNep.getMappedServiceInterfacePoint().contains(new Uuid(sip_prefix + ovs_nep_prefix + nepName))
                                && ownedNep.getUuid().getValue().equals(ovs_nep_prefix + nepName)
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
