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
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.unimgr.impl.UnimgrUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
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
        Set<InstanceIdentifier<UniAugmentation>> removedUnis = UnimgrUtils.extractRemoved(changes,
                                                                                          UniAugmentation.class);
        if (!removedUnis.isEmpty()) {
            for (InstanceIdentifier<UniAugmentation> removedUniIid: removedUnis) {
                UniAugmentation uniAugmentation = UnimgrUtils.read(dataBroker,
                                                                   LogicalDatastoreType.OPERATIONAL,
                                                                   removedUniIid);
                if (uniAugmentation != null) {
                    OvsdbNodeRef ovsNodedRef = uniAugmentation.getOvsdbNodeRef();
                    InstanceIdentifier<Node> ovsdbNodeIid = ovsNodedRef.getValue().firstIdentifierOf(Node.class);
                    Optional<Node> optionalNode = UnimgrUtils.readNode(dataBroker,
                                                                       LogicalDatastoreType.OPERATIONAL,
                                                                       ovsdbNodeIid);
                    if (optionalNode.isPresent()) {
                        LOG.info("Delete bride node");
                        Node ovsdbNode = optionalNode.get();
                        InstanceIdentifier<Node> bridgeIid = UnimgrMapper.getOvsdbBridgeNodeIid(ovsdbNode);
                        Optional<Node> optBridgeNode = UnimgrUtils.readNode(dataBroker, bridgeIid);
                        if (optBridgeNode.isPresent()) {
                            Node bridgeNode = optBridgeNode.get();
                            InstanceIdentifier<TerminationPoint> iidTermPoint = UnimgrMapper.getTerminationPointIid(bridgeNode,
                                                                                            UnimgrConstants.DEFAULT_BRIDGE_NAME);
                            UnimgrUtils.deleteNode(dataBroker, iidTermPoint, LogicalDatastoreType.CONFIGURATION);
                        }
                        UnimgrUtils.deleteNode(dataBroker, bridgeIid, LogicalDatastoreType.CONFIGURATION);
                    }
                    InstanceIdentifier<Node> iidUni = UnimgrMapper.getUniIid(dataBroker, uniAugmentation.getIpAddress(),
                                                                             LogicalDatastoreType.OPERATIONAL);
                    if (iidUni != null)
                        UnimgrUtils.deleteNode(dataBroker, iidUni, LogicalDatastoreType.OPERATIONAL);
                }
                else {
                    LOG.info("Received Uni Augmentation is null", removedUniIid);
                }
            }
        }
    }
}
