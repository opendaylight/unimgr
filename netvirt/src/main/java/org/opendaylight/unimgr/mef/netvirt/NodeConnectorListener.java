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
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.interfacemanager.globals.IfmConstants;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.UniBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.PhysicalLayersBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.LinksBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.Link;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.Device;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.DeviceBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.device.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.Identifier45;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.CheckedFuture;

public class NodeConnectorListener extends UnimgrDataTreeChangeListener<FlowCapableNodeConnector> {

    private static final Logger log = LoggerFactory.getLogger(NodeConnectorListener.class);
    private static final Logger logger = LoggerFactory.getLogger(NodeConnectorListener.class);
    private static boolean handleRemovedNodeConnectors = false;
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

            handleNodeConnectorAdded(dataBroker, dpnFromNodeConnectorId, data);
        } catch (final Exception e) {
            log.error("Add node connector failed !", e);
        }
    }

    private void removeFlowCapableNodeConnector(DataTreeModification<FlowCapableNodeConnector> removedDataObject) {
        try {
            FlowCapableNodeConnector data = removedDataObject.getRootNode().getDataBefore();

            String dpnFromNodeConnectorId = getDpnIdFromNodeConnector(removedDataObject);

            handleNodeConnectorRemoved(dataBroker, dpnFromNodeConnectorId, data);
        } catch (final Exception e) {
            log.error("Remove node connector failed !", e);
        }
    }

    private void updateFlowCapableNodeConnector(DataTreeModification<FlowCapableNodeConnector> modifiedDataObject) {
        try {
            FlowCapableNodeConnector original = modifiedDataObject.getRootNode().getDataBefore();
            FlowCapableNodeConnector update = modifiedDataObject.getRootNode().getDataAfter();

            String dpnFromNodeConnectorId = getDpnIdFromNodeConnector(modifiedDataObject);

            handleNodeConnectorUpdated(dataBroker, dpnFromNodeConnectorId, original, update);
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
        String[] split = portId.getValue().split(IfmConstants.OF_URI_SEPARATOR);
        return split[1];
    }

    private void handleNodeConnectorAdded(DataBroker dataBroker, String dpnId, FlowCapableNodeConnector nodeConnector) {

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();

        logger.info("Adding mef uni/device interface {} with device {}", nodeConnector.getName(), dpnId);

        String uniName = EvcUniUtils.getDeviceInterfaceName(dpnId, nodeConnector.getName());
        InstanceIdentifier interfacePath = MefUtils.getDeviceInterfaceInstanceIdentifier(dpnId, uniName);
        InterfaceBuilder interfaceBuilder = new InterfaceBuilder();
        interfaceBuilder.setPhy(new Identifier45(uniName));
        DataObject deviceInterface = interfaceBuilder.build();

        tx.merge(LogicalDatastoreType.CONFIGURATION, interfacePath, deviceInterface, true);

        InstanceIdentifier uniPath = MefUtils.getUniInstanceIdentifier(uniName);
        UniBuilder uniBuilder = new UniBuilder();
        uniBuilder.setUniId(new Identifier45(uniName));

        PhysicalLayersBuilder physicalLayersBuilder = new PhysicalLayersBuilder();
        LinksBuilder linksBuilder = new LinksBuilder();
        List<Link> links = new ArrayList();
        LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.setDevice(new Identifier45(dpnId));
        linkBuilder.setInterface(uniName);
        links.add(linkBuilder.build());
        linksBuilder.setLink(links);
        physicalLayersBuilder.setLinks(linksBuilder.build());
        uniBuilder.setPhysicalLayers(physicalLayersBuilder.build());
        DataObject uni = uniBuilder.build();

        tx.merge(LogicalDatastoreType.CONFIGURATION, uniPath, uni, true);
        CheckedFuture<Void, TransactionCommitFailedException> futures = tx.submit();

        try {
            futures.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error writing to datastore (path, data) : ({}, {}), ({}, {})", interfacePath, deviceInterface,
                    uniPath, uni);
            throw new RuntimeException(e.getMessage());
        }
    }

    private void handleNodeConnectorRemoved(DataBroker dataBroker, String dpnId,
            FlowCapableNodeConnector nodeConnector) {

        String uniName = EvcUniUtils.getDeviceInterfaceName(dpnId, nodeConnector.getName());

        if (!handleRemovedNodeConnectors) {
            return;
        }

        MdsalUtils.syncDelete(dataBroker, LogicalDatastoreType.CONFIGURATION,
                MefUtils.getDeviceInterfaceInstanceIdentifier(dpnId, uniName));

        MdsalUtils.syncDelete(dataBroker, LogicalDatastoreType.CONFIGURATION,
                MefUtils.getUniLinkInstanceIdentifier(nodeConnector.getName(), dpnId, uniName));
    }

    private void handleNodeConnectorUpdated(DataBroker dataBroker, String dpnFromNodeConnectorId,
            FlowCapableNodeConnector original, FlowCapableNodeConnector update) {

    }
}
