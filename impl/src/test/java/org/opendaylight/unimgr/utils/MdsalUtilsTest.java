/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.utils;

import ch.qos.logback.core.Appender;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LogicalDatastoreType.class})
public class MdsalUtilsTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();
    @SuppressWarnings("rawtypes")
    @Mock private Appender mockAppender;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(MdsalUtils.class, Mockito.CALLS_REAL_METHODS);
        PowerMockito.mockStatic(LogicalDatastoreType.class);
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        // Check logger messages
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
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

    @SuppressWarnings("unchecked")
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
