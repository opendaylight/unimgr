/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.mef.unimgr.ext.rev160725.ActivationStatus;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.forwarding.constructs.ForwardingConstruct;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.unimgr.impl.ForwardingConstructTestUtils.fcIid;
import static org.opendaylight.unimgr.impl.ForwardingConstructTestUtils.fcSingleNode;

/**
 * @author krzysztof.bijakowski@amartus.com
 */
public class ForwardingConstructActivationStateTrackerTest extends AbstractDataBrokerTest {

    private InstanceIdentifier fcIid;

    @Before
    public void setUp() {
        fcIid = fcIid();
    }

    @Test
    public void testIsActivatablePositive() {
        //given
        ForwardingConstructActivationStateTracker stateTracker = createStateTracker(mockDataBroker(false));

        //when
        boolean result = stateTracker.isActivatable();

        //then
        assertTrue(result);
    }

    @Test
    public void testIsActivatableNegative() {
        //given
        ForwardingConstructActivationStateTracker stateTracker = createStateTracker(mockDataBroker(true));

        //when
        boolean result = stateTracker.isActivatable();

        //then
        assertFalse(result);
    }

    @Test
    public void testIsDeactivatablePositive() {
        //given
        ForwardingConstructActivationStateTracker stateTracker = createStateTracker(mockDataBroker(true));

        //when
        boolean result = stateTracker.isDeactivatable();

        //then
        assertTrue(result);
    }

    @Test
    public void testIsDeactivatableNegative() {
        //given
        ForwardingConstructActivationStateTracker stateTracker = createStateTracker(mockDataBroker(false));

        //when
        boolean result = stateTracker.isDeactivatable();

        //then
        assertFalse(result);
    }

    @Test
    public void testActivated() {
        //given
        DataBroker dataBroker = getDataBroker();
        ForwardingConstructActivationStateTracker stateTracker = createStateTracker(dataBroker);
        ForwardingConstruct exceptedFc = fcSingleNode();

        //when
        stateTracker.activated(exceptedFc);

        //then
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        CheckedFuture<Optional<ForwardingConstruct>, ReadFailedException> result =
                transaction.read(LogicalDatastoreType.OPERATIONAL, fcIid);
        Optional<ForwardingConstruct> fcOptional = Optional.absent();

        try {
            fcOptional = result.checkedGet();
        } catch (ReadFailedException e) {
            fail("Error during test result verification - cannot read data : " + e.getMessage());
        }

        assertTrue(fcOptional.isPresent());
        ForwardingConstruct actualFc = fcOptional.get();
        ForwardingConstructTestUtils.assertEquals(exceptedFc, actualFc);
        ForwardingConstructTestUtils.assertActivationState(actualFc, ActivationStatus.ACTIVE);
    }

    @Test
    public void testActivationFailed() {
        //given
        DataBroker dataBroker = getDataBroker();
        ForwardingConstructActivationStateTracker stateTracker = createStateTracker(dataBroker);
        ForwardingConstruct exceptedFc = fcSingleNode();

        //when
        stateTracker.activationFailed(exceptedFc);

        //then
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        CheckedFuture<Optional<ForwardingConstruct>, ReadFailedException> result =
                transaction.read(LogicalDatastoreType.OPERATIONAL, fcIid);
        Optional<ForwardingConstruct> fcOptional = Optional.absent();

        try {
            fcOptional = result.checkedGet();
        } catch (ReadFailedException e) {
            fail("Error during test result verification - cannot read data : " + e.getMessage());
        }

        assertTrue(fcOptional.isPresent());
        ForwardingConstruct actualFc = fcOptional.get();
        ForwardingConstructTestUtils.assertEquals(exceptedFc, actualFc);
        ForwardingConstructTestUtils.assertActivationState(actualFc, ActivationStatus.FAILED);
    }

    @Test
    public void testDeactivated() {
        //given
        DataBroker dataBroker = getDataBroker();
        ForwardingConstructActivationStateTracker stateTracker = createStateTracker(dataBroker);
        ForwardingConstruct fc = fcSingleNode();
        stateTracker.activated(fc);

        //when
        stateTracker.deactivated();

        //then
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        CheckedFuture<Optional<ForwardingConstruct>, ReadFailedException> result =
                transaction.read(LogicalDatastoreType.OPERATIONAL, fcIid);
        Optional<ForwardingConstruct> fcOptional = Optional.absent();

        try {
            fcOptional = result.checkedGet();
        } catch (ReadFailedException e) {
            fail("Error during test result verification - cannot read data : " + e.getMessage());
        }

        assertFalse(fcOptional.isPresent());
    }

    @Test
    public void testDeactivationFailed() {
        //TODO write test when implemented
    }

    private DataBroker mockDataBroker(boolean fcExists) {
        DataBroker dataBroker = mock(DataBroker.class);
        final ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class);
        final CheckedFuture transactionResult = mock(CheckedFuture.class);
        final ForwardingConstruct forwardingConstruct = Mockito.mock(ForwardingConstruct.class);
        final Optional<ForwardingConstruct> optionalForwardingConstruct;

        if(fcExists) {
            optionalForwardingConstruct = Optional.of(forwardingConstruct);
        } else {
            optionalForwardingConstruct = Optional.absent();
        }

        try {
            when(transactionResult.checkedGet()).thenReturn(optionalForwardingConstruct);
        } catch (Exception e) {
            fail("Cannot create mocks : " + e.getMessage());
        }
        when(transaction.read(Mockito.eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class)))
                .thenReturn(transactionResult);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(transaction);

        return dataBroker;
    }

    private ForwardingConstructActivationStateTracker createStateTracker(DataBroker dataBroker) {
        return new ForwardingConstructActivationStateTracker(dataBroker, fcIid);
    }
}