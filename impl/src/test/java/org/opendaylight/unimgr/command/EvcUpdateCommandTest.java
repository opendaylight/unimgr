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
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.unimgr.utils.EvcUtils;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.unimgr.utils.OvsdbUtils;
import org.opendaylight.unimgr.utils.UniUtils;
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
import com.google.common.util.concurrent.CheckedFuture;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UniUtils.class, EvcUtils.class, MdsalUtils.class, OvsdbUtils.class, UnimgrMapper.class})
public class EvcUpdateCommandTest {
    private static final NodeId OVSDB_NODE_ID = new NodeId("ovsdb://7011db35-f44b-4aab-90f6-d89088caf9d8");

    private EvcUpdateCommand evcUpdateCommand;
    private Link link;
    private DataTreeModification<Link> evcLink;
    private DataBroker dataBroker;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(UniUtils.class);
        PowerMockito.mockStatic(MdsalUtils.class);
        PowerMockito.mockStatic(EvcUtils.class);
        PowerMockito.mockStatic(OvsdbUtils.class);
        PowerMockito.mockStatic(UnimgrMapper.class);
        dataBroker = mock(DataBroker.class);
        link = mock(Link.class);
        evcLink = DataTreeModificationHelper.getEvcLink(link);
        evcUpdateCommand = new EvcUpdateCommand(dataBroker, evcLink);
    }

    /**
     * Test method for {@link org.opendaylight.unimgr.command.EvcUpdateCommand#execute()}.
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testExecute() throws Exception {
        final ReadTransaction readTransac = mock(ReadOnlyTransaction.class);
        final CheckedFuture retFormerEvc = mock(CheckedFuture.class);
        final UniAugmentation uniAugmentation = mock(UniAugmentation.class);
        final EvcAugmentation evcAugmentation = mock(EvcAugmentation.class);
        final EvcAugmentation formerEvc = mock(EvcAugmentation.class);
        final List<UniSource> unisSource = new ArrayList<UniSource>();
        final List<UniDest> unisDest = new ArrayList<UniDest>();
        final UniSource uniSource = mock(UniSource.class);
        final UniDest uniDest = mock(UniDest.class);
        final List<UniSource> formerUnisSource = new ArrayList<UniSource>();
        final List<UniDest> formerUnisDest = new ArrayList<UniDest>();
        final UniSource formerUniSource = mock(UniSource.class);
        final UniDest formerUniDest = mock(UniDest.class);
        final IpAddress ipAddressSource = mock(IpAddress.class);
        final IpAddress ipAddressDest = mock(IpAddress.class);
        final IpAddress formerIpAddressSource = mock(IpAddress.class);
        final IpAddress formerIpAddressDest = mock(IpAddress.class);
        final Ipv4Address ipv4AddressSource = mock(Ipv4Address.class);
        final Ipv4Address ipv4AddressDest = mock(Ipv4Address.class);
        final Ipv4Address formerIpv4AddressSource = mock(Ipv4Address.class);
        final Ipv4Address formerIpv4AddressDest = mock(Ipv4Address.class);
        final Optional<Node> optionalOvsdbNode = mock(Optional.class);
        final Optional<EvcAugmentation> optionalEvcAugm = mock(Optional.class);
        final Node node = mock(Node.class);
        final OvsdbNodeRef ovsNodedRef = mock(OvsdbNodeRef.class);
        final InstanceIdentifier evcKey = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.EVC_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(OVSDB_NODE_ID));
        unisSource.add(uniSource);
        unisDest.add(uniDest);
        formerUnisSource.add(formerUniSource);
        formerUnisDest.add(formerUniDest);

        // Case EVC1 : formerUni1ip.equals(laterUni1Ip), formerUni2ip.equals(laterUni2Ip)
        //              iidSource != null, iidDest != null
        when(link.getAugmentation(EvcAugmentation.class)).thenReturn(evcAugmentation);
        when(dataBroker.newReadOnlyTransaction()).thenReturn((ReadOnlyTransaction) readTransac);
        when(readTransac.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(retFormerEvc);
        when(retFormerEvc.checkedGet()).thenReturn(optionalEvcAugm);
        when(optionalEvcAugm.get()).thenReturn(formerEvc);
        when(formerEvc.getUniSource()).thenReturn(formerUnisSource);
        when(formerEvc.getUniDest()).thenReturn(formerUnisDest);
        when(formerUniSource.getIpAddress()).thenReturn(ipAddressSource);
        when(formerUniDest.getIpAddress()).thenReturn(ipAddressDest);
        when(uniAugmentation.getOvsdbNodeRef()).thenReturn(ovsNodedRef);
        when(evcAugmentation.getUniSource()).thenReturn(unisSource);
        when(evcAugmentation.getUniDest()).thenReturn(unisDest);
        when(uniSource.getIpAddress()).thenReturn(ipAddressSource);
        when(uniDest.getIpAddress()).thenReturn(ipAddressDest);
        when(uniSource.getUni()).thenReturn(evcKey);
        when(uniDest.getUni()).thenReturn(evcKey);
        when(formerIpAddressSource.getIpv4Address()).thenReturn(formerIpv4AddressSource);
        when(formerIpAddressDest.getIpv4Address()).thenReturn(formerIpv4AddressDest);
        when(formerIpv4AddressSource.toString()).thenReturn("formerIpv4AddressSource_test");
        when(formerIpv4AddressSource.getValue()).thenReturn("formerIpv4AddressValueSource_test");
        when(formerIpv4AddressDest.toString()).thenReturn("formerIpv4AddressDest_test");
        when(formerIpv4AddressDest.getValue()).thenReturn("formerIpv4AddressValueDest_test");
        when(ipAddressSource.getIpv4Address()).thenReturn(ipv4AddressSource);
        when(ipAddressDest.getIpv4Address()).thenReturn(ipv4AddressDest);
        when(optionalOvsdbNode.isPresent()).thenReturn(true);
        when(optionalOvsdbNode.get()).thenReturn(node);
        when(node.getAugmentation(any(Class.class))).thenReturn(uniAugmentation);
        when(ovsNodedRef.getValue()).thenReturn(evcKey);

        PowerMockito.doNothing().when(EvcUtils.class, "deleteEvcData", dataBroker, optionalOvsdbNode);
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
        when(UnimgrMapper.getOvsdbBridgeNodeIid(any(Node.class))).thenReturn(evcKey);
        when(UnimgrMapper.getUniIid(any(DataBroker.class), any(IpAddress.class),
                any(LogicalDatastoreType.class))).thenReturn(evcKey);
        verifyExecute(0, 0, 6, 2);

        // iidSource != null, iidDest != null
        when(formerUniSource.getIpAddress()).thenReturn(formerIpAddressSource);
        when(formerUniDest.getIpAddress()).thenReturn(formerIpAddressDest);
        when(uniSource.getUni()).thenReturn(null);
        when(uniDest.getUni()).thenReturn(null);
        verifyExecute(2, 0, 12, 4);
    }

    @SuppressWarnings("unchecked")
    private void verifyExecute(int getUniTimes, int deleteTimes, int readNodeTimes, int updateEvcTime) {
        evcUpdateCommand.execute();
        PowerMockito.verifyStatic(times(getUniTimes));
        UnimgrMapper.getUniIid(any(DataBroker.class), any(IpAddress.class),
                any(LogicalDatastoreType.class));
        PowerMockito.verifyStatic(times(deleteTimes));
        EvcUtils.deleteEvcData(any(DataBroker.class), any(Optional.class));
        PowerMockito.verifyStatic(times(readNodeTimes));
        MdsalUtils.readNode(any(DataBroker.class), any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class));
        PowerMockito.verifyStatic(times(updateEvcTime));
        UnimgrMapper.getOvsdbBridgeNodeIid(any(Node.class));
        PowerMockito.verifyStatic(times(updateEvcTime));
        OvsdbUtils.createTerminationPointNode(any(DataBroker.class), any(UniAugmentation.class),
                any(Node.class), any(String.class), any(String.class));
        PowerMockito.verifyStatic(times(updateEvcTime));
        OvsdbUtils.createGreTunnel(any(DataBroker.class), any(UniAugmentation.class),
                any(UniAugmentation.class), any(Node.class), any(String.class), any(String.class));
        PowerMockito.verifyStatic(times(updateEvcTime));
        EvcUtils.updateEvcNode(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(EvcAugmentation.class), any(InstanceIdentifier.class),
                any(InstanceIdentifier.class), any(DataBroker.class));
    }

}
