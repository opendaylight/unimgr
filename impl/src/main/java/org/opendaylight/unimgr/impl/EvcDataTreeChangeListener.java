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
import org.opendaylight.unimgr.commands.EvcAddCommand;
import org.opendaylight.unimgr.commands.EvcRemoveCommand;
import org.opendaylight.unimgr.commands.EvcUpdateCommand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvcDataTreeChangeListener implements IUnimgrDataTreeChangeListener<Link> {

    private static final Logger LOG = LoggerFactory.getLogger(EvcDataTreeChangeListener.class);
    public static InstanceIdentifier<EvcAugmentation> getEvcAugmentationTopologyPath() {
        final InstanceIdentifier<Link> linkPath = InstanceIdentifier
                                                   .create(NetworkTopology.class)
                                                   .child(Topology.class, new TopologyKey(UnimgrConstants.EVC_TOPOLOGY_ID))
                                                   .child(Link.class);
        final InstanceIdentifier<EvcAugmentation> evcPath = linkPath.augmentation(EvcAugmentation.class);
        return evcPath;
    }
    private final DataBroker dataBroker;

    private final  ListenerRegistration<EvcDataTreeChangeListener> listener;

    public EvcDataTreeChangeListener(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        final DataTreeIdentifier<Link> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, getEvcTopologyPath());
        listener = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
        LOG.info("EvcDataTreeChangeListener created and registered");
    }

    @Override
    public void add(final DataTreeModification<Link> newDataObject) {
        final EvcAddCommand evcAddCmd = new EvcAddCommand(dataBroker, newDataObject);
        evcAddCmd.execute();
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
    public void onDataTreeChanged(final Collection<DataTreeModification<Link>> collection) {
        for (final DataTreeModification<Link> change : collection) {
             final DataObjectModification<Link> rootLink = change.getRootNode();
             switch (rootLink.getModificationType()) {
                 case SUBTREE_MODIFIED:
                     LOG.info("evc link {} updated", rootLink.getIdentifier());
                     update(change);
                     break;
                 case WRITE:
                     LOG.info("evc link {} created", rootLink.getIdentifier());
                     add(change);
                     break;
                 case DELETE:
                     LOG.info("evc link {} deleted", rootLink.getIdentifier());
                     remove(change);
                     break;
             }
         }
    }

    @Override
    public void remove(final DataTreeModification<Link> removedDataObject) {
        final EvcRemoveCommand evcRemovedCmd = new EvcRemoveCommand(dataBroker, removedDataObject);
        evcRemovedCmd.execute();
    }

    @Override
    public void update(final DataTreeModification<Link> modifiedDataObject) {
        final EvcUpdateCommand evcUpdateCmd = new EvcUpdateCommand(dataBroker, modifiedDataObject);
        evcUpdateCmd.execute();
    }
}
