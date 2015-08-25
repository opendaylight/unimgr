package org.opendaylight.vcpe.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.vcpe.api.IVcpeConsoleProvider;

@Command(name = "remove", scope = "Evc", description = "Removes Evc.")
public class EvcRemoveShellCommand extends OsgiCommandSupport {

    protected IVcpeConsoleProvider provider;

    @Argument(index = 0, name = "id", description = "Evc Id", required = true, multiValued = false)
    String id;

    public EvcRemoveShellCommand(IVcpeConsoleProvider provider) {
        this.provider = provider;
    }

    @Override
    protected Object doExecute() throws Exception {
        if (provider.removeEvc(id)) {
            return String.format("Evc successfully removed");
        } else {
            return String.format("Error removing Evc");
        }
    }

}
