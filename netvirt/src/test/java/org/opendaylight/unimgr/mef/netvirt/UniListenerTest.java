/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.PhysicalLayers;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.PhysicalLayersBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.Links;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.LinksBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.Link;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.Uni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.UniBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.uni.EvcUniCeVlans;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.uni.EvcUniCeVlansBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.uni.evc.uni.ce.vlans.EvcUniCeVlan;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.uni.evc.uni.ce.vlans.EvcUniCeVlanBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.Identifier45;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.VlanIdType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.util.concurrent.CheckedFuture;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ LogicalDatastoreType.class, EvcUniListener.class })
public class UniListenerTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private WriteTransaction transaction;
    private EvcUniListener uniListener;

    @Before
    public void setUp() {
        dataBroker = mock(DataBroker.class);
        uniListener = new EvcUniListener(dataBroker);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUniAdded() throws Exception {
        String uniId = "MMPOP1-ce0-Slot0-Port1";
        String deviceName = "ce0";
        String interfaceName = "GigabitEthernet-0-1";
        Uni uni = uni(uniId, deviceName, interfaceName);

        prepareWriteTransaction();

        Collection<DataTreeModification<Uni>> collection = new ArrayList();
        collection.add(TestHelper.getUniUni(null, uni, ModificationType.WRITE));

        uniListener.onDataTreeChanged(collection);

        Interface trunkInterface = NetvirtUtils.createTrunkInterface(uniId, interfaceName);

        verifyWriteInterface(trunkInterface);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUniWithVlansAdded() throws Exception {
        String uniId = "MMPOP1-ce0-Slot1-Port1";
        String deviceName = "ce0";
        String interfaceName = "GigabitEthernet-1-1";
        Uni uni = uni(uniId, deviceName, interfaceName, 3, 4);
        prepareWriteTransaction();

        Collection<DataTreeModification<Uni>> collection = new ArrayList();
        collection.add(TestHelper.getUniUni(null, uni, ModificationType.WRITE));
        uniListener.onDataTreeChanged(collection);

        Interface trunkInterface = NetvirtUtils.createTrunkInterface(uniId, interfaceName);
        verifyWriteInterface(trunkInterface);
        Interface vlan3Interface = NetvirtUtils.createTrunkMemberInterface(uniId + "#3", uniId, 3);
        verifyWriteInterface(vlan3Interface);
        Interface vlan4Interface = NetvirtUtils.createTrunkMemberInterface(uniId + "#4", uniId, 4);
        verifyWriteInterface(vlan4Interface);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUniRemoved() throws Exception {
        String uniId = "MMPOP1-ce0-Slot0-Port1";
        String deviceName = "ce0";
        String interfaceName = "GigabitEthernet-0-1";
        Uni uni = uni(uniId, deviceName, interfaceName);

        prepareWriteTransaction();

        Collection<DataTreeModification<Uni>> collection = new ArrayList();
        collection.add(TestHelper.getUniUni(uni, null, ModificationType.DELETE));

        uniListener.onDataTreeChanged(collection);

        verifyDeleteInterface(uniId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUniWithVlansRemoved() throws Exception {
        String uniId = "MMPOP1-ce0-Slot1-Port1";
        String deviceName = "ce0";
        String interfaceName = "GigabitEthernet-1-1";
        Uni uni = uni(uniId, deviceName, interfaceName, 3, 4);
        prepareWriteTransaction();

        Collection<DataTreeModification<Uni>> collection = new ArrayList();
        collection.add(TestHelper.getUniUni(uni, null, ModificationType.DELETE));

        uniListener.onDataTreeChanged(collection);


        verifyDeleteInterface(uniId);
        verifyDeleteInterface(uniId + "#3");
        verifyDeleteInterface(uniId + "#4");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUniInterfaceUpdated() throws Exception {
        String uniId = "MMPOP1-ce0-Slot0-Port1";
        String deviceName = "ce0";
        String origInterfaceName = "GigabitEthernet-0-1";
        String updatedInterfaceName = "GigabitEthernet-1-1";
        Uni origUni = uni(uniId, deviceName, origInterfaceName);
        Uni updatedUni = uni(uniId, deviceName, updatedInterfaceName);
        prepareWriteTransaction();

        Collection<DataTreeModification<Uni>> collection = new ArrayList();
        collection.add(TestHelper.getUniUni(origUni, updatedUni, ModificationType.SUBTREE_MODIFIED));

        uniListener.onDataTreeChanged(collection);

        Interface trunkInterface = NetvirtUtils.createTrunkInterface(uniId, updatedInterfaceName);
        verifyWriteInterface(trunkInterface);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUniWithVlanInterfaceUpdated() throws Exception {
        String uniId = "MMPOP1-ce0-Slot0-Port1";
        String deviceName = "ce0";
        String origInterfaceName = "GigabitEthernet-0-1";
        String updatedInterfaceName = "GigabitEthernet-1-1";
        Uni origUni = uni(uniId, deviceName, origInterfaceName, 3, 4);
        Uni updatedUni = uni(uniId, deviceName, updatedInterfaceName, 3, 4);
        prepareWriteTransaction();

        Collection<DataTreeModification<Uni>> collection = new ArrayList();
        collection.add(TestHelper.getUniUni(origUni, updatedUni, ModificationType.SUBTREE_MODIFIED));

        uniListener.onDataTreeChanged(collection);

        Interface trunkInterface = NetvirtUtils.createTrunkInterface(uniId, updatedInterfaceName);
        verifyWriteInterface(trunkInterface);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUniVlanUpdated() throws Exception {
        String uniId = "MMPOP1-ce0-Slot0-Port1";
        String deviceName = "ce0";
        String interfaceName = "GigabitEthernet-1-1";
        Uni origUni = uni(uniId, deviceName, interfaceName, 1, 2, 3);
        Uni updatedUni = uni(uniId, deviceName, interfaceName, 2, 3, 4, 5);
        prepareWriteTransaction();

        Collection<DataTreeModification<Uni>> collection = new ArrayList();
        collection.add(TestHelper.getUniUni(origUni, updatedUni, ModificationType.SUBTREE_MODIFIED));

        uniListener.onDataTreeChanged(collection);

        Interface vlan4Interface = NetvirtUtils.createTrunkMemberInterface(uniId + "#4", uniId, 4);
        verifyWriteInterface(vlan4Interface);
        Interface vlan5Interface = NetvirtUtils.createTrunkMemberInterface(uniId + "#5", uniId, 5);
        verifyWriteInterface(vlan5Interface);
        verifyDeleteInterface(uniId + "#1");

    }

    private void verifyWriteInterface(Interface iface) {
        verify(transaction).put(LogicalDatastoreType.CONFIGURATION, interfaceInstanceIdentifier(iface.getName()), iface,
                true);
        verify(transaction).submit();
    }

    private void verifyDeleteInterface(String interfaceName) {
        verify(transaction).delete(LogicalDatastoreType.CONFIGURATION, interfaceInstanceIdentifier(interfaceName));
        verify(transaction).submit();
    }

    @SuppressWarnings("unchecked")
    private void prepareWriteTransaction() {
        transaction = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        CheckedFuture<Void, TransactionCommitFailedException> future = mock(CheckedFuture.class);
        when(transaction.submit()).thenReturn(future);
    }

    private InstanceIdentifier<Interface> interfaceInstanceIdentifier(String interfaceName) {
        return InstanceIdentifier.builder(Interfaces.class).child(Interface.class, new InterfaceKey(interfaceName))
                .build();
    }

    private Uni uni(String uniId, String deviceName, String interfaceName, long... vlans) {
        Link link = new LinkBuilder().setDevice(new Identifier45(deviceName)).setInterface(interfaceName).build();
        Links links = new LinksBuilder().setLink(Arrays.asList(link)).build();
        PhysicalLayers physicalLayers = new PhysicalLayersBuilder().setLinks(links).build();
        UniBuilder uniBuilder = new UniBuilder().setUniId(new Identifier45(uniId)); //.setPhysicalLayers(physicalLayers);
                
                
        
        if (vlans != null) {
            List<EvcUniCeVlan> vlanList = new ArrayList<>();
            for (long vlan : vlans) {
                vlanList.add(ceVlan(vlan));
            }
            EvcUniCeVlans ceVlans = new EvcUniCeVlansBuilder().setEvcUniCeVlan(vlanList).build();
            uniBuilder.setEvcUniCeVlans(ceVlans);
        }

        return uniBuilder.build();
    }

    private EvcUniCeVlan ceVlan(long vlanId) {
        return new EvcUniCeVlanBuilder().setVid(new VlanIdType(vlanId)).build();
    }
}
