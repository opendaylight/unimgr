/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.impl;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.command.Command;
import org.opendaylight.unimgr.command.EvcCreateCommand;
import org.opendaylight.unimgr.command.EvcDeleteCommand;
import org.opendaylight.unimgr.command.EvcUpdateCommand;
import org.opendaylight.unimgr.command.TransactionInvoker;
import org.opendaylight.unimgr.command.UniCreateCommand;
import org.opendaylight.unimgr.command.UniDeleteCommand;
import org.opendaylight.unimgr.command.UniUpdateCommand;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UnimgrMapper.class,
    UnimgrDataChangeListener.class})
public class UnimgrDataChangeListenerTest {

    @Mock private UnimgrDataChangeListener unimgrDataChangeListener;
    @Mock private HashSet<ListenerRegistration<DataChangeListener>> listeners;
    @Mock private DataBroker dataBroker;
    @Mock private TransactionInvoker invoker;

    @Before
    public void setUp() throws Exception {
        unimgrDataChangeListener = mock(UnimgrDataChangeListener.class, Mockito.CALLS_REAL_METHODS);
        MemberModifier.field(UnimgrDataChangeListener.class, "invoker").set(unimgrDataChangeListener, invoker);
    }

    @SuppressWarnings({ "unchecked", "resource" })
    @Test
    public void testUnimgrDataChangeListener() throws Exception {
        final InstanceIdentifier<Topology> path = mock(InstanceIdentifier.class);
        final ListenerRegistration<DataChangeListener> listenerRegistration = mock(ListenerRegistration.class);
        PowerMockito.mockStatic(UnimgrMapper.class);
        PowerMockito.when(UnimgrMapper.getUniTopologyIid()).thenReturn(path);
        PowerMockito.when(UnimgrMapper.getEvcTopologyIid()).thenReturn(path);
        PowerMockito.when(UnimgrMapper.getOvsdbTopologyIid()).thenReturn(path);
        PowerMockito.whenNew(HashSet.class).withNoArguments().thenReturn(listeners);
        when(listeners.add(any(ListenerRegistration.class))).thenReturn(true);
        when(dataBroker.registerDataChangeListener(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class),
                any(UnimgrDataChangeListener.class),
                eq(DataChangeScope.SUBTREE))).thenReturn(listenerRegistration);
        new UnimgrDataChangeListener(dataBroker, invoker);
        assertFalse(listeners.isEmpty());
        verify(listeners, atLeast(3)).add(any(ListenerRegistration.class));
        verify(dataBroker, atLeast(3)).registerDataChangeListener(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class),
                any(UnimgrDataChangeListener.class),
                eq(DataChangeScope.SUBTREE));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testOnDataChanged() {
        final AsyncDataChangeEvent change = mock(AsyncDataChangeEvent.class);
        final Map map = mock(Map.class);
        when(change.getCreatedData()).thenReturn(map);
        doNothing().when(unimgrDataChangeListener).create(any(Map.class));
        doNothing().when(unimgrDataChangeListener).update(any(Map.class));
        doNothing().when(unimgrDataChangeListener).delete(any(AsyncDataChangeEvent.class));
        unimgrDataChangeListener.onDataChanged(change);
        verify(unimgrDataChangeListener).create(any(Map.class));
        verify(unimgrDataChangeListener).update(any(Map.class));
        verify(unimgrDataChangeListener).delete(any(AsyncDataChangeEvent.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreate() throws Exception {
        // False case
        unimgrDataChangeListener.create(null);

        // True case
        final Map<InstanceIdentifier<?>, DataObject> changes = mock(Map.class);
        final UniCreateCommand uniCreate = mock(UniCreateCommand.class);
        final EvcCreateCommand evcCreate = mock(EvcCreateCommand.class);
        final List<Command> commands = mock(ArrayList.class);
        PowerMockito.whenNew(ArrayList.class).withNoArguments()
                .thenReturn((ArrayList<Command>) commands);
        PowerMockito.whenNew(UniCreateCommand.class).withArguments(any(DataBroker.class),
                any(Map.class)).thenReturn(uniCreate);
        PowerMockito.whenNew(EvcCreateCommand.class).withArguments(any(DataBroker.class),
                any(Map.class)).thenReturn(evcCreate);
        when(commands.add(any(Command.class))).thenReturn(true);
        doNothing().when(invoker).setCommands(any(ArrayList.class));
        doNothing().when(invoker).invoke();
        unimgrDataChangeListener.create(changes);
        verify(invoker, times(1)).setCommands(any(List.class));
        verify(invoker, times(1)).invoke();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdate() throws Exception {
        // False case
        unimgrDataChangeListener.update(null);

        // True case
        final Map<InstanceIdentifier<?>, DataObject> changes = mock(Map.class);
        final UniUpdateCommand uniUpdate = mock(UniUpdateCommand.class);
        final EvcUpdateCommand evcUpdate = mock(EvcUpdateCommand.class);
        final List<Command> commands = mock(ArrayList.class);
        PowerMockito.whenNew(ArrayList.class).withNoArguments()
                .thenReturn((ArrayList<Command>) commands);
        PowerMockito.whenNew(UniUpdateCommand.class).withArguments(any(DataBroker.class),
                any(Map.class)).thenReturn(uniUpdate);
        PowerMockito.whenNew(EvcUpdateCommand.class).withArguments(any(DataBroker.class),
                any(Map.class)).thenReturn(evcUpdate);
        when(commands.add(any(Command.class))).thenReturn(true);
        doNothing().when(invoker).setCommands(any(ArrayList.class));
        doNothing().when(invoker).invoke();
        unimgrDataChangeListener.update(changes);
        verify(invoker, times(1)).setCommands(any(List.class));
        verify(invoker, times(1)).invoke();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDelete() throws Exception {
        // False case
        unimgrDataChangeListener.delete(null);

        // True case
        final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes = mock(AsyncDataChangeEvent.class);
        final UniDeleteCommand uniDelete = mock(UniDeleteCommand.class);
        final EvcDeleteCommand evcDelete = mock(EvcDeleteCommand.class);
        final List<Command> commands = mock(ArrayList.class);
        PowerMockito.whenNew(ArrayList.class).withNoArguments()
                .thenReturn((ArrayList<Command>) commands);
        PowerMockito.whenNew(UniDeleteCommand.class).withArguments(any(DataBroker.class),
                any(AsyncDataChangeEvent.class)).thenReturn(uniDelete);
        PowerMockito.whenNew(EvcDeleteCommand.class).withArguments(any(DataBroker.class),
                any(AsyncDataChangeEvent.class)).thenReturn(evcDelete);
        when(commands.add(any(Command.class))).thenReturn(true);
        doNothing().when(invoker).setCommands(any(ArrayList.class));
        doNothing().when(invoker).invoke();
        unimgrDataChangeListener.delete(changes);
        verify(invoker, times(1)).setCommands(any(List.class));
        verify(invoker, times(1)).invoke();
    }
}
