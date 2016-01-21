/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.cli;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.support.membermodification.MemberMatcher.method;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.EgressBw;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.IngressBw;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDestKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSourceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed100M;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed100MBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10G;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10GBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10M;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10MBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed1G;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed1GBuilder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest(EvcAddShellCommand.class)
public class EvcAddShellCommandTest {

    private final String IPs = "192.168.1.1";
    private final String IPd = "192.168.1.2";
    private final String egress = "100M";
    private final String ingress = "10M";

    private final UnimgrConsoleProviderTest provider = new UnimgrConsoleProviderTest();

    @Mock EvcAddShellCommand evcAddShellCommand;

    @Before
    public void setUp() throws IllegalArgumentException, IllegalAccessException{
        evcAddShellCommand = mock(EvcAddShellCommand.class, Mockito.CALLS_REAL_METHODS);
        MemberModifier.field(EvcAddShellCommand.class, "provider").set(evcAddShellCommand, provider);

    }

    /**
     * Test method for {@link org.opendaylight.unimgr.cli.EvcAddShellCommand#getSpeed()}.
     * @throws Exception
     */
    @Test
    public void testgetSpeed() throws Exception {
        // Test 10M
        String speed = "10M";
        final Speed10M speed10M = mock(Speed10M.class);
        final Speed10MBuilder speed10MBuilder = mock(Speed10MBuilder.class);
        when(speed10MBuilder.build()).thenReturn(speed10M);
        PowerMockito.whenNew(Speed10MBuilder.class).withNoArguments().thenReturn(speed10MBuilder);
        Object getSpeed = Whitebox.invokeMethod(evcAddShellCommand, "getSpeed", speed);
        assertEquals(speed10M, getSpeed);

        // Test 100M
        speed = "100M";
        final Speed100M speedObject100M = mock(Speed100M.class);
        final Speed100MBuilder speed100M = mock(Speed100MBuilder.class);
        PowerMockito.whenNew(Speed100MBuilder.class).withNoArguments().thenReturn(speed100M);
        when(speed100M.build()).thenReturn(speedObject100M);
        getSpeed = Whitebox.invokeMethod(evcAddShellCommand, "getSpeed", speed);
        assertEquals(speedObject100M, getSpeed);

        // Test 1G
        speed = "1G";
        final Speed1G speedObject1G = mock(Speed1G.class);
        final Speed1GBuilder speed1G = mock(Speed1GBuilder.class);
        PowerMockito.whenNew(Speed1GBuilder.class).withNoArguments().thenReturn(speed1G);
        when(speed1G.build()).thenReturn(speedObject1G);
        getSpeed = Whitebox.invokeMethod(evcAddShellCommand, "getSpeed", speed);
        assertEquals(speedObject1G, getSpeed);

        // Test 10G
        speed = "10G";
        final Speed10G speedObject10G = mock(Speed10G.class);
        final Speed10GBuilder speed10G = mock(Speed10GBuilder.class);
        PowerMockito.whenNew(Speed10GBuilder.class).withNoArguments().thenReturn(speed10G);
        when(speed10G.build()).thenReturn(speedObject10G);
        getSpeed = Whitebox.invokeMethod(evcAddShellCommand, "getSpeed", speed);
        assertEquals(speedObject10G, getSpeed);

        // Test other
        speed = "other";
        getSpeed = Whitebox.invokeMethod(evcAddShellCommand, "getSpeed", speed);
        assertEquals(null, getSpeed);
    }

    /**
     * Test method for {@link org.opendaylight.unimgr.cli.EvcAddShellCommand#doExecute()}.
     * @throws Exception
     */
    @Test
    public void testDoExecute() throws Exception {
        final EgressBw egressBw100M = mock(EgressBw.class);
        final IngressBw ingressBw10M = mock(IngressBw.class);
        final EvcAddShellCommand spyEvc = PowerMockito.spy(new EvcAddShellCommand(provider));
        PowerMockito.when(spyEvc, method(EvcAddShellCommand.class, "getSpeed", String.class))
            .withArguments("100M").thenReturn(egressBw100M);
        PowerMockito.when(spyEvc, method(EvcAddShellCommand.class, "getSpeed", String.class))
            .withArguments("10M").thenReturn(ingressBw10M);
        Whitebox.setInternalState(spyEvc, "IPs", IPs);
        Whitebox.setInternalState(spyEvc, "IPd", IPd);
        Whitebox.setInternalState(spyEvc, "egress", egress);
        Whitebox.setInternalState(spyEvc, "ingress", ingress);
        final Short order = new Short("0");
        final IpAddress ipAddreSource = new IpAddress(IPs.toCharArray());
        final UniSource uniSource = new UniSourceBuilder()
                                  .setIpAddress(ipAddreSource)
                                  .setKey(new UniSourceKey(order))
                                  .setOrder(order)
                                  .build();
        final List<UniSource> uniSourceList = new ArrayList<UniSource>();
        uniSourceList.add(uniSource);
        final IpAddress ipAddreDest = new IpAddress(IPd.toCharArray());
        final UniDest uniDest = new UniDestBuilder()
                          .setOrder(order)
                          .setKey(new UniDestKey(order))
                          .setIpAddress(ipAddreDest)
                          .build();
        final List<UniDest> uniDestList = new ArrayList<UniDest>();
        uniDestList.add(uniDest);
        final EvcAugmentation evcAug = new EvcAugmentationBuilder()
                                     .setCosId(UnimgrConstants.EVC_PREFIX + 1)
                                     .setEgressBw((EgressBw) egressBw100M)
                                     .setIngressBw((IngressBw) ingressBw10M)
                                     .setUniDest(uniDestList)
                                     .setUniSource(uniSourceList)
                                     .build();
        final Object success = new String("Evc with Source Uni " +IPs+" and destenation Uni " +IPd+" created");
        final Object error = new String("Error creating new Evc");
        Object stringReturn = spyEvc.doExecute();
        assertEquals(success, stringReturn);
        assertEquals(evcAug, provider.getEvc(null));
        stringReturn = spyEvc.doExecute();
        assertEquals(error, stringReturn);
    }

}
