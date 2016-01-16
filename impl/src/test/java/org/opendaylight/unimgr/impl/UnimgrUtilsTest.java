package org.opendaylight.unimgr.impl;

import static org.junit.Assert.assertEquals;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryBuilder;
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
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateBridgeNode() throws Exception {
        DataBroker dataBroker = PowerMockito.mock(DataBroker.class);
        Node ovsdbNode = new NodeBuilder().setNodeId(mock(NodeId.class)).build();
        OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(PowerMockito.mock(InstanceIdentifier.class));
        UniAugmentation uni = new UniAugmentationBuilder().setOvsdbNodeRef(ovsdbNodeRef).build();
        String bridgeName = "br0";
        WriteTransaction transaction = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                                          any(InstanceIdentifier.class),
                                          any(Node.class));
        when(transaction.submit()).thenReturn(mock(CheckedFuture.class));
        MemberModifier.suppress(MemberMatcher.method(UnimgrMapper.class, "createOvsdbBridgeNodeIid", Node.class, String.class));
        UnimgrUtils.createBridgeNode(dataBroker, ovsdbNode, uni, bridgeName);
        verify(transaction).put(any(LogicalDatastoreType.class),
                                any(InstanceIdentifier.class),
                                any(Node.class));
        verify(transaction).submit();
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
        //UnimgrUtils.createMdsalProtocols()
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

    @SuppressWarnings("unchecked")
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
    }

    /*
     *  This test for 2 functions with the
     *  same name that take different parameters.
     */
    @Test
    public void testCreateOvsdbNode() {
      //TODO
    }

    @Test
    public void testCreateOvsdbNodeAugmentation() {
      //TODO
    }

    @Test
    public void testCreateOvsdbNodeId() {
      //TODO
    }

    @Test
    public void testCreateOvsdbTerminationPointAugmentation() {
      //TODO
    }

    @Test
    public void testCreateEvc() {
      //TODO
    }

    @Test
    public void testCreateUniNode() {
      //TODO
    }

    @Test
    public void testCreateUniNodeId() {
      //TODO
    }

    /*
     *  This test for 2 functions with the
     *  same name that take different parameters.
     */
    @Test
    public void testCreateTerminationPointNode() {
      //TODO
    }

    @Test
    public void testDelete() {
      //TODO
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
      //TODO
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
