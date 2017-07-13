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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
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
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
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

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LogicalDatastoreType.class, UnimgrMapper.class, UniUtils.class, MdsalUtils.class})
public class UniUtilsTest {

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
    private static final IpAddress IP = new IpAddress(new Ipv4Address("192.168.1.2"));
    private static final NodeId OVSDB_NODE_ID = new NodeId("ovsdb://7011db35-f44b-4aab-90f6-d89088caf9d8");
    @SuppressWarnings("unchecked")
    private static final InstanceIdentifier<Node> MOCK_NODE_IID = PowerMockito.mock(InstanceIdentifier.class);
    private ch.qos.logback.classic.Logger root;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(UniUtils.class, Mockito.CALLS_REAL_METHODS);
        PowerMockito.mockStatic(MdsalUtils.class, Mockito.CALLS_REAL_METHODS);
        PowerMockito.mockStatic(UnimgrMapper.class, Mockito.CALLS_REAL_METHODS);
        PowerMockito.mockStatic(LogicalDatastoreType.class);
        root = (ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        // Check logger messages
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testCreateUniNode() {
        UniAugmentation uniAug = new UniAugmentationBuilder()
                                        .setIpAddress(new IpAddress(new Ipv4Address("192.168.1.2")))
                                        .build();
        when(UnimgrMapper.getUniNodeIid(any(NodeId.class))).thenReturn(mock(InstanceIdentifier.class));
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(Node.class));
        when(transaction.submit()).thenReturn(checkedFuture);
        assertEquals(false, UniUtils.createUniNode(dataBroker, uniAug));
        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent)argument).getFormattedMessage().contains("Exception while creating Uni Node");
            }
          }));
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getUniNodeIid", NodeId.class));
        PowerMockito.when(UnimgrMapper.getUniNodeIid(any(NodeId.class))).thenReturn(MOCK_NODE_IID);

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        UniUtils.createUniNode(dataBroker, uniAug);
        verify(transaction).put(any(LogicalDatastoreType.class),
                                any(InstanceIdentifier.class),
                                any(Node.class));
        verify(transaction).submit();
        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent)argument).getFormattedMessage().contains("Created and submitted a new Uni");
            }
          }));
    }

    @Test
    public void testCreateUniNodeId() {
        NodeId nodeId = new NodeId(UnimgrConstants.UNI_PREFIX + IP.getIpv4Address().getValue().toString());
        assertEquals(nodeId, UniUtils.createUniNodeId(IP));
    }

    @Test
    public void testFindUniNode() {
        DataBroker dataBroker = mock(DataBroker.class);
        IpAddress ipAddress = mock(IpAddress.class);
        UniAugmentation uniAugmentation = mock(UniAugmentation.class);
        List<Node> uniNodes = new ArrayList<Node>();
        Node nd = mock(Node.class);
        uniNodes.add(nd);
        MemberModifier.suppress(MemberMatcher.method(UniUtils.class, "getUniNodes", DataBroker.class));
        when(UniUtils.getUniNodes(any(DataBroker.class))).thenReturn(uniNodes);
        when(nd.getAugmentation(UniAugmentation.class)).thenReturn(uniAugmentation);
        when(uniAugmentation.getIpAddress()).thenReturn(ipAddress);
        Optional<Node> optNode = UniUtils.findUniNode(dataBroker, ipAddress);
        assertNotNull(optNode);
        assertTrue(optNode.isPresent());
        uniNodes.remove(0);
        optNode = UniUtils.findUniNode(dataBroker, ipAddress);
        assertTrue(Optional.absent() == Optional.absent());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetUniNodes() {
        Node node = mock(Node.class);
        List<Node> ndList = new ArrayList<Node>();
        ndList.add(node);
        Topology topology = mock(Topology.class);
        DataBroker dataBroker = mock(DataBroker.class);
        UniAugmentation uniAugmentation = mock(UniAugmentation.class);
        InstanceIdentifier<Topology> topologyInstanceIdentifier = mock(InstanceIdentifier.class);
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getUniTopologyIid"));
        when(UnimgrMapper.getUniTopologyIid()).thenReturn(topologyInstanceIdentifier);
        MemberModifier.suppress(MemberMatcher.method(MdsalUtils.class, "read", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(MdsalUtils.read(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(topology);
        when(topology.getNode()).thenReturn(ndList);
        when(node.getAugmentation(UniAugmentation.class)).thenReturn(uniAugmentation);
        List<Node> expectedListNnList = UniUtils.getUniNodes(dataBroker);
        assertNotNull(expectedListNnList);
        assertEquals(expectedListNnList, ndList);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetUniNodes2() {
        Node node = mock(Node.class);
        List<Node> ndList = new ArrayList<Node>();
        ndList.add(node);
        Topology topology = mock(Topology.class);
        DataBroker dataBroker = mock(DataBroker.class);
        UniAugmentation uniAugmentation = mock(UniAugmentation.class);
        InstanceIdentifier<Topology> topologyInstanceIdentifier = mock(InstanceIdentifier.class);
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getUniTopologyIid"));
        when(UnimgrMapper.getUniTopologyIid()).thenReturn(topologyInstanceIdentifier);
        MemberModifier.suppress(MemberMatcher.method(MdsalUtils.class, "read", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(MdsalUtils.read(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(topology);
        when(topology.getNode()).thenReturn(ndList);
        when(node.getAugmentation(UniAugmentation.class)).thenReturn(uniAugmentation);
        List<Node> expectedListNnList = UniUtils.getUniNodes(dataBroker, LogicalDatastoreType.OPERATIONAL);
        assertNotNull(expectedListNnList);
        assertEquals(expectedListNnList, ndList);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetUnis() {
        Node node = mock(Node.class);
        List<Node> ndList = new ArrayList<Node>();
        ndList.add(node);
        Topology topology = mock(Topology.class);
        DataBroker dataBroker = mock(DataBroker.class);
        UniAugmentation uniAugmentation = mock(UniAugmentation.class);
        InstanceIdentifier<Topology> topologyInstanceIdentifier = mock(InstanceIdentifier.class);
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getUniTopologyIid"));
        when(UnimgrMapper.getUniTopologyIid()).thenReturn(topologyInstanceIdentifier);
        MemberModifier.suppress(MemberMatcher.method(MdsalUtils.class, "read", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(MdsalUtils.read(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(topology);
        when(topology.getNode()).thenReturn(ndList);
        when(node.getAugmentation(UniAugmentation.class)).thenReturn(uniAugmentation);
        List<UniAugmentation> expectedListUni = UniUtils.getUnis(dataBroker, LogicalDatastoreType.CONFIGURATION);
        assertNotNull(expectedListUni);
        assertEquals(expectedListUni.iterator().next(), uniAugmentation);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetUni() {
        Node node = mock(Node.class);
        List<Node> ndList = new ArrayList<Node>();
        ndList.add(node);
        Topology topology = mock(Topology.class);
        DataBroker dataBroker = mock(DataBroker.class);
        UniAugmentation uniAugmentation = mock(UniAugmentation.class);
        IpAddress ipAddreDest = new IpAddress("10.10.0.2".toCharArray());
        InstanceIdentifier<Topology> topologyInstanceIdentifier = mock(InstanceIdentifier.class);
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getUniTopologyIid"));
        when(UnimgrMapper.getUniTopologyIid()).thenReturn(topologyInstanceIdentifier);
        MemberModifier.suppress(MemberMatcher.method(MdsalUtils.class, "read", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(MdsalUtils.read(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(topology);
        when(topology.getNode()).thenReturn(ndList);
        when(node.getAugmentation(UniAugmentation.class)).thenReturn(uniAugmentation);
        when(uniAugmentation.getIpAddress()).thenReturn(ipAddreDest);
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getUniTopologyIid"));
        when(UnimgrMapper.getUniTopologyIid()).thenReturn(topologyInstanceIdentifier);
        UniAugmentation expectedUniAug = UniUtils.getUni(dataBroker, LogicalDatastoreType.CONFIGURATION, ipAddreDest);
        assertNotNull(expectedUniAug);
        assertEquals(expectedUniAug, uniAugmentation);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateUniNode() {
        UniAugmentation uni = PowerMockito.mock(UniAugmentation.class);
        InstanceIdentifier<?> uniKey = InstanceIdentifier
                                           .create(NetworkTopology.class)
                                           .child(Topology.class,
                                                   new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID))//Any node id is fine for tests
                                           .child(Node.class,
                                                   new NodeKey(OVSDB_NODE_ID));
        InstanceIdentifier<Node> ovsdbNodeIid = mock(InstanceIdentifier.class);
        Optional<Node> optionalNode = mock(Optional.class);
        Node nd = mock(Node.class, Mockito.RETURNS_MOCKS);
        when(optionalNode.isPresent()).thenReturn(true);
        when(optionalNode.get()).thenReturn(nd);
        WriteTransaction transaction = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(Node.class));
        MemberModifier.suppress(MemberMatcher.method(MdsalUtils.class, "readNode", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(MdsalUtils.readNode(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(optionalNode);
        UniUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL, uniKey, uni, ovsdbNodeIid, dataBroker);
        verify(transaction).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class));
        verify(transaction).submit();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateUniNode2() {
        UniAugmentation uni = PowerMockito.mock(UniAugmentation.class);
        InstanceIdentifier<?> uniKey = InstanceIdentifier
                                           .create(NetworkTopology.class)
                                           .child(Topology.class,
                                                   new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID))
                                           .child(Node.class,
                                                   new NodeKey(OVSDB_NODE_ID));//Any node id is fine for tests
        Node ovsdbNode = mock(Node.class);
        InstanceIdentifier<Node> ovsdbNodeIid = mock(InstanceIdentifier.class);
        Optional<Node> optionalNode = mock(Optional.class);
        Node nd = mock(Node.class);
        when(optionalNode.isPresent()).thenReturn(true);
        when(optionalNode.get()).thenReturn(nd);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(Node.class));
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getOvsdbNodeIid", NodeId.class));
        when(UnimgrMapper.getOvsdbNodeIid(any(NodeId.class))).thenReturn(ovsdbNodeIid);
        MemberModifier.suppress(MemberMatcher.method(MdsalUtils.class, "readNode", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(MdsalUtils.readNode(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(optionalNode);
        UniUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL, uniKey, uni, ovsdbNode, dataBroker);
        verify(transaction).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class));
        verify(transaction).submit();
    }
}
