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
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Evc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvcDataTreeChangeListener implements DataTreeChangeListener<Evc>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(EvcDataTreeChangeListener.class);
    private DataBroker dataBroker;

    public EvcDataTreeChangeListener(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Evc>> collection) {
        for (DataTreeModification<Evc> change : collection) {
             final DataObjectModification<Evc> rootNode = change.getRootNode();
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
}
