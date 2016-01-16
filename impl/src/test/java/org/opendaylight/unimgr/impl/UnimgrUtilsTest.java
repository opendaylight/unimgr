package org.opendaylight.unimgr.impl;

import static org.junit.Assert.assertEquals;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.isNull;

import org.mockito.Mock;
import org.mockito.Mockito;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.common.util.concurrent.CheckedFuture;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.SouthboundProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.impl.rev151012.modules.module.configuration.UnimgrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Uni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDestBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


@RunWith(PowerMockRunner.class)
@PrepareForTest({InstanceIdentifier.class, LogicalDatastoreType.class, UnimgrMapper.class, UnimgrUtils.class})
public class UnimgrUtilsTest {


    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(UnimgrUtils.class, Mockito.CALLS_REAL_METHODS);
        PowerMockito.mockStatic(UnimgrMapper.class, Mockito.CALLS_REAL_METHODS);
    }

    /*
     *  This test for 2 functions with the
     *  same name that take different parameters.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateBridgeNode() throws Exception{
//        PowerMockito.mockStatic(InstanceIdentifier.class, Mockito.RETURNS_MOCKS);
//        PowerMockito.mockStatic(UnimgrMapper.class, Mockito.RETURNS_MOCKS);
//        PowerMockito.mockStatic(UnimgrUtils.class, Mockito.RETURNS_MOCKS);
//        DataBroker mockDb = mock(DataBroker.class);
//        // true case
//        UniAugmentationBuilder uniAugBuilder = new UniAugmentationBuilder();
//        InstanceIdentifier<?> ovsdbIid = PowerMockito.mock(InstanceIdentifier.class);
//        OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(ovsdbIid);
//        uniAugBuilder.setOvsdbNodeRef(ovsdbNodeRef);
//        UniAugmentation uniAug = mock(UniAugmentation.class);
//
//        InstanceIdentifier mockIid = PowerMockito.mock(InstanceIdentifier.class);
//
//
//        PowerMockito.when(uniAug.getOvsdbNodeRef().getValue().firstIdentifierOf(Node.class)).thenReturn(mockIid);
//        NodeBuilder mockNodeBuilder = mock(NodeBuilder.class);
//        PowerMockito.whenNew(NodeBuilder.class).withNoArguments().thenReturn(mockNodeBuilder);
//        InstanceIdentifier<Node> mockBridgeIid = mock(InstanceIdentifier.class);
//        mockNodeBuilder.setNodeId(new NodeId("abcde"));
//        Node mockOvsdbNode = mock(Node.class);
//        PowerMockito.when(UnimgrMapper.createOvsdbBridgeNodeIid(eq(mockOvsdbNode), anyString())).thenReturn(mockBridgeIid);
//        NodeId mockNodeId = mock(NodeId.class);
//        PowerMockito.whenNew(NodeId.class).withAnyArguments().thenReturn(mockNodeId);
//        mockNodeBuilder.setNodeId(mockNodeId);
//        OvsdbBridgeAugmentationBuilder mockBridgeAug = mock(OvsdbBridgeAugmentationBuilder.class);
//        OvsdbBridgeName mockBridgeName = mock(OvsdbBridgeName.class);
//        mockBridgeAug.setBridgeName(mockBridgeName);
//        List<ProtocolEntry> mockProtoEntry = mock(List.class);
//        when(UnimgrUtils.createMdsalProtocols()).thenReturn(mockProtoEntry);
//        mockBridgeAug.setProtocolEntry(mockProtoEntry);
//        OvsdbNodeRef mockOvsdbNodeRef = mock(OvsdbNodeRef.class);
//        PowerMockito.whenNew(OvsdbNodeRef.class).withAnyArguments().thenReturn(mockOvsdbNodeRef);
//        mockBridgeAug.setManagedBy(mockOvsdbNodeRef);
//        mockNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, mockBridgeAug.build());
//        WriteTransaction mockTransact = mock(WriteTransaction.class);
//        when(mockDb.newWriteOnlyTransaction()).thenReturn(mockTransact);
//        doNothing().when(mockTransact).put(any(LogicalDatastoreType.class),
//                                           any(InstanceIdentifier.class),
//                                           any(Node.class));
//        when(mockTransact.submit()).thenReturn(mock(CheckedFuture.class));
//
//        String bridgeName = new String("br0");
//        Whitebox.invokeMethod(UnimgrUtils.class, "createBridgeNode", mockDb, mockOvsdbNode, uniAug, bridgeName);
//        verify(mockTransact).put(any(LogicalDatastoreType.class),
//                                 any(InstanceIdentifier.class),
//                                 any(Node.class));
//        verify(mockTransact).submit();
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
        PowerMockito.mockStatic(InstanceIdentifier.class);
        PowerMockito.mockStatic(LogicalDatastoreType.class);
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

        OptionsBuilder mockOptBuilder = mock(OptionsBuilder.class);
        Options destinationIp = new OptionsBuilder()
                .setOption("192.168.1.1")
                .setKey(mock(OptionsKey.class))
                .setValue("192.168.1.2")
                .build();
        PowerMockito.when(mockOptBuilder.setOption(anyString())).thenReturn(mockOptBuilder);
        PowerMockito.when(mockOptBuilder.setKey(any(OptionsKey.class))).thenReturn(mockOptBuilder);
        PowerMockito.when(mockOptBuilder.setValue(anyString())).thenReturn(mockOptBuilder);
        PowerMockito.when(mockOptBuilder.build()).thenReturn(destinationIp);
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
      //TODO
    }

    @Test
    public void testCreateOvsdbBridgeAugmentation() {
      //TODO
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
