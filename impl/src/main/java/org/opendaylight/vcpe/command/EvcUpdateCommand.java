package org.opendaylight.vcpe.command;

import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvcUpdateCommand extends AbstractUpdateCommand {

    private static final Logger LOG = LoggerFactory.getLogger(EvcUpdateCommand.class);

    public EvcUpdateCommand(DataBroker dataBroker,
            Map<InstanceIdentifier<?>, DataObject> changes) {
        super.dataBroker = dataBroker;
        super.changes = changes;
    }

    @Override
    public void execute() {
        LOG.info("Update has not been implemented yet.");
    }

}
