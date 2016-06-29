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

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.uni.SpeedBuilder;
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
    private final String speed = "1G";

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
        Whitebox.setInternalState(uniAddShellCommand, "speed", speed);
    }

    /**
     * Test method for {@link org.opendaylight.unimgr.cli.UniAddShellCommand#doExecute()}.
     * @throws Exception
     */
    @Test
    public void testDoExecute() throws Exception {
        final UniAugmentation uni = new UniAugmentationBuilder()
                .setMacAddress(new MacAddress(macAddress))
                .setMacLayer(macLayer)
                .setMode(mode)
                .setMtuSize(BigInteger.valueOf(Long.valueOf(mtuSize)))
                .setPhysicalMedium(physicalMedium)
                .setSpeed(new SpeedBuilder().setSpeed(Utils.getSpeed(speed)).build())
                .setType(type)
                .setIpAddress(new IpAddress(ipAddress.toCharArray()))
                .build();
        final Object success = new String("Uni with ip " +ipAddress+" created");
        final Object error = new String("Error creating new Uni");
        Object stringReturn = uniAddShellCommand.doExecute();
        assertEquals(success, stringReturn);
        assertEquals(uni, provider.getUni(null));
        stringReturn = uniAddShellCommand.doExecute();
        assertEquals(error, stringReturn);
    }

}
