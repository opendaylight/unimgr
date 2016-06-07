/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.command;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.AbstractCommand;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.unimgr.utils.OvsdbUtils;
import org.opendaylight.unimgr.utils.UniUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class UniAddCommand extends AbstractCommand<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(UniAddCommand.class);

    public UniAddCommand(final DataBroker dataBroker, final DataTreeModification<Node> newUniNode) {
        super(dataBroker, newUniNode);
    }

    @Override
    public void execute() {
        final InstanceIdentifier<?> uniKey = dataObject.getRootPath().getRootIdentifier();
        final Optional<Node> optNode = MdsalUtils.readNode(dataBroker, LogicalDatastoreType.OPERATIONAL, uniKey);
        if (!optNode.isPresent()) {
            final Node uniNode = dataObject.getRootNode().getDataAfter();
            final UniAugmentation uni = uniNode.getAugmentation(UniAugmentation.class);
            if (uni != null) {
                LOG.info("New UNI created {}.", uni.getIpAddress().getIpv4Address());
                // We assume the ovs is in active mode tcp:ipAddress:6640
                if (uni.getOvsdbNodeRef() != null) {
                    final OvsdbNodeRef ovsdbNodeRef = uni.getOvsdbNodeRef();
                    final Optional<Node> optionalNode = MdsalUtils.readNode(dataBroker,
                                                                       LogicalDatastoreType.OPERATIONAL,
                                                                       ovsdbNodeRef.getValue());
                    if (!optionalNode.isPresent()) {
                        LOG.info("Invalid OVSDB node instance identifier specified, "
                               + "attempting to retrieve the node.");
                        final Optional<Node> optionalOvsdbNode = OvsdbUtils.findOvsdbNode(dataBroker,
                                                                                     uni);
                        Node ovsdbNode;
                        if (optionalOvsdbNode.isPresent()) {
                            ovsdbNode = optionalOvsdbNode.get();
                            LOG.info("Retrieved the OVSDB node {}", ovsdbNode.getNodeId());
                            // Update QoS entries to ovsdb if speed is configured to UNI node
                            if (uni.getSpeed() != null) {
                                OvsdbUtils.createQoSForOvsdbNode(dataBroker, uni);
                            }
                            UniUtils.updateUniNode(LogicalDatastoreType.CONFIGURATION,
                                                      uniKey,
                                                      uni,
                                                      ovsdbNode,
                                                      dataBroker);
                        } else {
                            ovsdbNode = OvsdbUtils.createOvsdbNode(dataBroker,
                                                                    uni);
                            LOG.info("Could not retrieve the OVSDB node,"
                                   + " created a new one: {}", ovsdbNode.getNodeId());
                            UniUtils.updateUniNode(LogicalDatastoreType.CONFIGURATION,
                                                      uniKey,
                                                      uni,
                                                      ovsdbNode,
                                                      dataBroker);
                        }
                    }
                } else {
                    // We assume the ovs is in passive mode
                    // Check if the ovsdb node exist
                    final Optional<Node> optionalOvsdbNode = OvsdbUtils.findOvsdbNode(dataBroker,
                                                                                 uni);
                    Node ovsdbNode;
                    if (optionalOvsdbNode.isPresent()) {
                        ovsdbNode = optionalOvsdbNode.get();
                        final InstanceIdentifier<Node> ovsdbIid = UnimgrMapper.getOvsdbNodeIid(ovsdbNode.getNodeId());
                        LOG.info("Retrieved the OVSDB node");
                        // Update QoS entries to ovsdb if speed is configured to UNI node
                        if (uni.getSpeed() != null) {
                            OvsdbUtils.createQoSForOvsdbNode(dataBroker, uni);
                        }
                        OvsdbUtils.createBridgeNode(dataBroker,
                                                     ovsdbIid,
                                                     uni,
                                                     UnimgrConstants.DEFAULT_BRIDGE_NAME);
                        UniUtils.updateUniNode(LogicalDatastoreType.CONFIGURATION,
                                                  uniKey,
                                                  uni,
                                                  ovsdbNode,
                                                  dataBroker);
                        UniUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL, uniKey, uni, ovsdbNode, dataBroker);
                    } else {
                        ovsdbNode = OvsdbUtils.createOvsdbNode(dataBroker,
                                                                uni);
                        if (ovsdbNode != null) {
                            LOG.info("Could not retrieve the OVSDB node,"
                                    + "created a new one: {}", ovsdbNode.getNodeId());
                            UniUtils.updateUniNode(LogicalDatastoreType.CONFIGURATION,
                                                       uniKey,
                                                       uni,
                                                       ovsdbNode,
                                                       dataBroker);
                        }
                    }
                }
            }
        }
    }
}
