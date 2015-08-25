package org.opendaylight.vcpe.command;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.vcpe.impl.VcpeUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vcpe.rev150622.Evc;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvcDeleteCommand extends AbstractDeleteCommand {

    private static final Logger LOG = LoggerFactory.getLogger(EvcDeleteCommand.class);

    public EvcDeleteCommand(DataBroker dataBroker,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super.changes = changes;
        super.dataBroker = dataBroker;
    }

    @Override
    public void execute() {
        Map<InstanceIdentifier<Evc>, Evc> originalEvcs = VcpeUtils.extractOriginal(changes, Evc.class);
        Set<InstanceIdentifier<Evc>> removedEvcs = VcpeUtils.extractRemoved(changes, Evc.class);

        Set<InstanceIdentifier<?>> removedPaths = changes.getRemovedPaths();
        if (!removedPaths.isEmpty()) {
            for (InstanceIdentifier<?> removedPath: removedPaths) {
                Class<?> type = removedPath.getTargetType();
                LOG.info("Removed paths instance identifier {}", type);
                if (type.equals(Evc.class)) {
                    LOG.info("Removed paths instance identifier {}", type);
                    for (Entry<InstanceIdentifier<Evc>, Evc> evc: originalEvcs.entrySet()) {
                        if (evc.getKey().equals(type)) {
                            Evc data = evc.getValue();
                            LOG.info("Removed EVC {}", data.getId());
                        }
                    }
                }
            }
        }
//        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
//        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, OVSDBTerminationPointIID);
    }
}
