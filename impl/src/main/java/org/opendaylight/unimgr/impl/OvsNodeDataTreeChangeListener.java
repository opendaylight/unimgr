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
import org.opendaylight.unimgr.command.OvsNodeAddCommand;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsNodeDataTreeChangeListener extends UnimgrDataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(OvsNodeDataTreeChangeListener.class);
    private final ListenerRegistration<OvsNodeDataTreeChangeListener> listener;

    public OvsNodeDataTreeChangeListener(final DataBroker dataBroker) {
        super(dataBroker);
        final InstanceIdentifier<Node> nodePath = getOvsNodeTopologyPath();
        final DataTreeIdentifier<Node> dataTreeIid =
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, nodePath);
        listener = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
        LOG.info("ovsNodeDataTreeChangeListener created and registered");
    }

    @Override
    public void add(final DataTreeModification<Node> newDataObject) {
        if (newDataObject.getRootPath() != null && newDataObject.getRootNode() != null) {
            LOG.info("ovs node {} created", newDataObject.getRootNode().getIdentifier());
            final OvsNodeAddCommand ovsNodeAddCmd = new OvsNodeAddCommand(dataBroker, newDataObject);
            ovsNodeAddCmd.execute();
        }
    }

    @Override
    public void close() throws Exception {
        listener.close();
    }

    private InstanceIdentifier<Node> getOvsNodeTopologyPath() {
        final InstanceIdentifier<Node> topoPath = InstanceIdentifier
                                                    .create(NetworkTopology.class)
                                                    .child(Topology.class,
                                                            new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                                                    .child(Node.class);
        return topoPath;
    }

    @Override
    public void remove(final DataTreeModification<Node> removedDataObject) {
        // TODO Auto-generated method stub
    }

    @Override
    public void update(final DataTreeModification<Node> modifiedDataObject) {
        // TODO Auto-generated method stub
    }

}
