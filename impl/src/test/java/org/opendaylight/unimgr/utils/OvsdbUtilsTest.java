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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes.VlanMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Uni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.util.concurrent.CheckedFuture;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LogicalDatastoreType.class, UnimgrMapper.class, OvsdbUtils.class, MdsalUtils.class, UUID.class})
public class OvsdbUtilsTest {

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
    @SuppressWarnings("unchecked")
    private static final InstanceIdentifier<TerminationPoint> tpIid = PowerMockito.mock(InstanceIdentifier.class);
    private static final IpAddress IP = new IpAddress(new Ipv4Address("192.168.1.2"));
    private static final NodeId OVSDB_NODE_ID = new NodeId("ovsdb://7011db35-f44b-4aab-90f6-d89088caf9d8");
    @SuppressWarnings("unchecked")
    private static final InstanceIdentifier<Node> MOCK_NODE_IID = PowerMockito.mock(InstanceIdentifier.class);
    private ch.qos.logback.classic.Logger root;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(OvsdbUtils.class, Mockito.CALLS_REAL_METHODS);
        PowerMockito.mockStatic(MdsalUtils.class, Mockito.CALLS_REAL_METHODS);
        PowerMockito.mockStatic(UnimgrMapper.class, Mockito.CALLS_REAL_METHODS);
        PowerMockito.mockStatic(LogicalDatastoreType.class);
        PowerMockito.mockStatic(UUID.class);
        root = (ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        // Check logger messages
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
    }

    /*
     *  This testAddCeps for 2 functions with the
     *  same name that take different parameters.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testCreateBridgeNode() throws Exception {
        // Function 1
        Node ovsdbNode = new NodeBuilder().setNodeId(OVSDB_NODE_ID).build();
        InstanceIdentifier<Node> nodeIid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class,
                        new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class,
                        new NodeKey(OVSDB_NODE_ID));
        OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(nodeIid);
        UniAugmentation uni = new UniAugmentationBuilder()
                                      .setIpAddress(new IpAddress(new Ipv4Address("192.168.1.1")))
                                      .setOvsdbNodeRef(ovsdbNodeRef)
                                      .build();

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(Node.class));
        when(transaction.submit()).thenReturn(checkedFuture);
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "createOvsdbBridgeNodeIid", Node.class, String.class));
        when(UnimgrMapper.createOvsdbBridgeNodeIid(any(Node.class),
                                                   any(String.class))).thenReturn(MOCK_NODE_IID);
        OvsdbUtils.createBridgeNode(dataBroker, ovsdbNode, uni, "br0");
        verify(transaction).put(any(LogicalDatastoreType.class),
                                any(InstanceIdentifier.class),
                                any(Node.class));
        verify(transaction).submit();

        // Function 2
        MemberModifier.suppress(MemberMatcher.method(MdsalUtils.class,
                                                     "readNode",
                                                     DataBroker.class,
                                                     LogicalDatastoreType.class,
                                                     InstanceIdentifier.class));
        Optional<Node> mockOptional = mock(Optional.class);
        when(MdsalUtils.readNode(any(DataBroker.class),
                                  any(LogicalDatastoreType.class),
                                  any(InstanceIdentifier.class))).thenReturn(mockOptional);
        OvsdbUtils.createBridgeNode(dataBroker, nodeIid, uni, "br0");
        verify(transaction).put(any(LogicalDatastoreType.class),
                                any(InstanceIdentifier.class),
                                any(Node.class));
        verify(transaction).submit();

        // Ensure correct logging
        verify(mockAppender, times(2)).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent)argument).getFormattedMessage().contains("Creating a bridge on node");
            }
          }));
    }

    @Test
    public void testCreateControllerEntries() {
        String targetString = new String("controllerEntry");
        List<ControllerEntry> controllerEntries = new ArrayList<ControllerEntry>();
        ControllerEntryBuilder controllerEntryBuilder = new ControllerEntryBuilder();
        controllerEntryBuilder.setTarget(new Uri(targetString));
        controllerEntries.add(controllerEntryBuilder.build());
        assertEquals(controllerEntries, OvsdbUtils.createControllerEntries(targetString));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateGreTunnel() throws Exception {

        UniAugmentation sourceUniAug = new UniAugmentationBuilder()
                .setIpAddress(new IpAddress(new Ipv4Address("192.168.1.1")))
                .build();
        UniAugmentation destUniAug = new UniAugmentationBuilder()
                .setIpAddress(new IpAddress(new Ipv4Address("192.168.1.2")))
                .build();

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(TerminationPoint.class));
        when(transaction.submit()).thenReturn(checkedFuture);

        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getTerminationPointIid", Node.class, String.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbUtils.class, "createMdsalProtocols"));

        Node bNode = new NodeBuilder().setKey(new NodeKey(OVSDB_NODE_ID)).build();
        InstanceIdentifier<TerminationPoint> tpIid = InstanceIdentifier
                                                        .create(NetworkTopology.class)
                                                        .child(Topology.class,
                                                                new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                                                        .child(Node.class, bNode.getKey())
                                                        .child(TerminationPoint.class,
                                                                new TerminationPointKey(new TpId(portName)));
        when(UnimgrMapper.getTerminationPointIid(any(Node.class), any(String.class))).thenReturn(tpIid);
        OvsdbUtils.createGreTunnel(dataBroker,
                                    sourceUniAug,
                                    destUniAug,
                                    bridgeNode,
                                    bridgeName,
                                    portName);
        verify(transaction).put(any(LogicalDatastoreType.class),
                                any(InstanceIdentifier.class),
                                any(TerminationPoint.class));
        verify(transaction).submit();
    }

    @Test
    public void testCreateMdsalProtocols() {
        List<ProtocolEntry> protocolList = new ArrayList<ProtocolEntry>();
        ImmutableBiMap<String, Class<? extends OvsdbBridgeProtocolBase>> mapper =
                SouthboundConstants.OVSDB_PROTOCOL_MAP.inverse();
        ProtocolEntry protoEntry = new ProtocolEntryBuilder().setProtocol((Class<? extends OvsdbBridgeProtocolBase>) mapper.get("OpenFlow13")).build();
        protocolList.add(protoEntry);
        assertEquals(protocolList, OvsdbUtils.createMdsalProtocols());
    }

    @Test
    public void testCreateOvsdbBridgeAugmentation() throws Exception {
        OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(PowerMockito.mock(InstanceIdentifier.class));
        UniAugmentation uni = new UniAugmentationBuilder().setOvsdbNodeRef(ovsdbNodeRef).build();
        UUID bridgeUuid = PowerMockito.mock(UUID.class);
        PowerMockito.when(UUID.randomUUID()).thenReturn(bridgeUuid);
        OvsdbBridgeAugmentation ovsdbNode = new OvsdbBridgeAugmentationBuilder()
                                                    .setBridgeName(new OvsdbBridgeName(UnimgrConstants.DEFAULT_BRIDGE_NAME))
                                                    .setManagedBy(ovsdbNodeRef)
                                                    .setBridgeUuid(new Uuid(bridgeUuid.toString()))
                                                    .build();
        assertEquals(ovsdbNode, OvsdbUtils.createOvsdbBridgeAugmentation(uni));
        // Force an exception
        Uni ovsdbNodeRefNull = new UniAugmentationBuilder().setOvsdbNodeRef(null).build();
        exception.expect(Exception.class);
        OvsdbUtils.createOvsdbBridgeAugmentation(ovsdbNodeRefNull);
    }

    /*
     *  This testAddCeps for 2 functions with the
     *  same name that take different parameters.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testCreateOvsdbNode() {
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getOvsdbNodeIid", IpAddress.class));
        PowerMockito.when(UnimgrMapper.getOvsdbNodeIid(mockIp)).thenReturn(MOCK_NODE_IID);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        //when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        Uni uni = new UniAugmentationBuilder().setIpAddress(new IpAddress(new Ipv4Address("192.168.1.2"))).build();
        // createOvsdbNode with NodeId and Uni
        OvsdbUtils.createOvsdbNode(dataBroker, OVSDB_NODE_ID, uni);
        // Ensure correct logging
        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent)argument).getFormattedMessage().contains("Created and submitted a new OVSDB node");
            }
          }));
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(Node.class));
        when(transaction.submit()).thenReturn(checkedFuture);
        verify(transaction).put(any(LogicalDatastoreType.class),
                                any(InstanceIdentifier.class),
                                any(Node.class));
        verify(transaction).submit();
        // Test with a null uni
        exception.expect(Exception.class);
        OvsdbUtils.createOvsdbNode(dataBroker, OVSDB_NODE_ID, null);
        // Ensure correct logging
        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent)argument).getFormattedMessage().contains("Created and submitted a new OVSDB node");
            }
          }));
        // createOvsdbNode with Uni
        UniAugmentation uniAug = new UniAugmentationBuilder(uni).build();
        OvsdbUtils.createOvsdbNode(dataBroker, uniAug);
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getOvsdbNodeIid", NodeId.class));
        PowerMockito.when(UnimgrMapper.getOvsdbNodeIid(any(NodeId.class))).thenReturn(MOCK_NODE_IID);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(Node.class));
        when(transaction.submit()).thenReturn(checkedFuture);
        verify(transaction).put(any(LogicalDatastoreType.class),
                                any(InstanceIdentifier.class),
                                any(Node.class));
        verify(transaction).submit();
        // try with a null uni
        PowerMockito.when(UnimgrMapper.getOvsdbNodeIid(any(NodeId.class))).thenReturn(null);
        OvsdbUtils.createOvsdbNode(dataBroker, null);
        exception.expect(Exception.class);
        OvsdbUtils.createOvsdbNode(dataBroker, null);
    }

    @Test
    public void testCreateOvsdbNodeAugmentation() {
        Uni uni = new UniAugmentationBuilder().setIpAddress(new IpAddress(new Ipv4Address("192.168.1.2"))).build();
        ConnectionInfo connectionInfos = new ConnectionInfoBuilder()
                .setRemoteIp(uni.getIpAddress())
                .setRemotePort(new PortNumber(UnimgrConstants.OVSDB_PORT))
                .build();
        OvsdbNodeAugmentation ovsdbNode = new OvsdbNodeAugmentationBuilder()
                .setConnectionInfo(connectionInfos).build();
        assertEquals(ovsdbNode, OvsdbUtils.createOvsdbNodeAugmentation(uni));
    }

    @Test
    public void testCreateOvsdbNodeId() {
        String nodeId = UnimgrConstants.OVSDB_PREFIX
                + IP.getIpv4Address().getValue().toString()
                + ":"
                + UnimgrConstants.OVSDB_PORT;
        assertEquals(new NodeId(nodeId), OvsdbUtils.createOvsdbNodeId(IP));
    }

    @Test
    public void testCreateOvsdbTerminationPointAugmentation() {
        Uni uni = new UniAugmentationBuilder().build();
        VlanId vlanID = new VlanId(1);
        OvsdbTerminationPointAugmentation terminationPoint = new OvsdbTerminationPointAugmentationBuilder()
                                                                     .setName(UnimgrConstants.DEFAULT_INTERNAL_IFACE)
                                                                     .setVlanTag(vlanID)
                                                                     .setVlanMode(VlanMode.Access)
                                                                     .build();
        assertEquals(terminationPoint, OvsdbUtils.createOvsdbTerminationPointAugmentation(uni));
    }

    /*
     *  This testAddCeps for 2 functions with the
     *  same name that take different parameters.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateTerminationPointNode() {
        Uni uni = new UniAugmentationBuilder().build();
        Node bridgeNode = new NodeBuilder().build();

        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class,
                                                     "getTerminationPointIid",
                                                     Node.class,
                                                     String.class));
        PowerMockito.when(UnimgrMapper.getTerminationPointIid(any(Node.class),
                                                              any(String.class))).thenReturn(tpIid);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        Node bNode = new NodeBuilder().setKey(new NodeKey(OVSDB_NODE_ID)).build();
        InstanceIdentifier<TerminationPoint> tpIid = InstanceIdentifier
                                                        .create(NetworkTopology.class)
                                                        .child(Topology.class,
                                                                new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                                                        .child(Node.class, bNode.getKey())
                                                        .child(TerminationPoint.class,
                                                                new TerminationPointKey(new TpId(portName)));
        when(UnimgrMapper.getTerminationPointIid(any(Node.class), any(String.class))).thenReturn(tpIid);
        // Function 1
        OvsdbUtils.createTerminationPointNode(dataBroker, uni, bNode, bridgeName, portName, type);

        //Function 2
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(TerminationPoint.class));
        when(transaction.submit()).thenReturn(checkedFuture);
        OvsdbUtils.createTerminationPointNode(dataBroker, uni, bridgeNode, bridgeName, portName);
        verify(transaction, times(2)).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(TerminationPoint.class));
        verify(transaction,times(2)).submit();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteTerminationPoint() {
        TerminationPointKey tpKey = new TerminationPointKey(new TpId("abcde"));
        TerminationPoint terminationPoint = new TerminationPointBuilder().setKey(tpKey).build();
        Node ovsdbNode = new NodeBuilder().setKey(new NodeKey(OVSDB_NODE_ID)).build();
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class),
                                             any(InstanceIdentifier.class));

        OvsdbUtils.deleteTerminationPoint(dataBroker, terminationPoint, ovsdbNode);
        verify(transaction,times(2)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction,times(1)).submit();
        CheckedFuture<Void, TransactionCommitFailedException> mockCheckedFuture = mock(CheckedFuture.class);
        when(transaction.submit()).thenReturn(mockCheckedFuture);
        assertEquals(mockCheckedFuture, OvsdbUtils.deleteTerminationPoint(dataBroker, terminationPoint, ovsdbNode));
    }

    @Test
    public void testExtract() {
        Map<InstanceIdentifier<?>, DataObject> changes = new HashMap<>();
        Class<DataObject> klazz = DataObject.class;
        assertEquals(HashMap.class, OvsdbUtils.extract(changes, klazz).getClass());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExtractOriginal() {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes = mock(AsyncDataChangeEvent.class);
        Class<DataObject> klazz = DataObject.class;
        Map<InstanceIdentifier<?>, DataObject> map = new HashMap<>();
        when(changes.getOriginalData()).thenReturn(map);
        Map<InstanceIdentifier<DataObject>, DataObject> map1 = new HashMap<>();
        when(OvsdbUtils.extract(any(Map.class),eq(DataObject.class))).thenReturn(map1);
        assertEquals(map1, OvsdbUtils.extractOriginal(changes, klazz));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExtractRemoved() {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes = mock(AsyncDataChangeEvent.class);
        Class<DataObject> klazz = DataObject.class;
        assertEquals(HashSet.class, OvsdbUtils.extractRemoved(changes, klazz).getClass());
    }

    @Test
    public void testFindOvsdbNode() {
        List<Node> ovsdbNodes = new ArrayList<Node>();
        UniAugmentation uni = new UniAugmentationBuilder()
                                      .setIpAddress(IP)
                                      .build();
        ConnectionInfo connInfo = new ConnectionInfoBuilder().setRemoteIp(IP).build();
        OvsdbNodeAugmentation augmentation = new OvsdbNodeAugmentationBuilder()
                                                     .setConnectionInfo(connInfo)
                                                     .build();
        Node node = new NodeBuilder().addAugmentation(OvsdbNodeAugmentation.class, augmentation).build();
        ovsdbNodes.add(node);
        MemberModifier.suppress(MemberMatcher.method(OvsdbUtils.class,
                                                     "getOvsdbNodes",
                                                     DataBroker.class));
        when(OvsdbUtils.getOvsdbNodes(any(DataBroker.class))).thenReturn(ovsdbNodes);
        Optional<Node> optNode = Optional.of(node);
        assertEquals(optNode, OvsdbUtils.findOvsdbNode(dataBroker, uni));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetConnectionInfo() {
        DataBroker dataBroker = mock(DataBroker.class);
        InstanceIdentifier<Node> nodeIid = PowerMockito.mock(InstanceIdentifier.class);
        NodeId ovsdbNodeId = mock(NodeId.class);
        Optional<Node> optNode = mock(Optional.class);
        Node node = mock(Node.class);
        OvsdbNodeAugmentation ovsdbNodeAugmentation = mock(OvsdbNodeAugmentation.class);
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getOvsdbNodeIid", NodeId.class));
        when(UnimgrMapper.getOvsdbNodeIid(any(NodeId.class))).thenReturn(nodeIid);
        MemberModifier.suppress(MemberMatcher.method(MdsalUtils.class, "readNode", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(MdsalUtils.readNode(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(optNode);
        when(optNode.isPresent()).thenReturn(true);
        when(optNode.get()).thenReturn(node);
        when(node.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsdbNodeAugmentation);
        when(ovsdbNodeAugmentation.getConnectionInfo()).thenReturn(connectionInfo);
        ConnectionInfo expectedConnInfo = OvsdbUtils.getConnectionInfo(dataBroker, ovsdbNodeId);
        assertNotNull(expectedConnInfo);
        assertEquals(expectedConnInfo, connectionInfo);
    }

    @Test
    public void testGetLocalIp() {
        String ip = "";
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            ip = "127.0.0.1";
        }
        IpAddress ipAddress = OvsdbUtils.getLocalIp();
        assertNotNull(ipAddress);
        String expectedIp = new String(ipAddress.getValue());
        assertTrue(expectedIp.equals(ip));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetOvsdbNodes() {
        Node node = mock(Node.class);
        List<Node> ndList = new ArrayList<Node>();
        ndList.add(node);
        Topology topology = mock (Topology.class);
        DataBroker dataBroker = mock(DataBroker.class);
        OvsdbNodeAugmentation ovsNdAugmentation = mock(OvsdbNodeAugmentation.class, Mockito.RETURNS_MOCKS);
        InstanceIdentifier<Topology> topologyInstanceIdentifier = mock(InstanceIdentifier.class);
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getOvsdbTopologyIid"));
        when(UnimgrMapper.getOvsdbTopologyIid()).thenReturn(topologyInstanceIdentifier);
        MemberModifier.suppress(MemberMatcher.method(MdsalUtils.class, "read", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(MdsalUtils.read(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(topology);
        when(topology.getNode()).thenReturn(ndList);
        when(node.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsNdAugmentation);
        List<Node> expectedListNnList = OvsdbUtils.getOvsdbNodes(dataBroker);
        assertNotNull(expectedListNnList);
        assertEquals(expectedListNnList.get(0), node);
    }

}
