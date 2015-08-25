package org.opendaylight.vcpe.command;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class AbstractDeleteCommand implements Command {

    protected AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes;
    protected DataBroker dataBroker;

    @Override
    public abstract void execute();

}
