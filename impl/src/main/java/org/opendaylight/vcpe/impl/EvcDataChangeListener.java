/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.vcpe.impl;

import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.vcpe.api.IVcpeDataChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvcDataChangeListener implements IVcpeDataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(EvcDataChangeListener.class);

    private DataBroker dataBroker;
    private ListenerRegistration<DataChangeListener> evcListener = null;

    public EvcDataChangeListener(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        evcListener = dataBroker.registerDataChangeListener(
                LogicalDatastoreType.CONFIGURATION, VcpeMapper.getEvcsIid(),
                this, DataChangeScope.SUBTREE);
    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        create(changes.getCreatedData());
        update(changes.getUpdatedData());
        delete(changes);
    }

    @Override
    public void create(Map<InstanceIdentifier<?>, DataObject> changes) {
    }

    @Override
    public void update(Map<InstanceIdentifier<?>, DataObject> changes) {
        // TODO Auto-generated method stub
    }

    @Override
    public void delete(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        // TODO Auto-generated method stub
    }

    @Override
    public void close() throws Exception {
        if (evcListener != null) {
            evcListener.close();
        }
    }
}
