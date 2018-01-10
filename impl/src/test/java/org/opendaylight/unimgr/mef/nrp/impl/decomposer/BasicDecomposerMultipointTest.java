/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.decomposer;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.unimgr.mef.nrp.api.FailureResult;
import org.opendaylight.unimgr.mef.nrp.api.Subrequrest;
import org.opendaylight.unimgr.mef.nrp.impl.AbstractTestWithTopo;
import org.opendaylight.unimgr.mef.nrp.impl.NrpInitializer;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.OperationalState;
import org.opendaylight.yangtools.yang.common.OperationFailedException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
/**
 * @author bartosz.michalik@amartus.com
 */
public class BasicDecomposerMultipointTest extends AbstractTestWithTopo {
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
    public void singleNodeTest() throws FailureResult, OperationFailedException {
        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, "n1", "n1:1", "n1:2", "n1:3", "n1:4");
        n(tx, "n2", "n2:1", "n2:2", "n2:3", "n2:4");
        tx.submit().checkedGet();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:1"), ep("n1:2"), ep("n1:4")), null);

        assertEquals(1, decomposed.size());
    }

    @Test
    public void twoConnectedNodesTest() throws FailureResult, OperationFailedException {
        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, "n1", "n1:1", "n1:2", "n1:3","n1:4");
        n(tx, "n2", "n2:1", "n2:2", "n2:3","n2:4");
        n(tx, "n3", "n3:1", "n3:2", "n3:3","n3:4");
        l(tx, "n1", "n1:1", "n2", "n2:1", OperationalState.ENABLED);
        l(tx, "n2", "n2:3", "n3", "n3:3", OperationalState.ENABLED);
        tx.submit().checkedGet();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:2"), ep("n2:2"), ep("n2:3")), null);
        assertNotNull(decomposed);
        assertEquals(2, decomposed.size());
        assertEquals(Stream.of(2,3).collect(Collectors.toSet()), decomposed.stream().map(s -> s.getEndpoints().size()).collect(Collectors.toSet()));

    }

    @Test
    public void fourNodesTopology() throws FailureResult, OperationFailedException {
        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, "n1", "n1:1", "n1:2", "n1:3", "n1:4");
        n(tx, "n2", "n2:1", "n2:2", "n2:3");
        n(tx, "n3", "n3:1", "n3:2", "n3:3");
        n(tx, "n3", "n3:1", "n3:2", "n3:3");
        l(tx, "n1", "n1:1", "n2", "n2:1", OperationalState.ENABLED);
        l(tx, "n1", "n1:2", "n3", "n3:2", OperationalState.ENABLED);
        l(tx, "n4", "n4:1", "n3", "n3:1", OperationalState.ENABLED);
        l(tx, "n4", "n4:2", "n2", "n2:2", OperationalState.ENABLED);

        tx.submit().checkedGet();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:3"), ep("n1:4"), ep("n2:3")), null);
        assertNotNull(decomposed);
        assertEquals(2, decomposed.size());
        assertEquals(Stream.of(2,3).collect(Collectors.toSet()), decomposed.stream().map(s -> s.getEndpoints().size()).collect(Collectors.toSet()));
        List<String> uuids = decomposed.stream().map(s -> s.getNodeUuid().getValue()).collect(Collectors.toList());
        Assert.assertThat(uuids, CoreMatchers.not(CoreMatchers.hasItems("n3", "n4")));
    }
}
