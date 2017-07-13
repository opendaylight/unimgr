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
import org.opendaylight.unimgr.utils.EvcUtils;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.unimgr.utils.OvsdbUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSource;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EvcUtils.class, MdsalUtils.class, OvsdbUtils.class, UnimgrMapper.class})
public class EvcAddCommandTest {

    private static final NodeId OVSDB_NODE_ID = new NodeId("ovsdb://7011db35-f44b-4aab-90f6-d89088caf9d8");

    private EvcAddCommand evcAddCommand;
    private DataTreeModification<Link> evcLink;
    private DataBroker dataBroker;
    private Link link;
    private Optional<Link> optLinks;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        PowerMockito.mockStatic(MdsalUtils.class);
        PowerMockito.mockStatic(EvcUtils.class);
        PowerMockito.mockStatic(OvsdbUtils.class);
        PowerMockito.mockStatic(UnimgrMapper.class);
        dataBroker = mock(DataBroker.class);
        link = mock(Link.class);
        optLinks = mock(Optional.class);
        evcLink = DataTreeModificationHelper.getEvcLink(link);
        evcAddCommand = new EvcAddCommand(dataBroker, evcLink);
    }

    /**
     * Test method for {@link org.opendaylight.unimgr.command.evcAddCommand#execute()}.
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testExecute() throws Exception {
        final UniAugmentation uniAugmentation = mock(UniAugmentation.class);
        final EvcAugmentation evcAugmentation = mock(EvcAugmentation.class);
        final List<UniSource> unisSource = new ArrayList<UniSource>();
        final UniSource uniSource = mock(UniSource.class);
        final List<UniDest> unisDest = new ArrayList<UniDest>();
        final UniDest uniDest = mock(UniDest.class);
        final IpAddress ipAddress = mock(IpAddress.class);
        final Ipv4Address ipv4Address = mock(Ipv4Address.class);
        final Optional<Node> optionalOvsdbNode = mock(Optional.class);
        final Node node = mock(Node.class);
        final OvsdbNodeRef ovsNodedRef = mock(OvsdbNodeRef.class);
        final InstanceIdentifier evcKey = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.EVC_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(OVSDB_NODE_ID));
        unisSource.add(uniSource);
        unisDest.add(uniDest);

        when(link.getAugmentation(EvcAugmentation.class)).thenReturn(evcAugmentation);
        when(optLinks.isPresent()).thenReturn(false);
        when(uniAugmentation.getOvsdbNodeRef()).thenReturn(ovsNodedRef);
        when(evcAugmentation.getUniSource()).thenReturn(unisSource);
        when(evcAugmentation.getUniDest()).thenReturn(unisDest);
        when(uniSource.getIpAddress()).thenReturn(ipAddress);
        when(uniDest.getIpAddress()).thenReturn(ipAddress);
        when(uniSource.getUni()).thenReturn(evcKey);
        when(uniDest.getUni()).thenReturn(evcKey);
        when(ipAddress.getIpv4Address()).thenReturn(ipv4Address);
        when(ipv4Address.toString()).thenReturn("ipv4Address_test");
        when(ipv4Address.getValue()).thenReturn("ipv4AddressValue_test");
        when(optionalOvsdbNode.isPresent()).thenReturn(true);
        when(optionalOvsdbNode.get()).thenReturn(node);
        when(node.getAugmentation(any(Class.class))).thenReturn(uniAugmentation);
        when(ovsNodedRef.getValue()).thenReturn(evcKey);

        when(MdsalUtils.readLink(any(DataBroker.class), any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class))).thenReturn(optLinks);
        when(MdsalUtils.readNode(any(DataBroker.class), any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class))).thenReturn(optionalOvsdbNode);
        PowerMockito.doNothing().when(OvsdbUtils.class, "updateMaxRate", dataBroker, uniAugmentation,
                uniAugmentation, evcAugmentation);
        PowerMockito.doNothing().when(OvsdbUtils.class, "createTerminationPointNode", dataBroker,
                uniAugmentation, node, UnimgrConstants.DEFAULT_BRIDGE_NAME, UnimgrConstants.DEFAULT_TUNNEL_IFACE);
        PowerMockito.doNothing().when(OvsdbUtils.class, "createGreTunnel", dataBroker, uniAugmentation,
                uniAugmentation, node, UnimgrConstants.DEFAULT_BRIDGE_NAME, UnimgrConstants.DEFAULT_GRE_TUNNEL_NAME);
        when(OvsdbUtils.createOvsdbNode(any(DataBroker.class), any(UniAugmentation.class)))
            .thenReturn(node);
        when(UnimgrMapper.getOvsdbNodeIid(any(NodeId.class))).thenReturn(evcKey);
        when(UnimgrMapper.getUniIid(any(DataBroker.class), any(IpAddress.class),
                any(LogicalDatastoreType.class))).thenReturn(evcKey);

        evcAddCommand.execute();
        PowerMockito.verifyStatic(times(2));
        OvsdbUtils.createTerminationPointNode(any(DataBroker.class), any(UniAugmentation.class),
                any(Node.class), any(String.class), any(String.class));
        PowerMockito.verifyStatic(times(2));
        OvsdbUtils.createGreTunnel(any(DataBroker.class), any(UniAugmentation.class),
                any(UniAugmentation.class), any(Node.class), any(String.class), any(String.class));
        PowerMockito.verifyStatic(times(2));
        EvcUtils.updateEvcNode(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(EvcAugmentation.class), any(InstanceIdentifier.class),
                any(InstanceIdentifier.class), any(DataBroker.class));
    }

}
