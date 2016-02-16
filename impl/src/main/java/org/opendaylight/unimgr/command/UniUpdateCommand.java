/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
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
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.unimgr.utils.OvsdbUtils;
import org.opendaylight.unimgr.utils.UniUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class UniUpdateCommand extends AbstractUpdateCommand {

    private static final Logger LOG = LoggerFactory.getLogger(UniUpdateCommand.class);

    public UniUpdateCommand(DataBroker dataBroker,
            Map<InstanceIdentifier<?>, DataObject> changes) {
        super.dataBroker = dataBroker;
        super.changes = changes;
    }

    @Override
    public void execute() {
        for (Entry<InstanceIdentifier<?>, DataObject> updated : changes.entrySet()) {
            if (updated.getValue() != null && updated.getValue() instanceof UniAugmentation) {
                final UniAugmentation updatedUni = (UniAugmentation) updated.getValue();
                final UniAugmentation formerUni = UniUtils.getUni(dataBroker, LogicalDatastoreType.OPERATIONAL, updatedUni.getIpAddress());

                if (formerUni != null) {
                    String formerUniIp = formerUni.getIpAddress().getIpv4Address().getValue();
                    String updatedUniIp = updatedUni.getIpAddress().getIpv4Address().getValue();
                    String uniKey = updated.getKey().firstKeyOf(Node.class).toString();
                    Preconditions.checkArgument(formerUniIp.equals(updatedUniIp),
                            "Can't update UNI with a different IP address. Former IP was %s"
                                    + "Updated IP is %s. Please create a UNI instead of updating this one %s",
                                    formerUniIp, updatedUniIp, uniKey);
                    Node ovsdbNode;
                    if (updatedUni.getOvsdbNodeRef() != null) {
                        LOG.info("OVSDB NODE ref retreive for updated UNI {}", updatedUni.getOvsdbNodeRef());
                        final OvsdbNodeRef ovsdbNodeRef = updatedUni.getOvsdbNodeRef();
                        Optional<Node> optOvsdbNode = MdsalUtils.readNode(dataBroker,LogicalDatastoreType.OPERATIONAL, ovsdbNodeRef.getValue());
                        if(optOvsdbNode.isPresent()) {
                            ovsdbNode= optOvsdbNode.get();
                            LOG.info("Retrieved the OVSDB node {}", ovsdbNode.getNodeId());
                            // Update QoS entries to ovsdb if speed is configured to UNI node
                            if (updatedUni.getSpeed() != null) {
                                OvsdbUtils.createQoSForOvsdbNode(dataBroker, updatedUni);
                            }
                            UniUtils.updateUniNode(LogicalDatastoreType.CONFIGURATION,
                                    updated.getKey(),
                                    updatedUni,
                                    ovsdbNode,
                                    dataBroker);
                        }  else {
                            // This should never happen, because on creation,
                            // the UNI is assigned and OVSDB node
                            LOG.error("OVSDB node not found for UNI {}, but got OVSDB ref", uniKey, updatedUni.getOvsdbNodeRef());
                            return;
                        }
                    } else {
                        Optional<Node> optOvsdbNode = OvsdbUtils.findOvsdbNode(dataBroker, updatedUni);
                        if (optOvsdbNode.isPresent()) {
                            ovsdbNode = optOvsdbNode.get();
                            LOG.info("Retrieved the OVSDB node {}", ovsdbNode.getNodeId());
                            // Update QoS entries to ovsdb if speed is configured to UNI node
                            if (updatedUni.getSpeed() != null) {

                                OvsdbUtils.createQoSForOvsdbNode(dataBroker, updatedUni);
                            }
                            UniUtils.updateUniNode(LogicalDatastoreType.CONFIGURATION,
                                    updated.getKey(),
                                    updatedUni,
                                    ovsdbNode,
                                    dataBroker);
                        } else {
                            // This should never happen, because on creation,
                            // the UNI is assigned and OVSDB node
                            LOG.error("OVSDB node not found for UNI {}", uniKey);
                            return;
                        }
                    }
                    LOG.info("UNI {} updated", uniKey);

                    final InstanceIdentifier<?> uniIID = UnimgrMapper.getUniIid(dataBroker, updatedUni.getIpAddress(), LogicalDatastoreType.OPERATIONAL);
                    MdsalUtils.deleteNode(dataBroker, uniIID, LogicalDatastoreType.OPERATIONAL);
                    UniUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL, uniIID, updatedUni, ovsdbNode, dataBroker);
                }
            }
            if (updated.getValue() != null
                    && updated.getValue() instanceof OvsdbNodeAugmentation) {
                OvsdbNodeAugmentation ovsdbNodeAugmentation = (OvsdbNodeAugmentation) updated.getValue();
                if (ovsdbNodeAugmentation != null) {
                    LOG.trace("Received an OVSDB node create {}",
                            ovsdbNodeAugmentation.getConnectionInfo()
                                    .getRemoteIp().getIpv4Address().getValue());
                    final List<ManagedNodeEntry> managedNodeEntries = ovsdbNodeAugmentation.getManagedNodeEntry();
                    if (managedNodeEntries != null) {
                        for (ManagedNodeEntry managedNodeEntry : managedNodeEntries) {
                            LOG.trace("Received an update from an OVSDB node {}.", managedNodeEntry.getKey());
                            // We received a node update from the southbound plugin
                            // so we have to check if it belongs to the UNI
                        }
                    }
                }
            }
        }
    }
}
