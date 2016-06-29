/*
 * Copyright (c) 2016 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.utils.UniUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest({UniUtils.class})
@RunWith(PowerMockRunner.class)
public class UnimgrMapperTest {

    private static final String BRIDGE_NAME = "br-0";
    private static final String NODE_ID = "uni://10.0.0.1";
    private static final String OVSDB_TOPOLOGY_KEY = "ovsdb:1";
    private static final String EVC_TOPOLOGY_KEY = "unimgr:evc";
    private static final String UNI_TOPOLOGY_KEY = "unimgr:uni";
    private static final String PORT_NAME = "port 0";
    private static final String IPV4_ADDRESS = "10.0.0.1";

    @Before
    public void setUp() throws Exception {
        PowerMockito.whenNew(TopologyKey.class).withAnyArguments().thenReturn(mock(TopologyKey.class));
        PowerMockito.whenNew(NodeKey.class).withAnyArguments().thenReturn(mock(NodeKey.class));
    }

    @Test
    public void testCreateOvsdbBridgeNodeIid() throws Exception {
        Node ovsdbNode = mock(Node.class);
        NodeId nodeId = mock(NodeId.class);
        when(ovsdbNode.getNodeId()).thenReturn(nodeId);
        when(nodeId.getValue()).thenReturn(NODE_ID);
        String bridgeNodeName = NODE_ID
                + UnimgrConstants.DEFAULT_BRIDGE_NODE_ID_SUFFIX
                + BRIDGE_NAME;
        PowerMockito.whenNew(NodeId.class).withArguments(bridgeNodeName).thenReturn(nodeId);

        InstanceIdentifier<Node> iid = UnimgrMapper.createOvsdbBridgeNodeIid(ovsdbNode, BRIDGE_NAME);
        assertTrue(iid.firstKeyOf(Topology.class).getTopologyId().getValue().contains(OVSDB_TOPOLOGY_KEY));
        assertTrue(iid.firstKeyOf(Node.class).getNodeId().getValue().equalsIgnoreCase(bridgeNodeName));
    }

    @Test
    public void testGetEvcLinkIid() {
        LinkId id = mock(LinkId.class);
        InstanceIdentifier<Link> iid = UnimgrMapper.getEvcLinkIid(id);
        assertTrue(iid.firstKeyOf(Topology.class).getTopologyId().getValue().contains(EVC_TOPOLOGY_KEY));
    }

    @Test
    public void testGetEvcTopologyIid() {
        InstanceIdentifier<Topology> iid = UnimgrMapper.getEvcTopologyIid();
        assertTrue(iid.firstKeyOf(Topology.class).getTopologyId().getValue().contains(EVC_TOPOLOGY_KEY));
    }

    @Test
    public void testGetEvcTopologyNodeIid() throws Exception {
        InstanceIdentifier<Node> iid = UnimgrMapper.getEvcTopologyNodeIid();
        assertTrue(iid.firstKeyOf(Topology.class).getTopologyId().getValue().contains(EVC_TOPOLOGY_KEY));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetOvsdbBridgeNodeIid() {
        Node ovsdbNode = mock(Node.class);
        OvsdbNodeAugmentation ovsdbNodeAugmentation = mock(OvsdbNodeAugmentation.class, Mockito.RETURNS_DEEP_STUBS);
        when(ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsdbNodeAugmentation);
        List<ManagedNodeEntry> managedNodes = new ArrayList<>();
        ManagedNodeEntry managedNodeEntry = mock(ManagedNodeEntry.class);
        managedNodes.add(managedNodeEntry);
        when(ovsdbNodeAugmentation.getManagedNodeEntry()).thenReturn(managedNodes);
        OvsdbBridgeRef ovsdbBridgeRef = mock(OvsdbBridgeRef.class);
        when(managedNodeEntry.getBridgeRef()).thenReturn(ovsdbBridgeRef);

        @SuppressWarnings("rawtypes")
        InstanceIdentifier nodePath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class,
                        new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class,
                        new NodeKey(new NodeId(NODE_ID)));
        when(ovsdbBridgeRef.getValue()).thenReturn(nodePath);

        InstanceIdentifier<Node> iid = UnimgrMapper.getOvsdbBridgeNodeIid(ovsdbNode);
        assertNotNull(iid);
        verify(ovsdbNodeAugmentation).getManagedNodeEntry();
    }

    /*
     *  This test for 2 functions with the
     *  same name that take different parameters.
     */
    @Test
    public void testGetOvsdbNodeIid() throws Exception {
        IpAddress ipAddress = mock(IpAddress.class);
        Ipv4Address ipV4Address = mock(Ipv4Address.class);
        when(ipAddress.getIpv4Address()).thenReturn(ipV4Address);
        when(ipV4Address.getValue()).thenReturn(IPV4_ADDRESS);
        String nodeIdUri = UnimgrConstants.OVSDB_PREFIX
                + IPV4_ADDRESS
                + ":"
                + UnimgrConstants.OVSDB_PORT;
        NodeId nodeId = mock(NodeId.class);
        PowerMockito.whenNew(NodeId.class).withArguments(nodeIdUri).thenReturn(nodeId);

        //test method 1
        InstanceIdentifier<Node> iid = UnimgrMapper.getOvsdbNodeIid(ipAddress);
        assertTrue(iid.firstKeyOf(Topology.class).getTopologyId().getValue().contains(OVSDB_TOPOLOGY_KEY));
        assertTrue(iid.firstKeyOf(Node.class).getNodeId().getValue().equalsIgnoreCase(nodeIdUri));

        //test method 2
        iid = UnimgrMapper.getOvsdbNodeIid(nodeId);
        assertTrue(iid.firstKeyOf(Topology.class).getTopologyId().getValue().contains(OVSDB_TOPOLOGY_KEY));
        assertTrue(iid.firstKeyOf(Node.class).getNodeId().equals(nodeId));
    }

    @Test
    public void testGetOvsdbTopologyIid() throws Exception {
        InstanceIdentifier<Topology> iid = UnimgrMapper.getOvsdbTopologyIid();
        assertTrue(iid.firstKeyOf(Topology.class).getTopologyId().getValue().contains(OVSDB_TOPOLOGY_KEY));
    }

    /*
     *  This test for 2 functions with the
     *  same name that take different parameters.
     */
    @Test
    public void testGetTerminationPointIid() throws Exception {
        Node bridgeNode = mock(Node.class);
        TpId tpId = mock(TpId.class);

        PowerMockito.whenNew(TerminationPointKey.class).withAnyArguments().thenReturn(mock(TerminationPointKey.class));
        when(bridgeNode.getKey()).thenReturn(mock(NodeKey.class));
        PowerMockito.whenNew(TpId.class).withAnyArguments().thenReturn(mock(TpId.class));

        // test method 1
        InstanceIdentifier<TerminationPoint> iid = UnimgrMapper.getTerminationPointIid(bridgeNode, PORT_NAME);
        assertTrue(iid.firstKeyOf(Topology.class).getTopologyId().getValue().contains(OVSDB_TOPOLOGY_KEY));
        assertTrue(iid.firstKeyOf(TerminationPoint.class).getTpId().getValue().contains(PORT_NAME));

        // test method 2
        iid = UnimgrMapper.getTerminationPointIid(bridgeNode, tpId);
        assertTrue(iid.firstKeyOf(Topology.class).getTopologyId().getValue().contains(OVSDB_TOPOLOGY_KEY));
        assertTrue(iid.firstKeyOf(TerminationPoint.class).getTpId().equals(tpId));
    }

    /*
     *  This test for 2 functions with the
     *  same name that take different parameters.
     */
    @Test
    public void testGetUniIid() throws Exception {
        DataBroker dataBroker = mock(DataBroker.class);
        IpAddress ip = mock(IpAddress.class);

        List<Node> uniNodes = new ArrayList<>();
        Node node = mock(Node.class);
        uniNodes.add(node);

        PowerMockito.mockStatic(UniUtils.class);
        PowerMockito.when(UniUtils.getUniNodes(any(DataBroker.class), any(LogicalDatastoreType.class))).thenReturn(uniNodes);

        UniAugmentation uniAugmentation = mock(UniAugmentation.class);
        when(node.getAugmentation(UniAugmentation.class)).thenReturn(uniAugmentation);
        when(uniAugmentation.getIpAddress()).thenReturn(ip);
        when(node.getKey()).thenReturn(mock(NodeKey.class));

        // test method 1
        InstanceIdentifier<Node> iid = UnimgrMapper.getUniIid(dataBroker, ip, LogicalDatastoreType.CONFIGURATION);
        assertTrue(iid.firstKeyOf(Topology.class).getTopologyId().getValue().contains(UNI_TOPOLOGY_KEY));

        // test method 2
        iid = UnimgrMapper.getUniIid(dataBroker, ip);
        assertTrue(iid.firstKeyOf(Topology.class).getTopologyId().getValue().contains(UNI_TOPOLOGY_KEY));
    }

    @Test
    public void testGetUniTopologyIid() {
        InstanceIdentifier<Topology> iid = UnimgrMapper.getUniTopologyIid();
        assertTrue(iid.firstKeyOf(Topology.class).getTopologyId().getValue().contains(UNI_TOPOLOGY_KEY));
    }

    @Test
    public void testGetUniTopologyNodeIid() {
        InstanceIdentifier<Node> iid = UnimgrMapper.getUniTopologyNodeIid();
        assertTrue(iid.firstKeyOf(Topology.class).getTopologyId().getValue().contains(UNI_TOPOLOGY_KEY));
    }

    @Test
    public void testGetUniNodeIid() throws Exception {
        NodeId nodeId = mock(NodeId.class);
        PowerMockito.whenNew(NodeId.class).withAnyArguments().thenReturn(nodeId);

        InstanceIdentifier<Node> iid = UnimgrMapper.getUniNodeIid(nodeId);
        assertTrue(iid.firstKeyOf(Topology.class).getTopologyId().getValue().contains(UNI_TOPOLOGY_KEY));
        assertTrue(iid.firstKeyOf(Node.class).getNodeId().equals(nodeId));
    }
}
