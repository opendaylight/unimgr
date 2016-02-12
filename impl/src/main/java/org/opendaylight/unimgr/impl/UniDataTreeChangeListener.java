/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.IUnimgrDataTreeChangeListener;
import org.opendaylight.unimgr.command.Command;
import org.opendaylight.unimgr.command.TransactionInvoker;
import org.opendaylight.unimgr.command.UniCreateCommand;
import org.opendaylight.unimgr.command.UniDeleteCommand;
import org.opendaylight.unimgr.command.UniUpdateCommand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Uni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniDataTreeChangeListener implements IUnimgrDataTreeChangeListener<UniAugmentation> {

    private static final Logger LOG = LoggerFactory.getLogger(UniDataTreeChangeListener.class);
    private DataBroker dataBroker;
    private ListenerRegistration<UniDataTreeChangeListener> listener;
    private ListenerRegistration<UniDataTreeChangeListener> listenerOper;

    public UniDataTreeChangeListener(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        InstanceIdentifier<UniAugmentation> uniPath = getUniAugmentationTopologyTreeIid();
        if(uniPath == null)
            LOG.info("uniPath is null");

        DataTreeIdentifier<UniAugmentation> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, uniPath);
        //DataTreeIdentifier<UniAugmentation> dataTreeIidOper = new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,getUniTopologyPath());
        listener = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
        //listenerOper = dataBroker.registerDataTreeChangeListener(dataTreeIidOper, this);
        LOG.info("UniDataTreeChangeListener created and registered");
    }

    @Override
    public void close() throws Exception {
         listener.close();
         //listenerOper.close();
    }

    public static InstanceIdentifier<Node> getUniTopologyNodeIid() {
        InstanceIdentifier<Node> nodePath = InstanceIdentifier
                                                .create(NetworkTopology.class)
                                                .child(Topology.class,
                                                        new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID))
                                                .child(Node.class);
        return nodePath;
    }

    public static InstanceIdentifier<Topology> getUniTopologyIid() {
        InstanceIdentifier<Topology> topologyPath = InstanceIdentifier
                                                .create(NetworkTopology.class)
                                                .child(Topology.class,
                                                        new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID));
        return topologyPath;
    }

    public static InstanceIdentifier<UniAugmentation> getUniAugmentationTopologyTreeIid() {
        InstanceIdentifier<Node> nodePath = InstanceIdentifier
                                                .create(NetworkTopology.class)
                                                .child(Topology.class,
                                                        new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID))
                                                .child(Node.class);
        InstanceIdentifier<UniAugmentation> uniPath = nodePath.augmentation(UniAugmentation.class);
        return uniPath;
    }

    public static InstanceIdentifier<Uni> getUniTopologyTreeIid() {
        InstanceIdentifier<Uni> uniPath = InstanceIdentifier.create(Uni.class);
        return uniPath;
    }

    @Override
	public void onDataTreeChanged(Collection<DataTreeModification<UniAugmentation>> collection) {
        LOG.debug("Config for uni node data tree modification.");
        for (DataTreeModification<UniAugmentation> change : collection) {
             final DataObjectModification<UniAugmentation> rootNode = change.getRootNode();
             //Map<InstanceIdentifier<?>, DataObject> changes = new HashMap();
             //changes.put(change.getRootPath().getRootIdentifier(), rootNode.getDataAfter());
             switch (rootNode.getModificationType()) {
                 case SUBTREE_MODIFIED:
                     LOG.debug("Config for uni node {} updated", rootNode.getIdentifier());
                     //UniUpdateCommand uniUpdate = new UniUpdateCommand(dataBroker, changes);
                     //uniUpdate.execute();
                     
                     break;
                 case WRITE:
                     LOG.debug("Config for uni node {} created", rootNode.getIdentifier());
                     //UniCreateCommand uniCreate = new UniCreateCommand(dataBroker, changes);
                     //uniCreate.execute();
                     break;
                 case DELETE:
                     LOG.debug("Config for uni node {} deleted", rootNode.getIdentifier());
                     //UniDeleteCommand uniDelete = new UniDeleteCommand(dataBroker, changes);
                     //uniDelete.execute();
                     break;
             }
         }
		
	}

	/* (non-Javadoc)
	 * @see org.opendaylight.unimgr.api.IUnimgrDataTreeChangeListener#add(org.opendaylight.controller.md.sal.binding.api.DataTreeModification)
	 */
	@Override
	public void add(DataTreeModification<UniAugmentation> newDataObject) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.opendaylight.unimgr.api.IUnimgrDataTreeChangeListener#update(org.opendaylight.controller.md.sal.binding.api.DataTreeModification)
	 */
	@Override
	public void update(DataTreeModification<UniAugmentation> modifiedDataObject) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.opendaylight.unimgr.api.IUnimgrDataTreeChangeListener#remove(org.opendaylight.controller.md.sal.binding.api.DataTreeModification)
	 */
	@Override
	public void remove(DataTreeModification<UniAugmentation> removedDataObject) {
		// TODO Auto-generated method stub
		
	}

}
