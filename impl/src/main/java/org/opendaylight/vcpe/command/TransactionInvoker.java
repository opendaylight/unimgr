/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
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
