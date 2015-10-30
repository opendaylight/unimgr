/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.IUnimgrDataChangeListener;
import org.opendaylight.unimgr.command.Command;
import org.opendaylight.unimgr.command.EvcCreateCommand;
import org.opendaylight.unimgr.command.EvcDeleteCommand;
import org.opendaylight.unimgr.command.EvcUpdateCommand;
import org.opendaylight.unimgr.command.TransactionInvoker;
import org.opendaylight.unimgr.command.UniCreateCommand;
import org.opendaylight.unimgr.command.UniDeleteCommand;
import org.opendaylight.unimgr.command.UniUpdateCommand;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnimgrDataChangeListener  implements IUnimgrDataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(UnimgrDataChangeListener.class);
    private Set<ListenerRegistration<DataChangeListener>> listeners;
    private DataBroker dataBroker;
    private TransactionInvoker invoker;

    public UnimgrDataChangeListener(DataBroker dataBroker, TransactionInvoker invoker) {
        this.dataBroker = dataBroker;
        this.invoker = invoker;
        listeners = new HashSet<ListenerRegistration<DataChangeListener>>();
        listeners.add(dataBroker.registerDataChangeListener(
                LogicalDatastoreType.CONFIGURATION, UnimgrMapper.createUniIid()
                , this, DataChangeScope.SUBTREE));
        listeners.add(dataBroker.registerDataChangeListener(
                LogicalDatastoreType.CONFIGURATION, UnimgrMapper.createEvcIid()
                , this, DataChangeScope.SUBTREE));
        listeners.add(dataBroker.registerDataChangeListener(
                LogicalDatastoreType.OPERATIONAL, UnimgrMapper.getOvsdbTopologyIdentifier()
                , this, DataChangeScope.SUBTREE));
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
        LOG.info("UnimgrDataChangeListener stopped.");
        for (ListenerRegistration<DataChangeListener> listener : listeners) {
            if (listener != null) {
                listener.close();
            }
        }
    }
}
