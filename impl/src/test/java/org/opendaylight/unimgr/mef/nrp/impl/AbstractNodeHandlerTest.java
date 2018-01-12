/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.impl;

import com.google.common.util.concurrent.CheckedFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.unimgr.mef.nrp.common.TapiUtils;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Context;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.ETH;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.TerminationDirection;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

/**
 * @author marek.ryznar@amartus.com
 */
public class AbstractNodeHandlerTest extends AbstractTestWithTopo {


    private AbstractNodeHandler abstractNodeHandler;
    private static final String testSystemNodeName = "testSystemNode";
    private static final String testNepName = "testNep";
    private static final String sipPrefix = "sip:";
    private static final int init_neps_count = 4;

    @Before
    public void setUp() {
        //given
        dataBroker = getDataBroker();

        abstractNodeHandler = new AbstractNodeHandler(dataBroker);
        abstractNodeHandler.init();
    }

    @After
    public void tearDown() throws Exception {

        abstractNodeHandler.close();
        removeContext();

    }

    private void removeContext() throws Exception {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();

        tx.delete(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier
                .create(Context.class));
        tx.submit().get();
    }

    @Test
    public void testNodeAddition() throws TransactionCommitFailedException {
        //when
        performNrpDaoAction(addNode,null).checkedGet();

        //then
        Node node = getAbstractNodeNotNullNep();

        assertEquals(
                node.getOwnedNodeEdgePoint().stream().map(nep -> nep.getUuid().getValue()).collect(Collectors.toSet()),
                new HashSet(Arrays.asList(testNepName+"0", testNepName+"1", testNepName+"2", testNepName+"3"))
        );

    }

    @Test
    public void testNepAddition() throws TransactionCommitFailedException {
        //given
        String newNepName = "newNep";
        performNrpDaoAction(addNode,null).checkedGet();

        //when
        OwnedNodeEdgePoint newNep = createNep(newNepName,TerminationDirection.BIDIRECTIONAL);
        performNrpDaoAction(update, newNep).checkedGet();

        //then
        Node node = getAbstractNode(n -> n.getOwnedNodeEdgePoint().size() == init_neps_count + 1);

        assertTrue(node.getOwnedNodeEdgePoint().contains(newNep));
    }

    @Test
    public void testNepUpdate() throws TransactionCommitFailedException {
        //given
        performNrpDaoAction(addNode, null).checkedGet();

        //when changing not sip related attribute
        OwnedNodeEdgePoint toUpdateNep = createNep(testNepName + "1", TerminationDirection.UNDEFINEDORUNKNOWN);
        performNrpDaoAction(update, toUpdateNep).checkedGet();


        Node node = getAbstractNodeNotNullNep();
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
        node = getAbstractNodeNotNullNep();
        //There could be more neps if our node was added instead of updated
        assertEquals(1,node.getOwnedNodeEdgePoint().size());

    }

    @Test
    public void testNepUpdatedWithSipRemoval() throws ExecutionException, InterruptedException, TransactionCommitFailedException {
        //given we have sips
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        Node n1 = n(tx, true, "n1", "n1:1", "n1:2");
        tx.submit().checkedGet();

        //assert
        Node node = getAbstractNodeNotNullNep();
        assertEquals(2,node.getOwnedNodeEdgePoint().size());

        //when
        tx = dataBroker.newReadWriteTransaction();
        OwnedNodeEdgePoint n11 = new OwnedNodeEdgePointBuilder(n1.getOwnedNodeEdgePoint().get(0))
                .setMappedServiceInterfacePoint(Collections.emptyList())
                .build();
        new NrpDao(tx).updateNep("n1", n11);
        tx.submit().checkedGet();

        //then a nep was removed
        getAbstractNode(n -> n.getOwnedNodeEdgePoint().size() == 1);
    }

    @Test
    public void testNodeRemoval() throws TransactionCommitFailedException {
        //given
        performNrpDaoAction(addNode,null).checkedGet();

        //when
        performNrpDaoAction(removeNode,null).checkedGet();

        //then
        Node node = getAbstractNode(n -> n.getOwnedNodeEdgePoint() != null && n.getOwnedNodeEdgePoint().isEmpty());
        assertTrue(node.getOwnedNodeEdgePoint().isEmpty());
    }

    @Test
    public void testNepRemoval() throws TransactionCommitFailedException {
        //given
        performNrpDaoAction(addNode,null).checkedGet();
        String nepNameToRemove = testNepName + "0";

        //when
        performNrpDaoAction(removeNep,nepNameToRemove).checkedGet();

        //then
        Node node = getAbstractNode(n -> n.getOwnedNodeEdgePoint().size() == init_neps_count - 1);

        assertFalse(node.getOwnedNodeEdgePoint().stream()
            .anyMatch(nep -> nep.getUuid().getValue().equals(nepNameToRemove)));
    }

    private BiConsumer<NrpDao,String> removeNep = (dao, nepId) -> dao.removeNep(testSystemNodeName,nepId,false);
    private BiConsumer<NrpDao,String> removeNode = (dao, nepId) -> dao.removeNode(testSystemNodeName,false);
    private BiConsumer<NrpDao,String> addNode = (dao, nepId) -> dao.createSystemNode(testSystemNodeName,createTestOwnedNodeEdgePointList());
    private BiConsumer<NrpDao,OwnedNodeEdgePoint> update = (dao, nep) -> dao.updateNep(testSystemNodeName,nep);

    private <T extends Object> CheckedFuture<Void, TransactionCommitFailedException> performNrpDaoAction(BiConsumer<NrpDao,T> action, T attr) {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        NrpDao nrpDao = new NrpDao(tx);
        action.accept(nrpDao,attr);
        return tx.submit();
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
                .setLayerProtocol(Collections.singletonList(TapiUtils.toNepPN(ETH.class)))
                .setUuid(uuid);
                // TODO donaldh .setTerminationDirection(td);

        if (associateSip) {
            builder.setMappedServiceInterfacePoint(Collections.singletonList(new Uuid(sipPrefix + nepName)));
        }

        return builder.build();
    }


    private Node getAbstractNodeNotNullNep() {

        return getAbstractNode(n -> n.getOwnedNodeEdgePoint() != null);
    }
}
