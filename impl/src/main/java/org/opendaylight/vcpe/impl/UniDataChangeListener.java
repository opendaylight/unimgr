/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.vcpe.impl;

import java.util.HashMap;
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

public class UniDataChangeListener implements IVcpeDataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(UniDataChangeListener.class);

    private Map<String, ListenerRegistration<DataChangeListener>> listeners;
    private DataBroker dataBroker;

    public UniDataChangeListener(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        listeners = new HashMap<String, ListenerRegistration<DataChangeListener>>();
        ListenerRegistration<DataChangeListener> uniListener = dataBroker.registerDataChangeListener(
                LogicalDatastoreType.CONFIGURATION, VcpeMapper.getUnisIid()
                , this, DataChangeScope.SUBTREE);
        // We want to listen for operational store changes on the ovsdb:1 network topology
        // because this is when we know Southbound has successfully connected to the
        // OVS instance.
        ListenerRegistration<DataChangeListener> ovsdbListener = dataBroker.registerDataChangeListener(
                LogicalDatastoreType.OPERATIONAL, VcpeMapper.getOvsdbTopologyIdentifier()
                , this, DataChangeScope.SUBTREE);
        listeners.put("uni", uniListener);
        listeners.put("ovsdb", ovsdbListener);

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
    }

    @Override
    public void delete(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        // TODO implement delete, verify old data versus new data
    }

    @Override
    public void close() throws Exception {
        for (Map.Entry<String, ListenerRegistration<DataChangeListener>> entry : listeners.entrySet()) {
            ListenerRegistration<DataChangeListener> value = entry.getValue();
            if (value != null) {
                value.close();
            }
        }
    }
}
