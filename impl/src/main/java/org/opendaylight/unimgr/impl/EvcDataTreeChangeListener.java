/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.impl;

import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.IUnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class EvcDataTreeChangeListener implements IUnimgrDataTreeChangeListener<Link> {

    private static final Logger LOG = LoggerFactory.getLogger(EvcDataTreeChangeListener.class);
    private DataBroker dataBroker;
    private  ListenerRegistration<EvcDataTreeChangeListener> listener;

    public EvcDataTreeChangeListener(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        DataTreeIdentifier<Link> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,getEvcTopologyPath());
        listener = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
        LOG.info("EvcDataTreeChangeListener created and registered");
    }

    @Override
    public void close() throws Exception {
        listener.close();
    }

    public static InstanceIdentifier<Link> getEvcTopologyPath() {
        InstanceIdentifier<Link> evcPath = InstanceIdentifier
                                                   .create(NetworkTopology.class)
                                                   .child(Topology.class, new TopologyKey(UnimgrConstants.EVC_TOPOLOGY_ID))
                                                   .child(Link.class);
        return evcPath;
    }

    public static InstanceIdentifier<EvcAugmentation> getEvcAugmentationTopologyPath() {
        InstanceIdentifier<Link> linkPath = InstanceIdentifier
                                                   .create(NetworkTopology.class)
                                                   .child(Topology.class, new TopologyKey(UnimgrConstants.EVC_TOPOLOGY_ID))
                                                   .child(Link.class);
        InstanceIdentifier<EvcAugmentation> evcPath = linkPath.augmentation(EvcAugmentation.class);
        return evcPath;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Link>> collection) {
        for (DataTreeModification<Link> change : collection) {
             final DataObjectModification<Link> rootNode = change.getRootNode();
             //Map<InstanceIdentifier<?>, DataObject> changes = new HashMap();
             //changes.put(change.getRootPath().getRootIdentifier(), rootNode.getDataAfter());
             switch (rootNode.getModificationType()) {
                 case SUBTREE_MODIFIED:
                     LOG.debug("Config for evc link {} updated", rootNode.getIdentifier());

                     break;
                 case WRITE:
                     LOG.debug("Config for evc link {} created", rootNode.getIdentifier());

                     break;
                 case DELETE:
                     LOG.debug("Config for evc link {} deleted", rootNode.getIdentifier());

                     break;
             }
         }
        
    }

    /* (non-Javadoc)
     * @see org.opendaylight.unimgr.api.IUnimgrDataTreeChangeListener#add(org.opendaylight.controller.md.sal.binding.api.DataTreeModification)
     */
    @Override
    public void add(DataTreeModification<Link> newDataObject) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.opendaylight.unimgr.api.IUnimgrDataTreeChangeListener#update(org.opendaylight.controller.md.sal.binding.api.DataTreeModification)
     */
    @Override
    public void update(DataTreeModification<Link> modifiedDataObject) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.opendaylight.unimgr.api.IUnimgrDataTreeChangeListener#remove(org.opendaylight.controller.md.sal.binding.api.DataTreeModification)
     */
    @Override
    public void remove(DataTreeModification<Link> removedDataObject) {
        // TODO Auto-generated method stub
        
    }
}
