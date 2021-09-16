/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.common.util;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.utils.NetconfConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.AvailableCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.available.capabilities.AvailableCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.available.capabilities.AvailableCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public final class NodeTestUtils {

    private NodeTestUtils() {
    }

    public static final String DEVICE_ID = "device";


    @SuppressWarnings({"unchecked","checkstyle:illegalcatch"})
    public static DataBroker mockDataBroker(Optional<Node> nodeOptional) {
        DataBroker dataBroker = mock(DataBroker.class);
        final ReadTransaction transaction = mock(ReadTransaction.class);
        final FluentFuture<Optional<Node>> transactionResult = mock(FluentFuture.class);

        try {
            when(transactionResult.get()).thenReturn(nodeOptional);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        when(transaction.read(eq(LogicalDatastoreType.OPERATIONAL),
                any(InstanceIdentifier.class))).thenReturn(transactionResult);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(transaction);

        return dataBroker;
    }

    public static Optional<Node> mockNode() {
        final Node node = mock(Node.class);
        when(node.getNodeId()).thenReturn(new NodeId(DEVICE_ID));

        return Optional.of(node);
    }

    public static Optional<Node> mockNetconfNode(boolean withNetconfCapabilities) {
        List<AvailableCapability> netconfCapabilityList = new ArrayList<>();
        if (withNetconfCapabilities) {
            netconfCapabilityList = Arrays.asList(createAvailableCapability(NetconfConstants.CAPABILITY_IOX_L2VPN),
                    createAvailableCapability(NetconfConstants.CAPABILITY_IOX_IFMGR),
                            createAvailableCapability(NetconfConstants.CAPABILITY_IOX_INFRA_POLICYMGR));
        }

        AvailableCapabilities availableCapabilities = Mockito.mock(AvailableCapabilities.class);
        when(availableCapabilities.getAvailableCapability()).thenReturn(netconfCapabilityList);

        Optional<Node> mockedNodeOptional = mockNode();
        NetconfNode netconfNode = mock(NetconfNode.class);
        when(netconfNode.getAvailableCapabilities()).thenReturn(availableCapabilities);

        Node mockedNode = mockedNodeOptional.get();
        when(mockedNode.augmentation(NetconfNode.class)).thenReturn(netconfNode);

        return mockedNodeOptional;
    }

    private static AvailableCapability createAvailableCapability(String name) {
        AvailableCapabilityBuilder availableCapabilityBuilder = new AvailableCapabilityBuilder();
        availableCapabilityBuilder.setCapability(name);
        return availableCapabilityBuilder.build();
    }
}
