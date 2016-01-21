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

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed100M;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed100MBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10G;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10GBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10M;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10MBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed1G;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed1GBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.uni.Speed;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest(UniAddShellCommand.class)
public class UniAddShellCommandTest {

    private final String physicalMedium = "UNI TypeFull Duplex 2 Physical Interface";
    private final String macAddress = "00:00:00:00:00:00";
    private final String mode = "Full Duplex";
    private final String macLayer = "IEEE 802.3-2005";
    private final String type = "";
    private final String mtuSize = "0";
    private final String ipAddress = "192.168.1.1";

    private final UnimgrConsoleProviderTest provider = new UnimgrConsoleProviderTest();

    @Mock UniAddShellCommand uniAddShellCommand;
    //@Mock UnimgrProvider provider;

    @Before
    public void setUp() throws IllegalArgumentException, IllegalAccessException{
        uniAddShellCommand = mock(UniAddShellCommand.class, Mockito.CALLS_REAL_METHODS);
        MemberModifier.field(UniAddShellCommand.class, "provider").set(uniAddShellCommand, provider);
        Whitebox.setInternalState(uniAddShellCommand, "macAddress", macAddress);
        Whitebox.setInternalState(uniAddShellCommand, "macLayer", macLayer);
        Whitebox.setInternalState(uniAddShellCommand, "mode", mode);
        Whitebox.setInternalState(uniAddShellCommand, "mtuSize", mtuSize);
        Whitebox.setInternalState(uniAddShellCommand, "physicalMedium", physicalMedium);
        Whitebox.setInternalState(uniAddShellCommand, "type", type);
        Whitebox.setInternalState(uniAddShellCommand, "ipAddress", ipAddress);
    }

    /**
     * Test method for {@link org.opendaylight.unimgr.cli.UniAddShellCommand#getSpeed()}.
     * @throws Exception
     */
    @Test
    public void testgetSpeed() throws Exception {
        // Test 10M
        Whitebox.setInternalState(uniAddShellCommand, "speed", "10M");
        final Speed10M speed10M = mock(Speed10M.class);
        final Speed10MBuilder speed10MBuilder = mock(Speed10MBuilder.class);
        when(speed10MBuilder.build()).thenReturn(speed10M);
        PowerMockito.whenNew(Speed10MBuilder.class).withNoArguments().thenReturn(speed10MBuilder);
        Object getSpeed = Whitebox.invokeMethod(uniAddShellCommand, "getSpeed");
        assertEquals(speed10M, getSpeed);

        // Test 100M
        Whitebox.setInternalState(uniAddShellCommand, "speed", "100M");
        final Speed100M speedObject100M = mock(Speed100M.class);
        final Speed100MBuilder speed100M = mock(Speed100MBuilder.class);
        PowerMockito.whenNew(Speed100MBuilder.class).withNoArguments().thenReturn(speed100M);
        when(speed100M.build()).thenReturn(speedObject100M);
        getSpeed = Whitebox.invokeMethod(uniAddShellCommand, "getSpeed");
        assertEquals(speedObject100M, getSpeed);

        // Test 1G
        Whitebox.setInternalState(uniAddShellCommand, "speed", "1G");
        final Speed1G speedObject1G = mock(Speed1G.class);
        final Speed1GBuilder speed1G = mock(Speed1GBuilder.class);
        PowerMockito.whenNew(Speed1GBuilder.class).withNoArguments().thenReturn(speed1G);
        when(speed1G.build()).thenReturn(speedObject1G);
        getSpeed = Whitebox.invokeMethod(uniAddShellCommand, "getSpeed");
        assertEquals(speedObject1G, getSpeed);

        // Test 10G
        Whitebox.setInternalState(uniAddShellCommand, "speed", "10G");
        final Speed10G speedObject10G = mock(Speed10G.class);
        final Speed10GBuilder speed10G = mock(Speed10GBuilder.class);
        PowerMockito.whenNew(Speed10GBuilder.class).withNoArguments().thenReturn(speed10G);
        when(speed10G.build()).thenReturn(speedObject10G);
        getSpeed = Whitebox.invokeMethod(uniAddShellCommand, "getSpeed");
        assertEquals(speedObject10G, getSpeed);

        // Test other
        Whitebox.setInternalState(uniAddShellCommand, "speed", "other");
        getSpeed = Whitebox.invokeMethod(uniAddShellCommand, "getSpeed");
        assertEquals(null, getSpeed);
    }

    /**
     * Test method for {@link org.opendaylight.unimgr.cli.UniAddShellCommand#doExecute()}.
     * @throws Exception
     */
    @Test
    public void testDoExecute() throws Exception {
        Whitebox.setInternalState(uniAddShellCommand, "speed", "other");
        final UniAugmentation uni = new UniAugmentationBuilder()
                .setMacAddress(new MacAddress(macAddress))
                .setMacLayer(macLayer)
                .setMode(mode)
                .setMtuSize(BigInteger.valueOf(Long.valueOf(mtuSize)))
                .setPhysicalMedium(physicalMedium)
                .setSpeed((Speed) null)
                .setType(type)
                .setIpAddress(new IpAddress(ipAddress.toCharArray()))
                .build();
        final Object stringWorked = new String("Uni with ip " +ipAddress+" created");
        final Object stringFailed = new String("Error creating new Uni");
        Object stringReturn = uniAddShellCommand.doExecute();
        assertEquals(stringWorked, stringReturn);
        assertEquals(uni, provider.getUni(null));
        stringReturn = uniAddShellCommand.doExecute();
        assertEquals(stringFailed, stringReturn);
    }

}