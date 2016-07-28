/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.impl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverBuilder;
import org.opendaylight.unimgr.mef.nrp.impl.ActivationDriverRepoServiceImpl;
import org.opendaylight.unimgr.utils.ActivationDriverMocks;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.forwarding.constructs.ForwardingConstruct;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;
import static org.opendaylight.unimgr.impl.ForwardingConstructTestUtils.fcSingleNode;
import static org.opendaylight.unimgr.impl.ForwardingConstructTestUtils.fcTwoNodes;

class TestBusinessEx extends RuntimeException {
    public TestBusinessEx() {
        super("expected exception");
    }
}

/**
 * @author bartosz.michalik@amartus.com
 * @author krzysztof.bijakowski@amartus.com [modifications]
 */
public class FcRouteActivatorServiceTest {

    private static final TopologyId topoA = new TopologyId("a");

    private static final TopologyId topoZ = new TopologyId("z");

    private ForwardingConstructActivationStateTracker stateTracker;

    public ForwardingConstructActivatorService createService(List<ActivationDriverBuilder> builders) {
        return new ForwardingConstructActivatorService(new ActivationDriverRepoServiceImpl(builders));
    }

    @Before
    public void setup() {
        stateTracker = Mockito.mock(ForwardingConstructActivationStateTracker.class);
        when(stateTracker.isActivatable()).thenReturn(true);
        when(stateTracker.isDeactivatable()).thenReturn(true);
    }

    @Test
    public void testActivateSingleNode() throws Exception {
        //having
        final ActivationDriver d1 = mock(ActivationDriver.class);

        ForwardingConstructActivatorService service = createService(Arrays.asList(
                ActivationDriverMocks.prepareDriver((port1, port2) -> topoA.equals(port1.getTopology()) ? d1 : null),
                ActivationDriverMocks.prepareDriver((port1, port2) -> null)
        ));

        //when
        service.activate(fcSingleNode(), stateTracker);

        //then
        verify(d1).activate();
        verify(d1).commit();
        verify(stateTracker).isActivatable();
        verify(stateTracker).activated(Mockito.any());
    }

    @Test
    public void testActivateTwoNodesSingleVendor() throws Exception {
        //having
        final ActivationDriver d1 = mock(ActivationDriver.class);

        ForwardingConstructActivatorService service = createService(Collections.singletonList(
                ActivationDriverMocks.prepareDriver(port -> d1)
        ));

        //when
        service.activate(fcTwoNodes(), stateTracker);

        //then
        verify(d1, times(2)).activate();
        verify(d1, times(2)).commit();
        verify(stateTracker).isActivatable();
        verify(stateTracker).activated(Mockito.any());
    }

    @Test
    public void testActivateTwoNodesMultiVendor() throws Exception {
        //having
        final ActivationDriver d1 = mock(ActivationDriver.class);
        final ActivationDriver d2 = mock(ActivationDriver.class);

        ForwardingConstructActivatorService service = createService(Arrays.asList(
                ActivationDriverMocks.prepareDriver(port -> topoA.equals(port.getTopology()) ? d1 : null),
                ActivationDriverMocks.prepareDriver(port -> topoZ.equals(port.getTopology()) ? d2 : null)
        ));

        //when
        service.activate(fcTwoNodes(), stateTracker);
        //then
        verify(d1).activate();
        verify(d1).commit();
        verify(d2).activate();
        verify(d2).commit();
        verify(stateTracker).isActivatable();
        verify(stateTracker).activated(Mockito.any());
    }

    @Test
    public void testActivateSingleNodeFailure() throws Exception {
        //having
        final ActivationDriver d1 = spy(new FailingActivationDriver(p -> { if(p.getTopology().equals(topoA)) throw new TestBusinessEx();}));

        ForwardingConstructActivatorService service = createService(Collections.singletonList(
                ActivationDriverMocks.prepareDriver((p1,p2) -> d1)
        ));

        //when
        service.activate(fcSingleNode(), stateTracker);

        //then
        verify(d1, times(1)).rollback();
        verify(stateTracker).isActivatable();
        verify(stateTracker).activationFailed(Mockito.any());
    }

    @Test
    public void testActivateFcExists() throws Exception {
        //having
        ForwardingConstructActivationStateTracker stateTrackerFcExists = Mockito.mock(ForwardingConstructActivationStateTracker.class);
        when(stateTrackerFcExists.isActivatable()).thenReturn(false);

        final ActivationDriver d1 = mock(ActivationDriver.class);

        ForwardingConstructActivatorService service = createService(Arrays.asList(
                ActivationDriverMocks.prepareDriver((port1, port2) -> topoA.equals(port1.getTopology()) ? d1 : null),
                ActivationDriverMocks.prepareDriver((port1, port2) -> null)
        ));

        //when
        service.activate(fcSingleNode(), stateTrackerFcExists);

        //then
        verify(d1, never()).activate();
        verify(d1, never()).commit();
        verify(stateTrackerFcExists).isActivatable();
        verify(stateTrackerFcExists, never()).activated(Mockito.any());
        verify(stateTrackerFcExists, never()).activationFailed(Mockito.any());
    }

    @Test
    public void testActivateMultiNodeFailure() throws Exception {
        //having
        final ActivationDriver d1 = spy(new FailingActivationDriver(p -> { if(p.getTopology().equals(topoA)) throw new TestBusinessEx();}));

        ForwardingConstructActivatorService service = createService(Collections.singletonList(
                ActivationDriverMocks.prepareDriver(p1 -> d1)
        ));

        //when
        service.activate(fcTwoNodes(), stateTracker);

        //then
        verify(d1, times(1)).activate();
        verify(d1, times(2)).rollback();
        verify(stateTracker).isActivatable();
        verify(stateTracker).activationFailed(Mockito.any());
    }

    @Test
    public void testDeactivateSingleNodeFailure() throws Exception {
        //having
        final ActivationDriver d1 = spy(new FailingActivationDriver(p -> { if(p.getTopology().equals(topoA)) throw new TestBusinessEx();}));

        ForwardingConstructActivatorService service = createService(Arrays.asList(
                ActivationDriverMocks.prepareDriver((p1,p2) -> null),
                ActivationDriverMocks.prepareDriver((p1,p2) -> d1)
        ));

        //when
        service.deactivate(fcSingleNode(), stateTracker);

        //then
        verify(d1, times(1)).deactivate();
        verify(d1, times(1)).rollback();
        verify(stateTracker).isDeactivatable();
        verify(stateTracker).deactivationFailed();
    }

    @Test
    public void testDeactivateTwoNodesSingleVendor() throws Exception {
        //having
        final ActivationDriver d1 = mock(ActivationDriver.class);

        ForwardingConstructActivatorService service = createService(Collections.singletonList(
                ActivationDriverMocks.prepareDriver(port -> d1)
        ));

        //when
        service.deactivate(fcTwoNodes(), stateTracker);

        //then
        verify(d1, times(2)).deactivate();
        verify(d1, times(2)).commit();
        verify(stateTracker).isDeactivatable();
        verify(stateTracker).deactivated();
    }

    @Test
    public void testDeactivateFcNotExists() throws Exception {
        //having
        ForwardingConstructActivationStateTracker stateTrackerFcNotExists = Mockito.mock(ForwardingConstructActivationStateTracker.class);
        when(stateTrackerFcNotExists.isDeactivatable()).thenReturn(false);

        final ActivationDriver d1 = mock(ActivationDriver.class);

        ForwardingConstructActivatorService service = createService(Collections.singletonList(
                ActivationDriverMocks.prepareDriver(port -> d1)
        ));

        //when
        service.deactivate(fcTwoNodes(), stateTrackerFcNotExists);

        //then
        verify(d1, never()).deactivate();
        verify(d1, never()).commit();
        verify(stateTrackerFcNotExists).isDeactivatable();
        verify(stateTrackerFcNotExists, never()).deactivated();
        verify(stateTrackerFcNotExists, never()).deactivationFailed();
    }

    private static class FailingActivationDriver implements ActivationDriver {

        private final Consumer<FcPort> consumer;

        private FcPort from;

        FailingActivationDriver(Consumer<FcPort> portConsumer) {
            this.consumer  = portConsumer;
        }

        @Override
        public void commit() {

        }

        @Override
        public void rollback() {

        }

        @Override
        public void initialize(FcPort from, FcPort to, ForwardingConstruct context) {
            if(this.from == null)
                this.from = from;
        }

        @Override
        public void activate() {
            consumer.accept(from);
        }

        @Override
        public void deactivate() {
            consumer.accept(from);
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
