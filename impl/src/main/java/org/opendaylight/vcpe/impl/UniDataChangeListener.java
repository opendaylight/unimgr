/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.vcpe.impl;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.vcpe.api.IVcpeDataChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniDataChangeListener implements IVcpeDataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(UniDataChangeListener.class);

    private Map<String, ListenerRegistration<DataChangeListener>> listeners;
    private DataBroker dataBroker;

    public UniDataChangeListener(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        listeners = new HashMap<String, ListenerRegistration<DataChangeListener>>();
        ListenerRegistration<DataChangeListener> uniListener = dataBroker.registerDataChangeListener(
                LogicalDatastoreType.CONFIGURATION, VcpeMapper.getUnisIid()
                , this, DataChangeScope.SUBTREE);
        // We want to listen for operational store changes on the ovsdb:1 network topology
        // because this is when we know Southbound has successfully connected to the
        // OVS instance.
        ListenerRegistration<DataChangeListener> ovsdbListener = dataBroker.registerDataChangeListener(
                LogicalDatastoreType.OPERATIONAL, VcpeMapper.getOvsdbTopologyIdentifier()
                , this, DataChangeScope.SUBTREE);
        listeners.put("uni", uniListener);
        listeners.put("ovsdb", ovsdbListener);

    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        create(changes.getCreatedData());
        update(changes.getUpdatedData());
        delete(changes);
    }

    @Override
    public void create(Map<InstanceIdentifier<?>, DataObject> changes) {
//        for (Entry<InstanceIdentifier<?>, DataObject> created : changes.entrySet()) {
//            if (created.getValue() != null && created.getValue() instanceof Uni) {
//                Uni uni = (Uni) created.getValue();
//                LOG.info("New UNI created with id {}.", uni.getId());
//                /* We assume that when the user specifies the
//                 * ovsdb-node-id that the node already exists in
//                 * the controller and that the OVS instance is in
//                 * active mode.
//                 *
//                 * We assume that when the user doesn't specify the
//                 * ovsdb-node-id that the node doesn't exist therefor
//                 * has to be created with the IP address because it's
//                 * in passive mode.
//                 *
//                 * Active mode (TCP): the UUID is in format ovsdb://UUID
//                 * Passwove mode (PTCP): the UUID is in format ovsdb://IP:6640
//                 *
//                 */
//                NodeId ovsdbNodeId = uni.getOvsdbNodeId();
//                if (ovsdbNodeId == null || ovsdbNodeId.getValue().isEmpty()) {
//                    // We assume the ovs is in passive mode
//                    ovsdbNodeId = VcpeMapper.createNodeId(uni.getIpAddress());
//                }
//                // We retrieve the node from the store
//                Optional<Node> node = VcpeUtils.readNode(dataBroker, VcpeMapper.getOvsdbNodeIID(ovsdbNodeId));
//                if (!node.isPresent()) {
//                    VcpeUtils.createOvsdbNode(dataBroker, ovsdbNodeId, uni);
//                }
//            }
//            if (created.getValue() != null && created.getValue() instanceof OvsdbNodeAugmentation) {
//                OvsdbNodeAugmentation ovsdbNodeAugmentation = (OvsdbNodeAugmentation) created
//                        .getValue();
//                if (ovsdbNodeAugmentation != null) {
//                    LOG.info("Received an OVSDB node create {}",
//                            ovsdbNodeAugmentation.getConnectionInfo()
//                                    .getRemoteIp().getIpv4Address().getValue());
//                    Unis unis = VcpeUtils.readUnisFromStore(dataBroker, LogicalDatastoreType.CONFIGURATION);
//                    if (unis != null && unis.getUni() != null) {
//                        // This will not scale up very well when the UNI quantity gets to higher numbers.
//                        for (Uni uni: unis.getUni()) {
//                            if (uni.getOvsdbNodeId() != null && uni.getOvsdbNodeId().getValue() != null) {
//                                // The OVS instance is in tcp mode.
//                                NodeKey key = created.getKey().firstKeyOf(Node.class, NodeKey.class);
//                                if (uni.getOvsdbNodeId().equals(key.getNodeId())) {
////                                    NodeId ovsdbNodeId = key.getNodeId();
//                                    VcpeUtils.createBridgeNode(dataBroker,
//                                            uni.getOvsdbNodeId(), uni,
//                                            VcpeConstants.DEFAULT_BRIDGE_NAME);
////                                    VcpeUtils.createBridgeNode(dataBroker,
////                                            uni.getOvsdbNodeId(), uni,
////                                            VcpeConstants.DEFAULT_BRIDGE2_NAME);
//                                    VcpeUtils.copyUniToDataStore(dataBroker, uni, LogicalDatastoreType.OPERATIONAL);
//                                }
//                                // The OVS instance is in ptcp mode.
//                            } else if (ovsdbNodeAugmentation
//                                            .getConnectionInfo()
//                                            .getRemoteIp()
//                                            .equals(uni.getIpAddress())) {
//                                InstanceIdentifier<Node> ovsdbNodeIid = VcpeMapper
//                                                                    .getOvsdbNodeIID(uni.getIpAddress());
//                                Optional<Node> ovsdbNode = VcpeUtils.readNode(dataBroker, ovsdbNodeIid);
//                                NodeId ovsdbNodeId;
//                                if (ovsdbNode.isPresent()) {
//                                    ovsdbNodeId = ovsdbNode.get().getNodeId();
//                                    VcpeUtils.createBridgeNode(dataBroker,
//                                            ovsdbNodeId, uni,
//                                            VcpeConstants.DEFAULT_BRIDGE_NAME);
////                                    VcpeUtils.createBridgeNode(dataBroker,
////                                            ovsdbNodeId, uni,
////                                            VcpeConstants.DEFAULT_BRIDGE2_NAME);
//                                    VcpeUtils.copyUniToDataStore(dataBroker, uni, LogicalDatastoreType.OPERATIONAL);
//                                } else {
//                                    LOG.error("Unable to read node with IID {}", ovsdbNodeIid);
//                                }
//                            }
//                        }
//                    } else {
//                        LOG.info("Received a new OVSDB node connection from {}"
//                                + ovsdbNodeAugmentation.getConnectionInfo()
//                                        .getRemoteIp().getIpv4Address());
//                    }
//                }
//            }
//        }
    }

    @Override
    public void update(Map<InstanceIdentifier<?>, DataObject> changes) {
//        for (Entry<InstanceIdentifier<?>, DataObject> created : changes
//                .entrySet()) {
//            if (created.getValue() != null
//                    && created.getValue() instanceof OvsdbNodeAugmentation) {
//                OvsdbNodeAugmentation ovsdbNodeAugmentation = (OvsdbNodeAugmentation) created
//                        .getValue();
//                if (ovsdbNodeAugmentation != null) {
//                    LOG.info("Received an OVSDB node create {}",
//                            ovsdbNodeAugmentation.getConnectionInfo()
//                                    .getRemoteIp().getIpv4Address().getValue());
//                    final List<ManagedNodeEntry> managedNodeEntries = ovsdbNodeAugmentation.getManagedNodeEntry();
//                    if (managedNodeEntries != null) {
//                        for (ManagedNodeEntry managedNodeEntry : managedNodeEntries) {
//                            LOG.info("Received an update from an OVSDB node {}.", managedNodeEntry.getKey());
//                            // We received a node update from the southbound plugin
//                            // so we have to check if it belongs to the UNI
//                        }
//                    }
//                }
//            }
//        }
    }

    @Override
    public void delete(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        // TODO implement delete, verify old data versus new data
    }

    @Override
    public void close() throws Exception {
        for (Map.Entry<String, ListenerRegistration<DataChangeListener>> entry : listeners.entrySet()) {
            ListenerRegistration<DataChangeListener> value = entry.getValue();
            if (value != null) {
                value.close();
            }
        }
    }
}
