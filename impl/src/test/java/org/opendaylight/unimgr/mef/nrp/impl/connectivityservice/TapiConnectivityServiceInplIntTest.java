/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.connectivityservice;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverRepoService;
import org.opendaylight.unimgr.mef.nrp.api.RequestValidator;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivatorException;
import org.opendaylight.unimgr.mef.nrp.impl.AbstractTestWithTopo;
import org.opendaylight.unimgr.mef.nrp.impl.ConnectivityServiceIdResourcePool;
import org.opendaylight.unimgr.mef.nrp.impl.decomposer.BasicDecomposer;
import org.opendaylight.unimgr.utils.ActivationDriverMocks;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.PortRole;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.TerminationDirection;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.UniversalId;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170227.*;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170227.connection.*;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170227.connection.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170227.connectivity.context.*;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170227.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170227.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170227.create.connectivity.service.input.EndPoint;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170227.create.connectivity.service.input.EndPointBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author bartosz.michalik@amartus.com
 */
public class TapiConnectivityServiceInplIntTest extends AbstractTestWithTopo {

    private ActivationDriver ad1;
    private ActivationDriver ad2;

    private String uuid1 = "uuid1";
    private String uuid2 = "uuid2";
    private TapiConnectivityServiceImpl connectivityService;
    private ConnectivityServiceIdResourcePool mockPool;

    @Before
    public void setUp() throws Exception {
        BasicDecomposer decomposer = new BasicDecomposer(dataBroker);

        ad1 = mock(ActivationDriver.class);
        ad2 = mock(ActivationDriver.class);
        ActivationDriverRepoService repo = ActivationDriverMocks.builder()
                .add(new UniversalId(uuid1), ad1)
                .add(new UniversalId(uuid2), ad2)
                .build();

        RequestValidator validator = mock(RequestValidator.class);
        when(validator.checkValid(any())).thenReturn(new RequestValidator.ValidationResult());

        mockPool = mock(ConnectivityServiceIdResourcePool.class);

        connectivityService = new TapiConnectivityServiceImpl();
        connectivityService.setDriverRepo(repo);
        connectivityService.setDecomposer(decomposer);
        connectivityService.setValidator(validator);
        connectivityService.setBroker(getDataBroker());
        connectivityService.setServiceIdPool(mockPool);
        connectivityService.init();
    }

    @Test
    public void testSingleDriverActivation() throws Exception {
        //having
        final String servId = "service-id";
        CreateConnectivityServiceInput input = new CreateConnectivityServiceInputBuilder()
                .setEndPoint(eps(uuid1 + ":1", uuid1 + ":2"))
                .build();

        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, uuid1, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");

        when(mockPool.getServiceId()).thenReturn(servId);

        tx.submit().checkedGet();


        //when
        RpcResult<CreateConnectivityServiceOutput> result = this.connectivityService.createConnectivityService(input).get();
        //then
        assertTrue(result.isSuccessful());
        verify(ad1).activate();
        verify(ad1).commit();
        verifyZeroInteractions(ad2);
        verifyZeroInteractions(ad2);

        ReadOnlyTransaction tx2 = dataBroker.newReadOnlyTransaction();
        Context1 connCtx = tx2.read(LogicalDatastoreType.OPERATIONAL, TapiConnectivityServiceImpl.connectivityCtx).checkedGet().get();
        assertEquals(2, connCtx.getConnection().size());
        connCtx.getConnection().forEach(this::verifyConnection);

        assertEquals(1, connCtx.getConnectivityService().size());
        assertFalse(connCtx.getConnectivityService().get(0).getEndPoint().isEmpty());
        assertEquals("cs:"+servId, connCtx.getConnectivityService().get(0).getUuid().getValue());

    }

    @Test
    public void testNoServiceDeactivation() throws ExecutionException, InterruptedException {
        DeleteConnectivityServiceInput input = new DeleteConnectivityServiceInputBuilder().setServiceIdOrName("some-service").build();
        RpcResult<DeleteConnectivityServiceOutput> result = connectivityService.deleteConnectivityService(input).get();
        assertFalse(result.isSuccessful());
    }


    @Test
    public void testServiceDeactivationWithSingleDriver() throws ExecutionException, InterruptedException, TransactionCommitFailedException, ReadFailedException, ResourceActivatorException {
        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, uuid1, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");
        Connection system = c(uuid1, uuid1 + ":1", uuid1 + ":2");
        Connection global = c(TapiConstants.PRESTO_ABSTRACT_NODE, Collections.singletonList(system.getUuid()), uuid1 + ":1", uuid1 + ":2");
        ConnectivityService cs = cs("some-service", global.getUuid());

        InstanceIdentifier<Context1> connectivityCtx = NrpDao.ctx().augmentation(Context1.class);

        tx.put(LogicalDatastoreType.OPERATIONAL, connectivityCtx.child(Connection.class,  new ConnectionKey(system.getUuid())), system);
        tx.put(LogicalDatastoreType.OPERATIONAL, connectivityCtx.child(Connection.class,  new ConnectionKey(global.getUuid())), global);
        tx.put(LogicalDatastoreType.OPERATIONAL, connectivityCtx.child(ConnectivityService.class,  new ConnectivityServiceKey(cs.getUuid())), cs);
        tx.submit().checkedGet();

        //when
        DeleteConnectivityServiceInput input = new DeleteConnectivityServiceInputBuilder().setServiceIdOrName("some-service").build();
        RpcResult<DeleteConnectivityServiceOutput> result = connectivityService.deleteConnectivityService(input).get();

        //then
        assertTrue(result.isSuccessful());
        ReadOnlyTransaction tx2 = dataBroker.newReadOnlyTransaction();
        Context1 connCtx = tx2.read(LogicalDatastoreType.OPERATIONAL, TapiConnectivityServiceImpl.connectivityCtx).checkedGet().get();
        verify(ad1).deactivate();
        verify(ad1).commit();
        assertEquals(0, connCtx.getConnection().size());
        assertEquals(0, connCtx.getConnectivityService().size());
    }



    private void verifyConnection(Connection connection) {
        assertFalse(connection.getConnectionEndPoint().isEmpty());
    }

    private List<EndPoint> eps(String ... uuids) {
        return Arrays.stream(uuids).map(uuid -> new EndPointBuilder()
                .setLocalId("e:" + uuid)
                .setRole(PortRole.Symmetric)
                .setServiceInterfacePoint(new UniversalId("sip:" + uuid))
                .build()).collect(Collectors.toList());
    }

    private org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170227.connectivity.context.ConnectivityService cs(String csId, UniversalId connectionId) {
        return new ConnectivityServiceBuilder()
                .setUuid(new UniversalId(csId))
                .setConnection(Arrays.asList(connectionId))
                .build();
    }

    private org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170227.connectivity.context.Connection c(String nodeUuid, List<UniversalId> route, String... neps) {
        ConnectionBuilder builder = new ConnectionBuilder()
                .setUuid(new UniversalId("c:" + nodeUuid))
                .setNode(new UniversalId(nodeUuid))
                .setConnectionEndPoint(ceps(neps));

        if(!route.isEmpty()) {
            builder.setRoute(Collections.singletonList(new RouteBuilder()
                    .setLowerConnection(route)
                    .setLocalId("route")
                    .build()
            ));
        }
        return builder.build();
    }

    private org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170227.connectivity.context.Connection c(String nodeUuid, String... neps) {
        return c(nodeUuid, Collections.emptyList(), neps);
    }

    private List<ConnectionEndPoint> ceps(String... neps) {
        return Arrays.stream(neps).map(nep -> new ConnectionEndPointBuilder()
                .setUuid(new UniversalId("cep:" + nep))
                .setTerminationDirection(TerminationDirection.Bidirectional)
                .setServerNodeEdgePoint(new UniversalId(nep))
                .build()).collect(Collectors.toList());
    }

}
