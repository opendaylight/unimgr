/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.impl;

import java.util.Collection;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.IUnimgrDataTreeChangeListener;
import org.opendaylight.unimgr.commands.UniAddCommand;
import org.opendaylight.unimgr.commands.UniRemoveCommand;
import org.opendaylight.unimgr.commands.UniUpdateCommand;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniDataTreeChangeListener implements IUnimgrDataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(UniDataTreeChangeListener.class);
    private final DataBroker dataBroker;
    private final ListenerRegistration<UniDataTreeChangeListener> listener;


    public UniDataTreeChangeListener(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        final InstanceIdentifier<Node> uniPath = getUniTopologyPath();
        final DataTreeIdentifier<Node> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, uniPath);
        listener = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
        LOG.info("UniDataTreeChangeListener created and registered");
    }

    @Override
    public void add(final DataTreeModification<Node> newDataObject) {
        final UniAddCommand uniAddCmd = new UniAddCommand(dataBroker, newDataObject);
        uniAddCmd.execute();
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
    public void onDataTreeChanged(final Collection<DataTreeModification<Node>> collection) {
        for (final DataTreeModification<Node> change : collection) {
             final DataObjectModification<Node> rootNode = change.getRootNode();
             switch (rootNode.getModificationType()) {
                 case SUBTREE_MODIFIED:
                     LOG.info("uni node {} updated", rootNode.getIdentifier());
                     update(change);
                     break;
                 case WRITE:
                     LOG.info("uni node {} created", rootNode.getIdentifier());
                     add(change);
                     break;
                 case DELETE:
                     LOG.info("uni node {} deleted", rootNode.getIdentifier());
                     remove(change);
                     break;
             }
         }
    }

    @Override
    public void remove(final DataTreeModification<Node> removedDataObject) {
        final UniRemoveCommand uniRemoveCmd = new UniRemoveCommand(dataBroker, removedDataObject);
        uniRemoveCmd.execute();
    }

    @Override
    public void update(final DataTreeModification<Node> modifiedDataObject) {
        final UniUpdateCommand uniUpdateCmd = new UniUpdateCommand(dataBroker, modifiedDataObject);
        uniUpdateCmd.execute();
    }

}
