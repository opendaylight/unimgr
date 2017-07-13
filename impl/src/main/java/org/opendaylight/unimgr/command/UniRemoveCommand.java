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
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntriesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Queues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QueuesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class UniRemoveCommand extends AbstractCommand<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(UniRemoveCommand.class);

    public UniRemoveCommand(final DataBroker dataBroker, final DataTreeModification<Node> removedUniNode) {
        super(dataBroker, removedUniNode);
    }

    @Override
    public void execute() {
        final Node uniNode = dataObject.getRootNode().getDataBefore();
        final UniAugmentation uniAugmentation = uniNode.getAugmentation(UniAugmentation.class);
        final InstanceIdentifier<?> removedUniIid = dataObject.getRootPath().getRootIdentifier()
                                                                    .firstIdentifierOf(UniAugmentation.class);
        if (uniAugmentation != null) {
            final OvsdbNodeRef ovsNodedRef = uniAugmentation.getOvsdbNodeRef();
            if (ovsNodedRef != null) {
                final InstanceIdentifier<Node> ovsdbNodeIid = ovsNodedRef.getValue().firstIdentifierOf(Node.class);
                final Optional<Node> optionalNode = MdsalUtils.readNode(dataBroker,
                        LogicalDatastoreType.OPERATIONAL,
                        ovsdbNodeIid);
                if (optionalNode.isPresent()) {
                    final Node ovsdbNode = optionalNode.get();
                    LOG.info("Delete QoS and Queues entries");
                    List<QosEntries> qosList = ovsdbNode
                            .getAugmentation(OvsdbNodeAugmentation.class)
                            .getQosEntries();
                    QosEntriesKey qosEntryKey = null;
                    for (final QosEntries qosEntry : qosList) {
                        qosEntryKey = qosEntry.getKey();
                        InstanceIdentifier<QosEntries> qosIid =
                                UnimgrMapper.getOvsdbQoSEntriesIid(ovsdbNode, qosEntryKey);
                        MdsalUtils.deleteNode(dataBroker, qosIid, LogicalDatastoreType.CONFIGURATION);
                    }
                    List<Queues> queuesList = ovsdbNode
                            .getAugmentation(OvsdbNodeAugmentation.class)
                            .getQueues();
                    QueuesKey queuesKey = null;
                    for (final Queues queue : queuesList) {
                        queuesKey = queue.getKey();
                        InstanceIdentifier<Queues> queuesIid = UnimgrMapper.getOvsdbQueuesIid(ovsdbNode, queuesKey);
                        MdsalUtils.deleteNode(dataBroker, queuesIid, LogicalDatastoreType.CONFIGURATION);
                    }
                    LOG.info("Delete bride node");
                    final InstanceIdentifier<Node> bridgeIid = UnimgrMapper.getOvsdbBridgeNodeIid(ovsdbNode);
                    MdsalUtils.deleteNode(dataBroker, bridgeIid, LogicalDatastoreType.CONFIGURATION);
                }
                final InstanceIdentifier<Node> iidUni = UnimgrMapper.getUniIid(dataBroker,
                        uniAugmentation.getIpAddress(),
                        LogicalDatastoreType.OPERATIONAL);
                if (iidUni != null) {
                    MdsalUtils.deleteNode(dataBroker, iidUni, LogicalDatastoreType.OPERATIONAL);
                }
            } else {
                LOG.info("Received Uni Augmentation has null ovsdb node ref: {}", removedUniIid);
            }
        } else {
            LOG.info("Received Uni Augmentation is null: {}", removedUniIid);
        }
    }
}
