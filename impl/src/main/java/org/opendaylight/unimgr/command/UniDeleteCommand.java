/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.command;

import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.unimgr.impl.UnimgrUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class UniDeleteCommand extends AbstractDeleteCommand {

    private static final Logger LOG = LoggerFactory.getLogger(UniDeleteCommand.class);

    public UniDeleteCommand(DataBroker dataBroker,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super.changes = changes;
        super.dataBroker = dataBroker;
    }

    @Override
    public void execute() {
        Set<InstanceIdentifier<UniAugmentation>> removedUnis = UnimgrUtils.extractRemoved(changes, UniAugmentation.class);
        Set<InstanceIdentifier<Node>> removedNodes = UnimgrUtils.extractRemoved(changes, Node.class);
        if (!removedUnis.isEmpty()) {
            for (InstanceIdentifier<UniAugmentation> removedUniIid: removedUnis) {
                UniAugmentation uniAug = UnimgrUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION, removedUniIid);
                if(uniAug != null){
                    LOG.trace("Uni Augmentation present.");
                    OvsdbNodeRef ovsdbNodeRef = uniAug.getOvsdbNodeRef();
                    InstanceIdentifier<Node> ovsdbIid = ovsdbNodeRef.getValue().firstIdentifierOf(Node.class);
                    Optional<Node> optNode = UnimgrUtils.readNode(dataBroker, LogicalDatastoreType.CONFIGURATION, ovsdbIid);
                    if (optNode.isPresent()) {
                        Node ovsdbNode = optNode.get();
                        InstanceIdentifier<Node> iidBridgeNode = UnimgrMapper.getOvsdbBridgeNodeIid(ovsdbNode);
                        deleteNode(iidBridgeNode);
                        LOG.info("Received a request to remove a UNI BridgeNode ", iidBridgeNode);
                    }
                    deleteNode(ovsdbIid);
                    LOG.info("Received a request to remove an UNI ", removedUniIid);
                }
            }
        }
        else {LOG.info("Removed UNIs is empty.");}

        if(!removedNodes.isEmpty()) {
            for(InstanceIdentifier<Node> iidNode : removedNodes) {
                Optional<Node> optNode = UnimgrUtils.readNode(dataBroker, LogicalDatastoreType.CONFIGURATION, iidNode);
                if (optNode.isPresent()) {
                    Node ovsdbNode = optNode.get();
                    InstanceIdentifier<Node> iidBridgeNode = UnimgrMapper.getOvsdbBridgeNodeIid(ovsdbNode);
                    deleteNode(iidBridgeNode);
                    LOG.info("Received a request to remove a BridgeNode ", iidBridgeNode);
                }
                deleteNode(iidNode);
                LOG.info("Received a request to remove a Node ", iidNode);
           }
        }
        else {LOG.info("Removed Nodes is empty.");}
    }

    private void deleteNode(InstanceIdentifier<Node> iidNode) {
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.delete(LogicalDatastoreType.OPERATIONAL, iidNode);
        transaction.delete(LogicalDatastoreType.CONFIGURATION, iidNode);
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        try {
            future.checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.warn("Failed to delete iidNode {} {}", e.getMessage(), iidNode);
        }
    }
}
