/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
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
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSource;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
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
        Set<InstanceIdentifier<EvcAugmentation>> removedEvcs = UnimgrUtils.extractRemoved(changes,
                                                                                         EvcAugmentation.class);
        if (!removedEvcs.isEmpty()) {
            for (InstanceIdentifier<EvcAugmentation> removedEvcIid: removedEvcs) {
                EvcAugmentation evcAugmentation = UnimgrUtils.read(dataBroker,
                                                                   LogicalDatastoreType.OPERATIONAL,
                                                                   removedEvcIid);
                if (evcAugmentation != null) {
                    List<UniSource> unisSource = evcAugmentation.getUniSource();
                    List<UniDest> unisDest = evcAugmentation.getUniDest();
                    if (unisSource != null && !unisSource.isEmpty()) {
                        for (UniSource source: unisSource) {
                            if (unisSource != null) {
                                Optional<Node> optionalSourceUniNode =
                                                   UnimgrUtils.readNode(dataBroker,
                                                                        LogicalDatastoreType.OPERATIONAL,
                                                                        source.getUni());
                                deleteEvcData(optionalSourceUniNode);
                            }
                        }
                    }
                    if (unisDest != null && !unisDest.isEmpty()) {
                        for (UniDest dest : unisDest) {
                            if (dest != null) {
                                Optional<Node> optionalDestUniNode =
                                                   UnimgrUtils.readNode(dataBroker,
                                                                        LogicalDatastoreType.OPERATIONAL,
                                                                        dest.getUni());
                                deleteEvcData(optionalDestUniNode);
                            }
                        }
                    }
                }
                UnimgrUtils.deleteNode(dataBroker,
                        removedEvcIid,
                        LogicalDatastoreType.OPERATIONAL);
            }
        }
    }

    private void deleteEvcData(Optional<Node> optionalUni) {
        if (optionalUni.isPresent()) {
            UniAugmentation uniAugmentation =
                                optionalUni
                                    .get()
                                    .getAugmentation(UniAugmentation.class);
            InstanceIdentifier<Node> ovsdbNodeIid =
                                              uniAugmentation
                                             .getOvsdbNodeRef()
                                             .getValue()
                                             .firstIdentifierOf(Node.class);
            Optional<Node> optionalOvsdNode =
                    UnimgrUtils.readNode(dataBroker,
                                         LogicalDatastoreType.OPERATIONAL,
                                         ovsdbNodeIid);
            if (optionalOvsdNode.isPresent()) {
                Node ovsdbNode = optionalOvsdNode.get();
                        OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode
                                .getAugmentation(OvsdbNodeAugmentation.class);
                for (ManagedNodeEntry managedNodeEntry: ovsdbNodeAugmentation.getManagedNodeEntry()) {
                    InstanceIdentifier<Node> bridgeIid = managedNodeEntry
                                                             .getBridgeRef()
                                                             .getValue()
                                                             .firstIdentifierOf(Node.class);
                    UnimgrUtils.deleteNode(dataBroker, bridgeIid, LogicalDatastoreType.CONFIGURATION);
                    UnimgrUtils.createBridgeNode(dataBroker,
                                                 ovsdbNodeIid,
                                                 uniAugmentation,
                                                 UnimgrConstants.DEFAULT_BRIDGE_NAME);
                }
            }
        } else {
            LOG.info("Unable to retrieve UNI from the EVC.");
        }
    }
}
