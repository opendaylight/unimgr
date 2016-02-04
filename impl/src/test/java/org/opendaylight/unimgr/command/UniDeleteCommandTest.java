/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.command;

import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.support.membermodification.MemberMatcher.method;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.unimgr.impl.UnimgrUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UnimgrUtils.class,
        UnimgrMapper.class})
public class UniDeleteCommandTest {

    private static final NodeId OVSDB_NODE_ID = new NodeId("ovsdb://7011db35-f44b-4aab-90f6-d89088caf9d8");
    private static final String NODE_ID = "uni://10.0.0.1";

    private UniDeleteCommand uniDeleteCommand;
    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes;
    private DataBroker dataBroker;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp(){
        PowerMockito.mockStatic(UnimgrUtils.class);
        PowerMockito.mockStatic(UnimgrMapper.class);
        changes = mock(AsyncDataChangeEvent.class);
        dataBroker = mock(DataBroker.class);
        uniDeleteCommand = new UniDeleteCommand(dataBroker, changes);
    }

    /**
     * Test method for {@link org.opendaylight.unimgr.command.UniDeleteCommand#execute()}.
     */
    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    @Test
    public void testExecute() {
        final Set<InstanceIdentifier<UniAugmentation>> removedUnis =
                new HashSet<InstanceIdentifier<UniAugmentation>>();
        removedUnis.add(mock(InstanceIdentifier.class));
        final UniAugmentation uniAugmentation = mock(UniAugmentation.class);
        final OvsdbNodeRef ovsNodedRef = mock(OvsdbNodeRef.class);
        final Optional<Node> optionalNode = mock(Optional.class);
        final InstanceIdentifier instanceOfNode = mock(InstanceIdentifier.class);
        final InstanceIdentifier uniKey = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class,
                        new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID))//Any node id is fine for tests
                .child(Node.class,
                        new NodeKey(OVSDB_NODE_ID));

        when(uniAugmentation.getIpAddress()).thenReturn(mock(IpAddress.class));
        when(uniAugmentation.getOvsdbNodeRef()).thenReturn(ovsNodedRef);
        when(ovsNodedRef.getValue()).thenReturn(uniKey);
        when(optionalNode.isPresent()).thenReturn(true);
        when(optionalNode.get()).thenReturn(mock(Node.class));
        when(UnimgrUtils.extractRemoved(any(AsyncDataChangeEvent.class), any(Class.class)))
                .thenReturn(removedUnis);
        when(UnimgrUtils.read(any(DataBroker.class), any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class))).thenReturn(uniAugmentation);
        when(UnimgrUtils.readNode(any(DataBroker.class), any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class))).thenReturn(optionalNode);
        when(UnimgrUtils.readNode(any(DataBroker.class), any(InstanceIdentifier.class)))
                .thenReturn(optionalNode);
        when(UnimgrMapper.getOvsdbBridgeNodeIid(any(Node.class))).thenReturn(instanceOfNode);
        when(UnimgrMapper.getTerminationPointIid(any(Node.class), any(String.class)))
                .thenReturn(instanceOfNode);
        when(UnimgrMapper.getUniIid(any(DataBroker.class), any(IpAddress.class), any(LogicalDatastoreType.class)))
                .thenReturn(instanceOfNode);
        when(UnimgrUtils.deleteNode(any(DataBroker.class), any(InstanceIdentifier.class),
                any(LogicalDatastoreType.class))).thenReturn(true);
        uniDeleteCommand.execute();

        PowerMockito.verifyStatic(times(3));
        UnimgrUtils.deleteNode(any(DataBroker.class), any(InstanceIdentifier.class),
                any(LogicalDatastoreType.class));
    }

}
