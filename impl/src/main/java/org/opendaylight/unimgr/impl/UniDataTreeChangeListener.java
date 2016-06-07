/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.unimgr.command.UniAddCommand;
import org.opendaylight.unimgr.command.UniRemoveCommand;
import org.opendaylight.unimgr.command.UniUpdateCommand;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniDataTreeChangeListener extends UnimgrDataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(UniDataTreeChangeListener.class);
    private final ListenerRegistration<UniDataTreeChangeListener> listener;


    public UniDataTreeChangeListener(final DataBroker dataBroker) {
        super(dataBroker);
        final InstanceIdentifier<Node> uniPath = getUniTopologyPath();
        final DataTreeIdentifier<Node> dataTreeIid =
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, uniPath);
        listener = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
        LOG.info("UniDataTreeChangeListener created and registered");
    }

    @Override
    public void add(final DataTreeModification<Node> newDataObject) {
        if (newDataObject.getRootPath() != null && newDataObject.getRootNode() != null) {
            LOG.info("uni node {} created", newDataObject.getRootNode().getIdentifier());
            final UniAddCommand uniAddCmd = new UniAddCommand(dataBroker, newDataObject);
            uniAddCmd.execute();
        }
    }

    @Override
    public void close() throws Exception {
        listener.close();
    }

    private InstanceIdentifier<Node> getUniTopologyPath() {
        final InstanceIdentifier<Node> nodePath = InstanceIdentifier
                                                .create(NetworkTopology.class)
                                                .child(Topology.class,
                                                        new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID))
                                                .child(Node.class);
        return nodePath;
    }

    @Override
    public void remove(final DataTreeModification<Node> removedDataObject) {
        if (removedDataObject.getRootPath() != null && removedDataObject.getRootNode() != null) {
            LOG.info("uni node {} deleted", removedDataObject.getRootNode().getIdentifier());
            final UniRemoveCommand uniRemoveCmd = new UniRemoveCommand(dataBroker, removedDataObject);
            uniRemoveCmd.execute();
        }
    }

    @Override
    public void update(final DataTreeModification<Node> modifiedDataObject) {
        if (modifiedDataObject.getRootPath() != null && modifiedDataObject.getRootNode() != null) {
            LOG.info("uni node {} updated", modifiedDataObject.getRootNode().getIdentifier());
            final UniUpdateCommand uniUpdateCmd = new UniUpdateCommand(dataBroker, modifiedDataObject);
            uniUpdateCmd.execute();
        }
    }

}
