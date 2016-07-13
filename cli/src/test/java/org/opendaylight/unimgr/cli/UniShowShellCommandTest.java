/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.cli;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.uni.Speed;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.reflect.Whitebox;

public class UniShowShellCommandTest {

    private static final String macAddress = "00:00:00:00:00:00";
    private static final String physicalMedium = "UNI TypeFull Duplex 2 Physical Interface";
    private static final String speed = "";
    private static final String mode = "Full Duplex";
    private static final String macLayer = "IEEE 802.3-2005";
    private static final String type = "";
    private static final BigInteger mtuSize = BigInteger.valueOf(0);
    private static final String ipAddress = "192.168.1.1";

    private final UnimgrConsoleProviderTest provider = new UnimgrConsoleProviderTest();

    @Mock UniShowShellCommand uniShowShellCommand;

    @Before
    public void setUp() throws IllegalArgumentException, IllegalAccessException{
        uniShowShellCommand = mock(UniShowShellCommand.class, Mockito.CALLS_REAL_METHODS);
        MemberModifier.field(UniShowShellCommand.class, "provider").set(uniShowShellCommand, provider);
        MemberModifier.field(UniShowShellCommand.class, "ipAddress").set(uniShowShellCommand, ipAddress);
    }

    /**
     * Test method for {@link org.opendaylight.unimgr.cli.UniShowShellCommandTest#doExecute()}.
     * @throws Exception
     */
    @Test
    public void testDoExecute() throws Exception {
        // Test null uni
        final String error = "No uni found. Check the logs for more details.";
        Object answer = Whitebox.invokeMethod(uniShowShellCommand, "doExecute");
        assertEquals(error, answer);

        // Test uni
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format("Ip Address: <%s>\n", ipAddress));
        sb.append(String.format("Mac address: <%s>\n", macAddress));
        sb.append(String.format("Physical medium: <%s>\n", physicalMedium));
        sb.append(String.format("Speed: " + speed + "\n"));
        sb.append(String.format("Mode: <%s>\n", mode));
        sb.append(String.format("Mac layer: <%s>\n", macLayer));
        sb.append(String.format("Type: <%s>\n", type));
        sb.append(String.format("Mtu size: <%s>\n", mtuSize));
        final String success = sb.toString();

        final UniAugmentation uniAug = mock(UniAugmentation.class);
        final IpAddress ipAddr = mock(IpAddress.class);
        final Ipv4Address ip4 = mock(Ipv4Address.class);
        when(uniAug.getIpAddress()).thenReturn(ipAddr);
        when(ipAddr.getIpv4Address()).thenReturn(ip4);
        when(ip4.getValue()).thenReturn(ipAddress);
        final MacAddress mac = mock(MacAddress.class);
        when(uniAug.getMacAddress()).thenReturn(mac);
        when(mac.toString()).thenReturn(macAddress);
        when(uniAug.getPhysicalMedium()).thenReturn(physicalMedium);
        final Speed speedClass = mock(Speed.class);
        when(uniAug.getSpeed()).thenReturn(speedClass);
        when(speedClass.toString()).thenReturn(speed);
        when(uniAug.getMode()).thenReturn(mode);
        when(uniAug.getMacLayer()).thenReturn(macLayer);
        when(uniAug.getType()).thenReturn(type);
        when(uniAug.getMtuSize()).thenReturn(mtuSize);
        provider.addUni(uniAug);
        answer = Whitebox.invokeMethod(uniShowShellCommand, "doExecute");
        assertEquals(success, answer);
    }

}
