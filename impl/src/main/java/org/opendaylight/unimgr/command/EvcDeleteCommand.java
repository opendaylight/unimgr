/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.command;

import java.util.List;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.utils.EvcUtils;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.unimgr.utils.OvsdbUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSource;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

public class EvcDeleteCommand extends AbstractDeleteCommand {

    public EvcDeleteCommand(DataBroker dataBroker,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super.changes = changes;
        super.dataBroker = dataBroker;
    }

    @Override
    public void execute() {
        Set<InstanceIdentifier<EvcAugmentation>> removedEvcs = OvsdbUtils.extractRemoved(changes,
                                                                                         EvcAugmentation.class);
        if (!removedEvcs.isEmpty()) {
            for (InstanceIdentifier<EvcAugmentation> removedEvcIid: removedEvcs) {
                EvcAugmentation evcAugmentation = MdsalUtils.read(dataBroker,
                                                                   LogicalDatastoreType.OPERATIONAL,
                                                                   removedEvcIid);
                if (evcAugmentation != null) {
                    List<UniSource> unisSource = evcAugmentation.getUniSource();
                    List<UniDest> unisDest = evcAugmentation.getUniDest();
                    if (unisSource != null && !unisSource.isEmpty()) {
                        for (UniSource source: unisSource) {
                            if (source != null) {
                                Optional<Node> optionalSourceUniNode =
                                        MdsalUtils.readNode(dataBroker,
                                                                        LogicalDatastoreType.OPERATIONAL,
                                                                        source.getUni());
                                EvcUtils.deleteEvcData(dataBroker, optionalSourceUniNode);
                            }
                        }
                    }
                    if (unisDest != null && !unisDest.isEmpty()) {
                        for (UniDest dest : unisDest) {
                            if (dest != null) {
                                Optional<Node> optionalDestUniNode =
                                        MdsalUtils.readNode(dataBroker,
                                                                        LogicalDatastoreType.OPERATIONAL,
                                                                        dest.getUni());
                                EvcUtils.deleteEvcData(dataBroker, optionalDestUniNode);
                            }
                        }
                    }
                }
                MdsalUtils.deleteNode(dataBroker, removedEvcIid, LogicalDatastoreType.OPERATIONAL);
            }
        }
    }
}
