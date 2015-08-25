/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.cli;

import java.util.List;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.unimgr.api.IUnimgrConsoleProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev150622.unis.Uni;

@Command(name = "list", scope = "uni", description = "Lists all uni in the controller.")
public class UniListShellCommand extends OsgiCommandSupport {

    protected IUnimgrConsoleProvider provider;

    @Option(name = "-c",
            aliases = { "--config" },
            description = "List Configuration Data (optional).\n-c / --config <ENTER>",
            required = false,
            multiValued = false)
    Boolean isConfigurationData = false;

    public UniListShellCommand(IUnimgrConsoleProvider provider) {
        this.provider = provider;
    }

    @Override
    protected Object doExecute() throws Exception {

        List<Uni> listUnis = provider.listUnis(isConfigurationData);

        if (listUnis.size() > 0) {
            StringBuilder sb = new StringBuilder();
            Integer counter = 1;
            for (Uni uni : listUnis) {
                sb.append(String.format("#%d - id: %s\n", counter, uni.getId()));
                counter++;
            }
            return sb.toString();
        } else {
            return String.format("No uni found. Check the logs for more details.");
        }
    }
}
