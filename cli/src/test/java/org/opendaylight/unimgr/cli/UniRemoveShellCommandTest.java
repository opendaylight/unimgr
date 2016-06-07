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

public class UniRemoveShellCommandTest {
    private final String ipAddress = "192.168.1.1";
    private final UnimgrConsoleProviderTest provider = new UnimgrConsoleProviderTest();

    @Mock UniRemoveShellCommand uniRemoveShellCommand;

    @Before
    public void setUp() throws IllegalArgumentException, IllegalAccessException{
        uniRemoveShellCommand = mock(UniRemoveShellCommand.class, Mockito.CALLS_REAL_METHODS);
        MemberModifier.field(UniRemoveShellCommand.class, "provider").set(uniRemoveShellCommand, provider);
        MemberModifier.field(UniRemoveShellCommand.class, "ipAddress").set(uniRemoveShellCommand, ipAddress);
    }

    /**
     * Test method for {@link org.opendaylight.unimgr.cli.UniRemoveShellCommand#doExecute()}.
     * @throws Exception
     */
    @Test
    public void testDoExecute() throws Exception {
        final String success = "Uni successfully removed";
        final String error = "Error removing Uni";
        Object answer = Whitebox.invokeMethod(uniRemoveShellCommand, "doExecute");
        assertEquals(success, answer);
        answer = Whitebox.invokeMethod(uniRemoveShellCommand, "doExecute");
        assertEquals(error, answer);
    }

}
