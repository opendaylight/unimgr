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

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.EgressBwBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.IngressBwBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDestKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSourceKey;
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
     * Test method for {@link org.opendaylight.unimgr.cli.EvcAddShellCommand#doExecute()}.
     * @throws Exception
     */
    @Test
    public void testDoExecute() throws Exception {
        final EvcAddShellCommand spyEvc = PowerMockito.spy(new EvcAddShellCommand(provider));
        Whitebox.setInternalState(spyEvc, "ipSource", IPs);
        Whitebox.setInternalState(spyEvc, "ipDestination", IPd);
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
                                     .setEgressBw(new EgressBwBuilder().setSpeed(Utils.getSpeed(egress)).build())
                                     .setIngressBw(new IngressBwBuilder().setSpeed(Utils.getSpeed(ingress)).build())
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
