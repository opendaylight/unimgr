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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.reflect.Whitebox;

public class UniListShellCommandTest {

    private static final String ipAddress = "192.168.1.1";

    private final UnimgrConsoleProviderTest provider = new UnimgrConsoleProviderTest();

    @Mock UniListShellCommand uniListShellCommand;

    @Before
    public void setUp() throws IllegalArgumentException, IllegalAccessException{
        uniListShellCommand = mock(UniListShellCommand.class, Mockito.CALLS_REAL_METHODS);
        MemberModifier.field(UniListShellCommand.class, "provider").set(uniListShellCommand, provider);
        MemberModifier.field(UniListShellCommand.class, "isConfigurationData").set(uniListShellCommand, false);
    }

    /**
     * Test method for {@link org.opendaylight.unimgr.cli.UniListShellCommand#doExecute()}.
     * @throws Exception
     */
    @Test
    public void testDoExecute() throws Exception {
        // Test empty list
        final String error = "No uni found. Check the logs for more details.";
        Object answer = Whitebox.invokeMethod(uniListShellCommand, "doExecute");
        assertEquals(error, answer);

        // Test list with 3 uni
        final StringBuilder sb = new StringBuilder();
        Integer counter = 1;
        final int amount = 3;
        for (int i=0; i<amount; i++) {
            sb.append(String.format("#%d - IpAddress: %s\n", counter, ipAddress));
            counter++;
        }
        final String success = sb.toString();
        provider.setListUnis(3, ipAddress);
        answer = Whitebox.invokeMethod(uniListShellCommand, "doExecute");
        assertEquals(success, answer);
        //fail("Not yet implemented");
    }

}
