/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.command;

import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.unimgr.impl.UnimgrUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Evc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class EvcDeleteCommand extends AbstractDeleteCommand {

    private static final Logger LOG = LoggerFactory.getLogger(EvcDeleteCommand.class);

    public EvcDeleteCommand(DataBroker dataBroker,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super.changes = changes;
        super.dataBroker = dataBroker;
    }

    @Override
    public void execute() {
        Map<InstanceIdentifier<?>, DataObject> originalData = changes.getOriginalData();
        Set<InstanceIdentifier<?>> removedIids = changes.getRemovedPaths();
        for (InstanceIdentifier<?> evcIid : removedIids) {
            DataObject removedDataObject = originalData.get(evcIid);
            if (removedDataObject instanceof Evc) {
                Evc evc = (Evc)removedDataObject;
                Optional<Node> optionalSourceUniNode = UnimgrUtils.readNode(dataBroker,
                                                                             LogicalDatastoreType.OPERATIONAL,
                                                                             evc.getUniSource()
                                                                                .iterator()
                                                                                .next()
                                                                                .getUni());
                Optional<Node> optionalDestinationUniNode = UnimgrUtils.readNode(dataBroker,
                                                                                   LogicalDatastoreType.OPERATIONAL,
                                                                                   evc.getUniDest()
                                                                                      .iterator()
                                                                                      .next()
                                                                                      .getUni());
                if (optionalSourceUniNode.isPresent()
                        && optionalDestinationUniNode.isPresent()) {
                    Node sourceUniNode = optionalSourceUniNode.get();
                    Node destUniNode = optionalDestinationUniNode.get();
                    UniAugmentation sourceUniAugmentation = sourceUniNode
                                                                .getAugmentation(UniAugmentation.class);
                    UniAugmentation destUniAugmentation = destUniNode
                                                              .getAugmentation(UniAugmentation.class);
                    InstanceIdentifier<Node> sourceOvsdbIid =
                                                 sourceUniAugmentation
                                                     .getOvsdbNodeRef()
                                                     .getValue()
                                                     .firstIdentifierOf(Node.class);
                    InstanceIdentifier<Node> destOvsdbIid =
                                                 destUniAugmentation
                                                     .getOvsdbNodeRef()
                                                     .getValue()
                                                     .firstIdentifierOf(Node.class);
                    Optional<Node> optionalSourceOvsdNode =
                                       UnimgrUtils.readNode(dataBroker,
                                                            LogicalDatastoreType.OPERATIONAL,
                                                            sourceOvsdbIid);
                    Optional<Node> optionalDestinationOvsdbNode =
                                       UnimgrUtils.readNode(dataBroker,
                                                            LogicalDatastoreType.OPERATIONAL,
                                                            destOvsdbIid);
                    if (optionalSourceOvsdNode.isPresent()
                            && optionalDestinationOvsdbNode.isPresent()) {
                        Node sourceOvsdbNode = optionalSourceOvsdNode.get();
                        Node destinationOvsdbNode = optionalDestinationOvsdbNode.get();
                        OvsdbNodeAugmentation sourceOvsdbNodeAugmentation =
                                                  sourceOvsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
                        OvsdbNodeAugmentation destinationOvsdbNodeAugmentation =
                                                  destinationOvsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
                        InstanceIdentifier<Node> sourceBridgeIid =
                                                     sourceOvsdbNodeAugmentation
                                                         .getManagedNodeEntry()
                                                         .iterator()
                                                         .next()
                                                         .getBridgeRef()
                                                         .getValue()
                                                         .firstIdentifierOf(Node.class);
                        InstanceIdentifier<Node> destinationBridgeIid =
                                                     destinationOvsdbNodeAugmentation
                                                         .getManagedNodeEntry()
                                                         .iterator()
                                                         .next()
                                                         .getBridgeRef()
                                                         .getValue()
                                                         .firstIdentifierOf(Node.class);
                        Optional<Node> optionalSourceBridgeNode = UnimgrUtils.readNode(dataBroker,
                                                                                       LogicalDatastoreType.OPERATIONAL,
                                                                                       sourceBridgeIid);
                        Optional<Node>  optionalDestinationBridgeNode = UnimgrUtils.readNode(dataBroker,
                                                                                             LogicalDatastoreType.OPERATIONAL,
                                                                                             destinationBridgeIid);
                        if (optionalSourceBridgeNode.isPresent()
                                && optionalDestinationBridgeNode.isPresent()) {
                            Node sourceBridgeNode = optionalSourceBridgeNode.get();
                            Node destinationBridgeNode = optionalDestinationBridgeNode.get();
                            TpId sourceTp = sourceBridgeNode.getTerminationPoint().get(2).getTpId();
                            TpId destTp = destinationBridgeNode.getTerminationPoint().get(2).getTpId();
                            InstanceIdentifier<?> sourceTpIid = UnimgrMapper.getTerminationPointIid(sourceBridgeNode,
                                                                                                    sourceTp);
                            InstanceIdentifier<?> destinationTpIid = UnimgrMapper.getTerminationPointIid(destinationBridgeNode,
                                                                                                         destTp);
                            CheckedFuture<Void, TransactionCommitFailedException> deleteOperNodeResult =
                                                                                      UnimgrUtils.deleteNode(dataBroker,
                                                                                                             sourceBridgeIid,
                                                                                                             LogicalDatastoreType.CONFIGURATION);
                            CheckedFuture<Void, TransactionCommitFailedException> deleteConfigNodeResult =
                                                                                      UnimgrUtils.deleteNode(dataBroker,
                                                                                                             destinationBridgeIid, 
                                                                                                             LogicalDatastoreType.CONFIGURATION);
                            if (deleteOperNodeResult.isDone() && deleteConfigNodeResult.isDone()) {
                                UnimgrUtils.createBridgeNode(dataBroker,
                                                             sourceOvsdbIid,
                                                             sourceUniNode.getAugmentation(UniAugmentation.class),
                                                             UnimgrConstants.DEFAULT_BRIDGE_NAME);
                                UnimgrUtils.createBridgeNode(dataBroker,
                                                             destOvsdbIid,
                                                             destUniNode.getAugmentation(UniAugmentation.class),
                                                             UnimgrConstants.DEFAULT_BRIDGE_NAME);
                            }
                        } else {
                            LOG.info("Unable to retrieve the Ovsdb Bridge node source and/or destination.");
                        }
                    } else {
                        LOG.info("Unable to retrieve the Ovsdb node source and/or destination.");
                    }
                } else {
                    LOG.info("Unable to retrieve the Uni source and/or destination.");
                }
            }
        }
    }
}
