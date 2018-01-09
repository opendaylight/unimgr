/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.unimgr.mef.nrp.api.FailureResult;
import org.opendaylight.unimgr.mef.nrp.api.Subrequrest;
import org.opendaylight.unimgr.mef.nrp.impl.decomposer.BasicDecomposer;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.OperationalState;
import org.opendaylight.yangtools.yang.common.OperationFailedException;

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
        //having
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
