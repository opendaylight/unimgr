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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.unimgr.mef.nrp.api.FailureResult;
import org.opendaylight.unimgr.mef.nrp.api.Subrequrest;
import org.opendaylight.unimgr.mef.nrp.impl.AbstractTestWithTopo;
import org.opendaylight.unimgr.mef.nrp.impl.NrpInitializer;
import org.opendaylight.unimgr.mef.nrp.impl.decomposer.BasicDecomposer;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yangtools.yang.common.OperationFailedException;

import javax.sound.sampled.Port;

/**
 * @author bartosz.michalik@amartus.com
 */
public class BasicDecomposerTest extends AbstractTestWithTopo {
    private BasicDecomposer decomposer;

    @Before
    public void setUp() throws Exception {
        dataBroker = getDataBroker();
        new NrpInitializer(dataBroker).init();
        decomposer = new BasicDecomposer(dataBroker);

    }

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test()
    public void emptyNodeInventoryTest() throws FailureResult {
        expected.expect(FailureResult.class);
        decomposer.decompose(Arrays.asList(ep("n1:1"), ep("n1:2")), null);
    }

    @Test
    public void singleNodeTest() throws FailureResult, OperationFailedException {
        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, "n1", "n1:1", "n1:2", "n1:3");
        n(tx, "n2", "n2:1", "n2:2", "n2:3");
        tx.submit().checkedGet();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:1"), ep("n1:2")), null);

        assertEquals(1, decomposed.size());
    }

    @Test
    public void singleNodeTestSameEndpoint() throws FailureResult, OperationFailedException {
        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, "n1", "n1:1", "n1:2", "n1:3");
        n(tx, "n2", "n2:1", "n2:2", "n2:3");
        tx.submit().checkedGet();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:1"), ep("n1:1")), null);

        assertNull(decomposed);

    }

    @Test
    public void nonExistingEndpoint() throws FailureResult, OperationFailedException {
        expected.expect(FailureResult.class);
        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, "n1", "n1:1", "n1:2", "n1:3");
        n(tx, "n2", "n2:1", "n2:2", "n2:3");
        tx.submit().checkedGet();
        //when
        decomposer.decompose(Arrays.asList(ep("n1:1"), ep("n3:1")), null);
    }

    @Test
    public void noPathTest() throws FailureResult, OperationFailedException {
        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, "n1", "n1:1", "n1:2", "n1:3");
        n(tx, "n2", "n2:1", "n2:2", "n2:3");
        tx.submit().checkedGet();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:1"), ep("n2:2")), null);
        assertNull(decomposed);
    }

    @Test
    public void twoNodesTest() throws FailureResult, OperationFailedException {
        //having three nodes, but only two nodes connected
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, "n1", "n1:1", "n1:2", "n1:3");
        n(tx, "n2", "n2:1", "n2:2", "n2:3");
        n(tx, "n3", "n3:1", "n3:2", "n3:3");
        l(tx, "n1", "n1:1", "n2", "n2:1", OperationalState.ENABLED);
        l(tx, "n2", "n2:3", "n3", "n3:3", OperationalState.ENABLED);
        tx.submit().checkedGet();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:2"), ep("n2:2")), null);
        assertNotNull(decomposed);
        assertEquals(2, decomposed.size());
    }

    @Test
    public void threeNodesTest() throws FailureResult, OperationFailedException {
        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, "n1", "n1:1", "n1:2", "n1:3");
        n(tx, "n2", "n2:1", "n2:2", "n2:3");
        n(tx, "n3", "n3:1", "n3:2", "n3:3");
        l(tx, "n1", "n1:1", "n2", "n2:1", OperationalState.ENABLED);
        l(tx, "n2", "n2:3", "n3", "n3:3", OperationalState.ENABLED);
        tx.submit().checkedGet();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:2"), ep("n3:2")), null);
        assertNotNull(decomposed);
        assertEquals(3, decomposed.size());
        assertEquals(3, decomposed.stream().map(subrequrest -> subrequrest.getActivationDriverId()).collect(Collectors.toSet()).size());
    }

    @Test
    public void threeNodesTwoDriversTest() throws FailureResult, OperationFailedException {
        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, new Uuid("n1"),"d1", "n1:1", "n1:2", "n1:3");
        n(tx, new Uuid("n2"),"d2", "n2:1", "n2:2", "n2:3");
        n(tx, new Uuid("n3"),"d1", "n3:1", "n3:2", "n3:3");
        l(tx, "n1", "n1:1", "n2", "n2:1", OperationalState.ENABLED);
        l(tx, "n2", "n2:3", "n3", "n3:3", OperationalState.ENABLED);
        tx.submit().checkedGet();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:2"), ep("n3:2")), null);
        assertNotNull(decomposed);
        System.out.println();
        assertEquals(3, decomposed.size());
        assertEquals(2, decomposed.stream().map(subrequrest -> subrequrest.getActivationDriverId()).collect(Collectors.toSet()).size());
    }

    @Test
    public void threeNodesDisabledLinkTest() throws FailureResult, OperationFailedException {
        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, "n1", "n1:1", "n1:2", "n1:3");
        n(tx, "n2", "n2:1", "n2:2", "n2:3");
        n(tx, "n3", "n3:1", "n3:2", "n3:3");
        l(tx, "n1", "n1:1", "n2", "n2:1", OperationalState.DISABLED);
        l(tx, "n2", "n2:3", "n3", "n3:3", OperationalState.ENABLED);
        tx.submit().checkedGet();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:2"), ep("n3:2")), null);
        assertNull(decomposed);
    }


}
