/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.bandwidth.profile.rev160630.GNRPBwpFlow;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.specs.rev160630.g_nrp_connadaptspec.EgressBwpFlow;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.specs.rev160630.g_nrp_connadaptspec.IngressBwpFlow;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.specs.rev160630.g_nrp_uni_terminationspec.EgressBwpUni;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.specs.rev160630.g_nrp_uni_terminationspec.IngressBwpUni;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.BandwidthProfileComposition.BwpApplicability.*;
import static org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.BandwidthProfileComposition.BwpDirection.EGRESS;
import static org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.BandwidthProfileComposition.BwpDirection.INGRESS;

public class BandwidthProfileCompositionTest {

    private IngressBwpFlow defaultIngressBwProfileMock;

    private EgressBwpFlow defaultEgressBwProfileMock;

    private IngressBwpFlow ingressBwProfilePerEvcMock;

    private EgressBwpFlow egressBwProfilePerEvcMock;

    private  IngressBwpUni ingressBwProfilePerUniMock;

    private  EgressBwpUni egressBwProfilePerUniMock;

    @Before
    public void setup() {
        defaultIngressBwProfileMock = mock(IngressBwpFlow.class);
        defaultEgressBwProfileMock = mock(EgressBwpFlow.class);
        ingressBwProfilePerEvcMock = mock(IngressBwpFlow.class);
        egressBwProfilePerEvcMock = mock(EgressBwpFlow.class);
        ingressBwProfilePerUniMock = mock(IngressBwpUni.class);
        egressBwProfilePerUniMock = mock(EgressBwpUni.class);
    }

    @Test
    public void testBuilder() {
        //given
        BandwidthProfileComposition.BandwidthProfileCompositionBuilder compositionBuilder = builderWithAllProfiles();

        //when
        BandwidthProfileComposition composition = compositionBuilder.build();

        //then
        assertTrue(composition.getDefaultIngressBwProfile().isPresent());
        assertTrue(composition.getDefaultEgressBwProfile().isPresent());
        assertTrue(composition.getIngressBwProfilePerEvc().isPresent());
        assertTrue(composition.getEgressBwProfilePerEvc().isPresent());
        assertTrue(composition.getIngressBwProfilePerUni().isPresent());
        assertTrue(composition.getEgressBwProfilePerUni().isPresent());

        assertEquals(defaultIngressBwProfileMock, composition.getDefaultIngressBwProfile().get());
        assertEquals(defaultEgressBwProfileMock, composition.getDefaultEgressBwProfile().get());
        assertEquals(ingressBwProfilePerEvcMock, composition.getIngressBwProfilePerEvc().get());
        assertEquals(egressBwProfilePerEvcMock, composition.getEgressBwProfilePerEvc().get());
        assertEquals(ingressBwProfilePerUniMock, composition.getIngressBwProfilePerUni().get());
        assertEquals(egressBwProfilePerUniMock, composition.getEgressBwProfilePerUni().get());
    }

    @Test
    public void testBuilderEmpty() {
        //given
        BandwidthProfileComposition.BandwidthProfileCompositionBuilder compositionBuilder = BandwidthProfileComposition.builder();

        //when
        BandwidthProfileComposition composition = compositionBuilder.build();

        //then
        assertNotNull(composition.getDefaultIngressBwProfile());
        assertNotNull(composition.getDefaultEgressBwProfile());
        assertNotNull(composition.getIngressBwProfilePerEvc());
        assertNotNull(composition.getEgressBwProfilePerEvc());
        assertNotNull(composition.getIngressBwProfilePerUni());
        assertNotNull(composition.getEgressBwProfilePerUni());

        assertFalse(composition.getDefaultIngressBwProfile().isPresent());
        assertFalse(composition.getDefaultEgressBwProfile().isPresent());
        assertFalse(composition.getIngressBwProfilePerEvc().isPresent());
        assertFalse(composition.getEgressBwProfilePerEvc().isPresent());
        assertFalse(composition.getIngressBwProfilePerUni().isPresent());
        assertFalse(composition.getEgressBwProfilePerUni().isPresent());
    }

    @Test
    public void testGet() {
        //given
        BandwidthProfileComposition composition = builderWithAllProfiles().build();

        //when
        Optional<GNRPBwpFlow> actualIngerssDefaultOptional = composition.get(INGRESS, DEFAULT);
        Optional<GNRPBwpFlow> actualIngressEvcOptional = composition.get(INGRESS, EVC);
        Optional<GNRPBwpFlow> actualIngerssUniOptional = composition.get(INGRESS, UNI);
        Optional<GNRPBwpFlow> actualEgerssDefaultOptional = composition.get(EGRESS, DEFAULT);
        Optional<GNRPBwpFlow> actualEgerssEvcOptional = composition.get(EGRESS, EVC);
        Optional<GNRPBwpFlow> actualEgerssUniOptional = composition.get(EGRESS, UNI);

        //then
        assertTrue(actualIngerssDefaultOptional.isPresent());
        assertTrue(actualIngressEvcOptional.isPresent());
        assertTrue(actualIngerssUniOptional.isPresent());
        assertTrue(actualEgerssDefaultOptional.isPresent());
        assertTrue(actualEgerssEvcOptional.isPresent());
        assertTrue(actualEgerssUniOptional.isPresent());

        assertEquals(defaultIngressBwProfileMock, actualIngerssDefaultOptional.get());
        assertEquals(defaultEgressBwProfileMock, actualEgerssDefaultOptional.get());
        assertEquals(ingressBwProfilePerEvcMock, actualIngressEvcOptional.get());
        assertEquals(egressBwProfilePerEvcMock, actualEgerssEvcOptional.get());
        assertEquals(ingressBwProfilePerUniMock, actualIngerssUniOptional.get());
        assertEquals(egressBwProfilePerUniMock, actualEgerssUniOptional.get());
    }

    @Test
    public void testHasAnyProfileDefinedPositive() {
        //given
        BandwidthProfileComposition composition = builderWithOneProfile().build();

        //when
        boolean actual = composition.hasAnyProfileDefined();

        //then
        assertTrue(actual);
    }

    @Test
    public void testHasAnyProfileDefinedNegative() {
        //given
        BandwidthProfileComposition composition = builderWithNoProfile().build();

        //when
        boolean actual = composition.hasAnyProfileDefined();

        //then
        assertFalse(actual);
    }

    private BandwidthProfileComposition.BandwidthProfileCompositionBuilder builderWithAllProfiles() {
        return BandwidthProfileComposition.builder()
                .defaultIngressBwProfile(Optional.of(defaultIngressBwProfileMock))
                .defaultEgressBwProfile(Optional.of(defaultEgressBwProfileMock))
                .ingressBwProfilePerEvc(Optional.of(ingressBwProfilePerEvcMock))
                .egressBwProfilePerEvc(Optional.of(egressBwProfilePerEvcMock))
                .ingressBwProfilePerUni(Optional.of(ingressBwProfilePerUniMock))
                .egressBwProfilePerUni(Optional.of(egressBwProfilePerUniMock));
    }

    private BandwidthProfileComposition.BandwidthProfileCompositionBuilder builderWithOneProfile() {
        return BandwidthProfileComposition.builder()
                .defaultIngressBwProfile(Optional.empty())
                .defaultEgressBwProfile(Optional.empty())
                .ingressBwProfilePerEvc(Optional.empty())
                .egressBwProfilePerEvc(Optional.of(egressBwProfilePerEvcMock))
                .ingressBwProfilePerUni(Optional.empty())
                .egressBwProfilePerUni(Optional.empty());
    }

    private BandwidthProfileComposition.BandwidthProfileCompositionBuilder builderWithNoProfile() {
        return BandwidthProfileComposition.builder()
                .defaultIngressBwProfile(Optional.empty())
                .defaultEgressBwProfile(Optional.empty())
                .ingressBwProfilePerEvc(Optional.empty())
                .egressBwProfilePerEvc(Optional.empty())
                .ingressBwProfilePerUni(Optional.empty())
                .egressBwProfilePerUni(Optional.empty());
    }
}
