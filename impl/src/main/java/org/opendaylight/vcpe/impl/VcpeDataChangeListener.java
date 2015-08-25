/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.vcpe.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.vcpe.api.IVcpeDataChangeListener;
import org.opendaylight.vcpe.command.Command;
import org.opendaylight.vcpe.command.EvcCreateCommand;
import org.opendaylight.vcpe.command.EvcDeleteCommand;
import org.opendaylight.vcpe.command.EvcUpdateCommand;
import org.opendaylight.vcpe.command.TransactionInvoker;
import org.opendaylight.vcpe.command.UniCreateCommand;
import org.opendaylight.vcpe.command.UniDeleteCommand;
import org.opendaylight.vcpe.command.UniUpdateCommand;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VcpeDataChangeListener  implements IVcpeDataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(VcpeDataChangeListener.class);
    private Map<String, ListenerRegistration<DataChangeListener>> listeners;
    private DataBroker dataBroker;
    private TransactionInvoker invoker;

    public VcpeDataChangeListener(DataBroker dataBroker, TransactionInvoker invoker) {
        this.dataBroker = dataBroker;
        this.invoker = invoker;
        listeners = new HashMap<String, ListenerRegistration<DataChangeListener>>();
        ListenerRegistration<DataChangeListener> uniListener = dataBroker.registerDataChangeListener(
                LogicalDatastoreType.CONFIGURATION, VcpeMapper.getUnisIid()
                , this, DataChangeScope.SUBTREE);
        ListenerRegistration<DataChangeListener> evcListener = dataBroker.registerDataChangeListener(
                LogicalDatastoreType.CONFIGURATION, VcpeMapper.getEvcIid()
                , this, DataChangeScope.SUBTREE);
        ListenerRegistration<DataChangeListener> ovsdbListener = dataBroker.registerDataChangeListener(
                LogicalDatastoreType.OPERATIONAL, VcpeMapper.getOvsdbTopologyIdentifier()
                , this, DataChangeScope.SUBTREE);
        listeners.put("uni", uniListener);
        listeners.put("evc", evcListener);
        listeners.put("ovsdb", ovsdbListener);
    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        create(change.getCreatedData());
        update(change.getUpdatedData());
        delete(change);
    }

    @Override
    public void create(Map<InstanceIdentifier<?>, DataObject> changes) {
        if (changes != null) {
            List<Command> commands = new ArrayList<Command>();
            commands.add(new UniCreateCommand(dataBroker, changes));
            commands.add(new EvcCreateCommand(dataBroker, changes));
            invoker.setCommands(commands);
            invoker.invoke();
        }
    }

    @Override
    public void update(Map<InstanceIdentifier<?>, DataObject> changes) {
        if (changes != null) {
            List<Command> commands = new ArrayList<Command>();
            commands.add(new UniUpdateCommand(dataBroker, changes));
            commands.add(new EvcUpdateCommand(dataBroker, changes));
            invoker.setCommands(commands);
            invoker.invoke();
        }
    }

    @Override
    public void delete(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        if (changes != null) {
            List<Command> commands = new ArrayList<Command>();
            commands.add(new UniDeleteCommand(dataBroker, changes));
            commands.add(new EvcDeleteCommand(dataBroker, changes));
            invoker.setCommands(commands);
            invoker.invoke();
        }
    }

    @Override
    public void close() throws Exception {
        LOG.info("VcpeDataChangeListener stopped.");
        for (Map.Entry<String, ListenerRegistration<DataChangeListener>> entry : listeners.entrySet()) {
            ListenerRegistration<DataChangeListener> value = entry.getValue();
            if (value != null) {
                value.close();
            }
        }
    }
}
