package org.opendaylight.unimgr.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Matchers.argThat;
import org.mockito.Mock;
import org.mockito.Mockito;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.util.concurrent.CheckedFuture;

import ch.qos.logback.core.Appender;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.mockito.ArgumentMatcher;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;

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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes.VlanMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Uni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentationBuilder;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


@RunWith(PowerMockRunner.class)
@PrepareForTest({LogicalDatastoreType.class, UnimgrMapper.class, UnimgrUtils.class, UUID.class})
public class UnimgrUtilsTest {

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
        PowerMockito.mockStatic(UnimgrUtils.class, Mockito.CALLS_REAL_METHODS);
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
     *  This test for 2 functions with the
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
        UnimgrUtils.createBridgeNode(dataBroker, ovsdbNode, uni, "br0");
        verify(transaction).put(any(LogicalDatastoreType.class),
                                any(InstanceIdentifier.class),
                                any(Node.class));
        verify(transaction).submit();

        // Function 2
        MemberModifier.suppress(MemberMatcher.method(UnimgrUtils.class,
                                                     "readNode",
                                                     DataBroker.class,
                                                     LogicalDatastoreType.class,
                                                     InstanceIdentifier.class));
        Optional<Node> mockOptional = mock(Optional.class);
        when(UnimgrUtils.readNode(any(DataBroker.class),
                                  any(LogicalDatastoreType.class),
                                  any(InstanceIdentifier.class))).thenReturn(mockOptional);
        UnimgrUtils.createBridgeNode(dataBroker, nodeIid, uni, "br0");
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
        assertEquals(controllerEntries, UnimgrUtils.createControllerEntries(targetString));
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
        MemberModifier.suppress(MemberMatcher.method(UnimgrUtils.class, "createMdsalProtocols"));

        Node bNode = new NodeBuilder().setKey(new NodeKey(OVSDB_NODE_ID)).build();
        InstanceIdentifier<TerminationPoint> tpIid = InstanceIdentifier
                                                        .create(NetworkTopology.class)
                                                        .child(Topology.class,
                                                                new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                                                        .child(Node.class, bNode.getKey())
                                                        .child(TerminationPoint.class,
                                                                new TerminationPointKey(new TpId(portName)));
        when(UnimgrMapper.getTerminationPointIid(any(Node.class), any(String.class))).thenReturn(tpIid);
        UnimgrUtils.createGreTunnel(dataBroker,
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
        assertEquals(protocolList, UnimgrUtils.createMdsalProtocols());
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
        assertEquals(ovsdbNode, UnimgrUtils.createOvsdbBridgeAugmentation(uni));
        // Force an exception
        Uni ovsdbNodeRefNull = new UniAugmentationBuilder().setOvsdbNodeRef(null).build();
        exception.expect(Exception.class);
        UnimgrUtils.createOvsdbBridgeAugmentation(ovsdbNodeRefNull);
    }

    /*
     *  This test for 2 functions with the
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
        UnimgrUtils.createOvsdbNode(dataBroker, OVSDB_NODE_ID, uni);
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
        UnimgrUtils.createOvsdbNode(dataBroker, OVSDB_NODE_ID, null);
        // Ensure correct logging
        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
              return ((LoggingEvent)argument).getFormattedMessage().contains("Created and submitted a new OVSDB node");
            }
          }));
        // createOvsdbNode with Uni
        UniAugmentation uniAug = new UniAugmentationBuilder(uni).build();
        UnimgrUtils.createOvsdbNode(dataBroker, uniAug);
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
        UnimgrUtils.createOvsdbNode(dataBroker, null);
        exception.expect(Exception.class);
        UnimgrUtils.createOvsdbNode(dataBroker, null);
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
        assertEquals(ovsdbNode, UnimgrUtils.createOvsdbNodeAugmentation(uni));
    }

    @Test
    public void testCreateOvsdbNodeId() {
        String nodeId = UnimgrConstants.OVSDB_PREFIX
                + IP.getIpv4Address().getValue().toString()
                + ":"
                + UnimgrConstants.OVSDB_PORT;
        assertEquals(new NodeId(nodeId), UnimgrUtils.createOvsdbNodeId(IP));
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
        assertEquals(terminationPoint, UnimgrUtils.createOvsdbTerminationPointAugmentation(uni));
    }

    @Test
    public void testCreateEvc() {
        EvcAugmentation evc = mock(EvcAugmentation.class);
        assertEquals(false, UnimgrUtils.createEvc(dataBroker, evc));
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
        assertEquals(false, UnimgrUtils.createUniNode(dataBroker, uniAug));
        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
              return ((LoggingEvent)argument).getFormattedMessage().contains("Exception while creating Uni Node");
            }
          }));
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getUniNodeIid", NodeId.class));
        PowerMockito.when(UnimgrMapper.getUniNodeIid(any(NodeId.class))).thenReturn(MOCK_NODE_IID);

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        UnimgrUtils.createUniNode(dataBroker, uniAug);
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
        assertEquals(nodeId, UnimgrUtils.createUniNodeId(IP));
    }

    /*
     *  This test for 2 functions with the
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
        UnimgrUtils.createTerminationPointNode(dataBroker, uni, bNode, bridgeName, portName, type);

        //Function 2
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(TerminationPoint.class));
        when(transaction.submit()).thenReturn(checkedFuture);
        UnimgrUtils.createTerminationPointNode(dataBroker, uni, bridgeNode, bridgeName, portName);
        verify(transaction, times(2)).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(TerminationPoint.class));
        verify(transaction,times(2)).submit();
    }

    @Test
    public void testDelete() {
        // FIXME this function will be moved into an MdsalUtils class.
        // see bug: https://bugs.opendaylight.org/show_bug.cgi?id=5035
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

        UnimgrUtils.deleteTerminationPoint(dataBroker, terminationPoint, ovsdbNode);
        verify(transaction,times(2)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction,times(2)).submit();
        CheckedFuture<Void, TransactionCommitFailedException> mockCheckedFuture = mock(CheckedFuture.class);
        when(transaction.submit()).thenReturn(mockCheckedFuture);
        assertEquals(mockCheckedFuture, UnimgrUtils.deleteTerminationPoint(dataBroker, terminationPoint, ovsdbNode));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testDeleteNode() throws Exception {
        InstanceIdentifier<Node> genericNode = InstanceIdentifier
                                                   .create(NetworkTopology.class)
                                                   .child(Topology.class,
                                                           new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID))
                                                   .child(Node.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class),
                                             any(InstanceIdentifier.class));
        when(transaction.submit()).thenReturn(checkedFuture);
        assertEquals(true, UnimgrUtils.deleteNode(dataBroker, genericNode, LogicalDatastoreType.CONFIGURATION));
        verify(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction).submit();
        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
              return ((LoggingEvent)argument).getFormattedMessage().contains("Received a request to delete node");
            }
          }));
    }

    @Test
    public void testExtract() {
        Map<InstanceIdentifier<?>, DataObject> changes = new HashMap<>();
        Class<DataObject> klazz = DataObject.class;
        assertEquals(HashMap.class, UnimgrUtils.extract(changes, klazz).getClass());
    }

// FIXME
//    @SuppressWarnings("unchecked")
//    @Test
//    public void testExtractOriginal() {
//        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes = mock(AsyncDataChangeEvent.class);
//        Class<DataObject> klazz = DataObject.class;
//        Map<InstanceIdentifier<?>, DataObject> map = new HashMap<>();
//        when(changes.getOriginalData()).thenReturn(map);
//        when(UnimgrUtils.extract(any(Map.class),eq(DataObject.class))).thenReturn(map);
//        assertEquals(map, UnimgrUtils.extractOriginal(changes, klazz));
//    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExtractRemoved() {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes = mock(AsyncDataChangeEvent.class);
        Class<DataObject> klazz = DataObject.class;
        assertEquals(HashSet.class, UnimgrUtils.extractRemoved(changes, klazz).getClass());
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
        MemberModifier.suppress(MemberMatcher.method(UnimgrUtils.class,
                                                     "getOvsdbNodes",
                                                     DataBroker.class));
        when(UnimgrUtils.getOvsdbNodes(any(DataBroker.class))).thenReturn(ovsdbNodes);
        Optional<Node> optNode = Optional.of(node);
        assertEquals(optNode, UnimgrUtils.findOvsdbNode(dataBroker, uni));
    }

    @Test
    public void testFindUniNode() {
        DataBroker dataBroker = mock(DataBroker.class);
        IpAddress ipAddress = mock(IpAddress.class);
        UniAugmentation uniAugmentation = mock(UniAugmentation.class);
        List<Node> uniNodes = new ArrayList<Node>();
        Node nd = mock(Node.class);
        uniNodes.add(nd);
        MemberModifier.suppress(MemberMatcher.method(UnimgrUtils.class, "getUniNodes", DataBroker.class));
        when(UnimgrUtils.getUniNodes(any(DataBroker.class))).thenReturn(uniNodes);
        when(nd.getAugmentation(UniAugmentation.class)).thenReturn(uniAugmentation);
        when(uniAugmentation.getIpAddress()).thenReturn(ipAddress);
        Optional<Node> optNode = UnimgrUtils.findUniNode(dataBroker, ipAddress);
        assertNotNull(optNode);
        assertTrue(optNode.isPresent());
        uniNodes.remove(0);
        optNode = UnimgrUtils.findUniNode(dataBroker, ipAddress);
        assertTrue(Optional.absent() == Optional.absent());
    }

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
        MemberModifier.suppress(MemberMatcher.method(UnimgrUtils.class, "readNode", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(UnimgrUtils.readNode(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(optNode);
        when(optNode.isPresent()).thenReturn(true);
        when(optNode.get()).thenReturn(node);
        when(node.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsdbNodeAugmentation);
        when(ovsdbNodeAugmentation.getConnectionInfo()).thenReturn(connectionInfo);
        ConnectionInfo expectedConnInfo = UnimgrUtils.getConnectionInfo(dataBroker, ovsdbNodeId);
        assertNotNull(expectedConnInfo);
        assertEquals(expectedConnInfo, connectionInfo);
    }

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
        MemberModifier.suppress(MemberMatcher.method(UnimgrUtils.class, "read", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(UnimgrUtils.read(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(topology);
        when(topology.getLink()).thenReturn(lnkList);
        when(link.getAugmentation(EvcAugmentation.class)).thenReturn(evcAugmentation);
        List<Link> expectedListLink = UnimgrUtils.getEvcLinks(dataBroker);
        assertNotNull(expectedListLink);
        assertEquals(expectedListLink.get(0), link);
    }

    @Test
    public void testGetLocalIp() {
        String ip = "";
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            ip = "127.0.0.1";
        }
        IpAddress ipAddress = UnimgrUtils.getLocalIp();
        assertNotNull(ipAddress);
        String expectedIp = new String(ipAddress.getValue());
        assertTrue(expectedIp.equals(ip));
    }

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
        MemberModifier.suppress(MemberMatcher.method(UnimgrUtils.class, "read", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(UnimgrUtils.read(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(topology);
        when(topology.getNode()).thenReturn(ndList);
        when(node.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsNdAugmentation);
        List<Node> expectedListNnList = UnimgrUtils.getOvsdbNodes(dataBroker);
        assertNotNull(expectedListNnList);
        assertEquals(expectedListNnList.get(0), node);
    }

    @Test
    public void testGetUniNodes() {
        Node node = mock(Node.class);
        List<Node> ndList = new ArrayList<Node>();
        ndList.add(node);
        Topology topology = mock (Topology.class);
        DataBroker dataBroker = mock(DataBroker.class);
        UniAugmentation uniAugmentation = mock(UniAugmentation.class);
        InstanceIdentifier<Topology> topologyInstanceIdentifier = mock(InstanceIdentifier.class);
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getUniTopologyIid"));
        when(UnimgrMapper.getUniTopologyIid()).thenReturn(topologyInstanceIdentifier);
        MemberModifier.suppress(MemberMatcher.method(UnimgrUtils.class, "read", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(UnimgrUtils.read(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(topology);
        when(topology.getNode()).thenReturn(ndList);
        when(node.getAugmentation(UniAugmentation.class)).thenReturn(uniAugmentation);
        List<Node> expectedListNnList = UnimgrUtils.getUniNodes(dataBroker);
        assertNotNull(expectedListNnList);
        assertEquals(expectedListNnList, ndList);
    }

    @Test
    public void testGetUniNodes2() {
        Node node = mock(Node.class);
        List<Node> ndList = new ArrayList<Node>();
        ndList.add(node);
        Topology topology = mock (Topology.class);
        DataBroker dataBroker = mock(DataBroker.class);
        UniAugmentation uniAugmentation = mock(UniAugmentation.class);
        InstanceIdentifier<Topology> topologyInstanceIdentifier = mock(InstanceIdentifier.class);
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getUniTopologyIid"));
        when(UnimgrMapper.getUniTopologyIid()).thenReturn(topologyInstanceIdentifier);
        MemberModifier.suppress(MemberMatcher.method(UnimgrUtils.class, "read", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(UnimgrUtils.read(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(topology);
        when(topology.getNode()).thenReturn(ndList);
        when(node.getAugmentation(UniAugmentation.class)).thenReturn(uniAugmentation);
        List<Node> expectedListNnList = UnimgrUtils.getUniNodes(dataBroker, LogicalDatastoreType.OPERATIONAL);
        assertNotNull(expectedListNnList);
        assertEquals(expectedListNnList, ndList);
    }

    @Test
    public void testGetUnis() {
        Node node = mock(Node.class);
        List<Node> ndList = new ArrayList<Node>();
        ndList.add(node);
        Topology topology = mock (Topology.class);
        DataBroker dataBroker = mock(DataBroker.class);
        UniAugmentation uniAugmentation = mock(UniAugmentation.class);
        InstanceIdentifier<Topology> topologyInstanceIdentifier = mock(InstanceIdentifier.class);
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getUniTopologyIid"));
        when(UnimgrMapper.getUniTopologyIid()).thenReturn(topologyInstanceIdentifier);
        MemberModifier.suppress(MemberMatcher.method(UnimgrUtils.class, "read", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(UnimgrUtils.read(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(topology);
        when(topology.getNode()).thenReturn(ndList);
        when(node.getAugmentation(UniAugmentation.class)).thenReturn(uniAugmentation);
        List<UniAugmentation> expectedListUni = UnimgrUtils.getUnis(dataBroker, LogicalDatastoreType.CONFIGURATION);
        assertNotNull(expectedListUni);
        assertEquals(expectedListUni.iterator().next(), uniAugmentation);
    }

    @Test
    public void testGetUni() {
        Node node = mock(Node.class);
        List<Node> ndList = new ArrayList<Node>();
        ndList.add(node);
        Topology topology = mock (Topology.class);
        DataBroker dataBroker = mock(DataBroker.class);
        UniAugmentation uniAugmentation = mock(UniAugmentation.class);
        IpAddress ipAddreDest = new IpAddress("10.10.0.2".toCharArray());
        InstanceIdentifier<Topology> topologyInstanceIdentifier = mock(InstanceIdentifier.class);
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getUniTopologyIid"));
        when(UnimgrMapper.getUniTopologyIid()).thenReturn(topologyInstanceIdentifier);
        MemberModifier.suppress(MemberMatcher.method(UnimgrUtils.class, "read", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(UnimgrUtils.read(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(topology);
        when(topology.getNode()).thenReturn(ndList);
        when(node.getAugmentation(UniAugmentation.class)).thenReturn(uniAugmentation);
        when(uniAugmentation.getIpAddress()).thenReturn(ipAddreDest);
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getUniTopologyIid"));
        when(UnimgrMapper.getUniTopologyIid()).thenReturn(topologyInstanceIdentifier);
        UniAugmentation expectedUniAug = UnimgrUtils.getUni(dataBroker, LogicalDatastoreType.CONFIGURATION, ipAddreDest);
        assertNotNull(expectedUniAug);
        assertEquals(expectedUniAug, uniAugmentation);
    }

    @Test
    public void testRead() throws ReadFailedException {
        DataBroker dataBroker = mock(DataBroker.class);
        InstanceIdentifier<Node> nodeIid = PowerMockito.mock(InstanceIdentifier.class);
        ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(transaction);
        Optional<Node> optionalDataObject = mock(Optional.class);
        CheckedFuture<Optional<Node>, ReadFailedException> future = mock(CheckedFuture.class);
        Node nd = mock(Node.class);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(future);
        when(future.checkedGet()).thenReturn(optionalDataObject);
        when(optionalDataObject.isPresent()).thenReturn(true);
        when(optionalDataObject.get()).thenReturn(nd);
        Node expectedNode = UnimgrUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION, nodeIid);
        verify(transaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction).close();
        assertNotNull(expectedNode);
        assertEquals(expectedNode, nd);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadLink() throws ReadFailedException {
        LinkId linkId = new LinkId("evc://7011db35-f44b-4aab-90f6-d89088caf9d8");
        InstanceIdentifier<?> nodeIid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class,
                        new TopologyKey(UnimgrConstants.EVC_TOPOLOGY_ID))
                .child(Link.class,
                        new LinkKey(linkId));
        ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(transaction);
        CheckedFuture<Optional<Link>, ReadFailedException> linkFuture = mock(CheckedFuture.class);
        Optional<Link> optLink = mock(Optional.class);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(linkFuture);
        when(linkFuture.checkedGet()).thenReturn(optLink);
        Optional<Link> expectedOpt = UnimgrUtils.readLink(dataBroker, LogicalDatastoreType.CONFIGURATION, nodeIid);
        verify(transaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        assertNotNull(expectedOpt);
        assertEquals(expectedOpt, optLink);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadNode() throws ReadFailedException {
        InstanceIdentifier<?> nodeIid = InstanceIdentifier
                                            .create(NetworkTopology.class)
                                            .child(Topology.class,
                                                    new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID))
                                            .child(Node.class,
                                                    new NodeKey(OVSDB_NODE_ID));
        ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(transaction);
        CheckedFuture<Optional<Node>, ReadFailedException> nodeFuture = mock(CheckedFuture.class);
        Optional<Node> optNode = mock(Optional.class);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(nodeFuture);
        when(nodeFuture.checkedGet()).thenReturn(optNode);
        Optional<Node> expectedOpt = UnimgrUtils.readNode(dataBroker, LogicalDatastoreType.CONFIGURATION, nodeIid);
        verify(transaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        assertNotNull(expectedOpt);
        assertEquals(expectedOpt, optNode);
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
        MemberModifier.suppress(MemberMatcher.method(UnimgrUtils.class, "readNode", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(UnimgrUtils.readNode(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(optionalNode);
        UnimgrUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL, uniKey, uni, ovsdbNodeIid, dataBroker);
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
        MemberModifier.suppress(MemberMatcher.method(UnimgrUtils.class, "readNode", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(UnimgrUtils.readNode(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(optionalNode);
        UnimgrUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL, uniKey, uni, ovsdbNode, dataBroker);
        verify(transaction).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class));
        verify(transaction).submit();
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
        Link lnk = mock (Link.class);
        when(optionalEvcLink.isPresent()).thenReturn(true);
        when(optionalEvcLink.get()).thenReturn(lnk);
        MemberModifier.suppress(MemberMatcher.method(UnimgrUtils.class, "readLink", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(UnimgrUtils.readLink(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(optionalEvcLink);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(Node.class));
        UnimgrUtils.updateEvcNode(LogicalDatastoreType.OPERATIONAL, evcKey, evcAug,
                                        sourceUniIid, destinationUniIid, dataBroker);
        verify(transaction).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class));
        verify(transaction).submit();
    }

}
