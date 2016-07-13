/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.opendaylight.unimgr.api.IUnimgrConsoleProvider;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.unimgr.utils.OvsdbUtils;
import org.opendaylight.unimgr.utils.UniUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSource;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
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
                 UniUtils.class,
                 MdsalUtils.class,
                 OvsdbUtils.class,
                 UnimgrMapper.class})
public class UnimgrProviderTest {

    @Mock private IUnimgrConsoleProvider console;
    @Mock private UniDataTreeChangeListener uniListener;
    @Mock private EvcDataTreeChangeListener evcListener;
    @Mock private OvsNodeDataTreeChangeListener ovsListener;
    @Mock private DataBroker dataBroker;
    @Mock private ServiceRegistration<IUnimgrConsoleProvider> mockUnimgrConsoleRegistration;
    @Mock private UnimgrProvider unimgrProvider;

    @Before
    public void setUp() throws Exception {
        unimgrProvider = PowerMockito.mock(UnimgrProvider.class, Mockito.CALLS_REAL_METHODS);
        MemberModifier.field(UnimgrProvider.class, "dataBroker").set(unimgrProvider, dataBroker);
        MemberModifier.field(UnimgrProvider.class, "uniListener").set(unimgrProvider, uniListener);
        MemberModifier.field(UnimgrProvider.class, "evcListener").set(unimgrProvider, evcListener);
        MemberModifier.field(UnimgrProvider.class, "ovsListener").set(unimgrProvider, ovsListener);
        console = unimgrProvider;
    }

    @Test
    public void testAddEvc() throws Exception {
        assertEquals(false, unimgrProvider.addEvc(any(EvcAugmentation.class)));
    }

    @Test
    public void testAddUni() throws Exception {
        PowerMockito.mockStatic(UniUtils.class);
        assertEquals(unimgrProvider.addUni(null), false);
        final UniAugmentation mockUniAug = mock(UniAugmentation.class);
        // false case
        when(mockUniAug.getIpAddress()).thenReturn(null);
        assertEquals(unimgrProvider.addUni(mockUniAug), false);
        when(mockUniAug.getMacAddress()).thenReturn(null);
        assertEquals(unimgrProvider.addUni(mockUniAug), false);
        // true case
        when(mockUniAug.getIpAddress()).thenReturn(mock(IpAddress.class));
        when(mockUniAug.getMacAddress()).thenReturn(mock(MacAddress.class));
        final UniAugmentationBuilder uniAugBuilder = new UniAugmentationBuilder()
                                                    .setIpAddress(mock(IpAddress.class))
                                                    .setMacAddress(mock(MacAddress.class));
        when(UniUtils.createUniNode(any(DataBroker.class),
                                       any(UniAugmentation.class)))
                        .thenReturn(true);
        assertEquals(true, unimgrProvider.addUni(uniAugBuilder.build()));
    }

    @Test
    public void testGetEvc() throws Exception {
        assertEquals(null, unimgrProvider.getEvc(any(String.class)));
    }

    @Test
    public void testgetUni() throws Exception {
        PowerMockito.mockStatic(UniUtils.class);
        final UniAugmentation mockUniAug = mock(UniAugmentation.class);
        when(UniUtils.getUni(any(DataBroker.class),
                                any(LogicalDatastoreType.class),
                                any(IpAddress.class)))
                        .thenReturn(mockUniAug);
        final IpAddress mockIpAddress = mock(IpAddress.class);
        assertEquals(mockUniAug, unimgrProvider.getUni(mockIpAddress));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInitDatastore() throws Exception {
        final ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        when(dataBroker.newReadWriteTransaction()).thenReturn(transaction);

        //suppress calls to initializeTopology()
        MemberModifier.suppress(MemberMatcher.method(UnimgrProvider.class,
                                                     "initializeTopology",
                                                     LogicalDatastoreType.class));

        final CheckedFuture<Optional<Topology>, ReadFailedException> unimgrTp = mock(CheckedFuture.class);
        when(transaction.read(any(LogicalDatastoreType.class),
                              any(InstanceIdentifier.class))).thenReturn(unimgrTp);

        //true case
        final Optional<Topology> optTopo = mock(Optional.class);
        when(unimgrTp.get()).thenReturn(optTopo);
        when(optTopo.isPresent()).thenReturn(false);
        final TopologyBuilder tpb = mock(TopologyBuilder.class);
        PowerMockito.whenNew(TopologyBuilder.class).withNoArguments().thenReturn(tpb);
        when(tpb.setTopologyId(any(TopologyId.class))).thenReturn(tpb);
        final Topology data = mock(Topology.class);
        when(tpb.build()).thenReturn(data);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(Topology.class));
        when(transaction.submit()).thenReturn(mock(CheckedFuture.class));

        final LogicalDatastoreType type = PowerMockito.mock(LogicalDatastoreType.class);
        final TopologyId mockTopoId = mock(TopologyId.class);
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

    @SuppressWarnings("unchecked")
    @Test
    public void testInitializeTopology() throws Exception {
        final InstanceIdentifier<NetworkTopology> path = mock(InstanceIdentifier.class);
        PowerMockito.mockStatic(InstanceIdentifier.class);
        when(InstanceIdentifier.create(NetworkTopology.class)).thenReturn(path);

        final CheckedFuture<Optional<NetworkTopology>, ReadFailedException> topology = mock(CheckedFuture.class);
        final ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        when(dataBroker.newReadWriteTransaction()).thenReturn(transaction);
        when(transaction.read(any(LogicalDatastoreType.class),
                              any(InstanceIdentifier.class))).thenReturn(topology);

        final Optional<NetworkTopology> optNetTopo = mock(Optional.class);
        when(topology.get()).thenReturn(optNetTopo);
        when(optNetTopo.isPresent()).thenReturn(false);
        final NetworkTopologyBuilder ntb = mock(NetworkTopologyBuilder.class);
        PowerMockito.whenNew(NetworkTopologyBuilder.class).withNoArguments().thenReturn(ntb);
        final NetworkTopology networkTopology = mock(NetworkTopology.class);
        when(ntb.build()).thenReturn(networkTopology);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(NetworkTopology.class));
        when(transaction.submit()).thenReturn(mock(CheckedFuture.class));

        final LogicalDatastoreType type = PowerMockito.mock(LogicalDatastoreType.class);
        Whitebox.invokeMethod(unimgrProvider, "initializeTopology", type);
        verify(ntb).build();
        verify(transaction).put(any(LogicalDatastoreType.class),
                                any(InstanceIdentifier.class),
                                any(NetworkTopology.class));
        verify(transaction).submit();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testlistUnis() throws Exception {
        PowerMockito.mockStatic(UniUtils.class);
        final List<UniAugmentation> mockUniList = mock(List.class);
        when(UniUtils.getUnis(any(DataBroker.class),
                                 any(LogicalDatastoreType.class)))
                        .thenReturn(mockUniList);
        assertEquals(mockUniList,
                     unimgrProvider.listUnis(any(LogicalDatastoreType.class)));
    }

    @Test
    public void testInit() throws Exception {
        PowerMockito.whenNew(UniDataTreeChangeListener.class).withArguments(any(DataBroker.class)).thenReturn(uniListener);
        PowerMockito.whenNew(EvcDataTreeChangeListener.class).withArguments(any(DataBroker.class)).thenReturn(evcListener);
        PowerMockito.whenNew(OvsNodeDataTreeChangeListener.class).withArguments(any(DataBroker.class)).thenReturn(ovsListener);
        MemberModifier.suppress(MemberMatcher.method(UnimgrProvider.class, "initDatastore"));
        unimgrProvider.init();
        verify(unimgrProvider, atLeast(4)).initDatastore(any(LogicalDatastoreType.class), any(TopologyId.class));
    }

    @Test
    public void testRemoveEvc() throws Exception {
        assertEquals(false, unimgrProvider.removeEvc(any(String.class)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveUni() throws Exception {
        PowerMockito.mockStatic(MdsalUtils.class);
        PowerMockito.mockStatic(UnimgrMapper.class);
        PowerMockito.mockStatic(InstanceIdentifier.class);

        // false case
        final IpAddress mockIpAddress = mock(IpAddress.class);
        PowerMockito.when(UnimgrMapper.getUniIid(any(DataBroker.class),
                                                 any(IpAddress.class),
                                                 any(LogicalDatastoreType.class)))
                         .thenReturn(null);
        assertEquals(false, unimgrProvider.removeUni(mockIpAddress));

        // true case
        final InstanceIdentifier<Node> iid = mock(InstanceIdentifier.class);
        final IpAddress ipAddress = new IpAddress(new Ipv4Address("192.168.1.1"));
        PowerMockito.when(UnimgrMapper.getUniIid(any(DataBroker.class),
                                                 any(IpAddress.class),
                                                 any(LogicalDatastoreType.class)))
                                      .thenReturn(iid);
        PowerMockito.when(MdsalUtils.deleteNode(any(DataBroker.class),
                                                 any(InstanceIdentifier.class),
                                                 any(LogicalDatastoreType.class)))
                                     .thenReturn(true);
        assertEquals(true, unimgrProvider.removeUni(ipAddress));
    }

    @Test
    public void testUpdateEvc() throws Exception {
        final UniSource uniSource = mock(UniSource.class);
        final UniDest uniDest = mock(UniDest.class);
        when(uniSource.getUni()).thenReturn(null);
        when(uniDest.getUni()).thenReturn(null);
        //TODO
    }

    @Test
    public void testUpdateUni() throws Exception {
        assertEquals(false, unimgrProvider.updateUni(null));
    }

}
