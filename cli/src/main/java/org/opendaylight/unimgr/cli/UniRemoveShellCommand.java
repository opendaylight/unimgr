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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;

@Command(name = "uni-remove", scope = "uni", description = "Removes an Uni from the controller.")
public class UniRemoveShellCommand extends OsgiCommandSupport {

    protected IUnimgrConsoleProvider provider;

    @Argument(index = 0, name = "ip", description = "Uni ipAddress", required = true, multiValued = false)
    String ipAddress;

    public UniRemoveShellCommand(IUnimgrConsoleProvider provider) {
        this.provider = provider;
    }

    @Override
    protected Object doExecute() throws Exception {
        IpAddress ipAddre = new IpAddress(ipAddress.toCharArray());
        if (provider.removeUni(ipAddre)) {
            return String.format("Uni successfully removed");
        } else {
            return String.format("Error removing Uni");
        }
    }
}
