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
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.UniBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.PhysicalLayersBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.LinksBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.Link;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.device.interfaces.Interface;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.device.interfaces.InterfaceBuilder;
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

import com.google.common.util.concurrent.CheckedFuture;

public class NodeConnectorListener extends UnimgrDataTreeChangeListener<FlowCapableNodeConnector> {

    private static final String BRIDGE_PREFIX = "br-";
    private static final String TUNNEL_PREFIX = "tun";
    private static final Logger LOG = LoggerFactory.getLogger(NodeConnectorListener.class);
    private final UniPortManager uniPortManager;
    private ListenerRegistration<NodeConnectorListener> nodeConnectorListenerRegistration;

    public NodeConnectorListener(final DataBroker dataBroker, final UniPortManager uniPortManager) {
        super(dataBroker);
        this.uniPortManager = uniPortManager;
        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<FlowCapableNodeConnector> dataTreeIid = new DataTreeIdentifier<>(
                    LogicalDatastoreType.OPERATIONAL, getInstanceIdentifier());
            nodeConnectorListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
            LOG.info("NodeConnectorListener created and registered");

        } catch (final Exception e) {
            LOG.error("Node connector listener registration failed !", e);
            throw new IllegalStateException("Node connector listener registration failed.", e);
        }
    }

    @SuppressWarnings("deprecation")
    private InstanceIdentifier<FlowCapableNodeConnector> getInstanceIdentifier() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class).child(NodeConnector.class)
                .augmentation(FlowCapableNodeConnector.class);
    }

    @Override
    public void close() throws Exception {
        nodeConnectorListenerRegistration.close();
    }

    @Override
    public void add(DataTreeModification<FlowCapableNodeConnector> newDataObject) {
        if (newDataObject.getRootPath() != null && newDataObject.getRootNode() != null) {
            LOG.info("node connector {} created", newDataObject.getRootNode().getIdentifier());
            addFlowCapableNodeConnector(newDataObject);
        }
    }

    @Override
    public void remove(DataTreeModification<FlowCapableNodeConnector> removedDataObject) {
        if (removedDataObject.getRootPath() != null && removedDataObject.getRootNode() != null) {
            LOG.info("node connector {} deleted", removedDataObject.getRootNode().getIdentifier());
            removeFlowCapableNodeConnector(removedDataObject);
        }
    }

    @Override
    public void update(DataTreeModification<FlowCapableNodeConnector> modifiedDataObject) {
        if (modifiedDataObject.getRootPath() != null && modifiedDataObject.getRootNode() != null) {
            LOG.info("node connector {} updated", modifiedDataObject.getRootNode().getIdentifier());
            updateFlowCapableNodeConnector(modifiedDataObject);
        }
    }

    private void addFlowCapableNodeConnector(DataTreeModification<FlowCapableNodeConnector> newDataObject) {
        try {
            FlowCapableNodeConnector data = newDataObject.getRootNode().getDataAfter();

            String dpnFromNodeConnectorId = getDpnIdFromNodeConnector(newDataObject);

            handleNodeConnectorAdded(dataBroker, dpnFromNodeConnectorId, data);
        } catch (final Exception e) {
            LOG.error("Add node connector failed !", e);
        }
    }

    private void removeFlowCapableNodeConnector(DataTreeModification<FlowCapableNodeConnector> removedDataObject) {
        try {
            FlowCapableNodeConnector data = removedDataObject.getRootNode().getDataBefore();

            String dpnFromNodeConnectorId = getDpnIdFromNodeConnector(removedDataObject);

            handleNodeConnectorRemoved(dataBroker, dpnFromNodeConnectorId, data);
        } catch (final Exception e) {
            LOG.error("Remove node connector failed !", e);
        }
    }

    private void updateFlowCapableNodeConnector(DataTreeModification<FlowCapableNodeConnector> modifiedDataObject) {
        try {
            FlowCapableNodeConnector original = modifiedDataObject.getRootNode().getDataBefore();
            FlowCapableNodeConnector update = modifiedDataObject.getRootNode().getDataAfter();

            String dpnFromNodeConnectorId = getDpnIdFromNodeConnector(modifiedDataObject);

            handleNodeConnectorUpdated(dataBroker, dpnFromNodeConnectorId, original, update);
        } catch (final Exception e) {
            LOG.error("Update node connector failed !", e);
        }
    }

    @SuppressWarnings("deprecation")
    private String getDpnIdFromNodeConnector(DataTreeModification<FlowCapableNodeConnector> newDataObject) {
        InstanceIdentifier<FlowCapableNodeConnector> key = newDataObject.getRootPath().getRootIdentifier();
        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();

        String dpnFromNodeConnectorId = getDpnFromNodeConnectorId(nodeConnectorId);
        return dpnFromNodeConnectorId;
    }

    @SuppressWarnings("deprecation")
    private static String getDpnFromNodeConnectorId(NodeConnectorId portId) {
        /*
         * NodeConnectorId is of form 'openflow:dpnid:portnum'
         */
        String[] split = portId.getValue().split(IfmConstants.OF_URI_SEPARATOR);
        return split[1];
    }

    private void handleNodeConnectorAdded(DataBroker dataBroker, String dpnId, FlowCapableNodeConnector nodeConnector) {

        String uniName = MefInterfaceUtils.getDeviceInterfaceName(dpnId, nodeConnector.getName());

        if (shouldFilterOutNodeConnector(uniName)) {
            LOG.info("filtered out interface {} with device {}", nodeConnector.getName(), dpnId);
            return;
        }

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();

        LOG.info("Adding mef uni/device interface {} with device {}", nodeConnector.getName(), dpnId);

        InstanceIdentifier<Interface> interfacePath = MefInterfaceUtils.getDeviceInterfaceInstanceIdentifier(dpnId,
                uniName);
        InterfaceBuilder interfaceBuilder = new InterfaceBuilder();
        interfaceBuilder.setPhy(new Identifier45(uniName));
        Interface deviceInterface = interfaceBuilder.build();
        tx.merge(LogicalDatastoreType.OPERATIONAL, interfacePath, deviceInterface, true);

        InstanceIdentifier<Uni> uniPath = MefInterfaceUtils.getUniInstanceIdentifier(uniName);
        UniBuilder uniBuilder = new UniBuilder();
        uniBuilder.setUniId(new Identifier45(uniName));
        uniBuilder.setMacAddress(nodeConnector.getHardwareAddress());

        PhysicalLayersBuilder physicalLayersBuilder = new PhysicalLayersBuilder();
        LinksBuilder linksBuilder = new LinksBuilder();
        List<Link> links = new ArrayList<>();
        LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.setDevice(new Identifier45(dpnId));
        linkBuilder.setInterface(uniName);
        links.add(linkBuilder.build());
        linksBuilder.setLink(links);
        physicalLayersBuilder.setLinks(linksBuilder.build());
        uniBuilder.setPhysicalLayers(physicalLayersBuilder.build());
        Uni uni = uniBuilder.build();
        tx.merge(LogicalDatastoreType.OPERATIONAL, uniPath, uni, true);

        CheckedFuture<Void, TransactionCommitFailedException> futures = tx.submit();
        try {
            futures.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error writing to datastore (path, data) : ({}, {}), ({}, {})", interfacePath, deviceInterface,
                    uniPath, uni);
            throw new RuntimeException(e.getMessage());
        }

        // Reply UNI port configuration
        uniPortManager.updateOperUni(uni.getUniId().getValue());
    }

    private void handleNodeConnectorRemoved(DataBroker dataBroker, String dpnId,
            FlowCapableNodeConnector nodeConnector) {

        String uniName = MefInterfaceUtils.getDeviceInterfaceName(dpnId, nodeConnector.getName());
        InstanceIdentifier<Interface> interfacePath = MefInterfaceUtils.getDeviceInterfaceInstanceIdentifier(dpnId,
                uniName);
        if (MefInterfaceUtils.getInterface(dataBroker, dpnId, uniName, LogicalDatastoreType.OPERATIONAL) != null) {
            MdsalUtils.syncDelete(dataBroker, LogicalDatastoreType.OPERATIONAL, interfacePath);
        }

        // Reply UNI port configuration
        uniPortManager.removeUniPorts(uniName);
        InstanceIdentifier<Uni> uniPath = MefInterfaceUtils.getUniInstanceIdentifier(uniName);
        if (MefInterfaceUtils.getUni(dataBroker, uniName, LogicalDatastoreType.OPERATIONAL) != null) {
            MdsalUtils.syncDelete(dataBroker, LogicalDatastoreType.OPERATIONAL, uniPath);
        }
    }

    private void handleNodeConnectorUpdated(DataBroker dataBroker, String dpnFromNodeConnectorId,
            FlowCapableNodeConnector original, FlowCapableNodeConnector update) {

    }

    private boolean shouldFilterOutNodeConnector(String interfaceName) {
        String[] splits = interfaceName.split(":");
        return splits.length > 1 && (splits[1].startsWith(TUNNEL_PREFIX) || splits[1].startsWith(BRIDGE_PREFIX));
    }
}
