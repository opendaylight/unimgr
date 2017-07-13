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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntriesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Queues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QueuesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
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
@PrepareForTest({UniUtils.class, OvsdbUtils.class, MdsalUtils.class,
        UnimgrMapper.class})
public class UniRemoveCommandTest {

    private static final NodeId OVSDB_NODE_ID = new NodeId("ovsdb://7011db35-f44b-4aab-90f6-d89088caf9d8");
    private UniRemoveCommand uniRemoveCommand;
    private DataTreeModification<Node> uni;
    private DataBroker dataBroker;
    private Node uniNode;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(UniUtils.class);
        PowerMockito.mockStatic(OvsdbUtils.class);
        PowerMockito.mockStatic(MdsalUtils.class);
        PowerMockito.mockStatic(UnimgrMapper.class);
        dataBroker = mock(DataBroker.class);
        uniNode = mock(Node.class);
        uni = DataTreeModificationHelper.getUniNode(uniNode);
        uniRemoveCommand = new UniRemoveCommand(dataBroker, uni);
    }

    /**
     * Test method for {@link org.opendaylight.unimgr.command.UniRemoveCommand#execute()}.
     */
    @SuppressWarnings({ "unchecked", "rawtypes"})
    @Test
    public void testExecute() {
        final UniAugmentation uniAugmentation = mock(UniAugmentation.class);
        final OvsdbNodeRef ovsNodedRef = mock(OvsdbNodeRef.class);
        final Optional<Node> optionalNode = mock(Optional.class);
        final InstanceIdentifier instanceOfNode = mock(InstanceIdentifier.class);
        final InstanceIdentifier uniKey = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID))
                .child(Node.class,  new NodeKey(OVSDB_NODE_ID));
        final Node ovsdbNode = mock(Node.class);
        final OvsdbNodeAugmentation ovsdbNodeAugmentation = mock(OvsdbNodeAugmentation.class);
        final QosEntries qosEntries = mock(QosEntries.class);
        final Queues queues = mock(Queues.class);
        when(uniNode.getAugmentation(UniAugmentation.class)).thenReturn(uniAugmentation);
        when(uniAugmentation.getIpAddress()).thenReturn(mock(IpAddress.class));
        when(uniAugmentation.getOvsdbNodeRef()).thenReturn(ovsNodedRef);
        when(ovsNodedRef.getValue()).thenReturn(uniKey);
        when(optionalNode.isPresent()).thenReturn(true);
        when(optionalNode.get()).thenReturn(ovsdbNode);
        when(ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsdbNodeAugmentation);
        List<QosEntries> qosEntriesList = new ArrayList<>();
        when(qosEntries.getKey()).thenReturn(mock(QosEntriesKey.class));
        qosEntriesList.add(qosEntries);
        when(ovsdbNodeAugmentation.getQosEntries()).thenReturn(qosEntriesList);
        List<Queues> queuesList = new ArrayList<>();
        when(queues.getKey()).thenReturn(mock(QueuesKey.class));
        queuesList.add(queues);
        when(ovsdbNodeAugmentation.getQueues()).thenReturn(queuesList);
        when(MdsalUtils.read(any(DataBroker.class), any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class))).thenReturn(uniAugmentation);
        when(MdsalUtils.readNode(any(DataBroker.class), any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class))).thenReturn(optionalNode);
        when(MdsalUtils.readNode(any(DataBroker.class), any(InstanceIdentifier.class)))
                .thenReturn(optionalNode);
        when(UnimgrMapper.getOvsdbBridgeNodeIid(any(Node.class))).thenReturn(instanceOfNode);
        when(UnimgrMapper.getTerminationPointIid(any(Node.class), any(String.class)))
                .thenReturn(instanceOfNode);
        when(UnimgrMapper.getUniIid(any(DataBroker.class), any(IpAddress.class),
                any(LogicalDatastoreType.class))).thenReturn(instanceOfNode);
        when(UnimgrMapper.getOvsdbQoSEntriesIid(any(Node.class), any(QosEntriesKey.class))).thenReturn(instanceOfNode);
        when(UnimgrMapper.getOvsdbQueuesIid(any(Node.class), any(QueuesKey.class))).thenReturn(instanceOfNode);
        when(MdsalUtils.deleteNode(any(DataBroker.class), any(InstanceIdentifier.class),
                any(LogicalDatastoreType.class))).thenReturn(true);
        uniRemoveCommand.execute();
        PowerMockito.verifyStatic(times(4));
        MdsalUtils.deleteNode(any(DataBroker.class), any(InstanceIdentifier.class),
                any(LogicalDatastoreType.class));
    }

}
