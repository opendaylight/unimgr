/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.command;

import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.unimgr.impl.UnimgrUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev150622.Unis;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev150622.unis.Uni;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
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
            if (created.getValue() != null && created.getValue() instanceof Uni) {
                Uni uni = (Uni) created.getValue();
                LOG.info("New UNI created with id {}.", uni.getId());
                /* We assume that when the user specifies the
                 * ovsdb-node-id that the node already exists in
                 * the controller and that the OVS instance is in
                 * active mode.
                 *
                 * We assume that when the user doesn't specify the
                 * ovsdb-node-id that the node doesn't exist therefor
                 * has to be created with the IP address because it's
                 * in passive mode.
                 *
                 * Active mode (TCP): the UUID is in format ovsdb://UUID
                 * Passwove mode (PTCP): the UUID is in format ovsdb://IP:6640
                 *
                 */
                NodeId ovsdbNodeId = uni.getOvsdbNodeId();
                if (ovsdbNodeId == null || ovsdbNodeId.getValue().isEmpty()) {
                    // We assume the ovs is in passive mode
                    ovsdbNodeId = UnimgrMapper.createNodeId(uni.getIpAddress());
                }
                // We retrieve the node from the store
                Optional<Node> node = UnimgrUtils.readNode(dataBroker, UnimgrMapper.getOvsdbNodeIID(ovsdbNodeId));
                if (!node.isPresent()) {
                    UnimgrUtils.createOvsdbNode(dataBroker, ovsdbNodeId, uni);
                }
            }
            if (created.getValue() != null && created.getValue() instanceof OvsdbNodeAugmentation) {
                OvsdbNodeAugmentation ovsdbNodeAugmentation = (OvsdbNodeAugmentation) created
                        .getValue();
                if (ovsdbNodeAugmentation != null) {
                    LOG.info("Received an OVSDB node create {}",
                            ovsdbNodeAugmentation.getConnectionInfo()
                                    .getRemoteIp().getIpv4Address().getValue());
                    Unis unis = UnimgrUtils.readUnisFromStore(dataBroker, LogicalDatastoreType.CONFIGURATION);
                    if (unis != null && unis.getUni() != null) {
                        // This will not scale up very well when the UNI quantity gets to higher numbers.
                        for (Uni uni: unis.getUni()) {
                            if (uni.getOvsdbNodeId() != null && uni.getOvsdbNodeId().getValue() != null) {
                                // The OVS instance is in tcp mode.
                                NodeKey key = created.getKey().firstKeyOf(Node.class, NodeKey.class);
                                if (uni.getOvsdbNodeId().equals(key.getNodeId())) {

                                    UnimgrUtils.createBridgeNode(dataBroker,
                                            uni.getOvsdbNodeId(), uni,
                                            UnimgrConstants.DEFAULT_BRIDGE_NAME);

                                    UnimgrUtils.copyUniToDataStore(dataBroker, uni, LogicalDatastoreType.OPERATIONAL);
                                }
                                // The OVS instance is in ptcp mode.
                            } else if (ovsdbNodeAugmentation
                                            .getConnectionInfo()
                                            .getRemoteIp()
                                            .equals(uni.getIpAddress())) {
                                InstanceIdentifier<Node> ovsdbNodeIid = UnimgrMapper
                                        .getOvsdbNodeIID(uni.getIpAddress());
                                Optional<Node> ovsdbNode = UnimgrUtils.readNode(dataBroker, ovsdbNodeIid);
                                NodeId ovsdbNodeId;
                                if (ovsdbNode.isPresent()) {
                                    ovsdbNodeId = ovsdbNode.get().getNodeId();
                                    UnimgrUtils.createBridgeNode(dataBroker,
                                            ovsdbNodeId, uni,
                                            UnimgrConstants.DEFAULT_BRIDGE_NAME);

                                    UnimgrUtils.copyUniToDataStore(dataBroker, uni, LogicalDatastoreType.OPERATIONAL);
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
