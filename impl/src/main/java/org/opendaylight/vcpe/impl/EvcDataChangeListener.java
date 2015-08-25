/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.vcpe.impl;

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

public class EvcDataChangeListener implements IVcpeDataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(EvcDataChangeListener.class);

    private DataBroker dataBroker;
    private ListenerRegistration<DataChangeListener> evcListener = null;

    public EvcDataChangeListener(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        evcListener = dataBroker.registerDataChangeListener(
                LogicalDatastoreType.CONFIGURATION, VcpeMapper.getEvcsIid(),
                this, DataChangeScope.SUBTREE);
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
//        for (Entry<InstanceIdentifier<?>, DataObject> created : changes
//                .entrySet()) {
//            if (created.getValue() != null && created.getValue() instanceof Evc) {
//                Evc evc = (Evc) created.getValue();
//                LOG.info("New EVC created with id {}.", evc.getId());
//                if (evc.getUniDest() == null || evc.getUniDest().isEmpty()) {
//                    LOG.error("Destination UNI cannot be null.");
//                    break;
//                }
//                if (evc.getUniSource() == null || evc.getUniSource().isEmpty()) {
//                    LOG.error("Source UNI cannot be null.");
//                    break;
//                }
//                // Get the destination UNI
//                NodeId destUniNodeID = evc.getUniDest().get(0).getUni();
//                InstanceIdentifier<Uni> destinationNodeIid = VcpeMapper.getUniIid(destUniNodeID);
//                Optional<Uni> optionalDestination = VcpeUtils.readUniNode(dataBroker, destinationNodeIid);
//                Uni destinationUni = optionalDestination.get();
//                NodeId ovsdbDestinationNodeId = VcpeMapper.createNodeId(destinationUni.getIpAddress());
//                // Get the source UNI
//                NodeId sourceUniNodeID = evc.getUniSource().get(0).getUni();
//                InstanceIdentifier<Uni> sourceNodeIid = VcpeMapper.getUniIid(sourceUniNodeID);
//                Optional<Uni> optionalSource = VcpeUtils.readUniNode(dataBroker, sourceNodeIid);
//                Uni sourceUni = optionalSource.get();
//                NodeId ovsdbSourceNodeId = VcpeMapper.createNodeId(sourceUni.getIpAddress());
//
//                // Set source
//                Node sourceBr1 = VcpeUtils.readNode(
//                        dataBroker,
//                        VcpeMapper.getOvsdbBridgeNodeIID(ovsdbSourceNodeId,
//                                VcpeConstants.DEFAULT_BRIDGE_NAME)).get();
//                VcpeUtils.createTerminationPointNode(dataBroker,
//                        destinationUni, sourceBr1,
//                        VcpeConstants.DEFAULT_BRIDGE_NAME,
//                        VcpeConstants.DEFAULT_INTERNAL_IFACE, null);
//                Node sourceBr2 = VcpeUtils.readNode(
//                        dataBroker,
//                        VcpeMapper.getOvsdbBridgeNodeIID(ovsdbSourceNodeId,
//                                VcpeConstants.DEFAULT_BRIDGE_NAME)).get();
//                VcpeUtils.createGreTunnel(dataBroker, sourceUni,
//                        destinationUni, sourceBr2,
//                        VcpeConstants.DEFAULT_BRIDGE_NAME, "gre0");
//
//                // Set destination
//                Node destinationBr1 = VcpeUtils.readNode(
//                        dataBroker,
//                        VcpeMapper.getOvsdbBridgeNodeIID(ovsdbDestinationNodeId,
//                                VcpeConstants.DEFAULT_BRIDGE_NAME)).get();
//                VcpeUtils.createTerminationPointNode(dataBroker,
//                        destinationUni, destinationBr1,
//                        VcpeConstants.DEFAULT_BRIDGE_NAME,
//                        VcpeConstants.DEFAULT_INTERNAL_IFACE, null);
//                Node destinationBr2 = VcpeUtils.readNode(
//                        dataBroker,
//                        VcpeMapper.getOvsdbBridgeNodeIID(ovsdbDestinationNodeId,
//                                VcpeConstants.DEFAULT_BRIDGE_NAME)).get();
//                VcpeUtils.createGreTunnel(dataBroker, destinationUni,
//                        sourceUni, destinationBr2,
//                        VcpeConstants.DEFAULT_BRIDGE_NAME, "gre0");
//            }
//        }
    }

    @Override
    public void update(Map<InstanceIdentifier<?>, DataObject> changes) {
        // TODO Auto-generated method stub
    }

    @Override
    public void delete(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        // TODO Auto-generated method stub
    }

    @Override
    public void close() throws Exception {
        if (evcListener != null) {
            evcListener.close();
        }
    }
}
