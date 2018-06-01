/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.utils;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.common.TapiUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ConnectivityServiceEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.end.point.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LogicalDatastoreType.class, EvcUtils.class})
public class MdsalUtilsTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();
    @Mock private DataBroker dataBroker;
    @Mock private Node bridgeNode;
    @Mock private String bridgeName;
    @Mock private String portName;
    @Mock private String type;
    @Mock private WriteTransaction transaction;
    @Mock private IpAddress mockIp;
    @SuppressWarnings("rawtypes")
    @Mock private Appender mockAppender;
    @SuppressWarnings({ "rawtypes" })
    @Mock private CheckedFuture checkedFuture;
    private static final NodeId OVSDB_NODE_ID = new NodeId("ovsdb://7011db35-f44b-4aab-90f6-d89088caf9d8");
    private ch.qos.logback.classic.Logger root;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(MdsalUtils.class, Mockito.CALLS_REAL_METHODS);
        PowerMockito.mockStatic(LogicalDatastoreType.class);
        root = (ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        // Check logger messages
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testDeleteNode() throws Exception {
        InstanceIdentifier<Node> genericNode = InstanceIdentifier
                                                   .create(NetworkTopology.class)
                                                   .child(Topology.class,
                                                           new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID))
                                                   .child(Node.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class),
                                             any(InstanceIdentifier.class));
        when(transaction.submit()).thenReturn(checkedFuture);
        assertEquals(true, MdsalUtils.deleteNode(dataBroker, genericNode, LogicalDatastoreType.CONFIGURATION));
        verify(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction).submit();
        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent)argument).getFormattedMessage().contains("Received a request to delete node");
            }
            }));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRead() throws ReadFailedException {
        DataBroker dataBroker = mock(DataBroker.class);
        InstanceIdentifier<Node> nodeIid = PowerMockito.mock(InstanceIdentifier.class);
        ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(transaction);
        Optional<Node> optionalDataObject = mock(Optional.class);
        CheckedFuture<Optional<Node>, ReadFailedException> future = mock(CheckedFuture.class);
        Node nd = mock(Node.class);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(future);
        when(future.checkedGet()).thenReturn(optionalDataObject);
        when(optionalDataObject.isPresent()).thenReturn(true);
        when(optionalDataObject.get()).thenReturn(nd);
        Node expectedNode = MdsalUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION, nodeIid);
        verify(transaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction).close();
        assertNotNull(expectedNode);
        assertEquals(expectedNode, nd);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadLink() throws ReadFailedException {
        LinkId linkId = new LinkId("evc://7011db35-f44b-4aab-90f6-d89088caf9d8");
        InstanceIdentifier<?> nodeIid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class,
                        new TopologyKey(UnimgrConstants.EVC_TOPOLOGY_ID))
                .child(Link.class,
                        new LinkKey(linkId));
        ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(transaction);
        CheckedFuture<Optional<Link>, ReadFailedException> linkFuture = mock(CheckedFuture.class);
        Optional<Link> optLink = mock(Optional.class);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(linkFuture);
        when(linkFuture.checkedGet()).thenReturn(optLink);
        Optional<Link> expectedOpt = MdsalUtils.readLink(dataBroker, LogicalDatastoreType.CONFIGURATION, nodeIid);
        verify(transaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        assertNotNull(expectedOpt);
        assertEquals(expectedOpt, optLink);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadNode() throws ReadFailedException {
        InstanceIdentifier<?> nodeIid = InstanceIdentifier
                                            .create(NetworkTopology.class)
                                            .child(Topology.class,
                                                    new TopologyKey(UnimgrConstants.UNI_TOPOLOGY_ID))
                                            .child(Node.class,
                                                    new NodeKey(OVSDB_NODE_ID));
        ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(transaction);
        CheckedFuture<Optional<Node>, ReadFailedException> nodeFuture = mock(CheckedFuture.class);
        Optional<Node> optNode = mock(Optional.class);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(nodeFuture);
        when(nodeFuture.checkedGet()).thenReturn(optNode);
        Optional<Node> expectedOpt = MdsalUtils.readNode(dataBroker, LogicalDatastoreType.CONFIGURATION, nodeIid);
        verify(transaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        assertNotNull(expectedOpt);
        assertEquals(expectedOpt, optNode);
    }

    @Test
    public void testReadOptionalPositive() throws ReadFailedException {
        //given
        DataBroker dataBroker = mock(DataBroker.class);
        InstanceIdentifier<Node> nodeIid = PowerMockito.mock(InstanceIdentifier.class);
        ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class);
        Optional<Node> optionalDataObject = mock(Optional.class);
        CheckedFuture<Optional<Node>, ReadFailedException> future = mock(CheckedFuture.class);
        Node exceptedNode = mock(Node.class);

        when(dataBroker.newReadOnlyTransaction()).thenReturn(transaction);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(future);
        when(future.checkedGet()).thenReturn(optionalDataObject);
        when(optionalDataObject.isPresent()).thenReturn(true);
        when(optionalDataObject.get()).thenReturn(exceptedNode);

        //when
        Optional<Node> actualNodeOptional = MdsalUtils.readOptional(dataBroker, LogicalDatastoreType.CONFIGURATION, nodeIid);

        //then
        assertNotNull(actualNodeOptional);
        assertTrue(actualNodeOptional.isPresent());

        verify(transaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction).close();

        assertEquals(actualNodeOptional.get(), exceptedNode);
    }

    @Test
    public void testReadOptionalNegative() throws ReadFailedException {
        //given
        DataBroker dataBroker = mock(DataBroker.class);
        InstanceIdentifier<Node> nodeIid = PowerMockito.mock(InstanceIdentifier.class);
        ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class);
        Optional<Node> optionalDataObject = mock(Optional.class);
        CheckedFuture<Optional<Node>, ReadFailedException> future = mock(CheckedFuture.class);

        when(dataBroker.newReadOnlyTransaction()).thenReturn(transaction);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(future);
        when(future.checkedGet()).thenReturn(optionalDataObject);
        when(optionalDataObject.isPresent()).thenReturn(false);

        //when
        Optional<Node> actualNodeOptional = MdsalUtils.readOptional(dataBroker, LogicalDatastoreType.CONFIGURATION, nodeIid);

        //then
        assertNotNull(actualNodeOptional);
        assertFalse(actualNodeOptional.isPresent());

        verify(transaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction).close();
    }


}
