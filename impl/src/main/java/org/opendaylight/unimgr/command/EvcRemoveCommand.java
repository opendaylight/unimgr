/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.command;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.AbstractCommand;
import org.opendaylight.unimgr.utils.EvcUtils;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSource;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class EvcRemoveCommand extends AbstractCommand<Link> {

    private static final Logger LOG = LoggerFactory.getLogger(EvcRemoveCommand.class);

    public EvcRemoveCommand(final DataBroker dataBroker, final DataTreeModification<Link> removedEvcLink) {
        super(dataBroker, removedEvcLink);
    }

    @Override
    public void execute() {
        final Link evcLink = dataObject.getRootNode().getDataBefore();
        final EvcAugmentation evcAugmentation = evcLink.getAugmentation(EvcAugmentation.class);
        final InstanceIdentifier<?> removedEvcIid = dataObject.getRootPath().getRootIdentifier();
        if (evcAugmentation != null) {
            final List<UniSource> unisSource = evcAugmentation.getUniSource();
            final List<UniDest> unisDest = evcAugmentation.getUniDest();
            if (unisSource != null && !unisSource.isEmpty()) {
                for (final UniSource source: unisSource) {
                    if (source != null) {
                        final Optional<Node> optionalSourceUniNode =
                                MdsalUtils.readNode(dataBroker,
                                                                LogicalDatastoreType.OPERATIONAL,
                                                                source.getUni());
                        EvcUtils.deleteEvcData(dataBroker, optionalSourceUniNode);
                    }
                }
            }
            if (unisDest != null && !unisDest.isEmpty()) {
                for (final UniDest dest : unisDest) {
                    if (dest != null) {
                        final Optional<Node> optionalDestUniNode =
                                MdsalUtils.readNode(dataBroker,
                                                                LogicalDatastoreType.OPERATIONAL,
                                                                dest.getUni());
                        EvcUtils.deleteEvcData(dataBroker, optionalDestUniNode);
                    }
                }
            }
        } else {
            LOG.info("EvcAugmentation is null");
        }
        MdsalUtils.deleteNode(dataBroker, removedEvcIid, LogicalDatastoreType.OPERATIONAL);
    }
}
