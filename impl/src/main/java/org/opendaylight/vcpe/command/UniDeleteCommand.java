package org.opendaylight.vcpe.command;

import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.vcpe.impl.VcpeUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vcpe.rev150622.Uni;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniDeleteCommand extends AbstractDeleteCommand {

    private static final Logger LOG = LoggerFactory.getLogger(UniDeleteCommand.class);

    public UniDeleteCommand(DataBroker dataBroker,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super.changes = changes;
        super.dataBroker = dataBroker;
    }

    @Override
    public void execute() {
        Map<InstanceIdentifier<Uni>, Uni> originalUnis = VcpeUtils.extractOriginal(changes, Uni.class);
        Set<InstanceIdentifier<Uni>> removedUnis = VcpeUtils.extractRemoved(changes, Uni.class);
        if (!removedUnis.isEmpty()) {
            for (InstanceIdentifier<Uni> removedUniIid: removedUnis) {
                LOG.info("Received a request to remove an UNI ", removedUniIid);
            }
        }
    }

}
