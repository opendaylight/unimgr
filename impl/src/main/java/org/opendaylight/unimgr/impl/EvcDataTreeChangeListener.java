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
import org.opendaylight.unimgr.command.EvcAddCommand;
import org.opendaylight.unimgr.command.EvcRemoveCommand;
import org.opendaylight.unimgr.command.EvcUpdateCommand;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvcDataTreeChangeListener extends UnimgrDataTreeChangeListener<Link> {

    private static final Logger LOG = LoggerFactory.getLogger(EvcDataTreeChangeListener.class);
    private final  ListenerRegistration<EvcDataTreeChangeListener> listener;

    public EvcDataTreeChangeListener(final DataBroker dataBroker) {
        super(dataBroker);
        final DataTreeIdentifier<Link> dataTreeIid =
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, getEvcTopologyPath());
        listener = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
        LOG.info("EvcDataTreeChangeListener created and registered");
    }

    @Override
    public void add(final DataTreeModification<Link> newDataObject) {
        if (newDataObject.getRootPath() != null && newDataObject.getRootNode() != null) {
            LOG.info("evc link {} created", newDataObject.getRootNode().getIdentifier());
            final EvcAddCommand evcAddCmd = new EvcAddCommand(dataBroker, newDataObject);
            evcAddCmd.execute();
        }
    }

    @Override
    public void close() throws Exception {
        listener.close();
    }

    private InstanceIdentifier<Link> getEvcTopologyPath() {
        final InstanceIdentifier<Link> evcPath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.EVC_TOPOLOGY_ID))
                .child(Link.class);
        return evcPath;
    }

    @Override
    public void remove(final DataTreeModification<Link> removedDataObject) {
        if (removedDataObject.getRootPath() != null && removedDataObject.getRootNode() != null) {
            LOG.info("evc link {} deleted", removedDataObject.getRootNode().getIdentifier());
            final EvcRemoveCommand evcRemovedCmd = new EvcRemoveCommand(dataBroker, removedDataObject);
            evcRemovedCmd.execute();
        }
    }

    @Override
    public void update(final DataTreeModification<Link> modifiedDataObject) {
        if (modifiedDataObject.getRootPath() != null && modifiedDataObject.getRootNode() != null) {
            LOG.info("evc link {} updated", modifiedDataObject.getRootNode().getIdentifier());
            final EvcUpdateCommand evcUpdateCmd = new EvcUpdateCommand(dataBroker, modifiedDataObject);
            evcUpdateCmd.execute();
        }
    }
}
