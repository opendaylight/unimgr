package org.opendaylight.unimgr.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverBuilder;
import org.opendaylight.unimgr.mef.nrp.impl.ActivationDriverRepoServiceImpl;
import org.opendaylight.unimgr.utils.ActivationDriverMocks;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.forwarding.constructs.ForwardingConstruct;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.forwarding.constructs.ForwardingConstructBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPortBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;


class TestBusinessEx extends RuntimeException {
    public TestBusinessEx() {
        super("expected exception");
    }
}

/**
 * @author bartosz.michalik@amartus.com
 */
public class FcRouteActivatorServiceTest {


    private static final TopologyId topoA = new TopologyId("a");
    private static final TopologyId topoZ = new TopologyId("z");

    public ForwardingConstructActivatorService createService(List<ActivationDriverBuilder> builders) {
        return new ForwardingConstructActivatorService(new ActivationDriverRepoServiceImpl(builders));
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
        service.activate(singleNode());

        //then
        verify(d1).activate();
        verify(d1).commit();
    }

    @Test
    public void testActivateTwoNodesSingleVendor() throws Exception {
        //having
        final ActivationDriver d1 = mock(ActivationDriver.class);

        ForwardingConstructActivatorService service = createService(Collections.singletonList(
                ActivationDriverMocks.prepareDriver(port -> d1)
        ));

        //when
        service.activate(twoNodes());

        //then
        verify(d1, times(2)).activate();
        verify(d1, times(2)).commit();
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
        service.activate(twoNodes());

        //then
        verify(d1).activate();
        verify(d1).commit();
        verify(d2).activate();
        verify(d2).commit();
    }

    @Test
    public void testActivateSingleNodeFailure() throws Exception {

        //having
        final ActivationDriver d1 = spy(new FailingActivationDriver(p -> { if(p.getTopology().equals(topoA)) throw new TestBusinessEx();}));

        ForwardingConstructActivatorService service = createService(Collections.singletonList(
                ActivationDriverMocks.prepareDriver((p1,p2) -> d1)
        ));

        //when
        service.activate(singleNode());

        //then
        verify(d1, times(1)).rollback();
    }

    @Test
    public void testActivateMultiNodeFailure() throws Exception {

        //having
        final ActivationDriver d1 = spy(new FailingActivationDriver(p -> { if(p.getTopology().equals(topoA)) throw new TestBusinessEx();}));

        ForwardingConstructActivatorService service = createService(Collections.singletonList(
                ActivationDriverMocks.prepareDriver(p1 -> d1)
        ));

        //when
        service.activate(twoNodes());

        //then
        verify(d1, times(1)).activate();
        verify(d1, times(2)).rollback();
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
        service.deactivate(singleNode());

        //then
        verify(d1, times(1)).deactivate();
        verify(d1, times(1)).rollback();
    }

    @Test
    public void testDeactivateTwoNodesSingleVendor() throws Exception {
        //having
        final ActivationDriver d1 = mock(ActivationDriver.class);

        ForwardingConstructActivatorService service = createService(Collections.singletonList(
                ActivationDriverMocks.prepareDriver(port -> d1)
        ));

        //when
        service.deactivate(twoNodes());

        //then
        verify(d1, times(2)).deactivate();
        verify(d1, times(2)).commit();
    }

    private ForwardingConstruct singleNode() {
        return fc(
                port("a", "localhost", "80"),
                port("z", "localhost", "8080")
        );
    }

    private ForwardingConstruct twoNodes() {
        return fc(
                port("a", "192.168.1.1", "80"),
                port("z", "192.168.1.2", "80")
        );
    }

    private ForwardingConstruct fc(FcPort... ports) {
        return new ForwardingConstructBuilder()
                .setFcPort(Arrays.asList(ports))
                .build();
    }

    FcPort port(String topo, String host, String port) {
        return new FcPortBuilder()
                .setTopology(new TopologyId(topo))
                .setNode(new NodeId(host))
                .setTp(new TpId(port))
                .build();
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