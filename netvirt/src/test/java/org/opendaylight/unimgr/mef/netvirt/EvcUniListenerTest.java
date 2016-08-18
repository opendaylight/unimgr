/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

//package org.opendaylight.unimgr.mef.netvirt;
//
//import static org.mockito.Matchers.any;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.spy;
//import static org.mockito.Mockito.when;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.List;
//import java.util.concurrent.ExecutionException;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.Spy;
//import org.mockito.internal.stubbing.answers.Returns;
//import org.mockito.internal.stubbing.answers.ReturnsArgumentAt;
//import org.mockito.invocation.InvocationOnMock;
//import org.mockito.stubbing.Answer;
//import org.mockito.stubbing.OngoingStubbing;
//import org.opendaylight.controller.md.sal.binding.api.DataBroker;
//import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
//import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
//import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
//import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
//import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
//import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
//import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
//import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
//import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.CeVlans;
//import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.PhysicalLayers;
//import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.PhysicalLayersBuilder;
//import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.Links;
//import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.LinksBuilder;
//import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.Link;
//import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.LinkBuilder;
//import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.Uni;
//import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.UniBuilder;
//import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.uni.EvcUniCeVlans;
//import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.uni.EvcUniCeVlansBuilder;
//import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.uni.evc.uni.ce.vlans.EvcUniCeVlan;
//import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.uni.evc.uni.ce.vlans.EvcUniCeVlanBuilder;
//import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.Identifier45;
//import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.VlanIdType;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
//import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
//import org.powermock.core.classloader.annotations.PrepareForTest;
//import org.powermock.modules.junit4.PowerMockRunner;
//
//import com.google.common.base.Optional;
//import com.google.common.util.concurrent.CheckedFuture;
//
//import scala.collection.TraversableOnce.OnceCanBuildFrom;
//
//@RunWith(PowerMockRunner.class)
//@PrepareForTest({ LogicalDatastoreType.class, EvcUniListener.class })
//public class EvcUniListenerTest {
//
//    @Mock
//    private DataBroker dataBroker;
//    @Mock
//    private WriteTransaction transaction;
//    private EvcUniListener uniListener;
//
//    @Before
//    public void setUp() {
//        dataBroker = mock(DataBroker.class);
//        uniListener = new EvcUniListener(dataBroker);
//    }
//
//    @SuppressWarnings("unchecked")
//    @Test
//    public void testUniAdded() throws Exception {
//        String uniId = "MMPOP1-ce0-Slot0-Port1";
//        String deviceName = "ce0";
//        String interfaceName = "GigabitEthernet-0-1";
//        Uni uni = evcUni(uniId, deviceName, interfaceName);
//
//        prepareWriteTransaction();
//
//        Collection<DataTreeModification<Uni>> collection = new ArrayList();
//        collection.add(TestHelper.getUni(null, uni, ModificationType.WRITE));
//
//        prepareReadTransaction(uniId, deviceName, interfaceName);
//
//        uniListener.onDataTreeChanged(collection);
//
//        Interface trunkInterface = NetvirtUtils.createTrunkInterface(getInterfaceName(deviceName, uniId),
//                getInterfaceName(deviceName, interfaceName));
//
//        verifyWriteInterface(trunkInterface);
//    }
//
//    @SuppressWarnings("unchecked")
//    @Test
//    public void testUniWithVlansAdded() throws Exception {
//        String uniId = "MMPOP1-ce0-Slot1-Port1";
//        String deviceName = "ce0";
//        String interfaceName = "GigabitEthernet-1-1";
//        Uni uni = evcUni(uniId, deviceName, interfaceName, 3, 4);
//        prepareWriteTransaction();
//        prepareReadTransaction(uniId, deviceName, interfaceName);
//
//        Collection<DataTreeModification<Uni>> collection = new ArrayList();
//        collection.add(TestHelper.getUni(null, uni, ModificationType.WRITE));
//        uniListener.onDataTreeChanged(collection);
//
//        uniId = getInterfaceName(deviceName, uniId);
//        Interface trunkInterface = NetvirtUtils.createTrunkInterface(uniId,
//                getInterfaceName(deviceName, interfaceName));
//        verifyWriteInterface(trunkInterface);
//        Interface vlan3Interface = NetvirtUtils.createTrunkMemberInterface(uniId + ".3", uniId, 3);
//        verifyWriteInterface(vlan3Interface);
//        Interface vlan4Interface = NetvirtUtils.createTrunkMemberInterface(uniId + ".4", uniId, 4);
//        verifyWriteInterface(vlan4Interface);
//    }
//
//    @SuppressWarnings("unchecked")
//    @Test
//    public void testUniRemoved() throws Exception {
//        String uniId = "MMPOP1-ce0-Slot0-Port1";
//        String deviceName = "ce0";
//        String interfaceName = "GigabitEthernet-0-1";
//        Uni uni = evcUni(uniId, deviceName, interfaceName);
//
//        prepareWriteTransaction();
//        prepareReadTransaction(uniId, deviceName, interfaceName);
//
//        Collection<DataTreeModification<Uni>> collection = new ArrayList();
//        collection.add(TestHelper.getUni(uni, null, ModificationType.DELETE));
//
//        uniListener.onDataTreeChanged(collection);
//        
//        verifyDeleteInterface(getInterfaceName(deviceName, uniId));
//    }
//
//    @SuppressWarnings("unchecked")
//    @Test
//    public void testUniWithVlansRemoved() throws Exception {
//        String uniId = "MMPOP1-ce0-Slot1-Port1";
//        String deviceName = "ce0";
//        String interfaceName = "GigabitEthernet-1-1";
//        Uni uni = evcUni(uniId, deviceName, interfaceName, 3, 4);
//        prepareWriteTransaction();
//        prepareReadTransaction(uniId, deviceName, interfaceName);
//
//        Collection<DataTreeModification<Uni>> collection = new ArrayList();
//        collection.add(TestHelper.getUni(uni, null, ModificationType.DELETE));
//
//        uniListener.onDataTreeChanged(collection);
//
//        interfaceName = getInterfaceName(deviceName, uniId);
//        verifyDeleteInterface(interfaceName);
//        verifyDeleteInterface(interfaceName + ".3");
//        verifyDeleteInterface(interfaceName + ".4");
//    }
//
//    @SuppressWarnings("unchecked")
//    @Test
//    public void testUniInterfaceUpdated() throws Exception {
//        String uniId = "MMPOP1-ce0-Slot0-Port1";
//        String deviceName = "ce0";
//        String origInterfaceName = "GigabitEthernet-0-1";
//        String updatedInterfaceName = "GigabitEthernet-1-1";
//        Uni origUni = evcUni(uniId, deviceName, origInterfaceName);
//        Uni updatedUni = evcUni(uniId, deviceName, updatedInterfaceName);
//        prepareWriteTransaction();
//        prepareReadTransaction(uniId, deviceName, origInterfaceName, updatedInterfaceName);
//
//        Collection<DataTreeModification<Uni>> collection = new ArrayList();
//        collection.add(TestHelper.getUni(origUni, updatedUni, ModificationType.SUBTREE_MODIFIED));
//
//        uniListener.onDataTreeChanged(collection);
//
//        Interface trunkInterface = NetvirtUtils.createTrunkInterface(uniId,
//                getInterfaceName(deviceName, updatedInterfaceName));
//        verifyWriteInterface(trunkInterface);
//    }
//
//    @SuppressWarnings("unchecked")
//    @Test
//    public void testUniWithVlanInterfaceUpdated() throws Exception {
//        String uniId = "MMPOP1-ce0-Slot0-Port1";
//        String deviceName = "ce0";
//        String origInterfaceName = "GigabitEthernet-0-1";
//        String updatedInterfaceName = "GigabitEthernet-1-1";
//        Uni origUni = evcUni(uniId, deviceName, origInterfaceName, 3, 4);
//        Uni updatedUni = evcUni(uniId, deviceName, updatedInterfaceName, 3, 4);
//        prepareWriteTransaction();
//        prepareReadTransaction(uniId, deviceName, origInterfaceName, updatedInterfaceName);
//
//        Collection<DataTreeModification<Uni>> collection = new ArrayList();
//        collection.add(TestHelper.getUni(origUni, updatedUni, ModificationType.SUBTREE_MODIFIED));
//
//        uniListener.onDataTreeChanged(collection);
//
//        Interface trunkInterface = NetvirtUtils.createTrunkInterface(uniId,
//                getInterfaceName(deviceName, updatedInterfaceName));
//        verifyWriteInterface(trunkInterface);
//    }
//
//    @SuppressWarnings("unchecked")
//    @Test
//    public void testUniVlanUpdated() throws Exception {
//        String uniId = "MMPOP1-ce0-Slot0-Port1";
//        String deviceName = "ce0";
//        String interfaceName = "GigabitEthernet-1-1";
//        Uni origUni = evcUni(uniId, deviceName, interfaceName, 1, 2, 3);
//        Uni updatedUni = evcUni(uniId, deviceName, interfaceName, 2, 3, 4, 5);
//        prepareWriteTransaction();
//        prepareReadTransaction(uniId, deviceName, interfaceName);
//
//        Collection<DataTreeModification<Uni>> collection = new ArrayList();
//        collection.add(TestHelper.getUni(origUni, updatedUni, ModificationType.SUBTREE_MODIFIED));
//
//        uniListener.onDataTreeChanged(collection);
//
//        Interface vlan4Interface = NetvirtUtils.createTrunkMemberInterface(getInterfaceName(deviceName, uniId) + ".4",
//                getInterfaceName(deviceName, uniId), 4);
//        verifyWriteInterface(vlan4Interface);
//        Interface vlan5Interface = NetvirtUtils.createTrunkMemberInterface(getInterfaceName(deviceName, uniId) + ".5",
//                getInterfaceName(deviceName, uniId), 5);
//        verifyWriteInterface(vlan5Interface);
//        verifyDeleteInterface(getInterfaceName(deviceName, uniId) + ".1");
//
//    }
//
//    private void verifyWriteInterface(Interface iface) {
//        verify(transaction).put(LogicalDatastoreType.CONFIGURATION, interfaceInstanceIdentifier(iface.getName()), iface,
//                true);
//        verify(transaction).submit();
//    }
//
//    private void verifyDeleteInterface(String interfaceName) {
//        verify(transaction).delete(LogicalDatastoreType.CONFIGURATION, interfaceInstanceIdentifier(interfaceName));
//        verify(transaction).submit();
//    }
//
//    @SuppressWarnings("unchecked")
//    private void prepareWriteTransaction() {
//        transaction = mock(WriteTransaction.class);
//        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
//        CheckedFuture<Void, TransactionCommitFailedException> future = mock(CheckedFuture.class);
//        when(transaction.submit()).thenReturn(future);
//    }
//
//    @SuppressWarnings("unchecked")
//    private void prepareReadTransaction(String uniId, String deviceName, String... interfaceNames)
//            throws InterruptedException, ExecutionException {
//
//        ReadOnlyTransaction readTransaction = mock(ReadOnlyTransaction.class);
//        when(dataBroker.newReadOnlyTransaction()).thenReturn(readTransaction);
//
//        CheckedFuture<Optional<org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni>, ReadFailedException> future = mock(
//                CheckedFuture.class);
//
//        OngoingStubbing<Optional<org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni>> ongoingStubbing = when(
//                future.get());
//
//        for (String interfaceName : interfaceNames) {
//            ongoingStubbing = ongoingStubbing.thenReturn(Optional.of(uni(uniId, deviceName, interfaceName)));
//        }
//
//        when(readTransaction.read(LogicalDatastoreType.CONFIGURATION, MefUtils.getUniInstanceIdentifier(uniId)))
//                .thenReturn(future);
//    }
//
//    private InstanceIdentifier<Interface> interfaceInstanceIdentifier(String interfaceName) {
//        return InstanceIdentifier.builder(Interfaces.class).child(Interface.class, new InterfaceKey(interfaceName))
//                .build();
//    }
//
//    private org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni uni(
//            String uniId, String deviceName, String interfaceName) {
//        Link link = new LinkBuilder().setDevice(new Identifier45(deviceName)).setInterface(interfaceName).build();
//        Links links = new LinksBuilder().setLink(Arrays.asList(link)).build();
//        PhysicalLayers physicalLayers = new PhysicalLayersBuilder().setLinks(links).build();
//        org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.UniBuilder uniBuilder = new org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.UniBuilder()
//                .setUniId(new Identifier45(uniId)).setPhysicalLayers(physicalLayers);
//
//        return uniBuilder.build();
//    }
//
//    private Uni evcUni(String uniId, String deviceName, String interfaceName, long... vlans) {
//        UniBuilder uniBuilder = new UniBuilder().setUniId(new Identifier45(uniId));
//
//        if (vlans != null) {
//            List<EvcUniCeVlan> vlanList = new ArrayList<>();
//            for (long vlan : vlans) {
//                vlanList.add(ceVlan(vlan));
//            }
//            EvcUniCeVlans ceVlans = new EvcUniCeVlansBuilder().setEvcUniCeVlan(vlanList).build();
//            uniBuilder.setEvcUniCeVlans(ceVlans);
//        }
//
//        return uniBuilder.build();
//    }
//
//    private EvcUniCeVlan ceVlan(long vlanId) {
//        return new EvcUniCeVlanBuilder().setVid(vlanId).build();
//    }
//
//    private String getInterfaceName(String deviceName, String interfaceName) {
//        return deviceName + ":" + interfaceName;
//    }
//}
