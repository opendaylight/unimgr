package org.opendaylight.unimgr.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import org.mockito.Mock;
import org.mockito.Mockito;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.util.concurrent.CheckedFuture;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


@RunWith(PowerMockRunner.class)
@PrepareForTest({InstanceIdentifier.class, LogicalDatastoreType.class, UnimgrMapper.class, UnimgrUtils.class, UUID.class})
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
    @SuppressWarnings({ "rawtypes" })
    @Mock private CheckedFuture checkedFuture;
    @SuppressWarnings("unchecked")
    private static final InstanceIdentifier<TerminationPoint> tpIid = PowerMockito.mock(InstanceIdentifier.class);
    private static final IpAddress IP = new IpAddress(new Ipv4Address("192.168.1.2"));
    private static final NodeId OVSDB_NODE_ID = new NodeId("ovsdb://7011db35-f44b-4aab-90f6-d89088caf9d8");
    @SuppressWarnings("unchecked")
    private static final InstanceIdentifier<Node> MOCK_NODE_IID = PowerMockito.mock(InstanceIdentifier.class);

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(UnimgrUtils.class, Mockito.CALLS_REAL_METHODS);
        PowerMockito.mockStatic(UnimgrMapper.class, Mockito.CALLS_REAL_METHODS);
        PowerMockito.mockStatic(InstanceIdentifier.class);
        PowerMockito.mockStatic(LogicalDatastoreType.class);
        PowerMockito.mockStatic(UUID.class);
    }

    /*
     *  This test for 2 functions with the
     *  same name that take different parameters.
     */
    @Test
    public void testCreateBridgeNode() throws Exception {
        //TODO
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
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateOvsdbNode() {
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getOvsdbNodeIid", IpAddress.class));
        PowerMockito.when(UnimgrMapper.getOvsdbNodeIid(mockIp)).thenReturn(MOCK_NODE_IID);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        //when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        Uni uni = new UniAugmentationBuilder().setIpAddress(new IpAddress(new Ipv4Address("192.168.1.2"))).build();
        // createOvsdbNode with NodeId and Uni
        UnimgrUtils.createOvsdbNode(dataBroker, OVSDB_NODE_ID, uni);
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
        // createOvsdbNode with Uni
        UniAugmentation uniAug = new UniAugmentationBuilder(uni).build();
        UnimgrUtils.createOvsdbNode(dataBroker, uniAug);
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getOvsdbNodeIid", NodeId.class));
        PowerMockito.when(UnimgrMapper.getOvsdbNodeIid(OVSDB_NODE_ID)).thenReturn(MOCK_NODE_IID);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(Node.class));
        when(transaction.submit()).thenReturn(checkedFuture);
        verify(transaction).put(any(LogicalDatastoreType.class),
                                any(InstanceIdentifier.class),
                                any(Node.class));
        verify(transaction).submit();
        // try with a null uni
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

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateUniNode() {
        UniAugmentation uniAug = new UniAugmentationBuilder()
                                        .setIpAddress(new IpAddress(new Ipv4Address("192.168.1.2")))
                                        .build();
        // false case
        assertEquals(false, UnimgrUtils.createUniNode(dataBroker, uniAug));
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getUniNodeIid", NodeId.class));
        PowerMockito.when(UnimgrMapper.getUniNodeIid(any(NodeId.class))).thenReturn(MOCK_NODE_IID);

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        UnimgrUtils.createUniNode(dataBroker, uniAug);
        verify(transaction).put(any(LogicalDatastoreType.class),
                                any(InstanceIdentifier.class),
                                any(Node.class));
        verify(transaction).submit();
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
        // Function 1
        UnimgrUtils.createTerminationPointNode(dataBroker, uni, bridgeNode, bridgeName, portName, type);

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

    @Test
    public void testDeleteTerminationPoint() {
      //TODO
    }

    @Test
    public void testDeleteNode() {
      //TODO
    }

    @Test
    public void testExtract() {
        // FIXME this function will be moved into an MdsalUtils class.
        // see bug: https://bugs.opendaylight.org/show_bug.cgi?id=5035
    }

    @Test
    public void testExtractOriginal() {
      //TODO
    }

    @Test
    public void testExtractRemoved() {
      //TODO
    }

    @Test
    public void testFindOvsdbNode() {
      //TODO
    }

    @Test
    public void testFindUniNode() {
      //TODO
    }

    @Test
    public void testGetConnectionInfo() {
      //TODO
    }

    @Test
    public void testGetEvcLinks() {
      //TODO
    }

    @Test
    public void testGetLocalIp() {
      //TODO
    }

    @Test
    public void testGetOvsdbNodes() {
      //TODO
    }

    /*
     *  This test for 2 functions with the
     *  same name that take different parameters.
     */
    @Test
    public void testGetUniNodes() {
      //TODO
    }

    @Test
    public void testGetUnis() {
      //TODO
    }

    @Test
    public void testGetUni() {
      //TODO
    }

    @Test
    public void testRead() {
      //TODO
    }

    @Test
    public void testReadLink() throws ReadFailedException {
        DataBroker dataBroker = mock(DataBroker.class);
        InstanceIdentifier<?> nodeIid = PowerMockito.mock(InstanceIdentifier.class);
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

    @Test
    public void testReadNode() throws ReadFailedException {
        DataBroker dataBroker = mock(DataBroker.class);
        InstanceIdentifier<?> nodeIid = PowerMockito.mock(InstanceIdentifier.class);
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

    @Test
    public void testUpdateUniNode() {
        DataBroker dataBroker = mock(DataBroker.class);
        UniAugmentation uni = PowerMockito.mock(UniAugmentation.class);
        InstanceIdentifier<?> uniKey = PowerMockito.mock(InstanceIdentifier.class);
        InstanceIdentifier<Node> ovsdbNodeIid = mock(InstanceIdentifier.class);
        Optional<Node> optionalNode = mock(Optional.class, Mockito.RETURNS_MOCKS);
        Node nd = mock(Node.class, Mockito.RETURNS_MOCKS);
        when(optionalNode.isPresent()).thenReturn(true);
        when(optionalNode.get()).thenReturn(nd);
        WriteTransaction transaction = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        PowerMockito.suppress(MemberMatcher.method(UnimgrUtils.class, "readNode", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(UnimgrUtils.readNode(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(optionalNode);
        UnimgrUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL, uniKey, uni, ovsdbNodeIid, dataBroker);
        verify(transaction).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class));
        verify(transaction).submit();
    }

    @Test
    public void testUpdateUniNode2() {
        DataBroker dataBroker = mock(DataBroker.class);
        UniAugmentation uni = PowerMockito.mock(UniAugmentation.class);
        InstanceIdentifier<?> uniKey = PowerMockito.mock(InstanceIdentifier.class);
        Node ovsdbNode = mock(Node.class);
        InstanceIdentifier<Node> ovsdbNodeIid = mock(InstanceIdentifier.class);
        Optional<Node> optionalNode = mock(Optional.class, Mockito.RETURNS_MOCKS);
        Node nd = mock(Node.class, Mockito.RETURNS_MOCKS);
        when(optionalNode.isPresent()).thenReturn(true);
        when(optionalNode.get()).thenReturn(nd);
        WriteTransaction transaction = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        PowerMockito.suppress(MemberMatcher.method(UnimgrMapper.class, "getOvsdbNodeIid", NodeId.class));
        when(UnimgrMapper.getOvsdbNodeIid(any(NodeId.class))).thenReturn(ovsdbNodeIid);
        PowerMockito.suppress(MemberMatcher.method(UnimgrUtils.class, "readNode", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(UnimgrUtils.readNode(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(optionalNode);
        UnimgrUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL, uniKey, uni, ovsdbNode, dataBroker);
        verify(transaction).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class));
        verify(transaction).submit();
    }

    @Test
    public void testUpdateEvcNode() {
        DataBroker dataBroker = mock(DataBroker.class);
        InstanceIdentifier<?> evcKey = PowerMockito.mock(InstanceIdentifier.class);
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
        Optional<Link> optionalEvcLink = mock(Optional.class, Mockito.RETURNS_MOCKS);
        Link lnk = mock (Link.class, Mockito.RETURNS_MOCKS);
        when(optionalEvcLink.isPresent()).thenReturn(true);
        when(optionalEvcLink.get()).thenReturn(lnk);
        PowerMockito.suppress(MemberMatcher.method(UnimgrUtils.class, "readLink", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        when(UnimgrUtils.readLink(any(DataBroker.class), any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(optionalEvcLink);
        UnimgrUtils.updateEvcNode(LogicalDatastoreType.OPERATIONAL, evcKey, evcAug,
                                        sourceUniIid, destinationUniIid, dataBroker);
        verify(transaction).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class));
        verify(transaction).submit();
    }

}
