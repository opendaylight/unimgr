/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.six.one.common.util;

import static org.junit.Assert.assertEquals;

import com.google.common.util.concurrent.FluentFuture;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util.LoopbackUtils;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.six.one.l2vpn.activator.L2vpnTestUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.topology.ext.rev180531.LoopbackAugmentation;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.topology.ext.rev180531.LoopbackAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
 * @author marek.ryznar@amartus.com
 */
public class LoopbackUtilsTest extends AbstractConcurrentDataBrokerTest {
    private static final Logger LOG = LoggerFactory.getLogger(LoopbackUtilsTest.class);
    private DataBroker broker;
    private static String nodeName = "192.168.2.1";
    private static String topoName = "topo://";
    private static String loopbackAddress = "192.168.2.20";
    private static String portNumber = "8080";

    @Before
    public void setUp() {
        broker = getDataBroker();
    }

    @After
    public void clean() {
        deleteNode(LoopbackUtils.getNodeIid(new NodeId(nodeName),new TopologyId(topoName)));
    }

    @Test
    public void testLoopbackAddress() {
        //given
        ServicePort port = L2vpnTestUtils.port(topoName, nodeName, portNumber);
        createAndPersistNode(true);

        //when
        Ipv4AddressNoZone ipv4AddressNoZone = LoopbackUtils.getIpv4Address(port, broker);

        //then
        assertEquals(loopbackAddress,ipv4AddressNoZone.getValue());
    }

    @Test
    public void testAbsenceOfLoopbackAddress() {
        //given
        ServicePort port = L2vpnTestUtils.port(topoName, nodeName, portNumber);
        createAndPersistNode(false);

        //when
        Ipv4AddressNoZone ipv4AddressNoZone = LoopbackUtils.getIpv4Address(port, broker);

        //then
        assertEquals(LoopbackUtils.getDefaultLoopback(),ipv4AddressNoZone.getValue());
    }

    private void createAndPersistNode(boolean ifLoopbackAddress) {
        NodeId nodeId = new NodeId(nodeName);
        Node node = createNode(nodeId,ifLoopbackAddress);
        InstanceIdentifier<Node> nodeIid = writeNode(node);
    }

    private Node createNode(NodeId nodeId,boolean ifLoopbackAddress) {
        NodeBuilder nodeBuilder = new NodeBuilder();

        NodeId nodeIdTopo = new NodeId(topoName);
        NodeKey nodeKey = new NodeKey(nodeIdTopo);

        nodeBuilder.setNodeId(nodeId);

        if (ifLoopbackAddress) {
            Ipv4Address ipv4Address = new Ipv4Address(loopbackAddress);
            IpAddress ipAddress = new IpAddress(ipv4Address);
            LoopbackAugmentationBuilder loopbackAugmentationBuilder = new LoopbackAugmentationBuilder();
            loopbackAugmentationBuilder.setLoopbackAddress(ipAddress);
            nodeBuilder.addAugmentation(LoopbackAugmentation.class,loopbackAugmentationBuilder.build());
        }

        return nodeBuilder.build();
    }

    private InstanceIdentifier<Node> writeNode(Node node) {
        InstanceIdentifier<Node> nodeInstanceId = LoopbackUtils.getNodeIid(node.getNodeId(),new TopologyId(topoName));
        WriteTransaction transaction = broker.newWriteOnlyTransaction();

        transaction.put(LogicalDatastoreType.CONFIGURATION, nodeInstanceId, node,true);
        try {
            FluentFuture<? extends CommitInfo> future = transaction.commit();
            future.get();
            return nodeInstanceId;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to write node with Iid {} to store {}.",
                    nodeInstanceId, LogicalDatastoreType.CONFIGURATION);
        }
        return null;
    }

    private void deleteNode(InstanceIdentifier<Node> nodeIid) {
        WriteTransaction transaction = broker.newWriteOnlyTransaction();
        transaction.delete(LogicalDatastoreType.CONFIGURATION, nodeIid);
        try {
            transaction.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to remove node with Iid {} from store {}.", nodeIid, LogicalDatastoreType.CONFIGURATION);
        }
    }
}
