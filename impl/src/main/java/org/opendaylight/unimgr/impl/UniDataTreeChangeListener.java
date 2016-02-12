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
import org.opendaylight.unimgr.command.Command;
import org.opendaylight.unimgr.command.TransactionInvoker;
import org.opendaylight.unimgr.command.UniCreateCommand;
import org.opendaylight.unimgr.command.UniDeleteCommand;
import org.opendaylight.unimgr.command.UniUpdateCommand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Uni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniDataTreeChangeListener implements DataTreeChangeListener<UniAugmentation>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(UniDataTreeChangeListener.class);
    private DataBroker dataBroker;

    public UniDataTreeChangeListener(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        DataTreeIdentifier<Node> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                                    UnimgrMapper.getUniTopologyIid().child(Node.class));
        //dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<UniAugmentation>> collection) {
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

}
