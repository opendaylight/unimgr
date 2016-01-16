package org.opendaylight.unimgr.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import org.mockito.Mockito;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

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
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Uni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


@RunWith(PowerMockRunner.class)
@PrepareForTest({InstanceIdentifier.class, LogicalDatastoreType.class, UnimgrMapper.class, UnimgrUtils.class, UUID.class})
public class UnimgrUtilsTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

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
        // TODO
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
        DataBroker dataBroker = PowerMockito.mock(DataBroker.class);

        UniAugmentation sourceUniAug = new UniAugmentationBuilder()
                .setIpAddress(new IpAddress(new Ipv4Address("192.168.1.1")))
                .build();
        UniAugmentation destUniAug = new UniAugmentationBuilder()
                .setIpAddress(new IpAddress(new Ipv4Address("192.168.1.2")))
                .build();

        Node bridgeNode = PowerMockito.mock(Node.class);
        String bridgeName = PowerMockito.mock(String.class);
        String portName = PowerMockito.mock(String.class);

        WriteTransaction transaction = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(TerminationPoint.class));
        when(transaction.submit()).thenReturn(mock(CheckedFuture.class));

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
        DataBroker dataBroker = PowerMockito.mock(DataBroker.class);
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getOvsdbNodeIid", IpAddress.class));
        IpAddress mockIp = mock(IpAddress.class);
        InstanceIdentifier<Node> ovsdbNodeIid = PowerMockito.mock(InstanceIdentifier.class);
        PowerMockito.when(UnimgrMapper.getOvsdbNodeIid(mockIp)).thenReturn(ovsdbNodeIid);
        WriteTransaction transaction = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        NodeId ovsdbNodeId = new NodeId("abcde");
        Uni uni = new UniAugmentationBuilder().setIpAddress(new IpAddress(new Ipv4Address("192.168.1.2"))).build();
        // createOvsdbNode with NodeId and Uni
        UnimgrUtils.createOvsdbNode(dataBroker, ovsdbNodeId, uni);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(Node.class));
        verify(transaction).put(any(LogicalDatastoreType.class),
                                any(InstanceIdentifier.class),
                                any(Node.class));
        verify(transaction).submit();
        // Test with a null uni
        exception.expect(Exception.class);
        UnimgrUtils.createOvsdbNode(dataBroker, ovsdbNodeId, null);
        // createOvsdbNode with Uni
        UniAugmentation uniAug = new UniAugmentationBuilder(uni).build();
        UnimgrUtils.createOvsdbNode(dataBroker, uniAug);
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getOvsdbNodeIid", NodeId.class));
        PowerMockito.when(UnimgrMapper.getOvsdbNodeIid(ovsdbNodeId)).thenReturn(ovsdbNodeIid);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(Node.class));
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
        IpAddress ipAddress = new IpAddress(new Ipv4Address("192.168.1.2"));
        String nodeId = UnimgrConstants.OVSDB_PREFIX
                + ipAddress.getIpv4Address().getValue().toString()
                + ":"
                + UnimgrConstants.OVSDB_PORT;
        assertEquals(new NodeId(nodeId), UnimgrUtils.createOvsdbNodeId(ipAddress));
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
        DataBroker dataBroker = mock(DataBroker.class);
        EvcAugmentation evc = mock(EvcAugmentation.class);
        assertEquals(false, UnimgrUtils.createEvc(dataBroker, evc));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateUniNode() {
        DataBroker dataBroker = PowerMockito.mock(DataBroker.class);
        UniAugmentation uniAug = new UniAugmentationBuilder()
                                        .setIpAddress(new IpAddress(new Ipv4Address("192.168.1.2")))
                                        .build();
        // false case
        assertEquals(false, UnimgrUtils.createUniNode(dataBroker, uniAug));
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "getUniNodeIid", NodeId.class));
        InstanceIdentifier<Node> uniNodeIid = PowerMockito.mock(InstanceIdentifier.class);
        PowerMockito.when(UnimgrMapper.getUniNodeIid(any(NodeId.class))).thenReturn(uniNodeIid);
        WriteTransaction transaction = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        UnimgrUtils.createUniNode(dataBroker, uniAug);
        verify(transaction).put(any(LogicalDatastoreType.class),
                                any(InstanceIdentifier.class),
                                any(Node.class));
        verify(transaction).submit();
    }

    @Test
    public void testCreateUniNodeId() {
        IpAddress ipAddress = new IpAddress(new Ipv4Address("192.168.1.2"));
        NodeId nodeId = new NodeId(UnimgrConstants.UNI_PREFIX + ipAddress.getIpv4Address().getValue().toString());
        assertEquals(nodeId, UnimgrUtils.createUniNodeId(ipAddress));
    }

    /*
     *  This test for 2 functions with the
     *  same name that take different parameters.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateTerminationPointNode() {
        DataBroker dataBroker = mock(DataBroker.class);
        Uni uni = new UniAugmentationBuilder().build();
        InstanceIdentifier<TerminationPoint> tpIid = PowerMockito.mock(InstanceIdentifier.class);
        Node bridgeNode = new NodeBuilder().build();
        String bridgeName = "br0";
        String portName = "tp1";
        String type = "gre";
        WriteTransaction transaction = mock(WriteTransaction.class);
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
    public void testReadLink() {
      //TODO
    }

    @Test
    public void testReadNode() {
      //TODO
    }

    /*
     *  This test for 2 functions with the
     *  same name that take different parameters.
     */
    @Test
    public void testUpdateUniNode() {
      //TODO
    }

    @Test
    public void testUpdateEvcNode() {
      //TODO
    }

}
