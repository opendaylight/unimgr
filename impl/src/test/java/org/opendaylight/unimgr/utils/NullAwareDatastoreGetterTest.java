/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.utils;


import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.mef.unimgr.ext.rev160725.ForwardingConstruct1;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.forwarding.constructs.ForwardingConstruct;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NullAwareDatastoreGetterTest {

    @Test
    public void testCollectNotNull() {
        //given
        ForwardingConstruct fc = mock(ForwardingConstruct.class);
        FcSpec expected = mock(FcSpec.class);

        when(fc.getFcSpec()).thenReturn(expected);

        //when
        Optional<FcSpec> actualOptional = new NullAwareDatastoreGetter<>(fc)
                .collect(x -> x::getFcSpec)
                .get();

        //then
        assertNotNull(actualOptional);
        assertTrue(actualOptional.isPresent());
        assertEquals(expected, actualOptional.get());
    }

    @Test
    public void testCollectNull() {
        //given
        ForwardingConstruct fc = mock(ForwardingConstruct.class);
        when(fc.getFcSpec()).thenReturn(null);

        //when
        Optional<FcSpec> actualOptional = new NullAwareDatastoreGetter<>(fc)
                .collect(x -> x::getFcSpec)
                .get();

        //then
        assertNotNull(actualOptional);
        assertFalse(actualOptional.isPresent());
    }

    @Test
    public void testCollectInputNull() {
        //given
        ForwardingConstruct fc = null;

        //when
        Optional<FcSpec> actualOptional = new NullAwareDatastoreGetter<>(fc)
                .collect(x -> x::getFcSpec)
                .get();

        //then
        assertNotNull(actualOptional);
        assertFalse(actualOptional.isPresent());
    }

    @Test
    public void testCollectInputOptionalPresent() {
        //given
        ForwardingConstruct fc = mock(ForwardingConstruct.class);
        FcSpec expected = mock(FcSpec.class);

        when(fc.getFcSpec()).thenReturn(expected);

        Optional<ForwardingConstruct> fcOptional = Optional.of(fc);

        //when
        Optional<FcSpec> actualOptional = new NullAwareDatastoreGetter<>(fcOptional)
                .collect(x -> x::getFcSpec)
                .get();

        //then
        assertNotNull(actualOptional);
        assertTrue(actualOptional.isPresent());
        assertEquals(expected, actualOptional.get());
    }

    @Test
    public void testCollectInputOptionalAbsent() {
        //given
        Optional<ForwardingConstruct> fcOptional = Optional.empty();

        //when
        Optional<FcSpec> actualOptional = new NullAwareDatastoreGetter<>(fcOptional)
                .collect(x -> x::getFcSpec)
                .get();

        //then
        assertNotNull(actualOptional);
        assertFalse(actualOptional.isPresent());
    }

    @Test
    public void testCollectInputGoogleOptionalPresent() {
        //given
        ForwardingConstruct fc = mock(ForwardingConstruct.class);
        FcSpec expected = mock(FcSpec.class);

        when(fc.getFcSpec()).thenReturn(expected);

        com.google.common.base.Optional<ForwardingConstruct> fcOptional = com.google.common.base.Optional.of(fc);

        //when
        Optional<FcSpec> actualOptional = new NullAwareDatastoreGetter<>(fcOptional)
                .collect(x -> x::getFcSpec)
                .get();

        //then
        assertNotNull(actualOptional);
        assertTrue(actualOptional.isPresent());
        assertEquals(expected, actualOptional.get());
    }

    @Test
    public void testCollectInputGoogleOptionalAbsent() {
        //given
        com.google.common.base.Optional<ForwardingConstruct> fcOptional = com.google.common.base.Optional.absent();

        //when
        Optional<FcSpec> actualOptional = new NullAwareDatastoreGetter<>(fcOptional)
                .collect(x -> x::getFcSpec)
                .get();

        //then
        assertNotNull(actualOptional);
        assertFalse(actualOptional.isPresent());
    }

    @Test
    public void testCollectAugmentationNotNull() {
        //given
        ForwardingConstruct fc = mock(ForwardingConstruct.class);
        ForwardingConstruct1 expected = mock(ForwardingConstruct1.class);

        when(fc.getAugmentation(eq(ForwardingConstruct1.class))).thenReturn(expected);

        //when
        Optional<ForwardingConstruct1> actualOptional = new NullAwareDatastoreGetter<>(fc)
                .collect(x -> x::getAugmentation, ForwardingConstruct1.class)
                .get();

        //then
        assertNotNull(actualOptional);
        assertTrue(actualOptional.isPresent());
        assertEquals(expected, actualOptional.get());
    }

    @Test
    public void testCollectAugmentationNull() {
        //given
        ForwardingConstruct fc = mock(ForwardingConstruct.class);
        when(fc.getAugmentation(eq(ForwardingConstruct1.class))).thenReturn(null);

        //when
        Optional<ForwardingConstruct1> actualOptional = new NullAwareDatastoreGetter<>(fc)
                .collect(x -> x::getAugmentation, ForwardingConstruct1.class)
                .get();

        //then
        assertNotNull(actualOptional);
        assertFalse(actualOptional.isPresent());
    }

    @Test
    public void testCollectManyNotNull() {
        //given
        ForwardingConstruct fc = mock(ForwardingConstruct.class);

        FcPort fcPortA = mock(FcPort.class);
        FcPort fcPortZ = mock(FcPort.class);

        List<FcPort> fcPorts = new ArrayList<>();
        fcPorts.add(fcPortA);
        fcPorts.add(fcPortZ);

        when(fc.getFcPort()).thenReturn(fcPorts);

        //when
        List<NullAwareDatastoreGetter<FcPort>> nullAwareDatastoreGetters = new NullAwareDatastoreGetter<>(fc)
                .collectMany(x -> x::getFcPort);

        //then
        assertNotNull(nullAwareDatastoreGetters);
        assertEquals(2, nullAwareDatastoreGetters.size());

        NullAwareDatastoreGetter fcPortNadg1 = nullAwareDatastoreGetters.get(0);
        assertTrue(fcPortNadg1.get().isPresent());
        assertEquals(fcPortA, fcPortNadg1.get().get());

        NullAwareDatastoreGetter fcPortNadg2 = nullAwareDatastoreGetters.get(1);
        assertTrue(fcPortNadg2.get().isPresent());
        assertEquals(fcPortZ, fcPortNadg2.get().get());
    }

    @Test
    public void testCollectManyNull() {
        //given
        ForwardingConstruct fc = mock(ForwardingConstruct.class);
        List<FcPort> fcPorts = null;

        when(fc.getFcPort()).thenReturn(fcPorts);

        //when
        List<NullAwareDatastoreGetter<FcPort>> nullAwareDatastoreGetters = new NullAwareDatastoreGetter<>(fc)
                .collectMany(x -> x::getFcPort);

        //then
        assertNotNull(nullAwareDatastoreGetters);
        assertEquals(0, nullAwareDatastoreGetters.size());
    }
}
