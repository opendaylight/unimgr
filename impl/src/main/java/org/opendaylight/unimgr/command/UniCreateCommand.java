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

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.unimgr.impl.UnimgrUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class UniCreateCommand extends AbstractCreateCommand {

    private static final Logger LOG = LoggerFactory.getLogger(UniCreateCommand.class);

    public UniCreateCommand(DataBroker dataBroker,
            Map<InstanceIdentifier<?>, DataObject> changes) {
        super.dataBroker = dataBroker;
        super.changes = changes;
    }

    @Override
    public void execute() {
        for (Entry<InstanceIdentifier<?>, DataObject> created : changes.entrySet()) {
            if (created.getValue() != null && created.getValue() instanceof UniAugmentation) {
                UniAugmentation uni = (UniAugmentation) created.getValue();
                InstanceIdentifier<?> uniKey = created.getKey();
                LOG.info("New UNI created {}.", uni.getIpAddress().getIpv4Address());
                /* We assume that when the user specifies the
                 * ovsdb-node-ref that the node already exists in
                 * the controller and that the OVS instance is in
                 * active mode.
                 *
                 * We assume that when the user doesn't specify the
                 * ovsdb-node-id that the node doesn't exist therefor
                 * has to be created with the IP address because it's
                 * in passive mode.
                 *
                 * Active mode (TCP): the UUID is in format ovsdb://UUID
                 * Passive mode (PTCP): the UUID is in format ovsdb://IP:6640
                 *
                 */
                OvsdbNodeRef ovsdbNodeRef = uni.getOvsdbNodeRef();
                if (ovsdbNodeRef != null || ovsdbNodeRef.getValue() != null) {
                    Optional<Node> optionalNode = UnimgrUtils.readNode(dataBroker, ovsdbNodeRef.getValue());
                    if (optionalNode.isPresent()) {
                        Node ovsdbNode = optionalNode.get();
                        UnimgrUtils.createBridgeNode(dataBroker,
                                                     ovsdbNode, uni,
                                                     UnimgrConstants.DEFAULT_BRIDGE_NAME);
                    } else {
                        LOG.info("Invalid OVSDB node instance identifier specified, "
                               + "attempting to retrieve the node.");
                        Node ovsdbNode = UnimgrUtils.findOvsdbNode(dataBroker, uni);
                        if (ovsdbNode != null) {
                            LOG.info("Retrieved the OVSDB node {}", ovsdbNode.getNodeId());
                            UnimgrUtils.updateUniNode(LogicalDatastoreType.CONFIGURATION,
                                                      uniKey,
                                                      uni,
                                                      ovsdbNode,
                                                      dataBroker);
                            UnimgrUtils.createBridgeNode(dataBroker,
                                    ovsdbNode, uni,
                                    UnimgrConstants.DEFAULT_BRIDGE_NAME);
                        } else {
                            ovsdbNode = UnimgrUtils.createOvsdbNode(dataBroker, uni);
                            LOG.info("Could not retrieve the OVSDB node,"
                                   + "created a new one: {}", ovsdbNode.getNodeId());
                            UnimgrUtils.updateUniNode(LogicalDatastoreType.CONFIGURATION,
                                          uniKey,
                                          uni,
                                          ovsdbNode,
                                          dataBroker);
                            UnimgrUtils.createBridgeNode(dataBroker,
                                                         ovsdbNode,
                                                         uni,
                                    UnimgrConstants.DEFAULT_BRIDGE_NAME);
                        }
                    }
                } else {
                    // We assume the ovs is in passive mode
                    // Check if the ovsdb node exist
                    Node ovsdbNode = UnimgrUtils.findOvsdbNode(dataBroker, uni);
                    if (ovsdbNode != null) {
                        LOG.info("Retrieved the OVSDB node");
                        UnimgrUtils.updateUniNode(LogicalDatastoreType.CONFIGURATION,
                                                  uniKey,
                                                  uni,
                                                  ovsdbNode,
                                                  dataBroker);
                        UnimgrUtils.createBridgeNode(dataBroker,
                                                     ovsdbNode,
                                                     uni,
                                                     UnimgrConstants.DEFAULT_BRIDGE_NAME);
                    } else {
                        ovsdbNode = UnimgrUtils.createOvsdbNode(dataBroker, uni);
                        LOG.info("Could not retrieve the OVSDB node,"
                               + "created a new one: {}", ovsdbNode.getNodeId());
                        UnimgrUtils.updateUniNode(LogicalDatastoreType.CONFIGURATION,
                                                  uniKey,
                                                  uni,
                                                  ovsdbNode,
                                                  dataBroker);
                        UnimgrUtils.createBridgeNode(dataBroker,
                                                     ovsdbNode,
                                                     uni,
                                                     UnimgrConstants.DEFAULT_BRIDGE_NAME);
                    }
                }
            }
            if (created.getValue() != null && created.getValue() instanceof OvsdbNodeAugmentation) {
                OvsdbNodeAugmentation ovsdbNodeAugmentation = (OvsdbNodeAugmentation) created
                        .getValue();
                InstanceIdentifier<Node> ovsdbIid = created.getKey().firstIdentifierOf(Node.class);
                if (ovsdbNodeAugmentation != null) {
                    LOG.info("Received an OVSDB node create {}",
                            ovsdbNodeAugmentation.getConnectionInfo()
                                    .getRemoteIp().getIpv4Address().getValue());
                    List<Node> uniNodes = UnimgrUtils.getUniNodes(dataBroker);
                    if (uniNodes != null && !uniNodes.isEmpty()) {
                        // This will not scale up very well when the UNI quantity gets to higher numbers.
                        for (Node node: uniNodes) {
                            UniAugmentation uniAugmentation = node.getAugmentation(UniAugmentation.class);
                            if (uniAugmentation.getOvsdbNodeRef() != null
                                    && uniAugmentation.getOvsdbNodeRef().getValue() != null) {
                                // The OVS instance is in tcp mode.
                                InstanceIdentifier<Node> ovsdbNodeRefIid =
                                        uniAugmentation.getOvsdbNodeRef().getValue().firstIdentifierOf(Node.class);
                                if (ovsdbNodeRefIid.equals(ovsdbIid)) {
                                    UnimgrUtils.createBridgeNode(dataBroker, node, uniAugmentation,
                                            UnimgrConstants.DEFAULT_BRIDGE_NAME);
                                    Optional<Node> optionalOvsdbNode = UnimgrUtils.readNode(dataBroker, ovsdbIid);
                                    if (optionalOvsdbNode.isPresent()) {
                                        Node ovsdbNode = optionalOvsdbNode.get();
                                        InstanceIdentifier<Node> uniIid = UnimgrMapper.createUniIid(dataBroker,
                                                uniAugmentation.getIpAddress());
                                        UnimgrUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL, uniIid,
                                                uniAugmentation, ovsdbNode, dataBroker);
                                    }
                                }
                                // The OVS instance is in ptcp mode.
                            } else if (ovsdbNodeAugmentation.getConnectionInfo().getRemoteIp()
                                    .equals(uniAugmentation.getIpAddress())) {
                                InstanceIdentifier<Node> ovsdbNodeIid = uniAugmentation.getOvsdbNodeRef().getValue()
                                        .firstIdentifierOf(Node.class);
                                Optional<Node> ovsdbNode = UnimgrUtils.readNode(dataBroker, ovsdbNodeIid);
                                if (ovsdbNode.isPresent()) {
                                    InstanceIdentifier<Node> uniIid = UnimgrMapper.createUniIid(dataBroker,
                                            uniAugmentation.getIpAddress());
                                    UnimgrUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL, uniIid, uniAugmentation,
                                            ovsdbNode.get(), dataBroker);
                                } else {
                                    LOG.error("Unable to read node with IID {}", ovsdbNodeIid);
                                }
                            }
                        }
                    } else {
                        LOG.info("Received a new OVSDB node connection from {}"
                                + ovsdbNodeAugmentation.getConnectionInfo()
                                        .getRemoteIp().getIpv4Address());
                    }
                }
            }
        }
    }

}
