package org.opendaylight.vcpe.command;

import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class AbstractUpdateCommand implements Command {

    protected Map<InstanceIdentifier<?>, DataObject> changes;
    protected DataBroker dataBroker;

    @Override
    public abstract void execute();

}
