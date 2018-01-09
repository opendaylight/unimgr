/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Context;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.TerminationDirection;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.context.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

/**
 * @author marek.ryznar@amartus.com
 */
public class AbstractNodeHandlerTest extends AbstractTestWithTopo {

    private static final InstanceIdentifier NRP_ABSTRACT_NODE_IID = InstanceIdentifier
            .create(Context.class)
            .augmentation(Context1.class)
            .child(Topology.class, new TopologyKey(new Uuid(TapiConstants.PRESTO_EXT_TOPO)))
            .child(Node.class,new NodeKey(new Uuid(TapiConstants.PRESTO_ABSTRACT_NODE)));
    private AbstractNodeHandler abstractNodeHandler;
    private NrpDao nrpDao;
    private static final String testSystemNodeName = "testSystemNode";
    private static final String testNepName = "testNep";
    private static final String sipPrefix = "sip:";
    private static final int init_neps_count = 4;

    @Before
    public void setUp() {
        //given
        dataBroker = getDataBroker();

        NrpInitializer nrpInitializer = new NrpInitializer(dataBroker);
        try {
            nrpInitializer.init();
        } catch (Exception e) {
            fail("Could not initialize NRP topology.");
        }

        abstractNodeHandler = new AbstractNodeHandler(dataBroker);
        abstractNodeHandler.init();
    }

    @Test
    public void testNodeAddition() {
        //when
        performNrpDaoAction(addNode,null);

        //then
        Node node = getAbstractNode();
        assertTrue(node.getOwnedNodeEdgePoint().containsAll(createTestOwnedNodeEdgePointList()));
    }

    @Test
    public void testNepAddition() {
        //given
        String newNepName = "newNep";
        performNrpDaoAction(addNode,null);

        //when
        OwnedNodeEdgePoint newNep = createNep(newNepName,TerminationDirection.BIDIRECTIONAL);
        performNrpDaoAction(update, newNep);

        //then
        Node node = getAbstractNode();
        assertTrue(node.getOwnedNodeEdgePoint().contains(newNep));
    }

    @Test
    public void testNepUpdate() {
        //given
        performNrpDaoAction(addNode,null);

        //when changing not sip related attribute
        OwnedNodeEdgePoint toUpdateNep = createNep(testNepName + "1", TerminationDirection.UNDEFINEDORUNKNOWN);
        performNrpDaoAction(update, toUpdateNep);


        Node node = getAbstractNode();
        //There could be more neps if our node was added insted of updated
        assertEquals(init_neps_count,node.getOwnedNodeEdgePoint().size());
        assertTrue(node.getOwnedNodeEdgePoint().contains(toUpdateNep));
    }

    @Test
    public void testNepUpdatedWithSipAddition() throws ExecutionException, InterruptedException, TransactionCommitFailedException {
        //given
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        Node n1 = n(tx, false, "n1", "n1:1", "n1:2");
        tx.submit().get();

        Node node = getAbstractNode();
        int neps = node.getOwnedNodeEdgePoint() == null ? 0 : node.getOwnedNodeEdgePoint().size();
        assertEquals(0,neps);

        //when
        tx = dataBroker.newReadWriteTransaction();
        OwnedNodeEdgePoint n11 = new OwnedNodeEdgePointBuilder(n1.getOwnedNodeEdgePoint().get(0))
                .setMappedServiceInterfacePoint(Collections.singletonList(new Uuid("sip:n1:1")))
                .build();
        new NrpDao(tx).updateNep("n1", n11);
        tx.submit().checkedGet();

        //then
        node = getAbstractNode();
        //There could be more neps if our node was added instead of updated
        assertEquals(1,node.getOwnedNodeEdgePoint().size());

    }

    @Test
    public void testNepUpdatedWithSipRemoval() throws ExecutionException, InterruptedException, TransactionCommitFailedException {
        //given we have sips
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        Node n1 = n(tx, true, "n1", "n1:1", "n1:2");
        tx.submit().get();

        //assert
        Node node = getAbstractNode();
        assertEquals(2,node.getOwnedNodeEdgePoint().size());

        //when
        tx = dataBroker.newReadWriteTransaction();
        OwnedNodeEdgePoint n11 = new OwnedNodeEdgePointBuilder(n1.getOwnedNodeEdgePoint().get(0))
                .setMappedServiceInterfacePoint(Collections.emptyList())
                .build();
        new NrpDao(tx).updateNep("n1", n11);
        tx.submit().checkedGet();

        //then
        node = getAbstractNode();
        //a nep was removed
        assertEquals(1,node.getOwnedNodeEdgePoint().size());

    }

    @Test
    public void testNodeRemoval() {
        //given
        performNrpDaoAction(addNode,null);

        //when
        performNrpDaoAction(removeNode,null);

        //then
        Node node = getAbstractNode();
        assertEquals(0,node.getOwnedNodeEdgePoint().size());
    }

    @Test
    public void testNepRemoval() {
        //given
        performNrpDaoAction(addNode,null);
        String nepNameToRemove = testNepName + "0";

        //when
        performNrpDaoAction(removeNep,nepNameToRemove);

        //then
        Node node = getAbstractNode();
        assertEquals(init_neps_count - 1, node.getOwnedNodeEdgePoint().size());
        assertFalse(node.getOwnedNodeEdgePoint().stream()
            .anyMatch(nep -> nep.getUuid().getValue().equals(nepNameToRemove)));
    }

    BiConsumer<NrpDao,String> removeNep = (dao,nepId) -> dao.removeNep(testSystemNodeName,nepId,false);
    BiConsumer<NrpDao,String> removeNode = (dao,nepId) -> dao.removeNode(testSystemNodeName,false);
    BiConsumer<NrpDao,String> addNode = (dao,nepId) -> dao.createSystemNode(testSystemNodeName,createTestOwnedNodeEdgePointList());
    BiConsumer<NrpDao,OwnedNodeEdgePoint> update = (dao,nep) -> dao.updateNep(testSystemNodeName,nep);

    private <T extends Object> void performNrpDaoAction(BiConsumer<NrpDao,T> action, T attr) {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        nrpDao = new NrpDao(tx);
        action.accept(nrpDao,attr);
        tx.submit();
    }

    private List<OwnedNodeEdgePoint> createTestOwnedNodeEdgePointList() {
        return IntStream.range(0,init_neps_count)
            .mapToObj(i -> createNep(testNepName + i, TerminationDirection.BIDIRECTIONAL))
            .collect(Collectors.toList());
    }

    private OwnedNodeEdgePoint createNep(String nepName, TerminationDirection td) {
        return createNep(nepName, true, td);
    }

    private OwnedNodeEdgePoint createNep(String nepName, boolean associateSip, TerminationDirection td) {
        Uuid uuid = new Uuid(nepName);
        OwnedNodeEdgePointBuilder builder = new OwnedNodeEdgePointBuilder()
                .setKey(new OwnedNodeEdgePointKey(uuid))
                .setUuid(uuid);
                // TODO donaldh .setTerminationDirection(td);

        if (associateSip) {
            builder.setMappedServiceInterfacePoint(Arrays.asList(new Uuid(sipPrefix + nepName)));
        }

        return builder.build();
    }

    private Node getAbstractNode() {
        ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction();
        try {
            Optional<Node> opt =
                    (Optional<Node>) tx.read(LogicalDatastoreType.OPERATIONAL,NRP_ABSTRACT_NODE_IID).checkedGet();
            if (opt.isPresent()) {
                return opt.get();
            } else {
                return null;
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return null;
    }
}
