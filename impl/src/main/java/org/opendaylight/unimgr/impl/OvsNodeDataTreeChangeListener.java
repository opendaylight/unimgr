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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsNodeDataTreeChangeListener implements IUnimgrDataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(OvsNodeDataTreeChangeListener.class);
    private DataBroker dataBroker;
    private ListenerRegistration<OvsNodeDataTreeChangeListener> listener;

    public OvsNodeDataTreeChangeListener(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        InstanceIdentifier<Node> nodePath = getOvsNodeTopologyPath();

        if(nodePath == null)
            LOG.info("nodePath is null");

        DataTreeIdentifier<Node> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, nodePath);
        listener = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
        LOG.info("ovsNodeDataTreeChangeListener created and registered");
    }

    public static InstanceIdentifier<Node> getOvsNodeTopologyPath() {
        InstanceIdentifier<Node> topoPath = InstanceIdentifier
                                                    .create(NetworkTopology.class)
                                                    .child(Topology.class,
                                                            new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                                                    .child(Node.class);
        return topoPath;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Node>> collection) {
        LOG.info("recevied ovs node data tree modification.");
        for (DataTreeModification<Node> change : collection) {
             final DataObjectModification<Node> rootNode = change.getRootNode();
             LOG.info("Config for ovs node {} and modification type is {}", rootNode.getIdentifier(), rootNode.getModificationType());
             switch (rootNode.getModificationType()) {
                 case SUBTREE_MODIFIED:
                     LOG.info("Config for ovs node {} updated", rootNode.getIdentifier());

                     break;
                 case WRITE:
                     LOG.info("Config for ovs node {} created", rootNode.getIdentifier());

                     break;
                 case DELETE:
                     LOG.info("Config for ovs node {} deleted", rootNode.getIdentifier());

                     break;
             }
         }
    }

    @Override
    public void close() throws Exception {
        listener.close();
        
    }

    @Override
    public void add(DataTreeModification<Node> newDataObject) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void update(DataTreeModification<Node> modifiedDataObject) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void remove(DataTreeModification<Node> removedDataObject) {
        // TODO Auto-generated method stub
        
    }

}
