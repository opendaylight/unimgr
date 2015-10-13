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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Uni;

@Command(name = "show", scope = "uni", description = "Shows detailed information about an uni.")
public class UniShowShellCommand extends OsgiCommandSupport {

    protected IUnimgrConsoleProvider provider;

    @Argument(index = 0, name = "id", description = "Uni Id", required = true, multiValued = false)
    String id;

    public UniShowShellCommand(IUnimgrConsoleProvider provider) {
        this.provider = provider;
    }

    @Override
    protected Object doExecute() throws Exception {
        StringBuilder sb = new StringBuilder();
        Uni uni = provider.getUni(id);

        if (uni != null) {
            //sb.append(String.format("Uni Id: <%s>\n", uni.getUniId()));
            sb.append(String.format("Physical medium: <%s>\n", uni.getPhysicalMedium()));
            sb.append(String.format("Mac address: <%s>\n", uni.getMacAddress()));
            sb.append(String.format("Speed: " + uni.getSpeed() + "\n"));
            sb.append(String.format("Mode: <%s>\n", uni.getMode()));
            sb.append(String.format("Mac layer: <%s>\n", uni.getMacLayer()));
            sb.append(String.format("Type: <%s>\n", uni.getType()));
            sb.append(String.format("Mtu size: <%s>\n", uni.getMtuSize()));
            return sb.toString();
        } else {
            return String.format("No uni found. Check the logs for more details.");
        }
    }
}
