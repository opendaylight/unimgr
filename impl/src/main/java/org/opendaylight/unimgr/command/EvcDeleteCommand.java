/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.command;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.unimgr.impl.UnimgrUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Evc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Uni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSource;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class EvcDeleteCommand extends AbstractDeleteCommand {

    private static final Logger LOG = LoggerFactory.getLogger(EvcDeleteCommand.class);

    public EvcDeleteCommand(DataBroker dataBroker,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super.changes = changes;
        super.dataBroker = dataBroker;
    }

    @Override
    public void execute() {
        Map<InstanceIdentifier<Evc>, Evc> originalEvcs = UnimgrUtils.extractOriginal(changes, Evc.class);
        Set<InstanceIdentifier<Evc>> removedEvcs = UnimgrUtils.extractRemoved(changes, Evc.class);

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
                            List<UniSource> uniSourceLst = data.getUniSource();
                            for (UniSource uniSource : uniSourceLst) {
                                InstanceIdentifier<?> iidUni = uniSource.getUni();
                                OvsdbNodeRef ovsdbNdRef = getUniOvsdbNodeRef(iidUni);
                            }
                            //LOG.info("Removed EVC {}", data.getUniSource());
                            List<UniDest> uniDestLst = data.getUniDest();
                            for (UniDest uniDest : uniDestLst) {
                                InstanceIdentifier<?> iidUni = uniDest.getUni();
                                OvsdbNodeRef ovsdbNdRef = getUniOvsdbNodeRef(iidUni);
                            }
                            //LOG.info("Removed EVC {}", data.getUniDest());
                        }
                    }
                }
            }
        }
    }

    private OvsdbNodeRef getUniOvsdbNodeRef(InstanceIdentifier<?> iidUni) {
        Optional<Uni> uniOpt = UnimgrUtils.readUni(dataBroker, iidUni);
        if (uniOpt.isPresent()) {
            Uni uni = uniOpt.get();
            OvsdbNodeRef ovsdbNdRef = uni.getOvsdbNodeRef();
            return ovsdbNdRef;
        }
        return null;
    }
}
