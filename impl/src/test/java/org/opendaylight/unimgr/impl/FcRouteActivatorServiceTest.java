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
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corefoundationmodule.superclassesandcommonpackages.rev160413.UniversalId;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.GFcPort;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.GForwardingConstruct;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.fcroutelist.FcRoute;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.fcroutelist.FcRouteBuilder;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.g_fcroute.ForwardingConstruct;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.g_fcroute.ForwardingConstructBuilder;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.g_forwardingconstruct.FcPort;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.g_forwardingconstruct.FcPortBuilder;

/**
 * @author bartosz.michalik@amartus.com
 */
public class FcRouteActivatorServiceTest {

    public FcRouteActivatorService createService(List<ActivationDriverBuilder> builders) {
        return new FcRouteActivatorService(new ActivationDriverRepoServiceImpl(builders));
    }

    @Test
    public void testActivateSingleNode() throws Exception {

        //having
        final ActivationDriver d1 = mock(ActivationDriver.class);

        FcRouteActivatorService service = createService(Arrays.asList(
                ActivationDriverMocks.prepareDriver((port1, port2) -> "a".equals(port1.getId()) ? d1 : null),
                ActivationDriverMocks.prepareDriver((port1, port2) -> null)
        ));

        //when
        service.activate(buildFor(singleNode()));

        //then
        verify(d1).activate();
        verify(d1).commit();
    }

    @Test
    public void testActivateTwoNodesSingleVendor() throws Exception {
        //having
        final ActivationDriver d1 = mock(ActivationDriver.class);

        FcRouteActivatorService service = createService(Collections.singletonList(
                ActivationDriverMocks.prepareDriver(port -> d1)
        ));

        //when
        service.activate(buildFor(twoNodes()));

        //then
        verify(d1, times(2)).activate();
        verify(d1, times(2)).commit();
    }

    @Test
    public void testActivateTwoNodesMultiVendor() throws Exception {

        //having
        final ActivationDriver d1 = mock(ActivationDriver.class);
        final ActivationDriver d2 = mock(ActivationDriver.class);

        FcRouteActivatorService service = createService(Arrays.asList(
                ActivationDriverMocks.prepareDriver(port -> "a".equals(port.getId()) ? d1 : null),
                ActivationDriverMocks.prepareDriver(port -> "z".equals(port.getId()) ? d2 : null)
        ));

        //when
        service.activate(buildFor(twoNodes()));

        //then
        verify(d1).activate();
        verify(d1).commit();
        verify(d2).activate();
        verify(d2).commit();
    }

    @Test
    public void testActivateSingleNodeFailure() throws Exception {

        //having
        final ActivationDriver d1 = spy(new FailingActivationDriver(p -> { if(p.getId().equals("a")) throw new NullPointerException();}));

        FcRouteActivatorService service = createService(Collections.singletonList(
                ActivationDriverMocks.prepareDriver((p1,p2) -> d1)
        ));

        //when
        service.activate(buildFor(singleNode()));

        //then
        verify(d1, times(1)).rollback();
    }

    @Test
    public void testActivateMultiNodeFailure() throws Exception {

        //having
        final ActivationDriver d1 = spy(new FailingActivationDriver(p -> { if(p.getId().equals("a")) throw new NullPointerException();}));

        FcRouteActivatorService service = createService(Collections.singletonList(
                ActivationDriverMocks.prepareDriver(p1 -> d1)
        ));

        //when
        service.activate(buildFor(twoNodes()));

        //then
        verify(d1, times(1)).activate();
        verify(d1, times(2)).rollback();
    }

    @Test
    public void testDeactivateSingleNodeFailure() throws Exception {

        //having
        final ActivationDriver d1 = spy(new FailingActivationDriver(p -> { if(p.getId().equals("a")) throw new NullPointerException();}));

        FcRouteActivatorService service = createService(Arrays.asList(
                ActivationDriverMocks.prepareDriver((p1,p2) -> null),
                ActivationDriverMocks.prepareDriver((p1,p2) -> d1)
        ));

        //when
        service.deactivate(buildFor(singleNode()));

        //then
        verify(d1, times(1)).deactivate();
        verify(d1, times(1)).rollback();
    }

    @Test
    public void testDeactivateTwoNodesSingleVendor() throws Exception {
        //having
        final ActivationDriver d1 = mock(ActivationDriver.class);

        FcRouteActivatorService service = createService(Collections.singletonList(
                ActivationDriverMocks.prepareDriver(port -> d1)
        ));

        //when
        service.deactivate(buildFor(twoNodes()));

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

    private FcRoute buildFor(ForwardingConstruct fc) {
        return new FcRouteBuilder()
                .setForwardingConstruct(Collections.singletonList(fc))
                .build();
    }

    FcPort port(String id, String host, String port) {
        return new FcPortBuilder()
                .setId(id)
                .setLtpRefList(Arrays.asList(new UniversalId(host + ":" + port)))
                .build();
    }

    private static class FailingActivationDriver implements ActivationDriver {

        private final Consumer<GFcPort> consumer;
        private GFcPort from;

        FailingActivationDriver(Consumer<GFcPort> portConsumer) {
            this.consumer  = portConsumer;
        }

        @Override
        public void commit() {

        }

        @Override
        public void rollback() {

        }

        @Override
        public void initialize(GFcPort from, GFcPort to, GForwardingConstruct context) {
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