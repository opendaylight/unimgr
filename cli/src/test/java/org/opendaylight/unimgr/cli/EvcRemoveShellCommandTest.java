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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.reflect.Whitebox;

public class EvcRemoveShellCommandTest {

    private final UnimgrConsoleProviderTest provider = new UnimgrConsoleProviderTest();

    @Mock EvcRemoveShellCommand evcRemoveShellCommand;

    @Before
    public void setUp() throws IllegalArgumentException, IllegalAccessException{
        evcRemoveShellCommand = mock(EvcRemoveShellCommand.class, Mockito.CALLS_REAL_METHODS);
        MemberModifier.field(EvcRemoveShellCommand.class, "provider").set(evcRemoveShellCommand, provider);
    }
    /**
     * Test method for {@link org.opendaylight.unimgr.cli.EvcRemoveShellCommand#doExecute()}.
     * @throws Exception
     */
    @Test
    public void testDoExecute() throws Exception {
        final String success = "Evc successfully removed";
        final String error = "Error removing Evc";
        Object answer = Whitebox.invokeMethod(evcRemoveShellCommand, "doExecute");
        assertEquals(success, answer);
        answer = Whitebox.invokeMethod(evcRemoveShellCommand, "doExecute");
        assertEquals(error, answer);
    }

}
