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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class UniUpdateCommand extends AbstractUpdateCommand {

    private static final Logger LOG = LoggerFactory.getLogger(UniUpdateCommand.class);

    public UniUpdateCommand(DataBroker dataBroker,
            Map<InstanceIdentifier<?>, DataObject> changes) {
        super.dataBroker = dataBroker;
        super.changes = changes;
    }

    @Override
    public void execute() {
        for (final Entry<InstanceIdentifier<?>, DataObject> created : changes.entrySet()) {
            if (created.getValue() != null && created.getValue() instanceof UniAugmentation){
                final UniAugmentation uniAugmentation = (UniAugmentation) created.getValue();
                final InstanceIdentifier<Node> uniIid = created.getKey()
                        .firstIdentifierOf(Node.class);
                LOG.trace("New UNI updated {}.", uniAugmentation.getIpAddress().getIpv4Address());
                if (uniAugmentation.getOvsdbNodeRef() != null) {
                    final OvsdbNodeRef ovsdbNodeRef = uniAugmentation.getOvsdbNodeRef();
                    final Optional<Node> optionalNode = UnimgrUtils.readNode(dataBroker,
                            LogicalDatastoreType.OPERATIONAL, ovsdbNodeRef.getValue());
                    if (!optionalNode.isPresent()) {
                        LOG.info("Invalid OVSDB node instance identifier specified, "
                               + "attempting to retrieve the node.");
                        final Optional<Node> optionalOvsdbNode = UnimgrUtils.findOvsdbNode(dataBroker,
                                uniAugmentation);
                        Node ovsdbNode;
                        if (optionalOvsdbNode.isPresent()) {
                            ovsdbNode = optionalOvsdbNode.get();
                            LOG.info("Retrieved the OVSDB node {}", ovsdbNode.getNodeId());
                            final InstanceIdentifier<Node> uniKey =
                                    UnimgrMapper.getUniIid(dataBroker, uniAugmentation.getIpAddress(),
                                            LogicalDatastoreType.CONFIGURATION);
                            UnimgrUtils.deleteNode(dataBroker, uniIid, LogicalDatastoreType.CONFIGURATION);
                            UnimgrUtils.createUniNode(dataBroker, uniAugmentation,
                                    ((NodeAttributes) uniKey).getNodeId());
                            UnimgrUtils.updateUniNode(LogicalDatastoreType.CONFIGURATION, uniKey,
                                    uniAugmentation, ovsdbNode, dataBroker);
                            UnimgrUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL, uniKey,
                                    uniAugmentation, ovsdbNode, dataBroker);
                        } else {
                            ovsdbNode = UnimgrUtils.createOvsdbNode(dataBroker, uniAugmentation);
                            LOG.info("Could not retrieve the OVSDB node,"
                                   + " created a new one: {}", ovsdbNode.getNodeId());
                            final InstanceIdentifier<Node> uniKey =
                                    UnimgrMapper.getUniIid(dataBroker, uniAugmentation.getIpAddress(),
                                            LogicalDatastoreType.CONFIGURATION);
                            UnimgrUtils.deleteNode(dataBroker, uniIid, LogicalDatastoreType.CONFIGURATION);
                            UnimgrUtils.createUniNode(dataBroker, uniAugmentation, ((NodeAttributes) uniKey).getNodeId());
                            UnimgrUtils.updateUniNode(LogicalDatastoreType.CONFIGURATION, uniKey,
                                    uniAugmentation, ovsdbNode, dataBroker);
                        }
                    }
                } else {
                    // We assume the ovs is in passive mode
                    // Check if the ovsdb node exist
                    final Optional<Node> optionalOvsdbNode = UnimgrUtils.findOvsdbNode(dataBroker,
                            uniAugmentation);
                    Node ovsdbNode;
                    if (optionalOvsdbNode.isPresent()) {
                        ovsdbNode = optionalOvsdbNode.get();
                        final InstanceIdentifier<Node> ovsdbIid = UnimgrMapper.getOvsdbNodeIid(ovsdbNode
                                .getNodeId());
                        LOG.info("Retrieved the OVSDB node");
                        final InstanceIdentifier<Node> uniKey =
                                UnimgrMapper.getUniIid(dataBroker, uniAugmentation.getIpAddress(),
                                        LogicalDatastoreType.CONFIGURATION);
                        UnimgrUtils.createBridgeNode(dataBroker, ovsdbIid, uniAugmentation,
                                UnimgrConstants.DEFAULT_BRIDGE_NAME);
                        UnimgrUtils.deleteNode(dataBroker, uniIid, LogicalDatastoreType.CONFIGURATION);
                        //UnimgrUtils.deleteNode(dataBroker, uniIid, LogicalDatastoreType.OPERATIONAL);
                        UnimgrUtils.createUniNode(dataBroker, uniAugmentation,
                                ((NodeAttributes) uniKey).getNodeId());
                        UnimgrUtils.updateUniNode(LogicalDatastoreType.CONFIGURATION, uniKey,
                                uniAugmentation, ovsdbNode, dataBroker);
                        UnimgrUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL,
                                uniKey, uniAugmentation, ovsdbNode, dataBroker);
                    } else {
                        ovsdbNode = UnimgrUtils.createOvsdbNode(dataBroker, uniAugmentation);
                        if (ovsdbNode != null) {
                            LOG.info("Could not retrieve the OVSDB node,"
                                    + "created a new one: {}", ovsdbNode.getNodeId());
                            final InstanceIdentifier<Node> uniKey =
                                    UnimgrMapper.getUniIid(dataBroker, uniAugmentation.getIpAddress(),
                                            LogicalDatastoreType.CONFIGURATION);
                            UnimgrUtils.deleteNode(dataBroker, uniIid,
                                    LogicalDatastoreType.CONFIGURATION);
                            UnimgrUtils.createUniNode(dataBroker, uniAugmentation,
                                    ((NodeAttributes) uniKey).getNodeId());
                             UnimgrUtils.updateUniNode(LogicalDatastoreType.CONFIGURATION,
                                     uniKey, uniAugmentation, ovsdbNode, dataBroker);
                        }
                    }
                }
            }
            if (created.getValue() != null && created.getValue() instanceof OvsdbNodeAugmentation) {
                final OvsdbNodeAugmentation ovsdbNodeAugmentation = (OvsdbNodeAugmentation) created
                        .getValue();
                final InstanceIdentifier<Node> ovsdbIid = created.getKey()
                        .firstIdentifierOf(Node.class);
                if (ovsdbNodeAugmentation != null) {
                    LOG.trace("Received an OVSDB node create {}",
                            ovsdbNodeAugmentation.getConnectionInfo()
                                    .getRemoteIp().getIpv4Address().getValue());
                    final List<ManagedNodeEntry> managedNodeEntries = ovsdbNodeAugmentation
                            .getManagedNodeEntry();
                    if (managedNodeEntries != null) {
                        for (final ManagedNodeEntry managedNodeEntry : managedNodeEntries) {
                            LOG.trace("Received an update from an OVSDB node {}.",
                                    managedNodeEntry.getKey());
                            // We received a node update from the southbound plugin
                            // so we have to check if it belongs to the UNI

                            final InstanceIdentifier<Node> bridgeIid = managedNodeEntry.
                                    getBridgeRef().getValue().firstIdentifierOf(Node.class);
                            final Optional<Node> optNode = UnimgrUtils.readNode(dataBroker,
                                    LogicalDatastoreType.OPERATIONAL, bridgeIid);
                            if(optNode.isPresent()){
                                final Node bridgeNode = optNode.get();
                                final InstanceIdentifier<TerminationPoint> iidGreTermPoint =
                                        UnimgrMapper.getTerminationPointIid(bridgeNode,
                                                UnimgrConstants.DEFAULT_GRE_TUNNEL_NAME);
                                final InstanceIdentifier<TerminationPoint> iidEthTermPoint =
                                        UnimgrMapper.getTerminationPointIid(bridgeNode,
                                                UnimgrConstants.DEFAULT_TUNNEL_IFACE);
                                if (iidGreTermPoint.equals(ovsdbIid) ||
                                        iidEthTermPoint.equals(ovsdbIid)){
                                    //updateUni(ovsdbIid);
                                    final Optional<Node> optionalOvsdbNode = UnimgrUtils.readNode(
                                            dataBroker, LogicalDatastoreType.OPERATIONAL, ovsdbIid);
                                    if(optionalOvsdbNode.isPresent()){
                                        final Node node = optionalOvsdbNode.get();
                                        final UniAugmentation uniAug = node.getAugmentation(UniAugmentation.class);
                                        final InstanceIdentifier<Node> uniNodeIid = UnimgrMapper.getUniIid(dataBroker,
                                                uniAug.getIpAddress(), LogicalDatastoreType.CONFIGURATION);
                                        final InstanceIdentifier<Node> uniKey = UnimgrMapper.getUniIid(dataBroker,
                                                uniAug.getIpAddress(), LogicalDatastoreType.CONFIGURATION);
                                        final InstanceIdentifier<Node> ovsdbNodeIid = uniAug.getOvsdbNodeRef().
                                                getValue().firstIdentifierOf(Node.class);
                                        UnimgrUtils.deleteNode(dataBroker, uniKey, LogicalDatastoreType.CONFIGURATION);
                                        UnimgrUtils.createUniNode(dataBroker, uniAug, node.getNodeId());
                                        UnimgrUtils.updateUniNode(LogicalDatastoreType.CONFIGURATION, uniKey,
                                                uniAug, ovsdbNodeIid, dataBroker);
                                        UnimgrUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL, uniKey,
                                                uniAug, ovsdbNodeIid, dataBroker);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
