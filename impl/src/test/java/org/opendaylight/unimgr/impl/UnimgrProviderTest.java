/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.isNull;

import java.util.Dictionary;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.ovsdb.southbound.SouthboundProvider;
import org.opendaylight.unimgr.api.IUnimgrConsoleProvider;
import org.opendaylight.unimgr.command.TransactionInvoker;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FrameworkUtil.class})
public class UnimgrProviderTest {

    @Mock private UnimgrDataChangeListener listener;
    @Mock private DataBroker dataBroker;
    @Mock private UnimgrProvider unimgrProvider;
    @Mock private IUnimgrConsoleProvider console;
    @Mock private ServiceRegistration<IUnimgrConsoleProvider> mockUnimgrConsoleRegistration;

    @Before
    public void setUp() throws Exception {
        unimgrProvider = PowerMockito.mock(UnimgrProvider.class, Mockito.CALLS_REAL_METHODS);
        MemberModifier.field(UnimgrProvider.class, "listener").set(unimgrProvider, listener);
        MemberModifier.field(UnimgrProvider.class, "dataBroker").set(unimgrProvider, dataBroker);
        console = unimgrProvider;
    }

    @Test
    public void testOnSessionInitiated() throws Exception {
        ProviderContext session = mock(ProviderContext.class);
        when(session.getSALService(DataBroker.class)).thenReturn(dataBroker);
        BundleContext context = mock(BundleContext.class);
        PowerMockito.mockStatic(FrameworkUtil.class);
        Bundle bundle = mock(Bundle.class);
        when(bundle.getBundleContext()).thenReturn(context);
        PowerMockito.when(FrameworkUtil.getBundle(unimgrProvider.getClass())).thenReturn(bundle);
        ServiceRegistration<UnimgrProvider> unimgrConsoleRegistrationMock = mock(ServiceRegistration.class);
        mockUnimgrConsoleRegistration = mock(ServiceRegistration.class);
        when(context.registerService(eq(IUnimgrConsoleProvider.class), any(IUnimgrConsoleProvider.class), isNull(Dictionary.class))).thenReturn(mockUnimgrConsoleRegistration);
        PowerMockito.whenNew(UnimgrDataChangeListener.class).withArguments(any(DataBroker.class), any(TransactionInvoker.class)).thenReturn(listener);
        //when(unimgrProvider.initDatastore(any(LogicalDatastoreType.class), any(TopologyId.class)));
        ReadWriteTransaction mockTransaction = mock(ReadWriteTransaction.class);
        //TODO
        CheckedFuture<Optional<Topology>, ReadFailedException> ovsdbTp = mock(CheckedFuture.class);
        when(mockTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(ovsdbTp);
        MemberModifier.suppress(MemberMatcher.method(UnimgrProvider.class, "initializeTopology", LogicalDatastoreType.class));
        unimgrProvider.onSessionInitiated(session);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInitializeTopology() throws Exception {
        InstanceIdentifier<NetworkTopology> path = mock(InstanceIdentifier.class);
        PowerMockito.mockStatic(InstanceIdentifier.class);
        when(InstanceIdentifier.create(NetworkTopology.class)).thenReturn(path);

        CheckedFuture<Optional<NetworkTopology>, ReadFailedException> topology = mock(CheckedFuture.class);
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        when(dataBroker.newReadWriteTransaction()).thenReturn(transaction);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(topology);

        Optional<NetworkTopology> optNetTopo = mock(Optional.class);
        when(topology.get()).thenReturn(optNetTopo);
        when(optNetTopo.isPresent()).thenReturn(false);
        NetworkTopologyBuilder ntb = mock(NetworkTopologyBuilder.class);
        PowerMockito.whenNew(NetworkTopologyBuilder.class).withNoArguments().thenReturn(ntb);
        NetworkTopology networkTopology = mock(NetworkTopology.class);
        when(ntb.build()).thenReturn(networkTopology);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(NetworkTopology.class));
        when(transaction.submit()).thenReturn(mock(CheckedFuture.class));

        LogicalDatastoreType type = PowerMockito.mock(LogicalDatastoreType.class);
        Whitebox.invokeMethod(unimgrProvider, "initializeTopology", type);
        verify(ntb).build();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInitDatastore() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        when(dataBroker.newReadWriteTransaction()).thenReturn(transaction);

        //suppress calls to initializeTopology()
        MemberModifier.suppress(MemberMatcher.method(SouthboundProvider.class, "initializeTopology", LogicalDatastoreType.class));

        CheckedFuture<Optional<Topology>, ReadFailedException> ovsdbTp = mock(CheckedFuture.class);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(ovsdbTp);

        //true case
        Optional<Topology> optTopo = mock(Optional.class);
        when(ovsdbTp.get()).thenReturn(optTopo);
        when(optTopo.isPresent()).thenReturn(false);
        TopologyBuilder tpb = mock(TopologyBuilder.class);
        PowerMockito.whenNew(TopologyBuilder.class).withNoArguments().thenReturn(tpb);
        when(tpb.setTopologyId(any(TopologyId.class))).thenReturn(tpb);
        Topology data = mock(Topology.class);
        when(tpb.build()).thenReturn(data);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Topology.class));
        when(transaction.submit()).thenReturn(mock(CheckedFuture.class));

        LogicalDatastoreType type = PowerMockito.mock(LogicalDatastoreType.class);
        Whitebox.invokeMethod(unimgrProvider, "initDatastore", any(LogicalDatastoreType.class), type);
        PowerMockito.verifyPrivate(unimgrProvider).invoke("initializeTopology", any(LogicalDatastoreType.class));
        verify(tpb).setTopologyId(any(TopologyId.class));
        verify(tpb).build();

        //false case
        when(optTopo.isPresent()).thenReturn(false);
        when(transaction.cancel()).thenReturn(true);
        Whitebox.invokeMethod(unimgrProvider, "initDatastore", any(InstanceIdentifier.class));
        PowerMockito.verifyPrivate(unimgrProvider, times(2)).invoke("initializeTopology", any(LogicalDatastoreType.class));
    }

}
