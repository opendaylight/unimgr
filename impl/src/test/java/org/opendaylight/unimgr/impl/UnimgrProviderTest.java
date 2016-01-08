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

import static org.junit.Assert.assertEquals;

import java.util.Dictionary;
import java.util.List;

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
import org.opendaylight.unimgr.api.IUnimgrConsoleProvider;
import org.opendaylight.unimgr.command.TransactionInvoker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
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
@PrepareForTest({UnimgrProvider.class,
                 InstanceIdentifier.class,
                 LogicalDatastoreType.class,
                 FrameworkUtil.class,
                 UnimgrUtils.class,
                 UnimgrMapper.class})
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

    @SuppressWarnings("unchecked")
    @Test
    public void testOnSessionInitiated() throws Exception {
        ProviderContext session = mock(ProviderContext.class);
        when(session.getSALService(DataBroker.class)).thenReturn(dataBroker);
        BundleContext context = mock(BundleContext.class);
        PowerMockito.mockStatic(FrameworkUtil.class);
        Bundle bundle = mock(Bundle.class);
        when(bundle.getBundleContext()).thenReturn(context);
        PowerMockito.when(FrameworkUtil.getBundle(unimgrProvider.getClass())).thenReturn(bundle);
        mockUnimgrConsoleRegistration = mock(ServiceRegistration.class);
        when(context.registerService(eq(IUnimgrConsoleProvider.class),
                                     any(IUnimgrConsoleProvider.class),
                                     isNull(Dictionary.class))).thenReturn(mockUnimgrConsoleRegistration);
        PowerMockito.whenNew(UnimgrDataChangeListener.class).withArguments(any(DataBroker.class),
                                                                           any(TransactionInvoker.class))
                                                            .thenReturn(listener);
        MemberModifier.suppress(MemberMatcher.method(UnimgrProvider.class, "initDatastore"));
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
        when(transaction.read(any(LogicalDatastoreType.class),
                              any(InstanceIdentifier.class))).thenReturn(topology);

        Optional<NetworkTopology> optNetTopo = mock(Optional.class);
        when(topology.get()).thenReturn(optNetTopo);
        when(optNetTopo.isPresent()).thenReturn(false);
        NetworkTopologyBuilder ntb = mock(NetworkTopologyBuilder.class);
        PowerMockito.whenNew(NetworkTopologyBuilder.class).withNoArguments().thenReturn(ntb);
        NetworkTopology networkTopology = mock(NetworkTopology.class);
        when(ntb.build()).thenReturn(networkTopology);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(NetworkTopology.class));
        when(transaction.submit()).thenReturn(mock(CheckedFuture.class));

        LogicalDatastoreType type = PowerMockito.mock(LogicalDatastoreType.class);
        Whitebox.invokeMethod(unimgrProvider, "initializeTopology", type);
        verify(ntb).build();
        verify(transaction).put(any(LogicalDatastoreType.class),
                                any(InstanceIdentifier.class),
                                any(NetworkTopology.class));
        verify(transaction).submit();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInitDatastore() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        when(dataBroker.newReadWriteTransaction()).thenReturn(transaction);

        //suppress calls to initializeTopology()
        MemberModifier.suppress(MemberMatcher.method(UnimgrProvider.class,
                                                     "initializeTopology",
                                                     LogicalDatastoreType.class));

        CheckedFuture<Optional<Topology>, ReadFailedException> unimgrTp = mock(CheckedFuture.class);
        when(transaction.read(any(LogicalDatastoreType.class),
                              any(InstanceIdentifier.class))).thenReturn(unimgrTp);

        //true case
        Optional<Topology> optTopo = mock(Optional.class);
        when(unimgrTp.get()).thenReturn(optTopo);
        when(optTopo.isPresent()).thenReturn(false);
        TopologyBuilder tpb = mock(TopologyBuilder.class);
        PowerMockito.whenNew(TopologyBuilder.class).withNoArguments().thenReturn(tpb);
        when(tpb.setTopologyId(any(TopologyId.class))).thenReturn(tpb);
        Topology data = mock(Topology.class);
        when(tpb.build()).thenReturn(data);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(Topology.class));
        when(transaction.submit()).thenReturn(mock(CheckedFuture.class));

        LogicalDatastoreType type = PowerMockito.mock(LogicalDatastoreType.class);
        TopologyId mockTopoId = mock(TopologyId.class);
        Whitebox.invokeMethod(unimgrProvider, "initDatastore", type, mockTopoId);
        PowerMockito.verifyPrivate(unimgrProvider).invoke("initializeTopology", type);
        verify(tpb).setTopologyId(any(TopologyId.class));
        verify(tpb).build();
        verify(transaction).put(any(LogicalDatastoreType.class),
                                any(InstanceIdentifier.class),
                                any(NetworkTopology.class));
        verify(transaction).submit();

        //false case
        when(optTopo.isPresent()).thenReturn(false);
        when(transaction.cancel()).thenReturn(true);
        Whitebox.invokeMethod(unimgrProvider, "initDatastore", type, mockTopoId);
        PowerMockito.verifyPrivate(unimgrProvider, times(2)).invoke("initializeTopology",
                                                                    any(LogicalDatastoreType.class));
    }

    @Test
    public void testAddUni() throws Exception {
        PowerMockito.mockStatic(UnimgrUtils.class);
        assertEquals(unimgrProvider.addUni(null), false);
        UniAugmentation mockUniAug = mock(UniAugmentation.class);
        // false case
        when(mockUniAug.getIpAddress()).thenReturn(null);
        assertEquals(unimgrProvider.addUni(mockUniAug), false);
        when(mockUniAug.getMacAddress()).thenReturn(null);
        assertEquals(unimgrProvider.addUni(mockUniAug), false);
        // true case
        when(mockUniAug.getIpAddress()).thenReturn(mock(IpAddress.class));
        when(mockUniAug.getMacAddress()).thenReturn(mock(MacAddress.class));
        UniAugmentationBuilder uniAugBuilder = new UniAugmentationBuilder()
                                                    .setIpAddress(mock(IpAddress.class))
                                                    .setMacAddress(mock(MacAddress.class));
        when(UnimgrUtils.createUniNode(any(DataBroker.class),
                                       any(UniAugmentation.class)))
                        .thenReturn(true);
        assertEquals(true, unimgrProvider.addUni(uniAugBuilder.build()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveUni() throws Exception {
        PowerMockito.mockStatic(UnimgrUtils.class);
        PowerMockito.mockStatic(UnimgrMapper.class);
        PowerMockito.mockStatic(InstanceIdentifier.class);

        // false case
        IpAddress mockIpAddress = mock(IpAddress.class);
        PowerMockito.when(UnimgrMapper.getUniIid(any(DataBroker.class),
                                                 any(IpAddress.class),
                                                 any(LogicalDatastoreType.class)))
                         .thenReturn(null);
        assertEquals(false, unimgrProvider.removeUni(mockIpAddress));

        // true case
        InstanceIdentifier<Node> iid = mock(InstanceIdentifier.class);
        IpAddress ipAddress = new IpAddress(new Ipv4Address("192.168.1.1"));
        PowerMockito.when(UnimgrMapper.getUniIid(any(DataBroker.class),
                                                 any(IpAddress.class),
                                                 any(LogicalDatastoreType.class)))
                                      .thenReturn(iid);
        PowerMockito.when(UnimgrUtils.deleteNode(any(DataBroker.class),
                                                 any(InstanceIdentifier.class),
                                                 any(LogicalDatastoreType.class)))
                                     .thenReturn(true);
        assertEquals(true, unimgrProvider.removeUni(ipAddress));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testlistUnis() throws Exception {
        PowerMockito.mockStatic(UnimgrUtils.class);
        List<UniAugmentation> mockUniList = mock(List.class);
        when(UnimgrUtils.getUnis(any(DataBroker.class),
                                 any(LogicalDatastoreType.class)))
                        .thenReturn(mockUniList);
        assertEquals(mockUniList,
                     unimgrProvider.listUnis(any(LogicalDatastoreType.class)));
    }

    @Test
    public void testgetUni() throws Exception {
        PowerMockito.mockStatic(UnimgrUtils.class);
        UniAugmentation mockUniAug = mock(UniAugmentation.class);
        when(UnimgrUtils.getUni(any(DataBroker.class),
                                any(LogicalDatastoreType.class),
                                any(IpAddress.class)))
                        .thenReturn(mockUniAug);
        IpAddress mockIpAddress = mock(IpAddress.class);
        assertEquals(mockUniAug, unimgrProvider.getUni(mockIpAddress));
    }

    @Test
    public void testRemoveEvc() throws Exception {
        assertEquals(false, unimgrProvider.removeEvc(any(String.class)));
    }

    @Test
    public void testAddEvc() throws Exception {
        assertEquals(false, unimgrProvider.addEvc(any(EvcAugmentation.class)));
    }

    @Test
    public void testGetEvc() throws Exception {
        assertEquals(null, unimgrProvider.getEvc(any(String.class)));
    }

}
