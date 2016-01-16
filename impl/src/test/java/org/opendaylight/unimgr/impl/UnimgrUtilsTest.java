package org.opendaylight.unimgr.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.api.mockito.PowerMockito;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Uni;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


@PrepareForTest({InstanceIdentifier.class, LogicalDatastoreType.class})
public class UnimgrUtilsTest {

    @Before
    public void setUp() throws Exception {
    }

    /*
     *  This test for 2 functions with the
     *  same name that take different parameters.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateBridgeNode() throws Exception{
        PowerMockito.mockStatic(InstanceIdentifier.class);
        PowerMockito.mockStatic(UnimgrMapper.class);
        Uni mockUni = mock(Uni.class);
        InstanceIdentifier<Node> mockNodeIid = mock(InstanceIdentifier.class);
        //when(mockUni.getOvsdbNodeRef().getValue().firstIdentifierOf(Node.class)).thenReturn(mockNodeIid);
        // true case
        NodeBuilder mockNodeBuilder = mock(NodeBuilder.class);
        PowerMockito.whenNew(NodeBuilder.class).withNoArguments().thenReturn(mockNodeBuilder);
        InstanceIdentifier<Node> mockBridgeIid = mock(InstanceIdentifier.class);
        when(UnimgrMapper.createOvsdbBridgeNodeIid(any(Node.class),
                                                   any(String.class))).thenReturn(mockBridgeIid);
        NodeId mockNodeId = mock(NodeId.class);
        PowerMockito.whenNew(NodeId.class).withAnyArguments().thenReturn(mockNodeId);
        mockNodeBuilder.setNodeId(mockNodeId);
        OvsdbBridgeAugmentationBuilder mockBridgeAug = mock(OvsdbBridgeAugmentationBuilder.class);
        OvsdbBridgeName mockBridgeName = mock(OvsdbBridgeName.class);
        mockBridgeAug.setBridgeName(mockBridgeName);
        List<ProtocolEntry> mockProtoEntry = mock(List.class);
        when(UnimgrUtils.createMdsalProtocols()).thenReturn(mockProtoEntry);
        mockBridgeAug.setProtocolEntry(mockProtoEntry);
        OvsdbNodeRef mockOvsdbNodeRef = mock(OvsdbNodeRef.class);
        PowerMockito.whenNew(OvsdbNodeRef.class).withAnyArguments().thenReturn(mockOvsdbNodeRef);
        mockBridgeAug.setManagedBy(mockOvsdbNodeRef);
        
    }

    @Test
    public void testCreateControllerEntries() {
      //TODO
    }

    @Test
    public void testCreateGreTunnel() {
      //TODO
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
