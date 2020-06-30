/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.decomposer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.unimgr.mef.nrp.api.FailureResult;
import org.opendaylight.unimgr.mef.nrp.api.Subrequrest;
import org.opendaylight.unimgr.mef.nrp.impl.AbstractTestWithTopo;
import org.opendaylight.unimgr.mef.nrp.impl.NrpInitializer;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortDirection;
import org.opendaylight.yangtools.yang.common.OperationFailedException;


public class BasicDecomposerForDirectedTopologyTest extends AbstractTestWithTopo {

    private BasicDecomposer decomposer;

    @Before
    public void setUp() throws Exception {
        dataBroker = getDataBroker();
        new NrpInitializer(dataBroker).init();
        decomposer = new BasicDecomposer(dataBroker);

    }

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void twoNodesTestDirection()
            throws FailureResult, OperationFailedException, InterruptedException, ExecutionException {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, true, "n1","d1", Stream.of(pI("n1:1"), pO("n1:2")));
        n(tx, true, "n2", "d2", Stream.of(pO("n2:1"), pI("n2:2")));
        n(tx, true, "n3", "d3", Stream.of(pI("n3:1")));
        l(tx, "n1", "n1:1", "n2", "n2:1",
                OperationalState.ENABLED, ForwardingDirection.BIDIRECTIONAL);
        tx.commit().get();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(
                Arrays.asList(
                        ep("n1:2", PortDirection.OUTPUT),
                        ep("n2:2", PortDirection.INPUT)
                ), null);
        assertNotNull(decomposed);
        assertEquals(2, decomposed.size());
    }

    @Test
    public void threeNodesTestAll()
            throws FailureResult, OperationFailedException, InterruptedException, ExecutionException {
        //having
        threeNodesTopo();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(
                ep("n1:2", PortDirection.OUTPUT),
                ep("n3:3", PortDirection.INPUT)
        ), null);
        assertNotNull(decomposed);
        assertEquals(3, decomposed.size());
        assertEquals(2, decomposed.stream()
                .flatMap(s -> s.getEndpoints().stream()).filter(e -> e.getAttrs() != null).count());
    }

    @Test(expected = IllegalArgumentException.class)
    public void threeNodesTestIncompatible()
            throws FailureResult, OperationFailedException, InterruptedException, ExecutionException {
        //having
        threeNodesTopo();
        //when
        decomposer.decompose(Arrays.asList(ep("n1:2", PortDirection.INPUT),
                ep("n3:3", PortDirection.OUTPUT)), null);
        fail();
    }

    @Test
    public void fourNodesTestThreeSelected()
            throws FailureResult, OperationFailedException, InterruptedException, ExecutionException {
        //having
        fourNodesTopo();

        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(
                ep("n2:2"),
                ep("n3:2")
        ), null);
        assertNotNull(decomposed);
        assertEquals(3, decomposed.size());
        assertEquals(2, decomposed.stream()
                .flatMap(s -> s.getEndpoints().stream()).filter(e -> e.getAttrs() != null).count());
    }


    @Test
    public void fourNodesTestNone()
            throws FailureResult, OperationFailedException, InterruptedException, ExecutionException {
        //having
        fourNodesTopo();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(
                ep("n2:1", PortDirection.INPUT),
                ep("n1:1")
        ), null);
        assertNull(decomposed);

    }

    @Test
    public void fourTestPartialPath()
            throws FailureResult, InterruptedException, ExecutionException {
        //having
        fourNodesTopo();

        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(
                ep("n1:1", PortDirection.OUTPUT),
                ep("n2:1"),
                ep("n4:1")
        ), null);
        assertNull(decomposed);
    }

    @Test
    public void fourTestSingleSink() throws FailureResult, InterruptedException, ExecutionException {
        //having
        fourNodesTopo();

        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(
                ep("n1:1", PortDirection.INPUT),
                ep("n2:1", PortDirection.INPUT),
                ep("n4:1", PortDirection.OUTPUT)
                ), null);
        assertNotNull(decomposed);
    }

    @Test
    public void fiveNodesTestAll()
            throws FailureResult, OperationFailedException, InterruptedException, ExecutionException {
        //having
        fiveNodesTopo();

        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(
                ep("n1:2"),
                ep("n5:3")
        ), null);
        assertNotNull(decomposed);
        assertEquals(5, decomposed.size());
        assertEquals(2, decomposed.stream()
                .flatMap(s -> s.getEndpoints().stream()).filter(e -> e.getAttrs() != null).count());
    }

    @Test
    public void fiveNodesTestDirected() throws FailureResult, InterruptedException, ExecutionException {
        //having
        fiveNodesTopo();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(
                ep("n1:2", PortDirection.OUTPUT),
                ep("n5:3", PortDirection.INPUT)
        ), null);
        assertNotNull(decomposed);
        assertEquals(3, decomposed.size());
    }


    /*

    n2--(1)-->--(3)--n1
    n3--(1)-->--(2)--n2

     */
    private void threeNodesTopo() throws InterruptedException, ExecutionException {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, true, "n1", "d1",
                Stream.of(pI("n1:1"), pO("n1:2"), pI("n1:3")));
        n(tx, true, "n2", "d2",
                Stream.of(pO("n2:1"), pI("n2:2")));
        n(tx, true, "n3", "d3",
                Stream.of(pO("n3:1"), pO("n3:2"), pI("n3:3")));
        l(tx, "n1", "n1:3", "n2", "n2:1",
                OperationalState.ENABLED, ForwardingDirection.BIDIRECTIONAL);
        l(tx, "n2", "n2:2", "n3", "n3:1",
                OperationalState.ENABLED, ForwardingDirection.BIDIRECTIONAL);
        tx.commit().get();
    }

    /*

    n1--(5)-->--(5)--n2
    n1--(4)-->--(4)--n4
    n2--(3)-->--(3)--n4
    n3--(3)-->--(4)--n2
    n4--(5)-->--(5)--n3

     */
    private void fourNodesTopo() throws InterruptedException, ExecutionException {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, true, "n1", "d1",
                Stream.of(pB("n1:1"), pB("n1:2"), pI("n1:3"), pO("n1:4"), pO("n1:5")));
        n(tx, true, "n2", "d2",
                Stream.of(pB("n2:1"), pB("n2:2"), pO("n2:3"), pI("n2:4"), pI("n2:5")));
        n(tx, true, "n3", "d3",
                Stream.of(pB("n3:1"), pB("n3:2"), pO("n3:3"), pO("n3:4"), pI("n3:5")));
        n(tx, true, "n4", "d4",
                Stream.of(pB("n4:1"), pB("n4:2"), pI("n4:3"), pI("n4:4"), pO("n4:5")));
        l(tx, "n1", "n1:5", "n2", "n2:5",
                OperationalState.ENABLED, ForwardingDirection.UNIDIRECTIONAL);
        l(tx, "n1", "n1:4", "n4", "n4:4",
                OperationalState.ENABLED, ForwardingDirection.UNIDIRECTIONAL);
        l(tx, "n2", "n2:3", "n4", "n4:3",
                OperationalState.ENABLED, ForwardingDirection.UNIDIRECTIONAL);
        l(tx, "n3", "n3:4", "n2", "n2:4",
                OperationalState.ENABLED, ForwardingDirection.UNIDIRECTIONAL);
        l(tx, "n4", "n4:5", "n3", "n3:5",
                OperationalState.ENABLED, ForwardingDirection.UNIDIRECTIONAL);
        tx.commit().get();
    }

    /*

    n1--(4)-->--(4)--n2
    n1--(3)-->--(1)--n4
    n2--(3)-->--(4)--n3
    n3--(1)-->--(1)--n1
    n3--(3)-->--(1)--n5
    n5--(4)-->--(2)--n4

     */
    private  void fiveNodesTopo() throws InterruptedException, ExecutionException {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, true, "n1", "d1",
                Stream.of(pI("n1:1"), pB("n1:2"), pI("n1:3"), pO("n1:4")));
        n(tx, true, "n2", "d2",
                Stream.of(pI("n2:1"), pB("n2:2"), pO("n2:3"), pI("n2:4")));
        n(tx, true, "n3", "d3",
                Stream.of(pO("n3:1"), pB("n3:2"), pO("n3:3"), pI("n3:4")));
        n(tx, true, "n4", "d4",
                Stream.of(pO("n4:1"), pI("n4:2"), pB("n4:3"), pB("n4:4")));
        n(tx, true, "n5", "d5",
                Stream.of(pI("n5:1"), pB("n5:2"), pB("n5:3"), pO("n5:4")));
        l(tx, "n2", "n2:3", "n3", "n3:4",
                OperationalState.ENABLED, ForwardingDirection.BIDIRECTIONAL);
        l(tx, "n3", "n3:1", "n1", "n1:1",
                OperationalState.ENABLED, ForwardingDirection.BIDIRECTIONAL);
        l(tx, "n3", "n3:3", "n5", "n5:1",
                OperationalState.ENABLED, ForwardingDirection.BIDIRECTIONAL);
        l(tx, "n4", "n4:1", "n1", "n1:3",
                OperationalState.ENABLED, ForwardingDirection.BIDIRECTIONAL);
        l(tx, "n1", "n1:4", "n2", "n2:4",
                OperationalState.ENABLED, ForwardingDirection.BIDIRECTIONAL);
        l(tx, "n5", "n5:4", "n4", "n4:2",
                 OperationalState.ENABLED, ForwardingDirection.BIDIRECTIONAL);
        tx.commit().get();
    }


    private AbstractTestWithTopo.Pair pO(String id) {
        return new Pair(id, PortDirection.OUTPUT);
    }

    private AbstractTestWithTopo.Pair pI(String id) {
        return new Pair(id, PortDirection.INPUT);
    }

    private AbstractTestWithTopo.Pair pB(String id) {
        return new Pair(id, PortDirection.BIDIRECTIONAL);
    }


}
