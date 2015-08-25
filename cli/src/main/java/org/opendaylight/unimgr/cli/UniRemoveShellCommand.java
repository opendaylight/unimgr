/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.unimgr.api.IUnimgrConsoleProvider;

@Command(name = "remove", scope = "uni", description = "Removes an Uni from the controller.")
public class UniRemoveShellCommand extends OsgiCommandSupport {

    protected IUnimgrConsoleProvider provider;

    @Argument(index = 0, name = "id", description = "Uni Id", required = true, multiValued = false)
    String id;

    public UniRemoveShellCommand(IUnimgrConsoleProvider provider) {
        this.provider = provider;
    }

    @Override
    protected Object doExecute() throws Exception {
        if (provider.removeUni(id)) {
            return String.format("Uni successfully removed");
        } else {
            return String.format("Error removing Uni");
        }
    }
}
