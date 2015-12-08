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
                LOG.trace("New UNI created {}.", uni.getIpAddress().getIpv4Address());
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
                if (uni.getOvsdbNodeRef() != null) {
                    OvsdbNodeRef ovsdbNodeRef = uni.getOvsdbNodeRef();
                    Optional<Node> optionalNode = UnimgrUtils.readNode(dataBroker,
                                                                       LogicalDatastoreType.OPERATIONAL,
                                                                       ovsdbNodeRef.getValue());
                    if (!optionalNode.isPresent()) {
                        LOG.info("Invalid OVSDB node instance identifier specified, "
                               + "attempting to retrieve the node.");
                        Optional<Node> optionalOvsdbNode = UnimgrUtils.findOvsdbNode(dataBroker,
                                                                                     uni);
                        Node ovsdbNode;
                        if (optionalOvsdbNode.isPresent()) {
                            ovsdbNode = optionalOvsdbNode.get();
                            LOG.info("Retrieved the OVSDB node {}", ovsdbNode.getNodeId());
                            UnimgrUtils.updateUniNode(LogicalDatastoreType.CONFIGURATION,
                                                      uniKey,
                                                      uni,
                                                      ovsdbNode,
                                                      dataBroker);
                        } else {
                            ovsdbNode = UnimgrUtils.createOvsdbNode(dataBroker,
                                                                    uni);
                            LOG.info("Could not retrieve the OVSDB node,"
                                   + " created a new one: {}", ovsdbNode.getNodeId());
                            UnimgrUtils.updateUniNode(LogicalDatastoreType.CONFIGURATION,
                                                      uniKey,
                                                      uni,
                                                      ovsdbNode,
                                                      dataBroker);
                        }
                    }
                } else {
                    // We assume the ovs is in passive mode
                    // Check if the ovsdb node exist
                    Optional<Node> optionalOvsdbNode = UnimgrUtils.findOvsdbNode(dataBroker,
                                                                                 uni);
                    Node ovsdbNode;
                    if (optionalOvsdbNode.isPresent()) {
                        ovsdbNode = optionalOvsdbNode.get();
                        InstanceIdentifier<Node> ovsdbIid = UnimgrMapper.getOvsdbNodeIid(ovsdbNode.getNodeId());
                        LOG.info("Retrieved the OVSDB node");
                        UnimgrUtils.createBridgeNode(dataBroker,
                                                     ovsdbIid,
                                                     uni,
                                                     UnimgrConstants.DEFAULT_BRIDGE_NAME);
                        UnimgrUtils.updateUniNode(LogicalDatastoreType.CONFIGURATION,
                                                  uniKey,
                                                  uni,
                                                  ovsdbNode,
                                                  dataBroker);
                        UnimgrUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL, uniKey, uni, ovsdbNode, dataBroker);
                    } else {
                        ovsdbNode = UnimgrUtils.createOvsdbNode(dataBroker,
                                                                uni);
                        if (ovsdbNode != null) {
                            LOG.info("Could not retrieve the OVSDB node,"
                                    + "created a new one: {}", ovsdbNode.getNodeId());
                             UnimgrUtils.updateUniNode(LogicalDatastoreType.CONFIGURATION,
                                                       uniKey,
                                                       uni,
                                                       ovsdbNode,
                                                       dataBroker);
                        }
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
                                                 .getRemoteIp()
                                                 .getIpv4Address()
                                                 .getValue());
                    List<Node> uniNodes = UnimgrUtils.getUniNodes(dataBroker);
                    if (uniNodes != null && !uniNodes.isEmpty()) {
                        for (Node uniNode: uniNodes) {
                            UniAugmentation uniAugmentation = uniNode.getAugmentation(UniAugmentation.class);
                            if (uniAugmentation.getOvsdbNodeRef() != null
                                    && uniAugmentation.getOvsdbNodeRef().getValue() != null) {
                                InstanceIdentifier<Node> ovsdbNodeRefIid = uniAugmentation
                                                                            .getOvsdbNodeRef()
                                                                            .getValue()
                                                                            .firstIdentifierOf(Node.class);
                                if (ovsdbNodeRefIid.equals(ovsdbIid)) {
                                    Optional<Node> optionalOvsdbNode = UnimgrUtils.readNode(dataBroker,
                                                                                            LogicalDatastoreType.OPERATIONAL,
                                                                                            ovsdbIid);
                                    if (optionalOvsdbNode.isPresent()) {
                                        InstanceIdentifier<Node> uniIid =
                                                                    UnimgrMapper.getUniIid(dataBroker,
                                                                                           uniAugmentation.getIpAddress(),
                                                                                           LogicalDatastoreType.CONFIGURATION);
                                        UnimgrUtils.createBridgeNode(dataBroker,
                                                                     ovsdbIid,
                                                                     uniAugmentation,
                                                                     UnimgrConstants.DEFAULT_BRIDGE_NAME);
                                        UnimgrUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL,
                                                                  uniIid,
                                                                  uniAugmentation,
                                                                  ovsdbIid,
                                                                  dataBroker);
                                    }
                                }
                            } else if (ovsdbNodeAugmentation
                                          .getConnectionInfo()
                                          .getRemoteIp()
                                          .equals(uniAugmentation.getIpAddress())) {
                                InstanceIdentifier<Node> uniIid = UnimgrMapper.getUniIid(dataBroker,
                                                                                         uniAugmentation.getIpAddress(),
                                                                                         LogicalDatastoreType.CONFIGURATION);
                                UnimgrUtils.createBridgeNode(dataBroker,
                                                             ovsdbIid,
                                                             uniAugmentation,
                                                             UnimgrConstants.DEFAULT_BRIDGE_NAME);
                                UnimgrUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL,
                                                          uniIid,
                                                          uniAugmentation,
                                                          ovsdbIid,
                                                          dataBroker);
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
