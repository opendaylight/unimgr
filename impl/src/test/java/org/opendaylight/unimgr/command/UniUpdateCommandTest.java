/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.command;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.unimgr.utils.OvsdbUtils;
import org.opendaylight.unimgr.utils.UniUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.uni.Speed;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UniUtils.class, MdsalUtils.class, OvsdbUtils.class, UnimgrMapper.class})
public class UniUpdateCommandTest {

    private static final NodeId OVSDB_NODE_ID = new NodeId("ovsdb://7011db35-f44b-4aab-90f6-d89088caf9d8");

    private UniUpdateCommand uniUpdateCommand;
    private DataTreeModification<Node> uni;
    private DataBroker dataBroker;
    private Node uniNode;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(UniUtils.class);
        PowerMockito.mockStatic(MdsalUtils.class);
        PowerMockito.mockStatic(OvsdbUtils.class);
        PowerMockito.mockStatic(UnimgrMapper.class);
        dataBroker = mock(DataBroker.class);
        uniNode = mock(Node.class);
        uni = DataTreeModificationHelper.getUniNode(uniNode);
        uniUpdateCommand = new UniUpdateCommand(dataBroker, uni);
    }

    /**
     * Test method for {@link org.opendaylight.unimgr.command.UniUpdateCommand#execute()}.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testExecute() {
        final Optional<Node> optionalOvsdbNode = mock(Optional.class);
        final UniAugmentation uniAugmentation = mock(UniAugmentation.class);
        final OvsdbNodeAugmentation ovsdbNodeAugmentation = mock(OvsdbNodeAugmentation.class);
        final List<ManagedNodeEntry> managedNodeEntries = new ArrayList<ManagedNodeEntry>();
        final ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        final IpAddress ipAddress = mock(IpAddress.class);
        final Ipv4Address ipv4Address = mock(Ipv4Address.class);
        final OvsdbNodeRef ovsNodedRef = mock(OvsdbNodeRef.class);
        final Node node = mock(Node.class);
        final NodeId nodeId = mock(NodeId.class);
        final InstanceIdentifier uniKey = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(OVSDB_NODE_ID));

        // Case UNI1 : updatedUni.getOvsdbNodeRef() != null, optOvsdbNode.isPresent()
        when(uniNode.getAugmentation(UniAugmentation.class)).thenReturn(uniAugmentation);
        when(optionalOvsdbNode.isPresent()).thenReturn(true);
        when(optionalOvsdbNode.get()).thenReturn(node);
        when(uniAugmentation.getIpAddress()).thenReturn(ipAddress);
        when(uniAugmentation.getSpeed()).thenReturn(mock(Speed.class));
        when(uniAugmentation.getOvsdbNodeRef()).thenReturn(ovsNodedRef);
        when(ovsdbNodeAugmentation.getConnectionInfo()).thenReturn(connectionInfo);
        when(ovsdbNodeAugmentation.getManagedNodeEntry()).thenReturn(managedNodeEntries);
        when(connectionInfo.getRemoteIp()).thenReturn(ipAddress);
        when(ipAddress.getIpv4Address()).thenReturn(ipv4Address);
        when(ipv4Address.toString()).thenReturn("ipv4Address_test");
        when(ipv4Address.getValue()).thenReturn("ipv4AddressValue_test");
        when(ovsNodedRef.getValue()).thenReturn(uniKey);
        when(node.getAugmentation(any(Class.class))).thenReturn(uniAugmentation);
        when(node.getNodeId()).thenReturn(nodeId);
        when(nodeId.toString()).thenReturn("ovsdbNodeId_test");
        when(UniUtils.getUni(any(DataBroker.class), any(LogicalDatastoreType.class), any(IpAddress.class)))
                .thenReturn(uniAugmentation);
        when(MdsalUtils.readNode(any(DataBroker.class), any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class))).thenReturn(optionalOvsdbNode);
        when(OvsdbUtils.createQoSForOvsdbNode(any(DataBroker.class), any(UniAugmentation.class)))
            .thenReturn(null);
        when(UniUtils.updateUniNode(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(UniAugmentation.class), any(Node.class), any(DataBroker.class))).thenReturn(true);
        when(OvsdbUtils.findOvsdbNode(any(DataBroker.class), any(UniAugmentation.class)))
                .thenReturn(optionalOvsdbNode);
        when(MdsalUtils.deleteNode(any(DataBroker.class), any(InstanceIdentifier.class),
                any(LogicalDatastoreType.class))).thenReturn(true);
        when(UnimgrMapper.getUniIid(any(DataBroker.class), any(IpAddress.class),
                any(LogicalDatastoreType.class))).thenReturn(uniKey);
        verifyExecute(1, 1);

        // Case 2 : updatedUni.getOvsdbNodeRef() == null, optOvsdbNode.isPresent()
        when(optionalOvsdbNode.isPresent()).thenReturn(true);
        verifyExecute(2, 2);
    }

    private void verifyExecute(int qosTimes, int updateNodeTime) {
        uniUpdateCommand.execute();
        PowerMockito.verifyStatic(times(qosTimes));
        OvsdbUtils.createQoSForOvsdbNode(any(DataBroker.class), any(UniAugmentation.class));
        PowerMockito.verifyStatic(times(updateNodeTime));
        UniUtils.updateUniNode(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(UniAugmentation.class), any(Node.class), any(DataBroker.class));
    }

}
