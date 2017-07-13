/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.EgressBw;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.IngressBw;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDestKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSourceKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import ch.qos.logback.core.Appender;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LogicalDatastoreType.class, UnimgrMapper.class, EvcUtils.class, MdsalUtils.class})
public class EvcUtilsTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();
    @Mock private DataBroker dataBroker;
    @Mock private Node bridgeNode;
    @Mock private String bridgeName;
    @Mock private String portName;
    @Mock private String type;
    @Mock private WriteTransaction transaction;
    @Mock private IpAddress mockIp;
    @SuppressWarnings("rawtypes")
    @Mock private Appender mockAppender;
    @SuppressWarnings({ "rawtypes" })
    @Mock private CheckedFuture checkedFuture;
    private ch.qos.logback.classic.Logger root;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(EvcUtils.class, Mockito.CALLS_REAL_METHODS);
        PowerMockito.mockStatic(MdsalUtils.class, Mockito.CALLS_REAL_METHODS);
        PowerMockito.mockStatic(UnimgrMapper.class, Mockito.CALLS_REAL_METHODS);
        PowerMockito.mockStatic(LogicalDatastoreType.class);
        root = (ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        // Check logger messages
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetEvcLinks() {
        Link link = mock(Link.class);
        List<Link> lnkList = new ArrayList<Link>();
        lnkList.add(link);
        Topology topology = mock (Topology.class);
        DataBroker dataBroker = mock(DataBroker.class);
        EvcAugmentation evcAugmentation = mock(EvcAugmentation.class, Mockito.RETURNS_MOCKS);
        InstanceIdentifier<Topology> topologyInstanceIdentifier = mock(InstanceIdentifier.class);
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getEvcTopologyIid"));
        when(UnimgrMapper.getEvcTopologyIid()).thenReturn(topologyInstanceIdentifier);
        MemberModifier.suppress(MemberMatcher.method(MdsalUtils.class, "read", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(MdsalUtils.read(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(topology);
        when(topology.getLink()).thenReturn(lnkList);
        when(link.getAugmentation(EvcAugmentation.class)).thenReturn(evcAugmentation);
        List<Link> expectedListLink = EvcUtils.getEvcLinks(dataBroker);
        assertNotNull(expectedListLink);
        assertEquals(expectedListLink.get(0), link);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateEvcNode() {
        LinkId id = new LinkId("abcde");
        InstanceIdentifier<?> evcKey = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class,
                        new TopologyKey(UnimgrConstants.EVC_TOPOLOGY_ID))
                .child(Link.class,
                        new LinkKey(id));
        InstanceIdentifier<?> sourceUniIid = PowerMockito.mock(InstanceIdentifier.class);
        InstanceIdentifier<?> destinationUniIid = PowerMockito.mock(InstanceIdentifier.class);
        WriteTransaction transaction = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        Short order = new Short("0");
        IpAddress ipAddreSource = new IpAddress("10.10.1.1".toCharArray());
        UniSource uniSource = new UniSourceBuilder()
                .setIpAddress(ipAddreSource)
                .setKey(new UniSourceKey(order))
                .setOrder(order)
                .build();
        List<UniSource> uniSourceList = new ArrayList<UniSource>();
        uniSourceList.add(uniSource);
        IpAddress ipAddreDest = new IpAddress("10.10.0.2".toCharArray());
        UniDest uniDest = new UniDestBuilder()
                .setOrder(order)
                .setKey(new UniDestKey(order))
                .setIpAddress(ipAddreDest)
                .build();
        List<UniDest> uniDestList = new ArrayList<UniDest>();
        uniDestList.add(uniDest);
        EgressBw egressBw = mock(EgressBw.class);
        IngressBw ingressBw = mock(IngressBw.class);
        EvcAugmentation evcAug = new EvcAugmentationBuilder()
                .setCosId(UnimgrConstants.EVC_PREFIX + 1)
                .setEgressBw(egressBw)
                .setIngressBw(ingressBw)
                .setUniDest(uniDestList)
                .setUniSource(uniSourceList)
                .build();
        Optional<Link> optionalEvcLink = mock(Optional.class);
        Link lnk = mock(Link.class);
        when(optionalEvcLink.isPresent()).thenReturn(true);
        when(optionalEvcLink.get()).thenReturn(lnk);
        MemberModifier.suppress(MemberMatcher.method(MdsalUtils.class, "readLink", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(MdsalUtils.readLink(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(optionalEvcLink);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class),
                any(Node.class));
        EvcUtils.updateEvcNode(LogicalDatastoreType.OPERATIONAL, evcKey, evcAug,
                sourceUniIid, destinationUniIid, dataBroker);
        verify(transaction).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class));
        verify(transaction).submit();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteEvcData() {
        DataBroker dataBroker = mock(DataBroker.class);
        Optional<Node> optionalUni = mock(Optional.class);
        when(optionalUni.isPresent()).thenReturn(true);
        Node node = mock(Node.class);
        when(optionalUni.get()).thenReturn(node);
        UniAugmentation uniAugmentation = mock(UniAugmentation.class);
        when(node.getAugmentation(UniAugmentation.class)).thenReturn(uniAugmentation);
        OvsdbNodeRef ovsdbNodeRef = mock(OvsdbNodeRef.class);
        InstanceIdentifier<Node> iid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId("uni://10.0.0.1")));
        when((InstanceIdentifier<Node>) ovsdbNodeRef.getValue()).thenReturn(iid);
        when(uniAugmentation.getOvsdbNodeRef()).thenReturn(ovsdbNodeRef);
        MemberModifier.suppress(MemberMatcher.method(MdsalUtils.class, "readNode", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        Optional<Node> optionalOvsdNode = mock(Optional.class);
        when(MdsalUtils.readNode(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(optionalOvsdNode);
        when(optionalOvsdNode.isPresent()).thenReturn(true);
        Node ovsdbNode = mock(Node.class);
        when(optionalOvsdNode.get()).thenReturn(ovsdbNode);
        OvsdbNodeAugmentation ovsdbNodeAugmentation = mock(OvsdbNodeAugmentation.class);
        when(ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsdbNodeAugmentation);
        List<ManagedNodeEntry> managedNodeEntryList = new ArrayList<>();
        ManagedNodeEntry managedNodeEntry = mock(ManagedNodeEntry.class);
        managedNodeEntryList.add(managedNodeEntry);
        when(ovsdbNodeAugmentation.getManagedNodeEntry()).thenReturn(managedNodeEntryList);
        OvsdbBridgeRef ovsdbBridgeRef = mock(OvsdbBridgeRef.class);
        when(managedNodeEntry.getBridgeRef()).thenReturn(ovsdbBridgeRef);
        InstanceIdentifier<Node> bridgeIid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId("ovsbr0")));
        when((InstanceIdentifier<Node>) ovsdbBridgeRef.getValue()).thenReturn(bridgeIid);
        MemberModifier.suppress(MemberMatcher.method(MdsalUtils.class, "readNode", DataBroker.class, InstanceIdentifier.class));
        Optional<Node> optBridgeNode = mock(Optional.class);
        when(MdsalUtils.readNode(any(DataBroker.class), any(InstanceIdentifier.class))).thenReturn(optBridgeNode);
        when(optBridgeNode.isPresent()).thenReturn(true);
        when(optBridgeNode.get()).thenReturn(bridgeNode);

        MemberModifier.suppress(MemberMatcher.method(MdsalUtils.class, "deleteNode", DataBroker.class, InstanceIdentifier.class,LogicalDatastoreType.class));
        when(MdsalUtils.deleteNode(any(DataBroker.class), any(InstanceIdentifier.class), any(LogicalDatastoreType.class))).thenReturn(true);
        PowerMockito.mockStatic(UnimgrMapper.class);
        when(UnimgrMapper.getTerminationPointIid(any(Node.class), anyString())).thenReturn(mock(InstanceIdentifier.class));
        when(UnimgrMapper.getTerminationPointIid(any(Node.class), anyString())).thenReturn(mock(InstanceIdentifier.class));
        EvcUtils.deleteEvcData(dataBroker, optionalUni);
        verify(optionalUni).isPresent();
        verify(optionalOvsdNode).isPresent();
    }
}
