/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.vcpe.command;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniUpdateCommand extends AbstractUpdateCommand {

    private static final Logger LOG = LoggerFactory.getLogger(UniUpdateCommand.class);

    public UniUpdateCommand(DataBroker dataBroker,
            Map<InstanceIdentifier<?>, DataObject> changes) {
        super.dataBroker = dataBroker;
        super.changes = changes;
    }

    @Override
    public void execute() {
        for (Entry<InstanceIdentifier<?>, DataObject> created : changes
                .entrySet()) {
            if (created.getValue() != null
                    && created.getValue() instanceof OvsdbNodeAugmentation) {
                OvsdbNodeAugmentation ovsdbNodeAugmentation = (OvsdbNodeAugmentation) created
                        .getValue();
                if (ovsdbNodeAugmentation != null) {
                    LOG.info("Received an OVSDB node create {}",
                            ovsdbNodeAugmentation.getConnectionInfo()
                                    .getRemoteIp().getIpv4Address().getValue());
                    final List<ManagedNodeEntry> managedNodeEntries = ovsdbNodeAugmentation.getManagedNodeEntry();
                    if (managedNodeEntries != null) {
                        for (ManagedNodeEntry managedNodeEntry : managedNodeEntries) {
                            LOG.info("Received an update from an OVSDB node {}.", managedNodeEntry.getKey());
                            // We received a node update from the southbound plugin
                            // so we have to check if it belongs to the UNI
                        }
                    }
                }
            }
        }
    }

}
