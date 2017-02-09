/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.specs.rev160630.AdapterSpec1;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.specs.rev160630.TerminationSpec1;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.specs.rev160630.g_nrp_connadaptspec.EgressBwpFlow;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.specs.rev160630.g_nrp_connadaptspec.IngressBwpFlow;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.specs.rev160630.g_nrp_uni_terminationspec.EgressBwpUni;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.specs.rev160630.g_nrp_uni_terminationspec.IngressBwpUni;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.specs.rev160630.network.topology.topology.node.termination.point.ltp.attrs.lplist.lpspec.adapterspec.NrpConnAdaptSpecAttrs;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.specs.rev160630.network.topology.topology.node.termination.point.ltp.attrs.lplist.lpspec.adapterspec.NrpEvcEndpointConnAdaptSpecAttrs;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.specs.rev160630.network.topology.topology.node.termination.point.ltp.attrs.lplist.lpspec.terminationspec.NrpUniTerminationAttrs;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_layerprotocol.LpSpec;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_logicalterminationpoint.LpList;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.network.topology.topology.node.termination.point.LtpAttrs;
import org.opendaylight.yang.gen.v1.urn.onf.core.specs.rev160630.g_layerprotocolspec.AdapterSpec;
import org.opendaylight.yang.gen.v1.urn.onf.core.specs.rev160630.g_layerprotocolspec.TerminationSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Queues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesOtherConfig;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MdsalUtils.class)
public class BandwidthProfileHelperTest {

    @Test
    public void testRetrieveBandwidthProfiles() {
        //given
        FcPort fcPort = mock(FcPort.class);

        IngressBwpFlow expectedIngressDefaultBwp = mock(IngressBwpFlow.class);
        EgressBwpFlow expectedEgressDefaultBwp = mock(EgressBwpFlow.class);
        IngressBwpFlow expectedIngressEvcBwp = mock(IngressBwpFlow.class);
        EgressBwpFlow expectedEgressEvcBwp = mock(EgressBwpFlow.class);
        IngressBwpUni expectedIngressUniBwp = null;
        EgressBwpUni expectedEgressUniBwp = null;

        DataBroker dataBroker = mockDatastore(fcPort,
                Optional.ofNullable(expectedIngressDefaultBwp),
                Optional.ofNullable(expectedEgressDefaultBwp),
                Optional.ofNullable(expectedIngressEvcBwp),
                Optional.ofNullable(expectedEgressEvcBwp),
                Optional.ofNullable(expectedIngressUniBwp),
                Optional.ofNullable(expectedEgressUniBwp));

        //when
        List<BandwidthProfileComposition> actual  = new BandwidthProfileHelper(dataBroker, fcPort).getBandwidthProfiles();

        //then
        assertNotNull(actual);
        assertEquals(1, actual.size());

        BandwidthProfileComposition actualBpc = actual.get(0);
        assertTrue(actualBpc.hasAnyProfileDefined());

        assertTrue(actualBpc.getDefaultIngressBwProfile().isPresent());
        assertEquals(expectedIngressDefaultBwp, actualBpc.getDefaultIngressBwProfile().get());

        assertTrue(actualBpc.getDefaultEgressBwProfile().isPresent());
        assertEquals(expectedEgressDefaultBwp, actualBpc.getDefaultEgressBwProfile().get());

        assertTrue(actualBpc.getIngressBwProfilePerEvc().isPresent());
        assertEquals(expectedIngressEvcBwp, actualBpc.getIngressBwProfilePerEvc().get());

        assertTrue(actualBpc.getEgressBwProfilePerEvc().isPresent());
        assertEquals(expectedEgressEvcBwp, actualBpc.getEgressBwProfilePerEvc().get());

        assertFalse(actualBpc.getIngressBwProfilePerUni().isPresent());
        assertFalse(actualBpc.getEgressBwProfilePerUni().isPresent());
    }

    @Test
    public void testRetrieveBandwidthProfilesNoQos() {
        //given
        FcPort fcPort = mock(FcPort.class);

        DataBroker dataBroker = mockDatastore(fcPort,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        //when
        List<BandwidthProfileComposition> actual  = new BandwidthProfileHelper(dataBroker, fcPort).getBandwidthProfiles();

        //then
        assertNotNull(actual);
        assertEquals(1, actual.size());

        BandwidthProfileComposition actualBpc = actual.get(0);
        assertFalse(actualBpc.hasAnyProfileDefined());
        assertFalse(actualBpc.getDefaultIngressBwProfile().isPresent());
        assertFalse(actualBpc.getDefaultEgressBwProfile().isPresent());
        assertFalse(actualBpc.getIngressBwProfilePerEvc().isPresent());
        assertFalse(actualBpc.getEgressBwProfilePerEvc().isPresent());
        assertFalse(actualBpc.getIngressBwProfilePerUni().isPresent());
        assertFalse(actualBpc.getEgressBwProfilePerUni().isPresent());
    }

    @Test
    public void testRetrieveBandwidthProfilesEmpty() {
        //given
        FcPort fcPort = mock(FcPort.class);

        DataBroker dataBroker = mockDatastoreEmpty(fcPort);

        //when
        List<BandwidthProfileComposition> actual  = new BandwidthProfileHelper(dataBroker, fcPort).getBandwidthProfiles();

        //then
        assertNotNull(actual);
        assertEquals(0, actual.size());
    }

    private DataBroker mockDatastore(FcPort fcPort,
                                     Optional<IngressBwpFlow> ingressDefaultBwp,
                                     Optional<EgressBwpFlow> egressDefaultBwp,
                                     Optional<IngressBwpFlow> ingressEvcBwp,
                                     Optional<EgressBwpFlow> egressEvcBwp,
                                     Optional<IngressBwpUni> ingressUniBwp,
                                     Optional<EgressBwpUni> egressUniBwp) {
        DataBroker dataBroker = mock(DataBroker.class);

        TerminationPoint tp = mock(TerminationPoint.class);
        TerminationPoint1 tp1 = mock(TerminationPoint1.class);
        LtpAttrs ltpAttrs = mock(LtpAttrs.class);
        LpList lpList = mock(LpList.class);
        LpSpec lpSpec = mock(LpSpec.class);
        List<LpList> lpLists = new ArrayList<>();
        lpLists.add(lpList);

        when(tp.getAugmentation(TerminationPoint1.class)).thenReturn(tp1);
        when(tp1.getLtpAttrs()).thenReturn(ltpAttrs);
        when(ltpAttrs.getLpList()).thenReturn(lpLists);
        when(lpList.getLpSpec()).thenReturn(lpSpec);

        AdapterSpec adapterSpec = mock(AdapterSpec.class);
        AdapterSpec1 adapterSpec1 = mock(AdapterSpec1.class);
        NrpEvcEndpointConnAdaptSpecAttrs evcAttrs = mock(NrpEvcEndpointConnAdaptSpecAttrs.class);
        NrpConnAdaptSpecAttrs connAdaptSpecAttrs = mock(NrpConnAdaptSpecAttrs.class);

        when(lpSpec.getAdapterSpec()).thenReturn(adapterSpec);
        when(adapterSpec.getAugmentation(AdapterSpec1.class)).thenReturn(adapterSpec1);
        when(adapterSpec1.getNrpConnAdaptSpecAttrs()).thenReturn(connAdaptSpecAttrs);
        when(adapterSpec1.getNrpEvcEndpointConnAdaptSpecAttrs()).thenReturn(evcAttrs);

        if(ingressDefaultBwp.isPresent()) {
            when(connAdaptSpecAttrs.getIngressBwpFlow()).thenReturn(ingressDefaultBwp.get());
        }

        if(egressDefaultBwp.isPresent()) {
            when(connAdaptSpecAttrs.getEgressBwpFlow()).thenReturn(egressDefaultBwp.get());
        }

        if(ingressEvcBwp.isPresent()) {
            when(evcAttrs.getIngressBwpFlow()).thenReturn(ingressEvcBwp.get());
        }

        if(egressEvcBwp.isPresent()) {
            when(evcAttrs.getEgressBwpFlow()).thenReturn(egressEvcBwp.get());
        }

        TerminationSpec terminationSpec = mock(TerminationSpec.class);
        TerminationSpec1 terminationSpec1 = mock(TerminationSpec1.class);
        NrpUniTerminationAttrs nrpUniTerminationAttrs = mock(NrpUniTerminationAttrs.class);

        when(lpSpec.getTerminationSpec()).thenReturn(terminationSpec);
        when(terminationSpec.getAugmentation(TerminationSpec1.class)).thenReturn(terminationSpec1);
        when(terminationSpec1.getNrpUniTerminationAttrs()).thenReturn(nrpUniTerminationAttrs);

        if(ingressUniBwp.isPresent()) {
            when(nrpUniTerminationAttrs.getIngressBwpUni()).thenReturn(ingressUniBwp.get());
        }

        if(egressUniBwp.isPresent()) {
            when(nrpUniTerminationAttrs.getEgressBwpUni()).thenReturn(egressUniBwp.get());
        }

        PowerMockito.mockStatic(MdsalUtils.class);
        when(MdsalUtils.readTerminationPoint(eq(dataBroker), eq(CONFIGURATION), eq(fcPort))).thenReturn(com.google.common.base.Optional.of(tp));

        return dataBroker;
    }

    private DataBroker mockDatastoreEmpty(FcPort fcPort) {
        DataBroker dataBroker = mock(DataBroker.class);

        PowerMockito.mockStatic(MdsalUtils.class);
        when(MdsalUtils.readTerminationPoint(eq(dataBroker), eq(CONFIGURATION), eq(fcPort))).thenReturn(com.google.common.base.Optional.absent());

        return dataBroker;
    }
}
