package org.opendaylight.vcpe.command;

import java.util.List;

public class TransactionInvoker {

    private Command command;
    private List<Command> commands;

    public void setCommand(Command command) {
        this.command = command;
    }

    public void setCommands(List<Command> commands) {
        this.commands = commands;
    }

    public void invoke() {
        if (command != null) {
            command.execute();
        }
        if (!commands.isEmpty()) {
            for (Command invokableCommand: commands) {
                invokableCommand.execute();
            }
        }
    }

}
