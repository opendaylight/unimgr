/*
 * Copyright (c) 2018 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.common.TapiUtils;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ConnectivityServiceEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.end.point.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author bartosz.michalik@amartus.com
 */
public class MdsalUtilsTest {
    @Test
    public void testReadTerminationPoint() throws ReadFailedException {
        //given
        TerminationPoint expectedTp = mock(TerminationPoint.class);


        TopologyId topologyId = new TopologyId("topology-netconf");


        ConnectivityServiceEndPoint cep = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.EndPointBuilder()
                .setServiceInterfacePoint(TapiUtils.toSipRef(new Uuid("sip:r1:tp1"), ServiceInterfacePoint.class))
                .setDirection(PortDirection.BIDIRECTIONAL)
                .build();
        EndPoint ep = new EndPoint(cep, null);


        DataBroker dataBroker = mock(DataBroker.class);
        ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class);
        Optional<TerminationPoint> optionalDataObject = mock(Optional.class);
        CheckedFuture<Optional<TerminationPoint>, ReadFailedException> future = mock(CheckedFuture.class);

        when(dataBroker.newReadOnlyTransaction()).thenReturn(transaction);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(future);
        when(future.checkedGet()).thenReturn(optionalDataObject);
        when(optionalDataObject.isPresent()).thenReturn(true);
        when(optionalDataObject.get()).thenReturn(expectedTp);

        //when
        Optional<TerminationPoint> actualTpOptional = MdsalUtils.readTerminationPoint(dataBroker, LogicalDatastoreType.CONFIGURATION, topologyId, ep);

        //then
        assertNotNull(actualTpOptional);
        assertTrue(actualTpOptional.isPresent());
        assertEquals(expectedTp, actualTpOptional.get());

        verify(transaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction).close();
    }

}