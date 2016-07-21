/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.Device;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.DeviceBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.Identifier45;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeConnectorListener extends UnimgrDataTreeChangeListener<FlowCapableNodeConnector> {

    private static final String OF_URI_SEPARATOR = ":";
    private static final Logger log = LoggerFactory.getLogger(NodeConnectorListener.class);
    private ListenerRegistration<NodeConnectorListener> evcListenerRegistration;

    public NodeConnectorListener(final DataBroker dataBroker) {
        super(dataBroker);

        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<FlowCapableNodeConnector> dataTreeIid = new DataTreeIdentifier<>(
                    LogicalDatastoreType.OPERATIONAL, getInstanceIdentifier());
            evcListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
            log.info("NodeConnectorListener created and registered");
        } catch (final Exception e) {
            log.error("Node connector listener registration failed !", e);
            throw new IllegalStateException("Node connector listener registration failed.", e);
        }
    }

    private InstanceIdentifier<FlowCapableNodeConnector> getInstanceIdentifier() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class).child(NodeConnector.class)
                .augmentation(FlowCapableNodeConnector.class);
    }

    @Override
    public void close() throws Exception {
        evcListenerRegistration.close();
    }

    @Override
    public void add(DataTreeModification<FlowCapableNodeConnector> newDataObject) {
        if (newDataObject.getRootPath() != null && newDataObject.getRootNode() != null) {
            log.info("node connector {} created", newDataObject.getRootNode().getIdentifier());
            addFlowCapableNodeConnector(newDataObject);
        }
    }

    @Override
    public void remove(DataTreeModification<FlowCapableNodeConnector> removedDataObject) {
        if (removedDataObject.getRootPath() != null && removedDataObject.getRootNode() != null) {
            log.info("node connector {} deleted", removedDataObject.getRootNode().getIdentifier());
            removeFlowCapableNodeConnector(removedDataObject);
        }
    }

    @Override
    public void update(DataTreeModification<FlowCapableNodeConnector> modifiedDataObject) {
        if (modifiedDataObject.getRootPath() != null && modifiedDataObject.getRootNode() != null) {
            log.info("node connector {} updated", modifiedDataObject.getRootNode().getIdentifier());
            updateFlowCapableNodeConnector(modifiedDataObject);
        }
    }

    private void addFlowCapableNodeConnector(DataTreeModification<FlowCapableNodeConnector> newDataObject) {
        try {
            FlowCapableNodeConnector data = newDataObject.getRootNode().getDataAfter();

            String dpnFromNodeConnectorId = getDpnIdFromNodeConnector(newDataObject);

            MefUtils.handleNodeConnectorAdded(dataBroker, dpnFromNodeConnectorId, data);
        } catch (final Exception e) {
            log.error("Add node connector failed !", e);
        }
    }

    private void removeFlowCapableNodeConnector(DataTreeModification<FlowCapableNodeConnector> removedDataObject) {
        try {
            FlowCapableNodeConnector data = removedDataObject.getRootNode().getDataBefore();

            String dpnFromNodeConnectorId = getDpnIdFromNodeConnector(removedDataObject);

            MefUtils.handleNodeConnectorRemoved(dataBroker, dpnFromNodeConnectorId, data);
        } catch (final Exception e) {
            log.error("Remove node connector failed !", e);
        }
    }

    private void updateFlowCapableNodeConnector(DataTreeModification<FlowCapableNodeConnector> modifiedDataObject) {
        try {
            FlowCapableNodeConnector original = modifiedDataObject.getRootNode().getDataBefore();
            FlowCapableNodeConnector update = modifiedDataObject.getRootNode().getDataAfter();
            
            String dpnFromNodeConnectorId = getDpnIdFromNodeConnector(modifiedDataObject);

            MefUtils.handleNodeConnectorUpdated(dataBroker, dpnFromNodeConnectorId, original,update);
        } catch (final Exception e) {
            log.error("Update node connector failed !", e);
        }
    }

    private String getDpnIdFromNodeConnector(DataTreeModification<FlowCapableNodeConnector> newDataObject) {
        InstanceIdentifier<FlowCapableNodeConnector> key = newDataObject.getRootPath().getRootIdentifier();
        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();

        String dpnFromNodeConnectorId = getDpnFromNodeConnectorId(nodeConnectorId);
        return dpnFromNodeConnectorId;
    }

    private static String getDpnFromNodeConnectorId(NodeConnectorId portId) {
        /*
         * NodeConnectorId is of form 'openflow:dpnid:portnum'
         */
        String[] split = portId.getValue().split(OF_URI_SEPARATOR);
        return split[1];
    }
}
