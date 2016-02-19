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
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.unimgr.utils.OvsdbUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntriesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Queues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QueuesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class UniDeleteCommand extends AbstractDeleteCommand {

    private static final Logger LOG = LoggerFactory.getLogger(UniDeleteCommand.class);

    public UniDeleteCommand(DataBroker dataBroker,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super.changes = changes;
        super.dataBroker = dataBroker;
    }

    @Override
    public void execute() {
        Set<InstanceIdentifier<UniAugmentation>> removedUnis = OvsdbUtils.extractRemoved(changes,
                                                                                          UniAugmentation.class);
        if (!removedUnis.isEmpty()) {
            for (InstanceIdentifier<UniAugmentation> removedUniIid: removedUnis) {
                UniAugmentation uniAugmentation = MdsalUtils.read(dataBroker,
                                                                   LogicalDatastoreType.OPERATIONAL,
                                                                   removedUniIid);
                if (uniAugmentation != null) {
                    OvsdbNodeRef ovsNodedRef = uniAugmentation.getOvsdbNodeRef();
                    InstanceIdentifier<Node> ovsdbNodeIid = ovsNodedRef.getValue().firstIdentifierOf(Node.class);
                    Optional<Node> optionalNode = MdsalUtils.readNode(dataBroker,
                                                                       LogicalDatastoreType.OPERATIONAL,
                                                                       ovsdbNodeIid);
                    if (optionalNode.isPresent()) {
                        Node ovsdbNode = optionalNode.get();
                        LOG.info("Delete QoS and Queues entries");
                        List<QosEntries> qosList = ovsdbNode
                                .getAugmentation(OvsdbNodeAugmentation.class)
                                .getQosEntries();
                        QosEntriesKey qosEntryKey = null;
                        for (final QosEntries qosEntry : qosList) {
                            qosEntryKey = qosEntry.getKey();
                            InstanceIdentifier<QosEntries> qosIid = UnimgrMapper.getOvsdbQoSEntriesIid(ovsdbNode, qosEntryKey);
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

                        LOG.info("Delete bridge node");
                        InstanceIdentifier<Node> bridgeIid = UnimgrMapper.getOvsdbBridgeNodeIid(ovsdbNode);
                        Optional<Node> optBridgeNode = MdsalUtils.readNode(dataBroker, bridgeIid);
                        if (optBridgeNode.isPresent()) {
                            Node bridgeNode = optBridgeNode.get();
                            InstanceIdentifier<TerminationPoint> iidTermPoint = UnimgrMapper.getTerminationPointIid(bridgeNode,
                                                                                            UnimgrConstants.DEFAULT_BRIDGE_NAME);
                            MdsalUtils.deleteNode(dataBroker, iidTermPoint, LogicalDatastoreType.CONFIGURATION);
                        }
                        MdsalUtils.deleteNode(dataBroker, bridgeIid, LogicalDatastoreType.CONFIGURATION);
                    }
                    InstanceIdentifier<Node> iidUni = UnimgrMapper.getUniIid(dataBroker, uniAugmentation.getIpAddress(),
                                                                             LogicalDatastoreType.OPERATIONAL);
                    if (iidUni != null)
                        MdsalUtils.deleteNode(dataBroker, iidUni, LogicalDatastoreType.OPERATIONAL);
                }
                else {
                    LOG.info("Received Uni Augmentation is null", removedUniIid);
                }
            }
        }
    }
}
