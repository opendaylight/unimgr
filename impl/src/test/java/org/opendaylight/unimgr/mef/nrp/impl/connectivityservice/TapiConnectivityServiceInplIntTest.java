/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.connectivityservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.TerminationDirection;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.CreateConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.CreateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.DeleteConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.DeleteConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.DeleteConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.GetConnectionDetailsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.GetConnectionDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.GetConnectivityServiceDetailsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.GetConnectivityServiceDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.GetConnectivityServiceListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.connection.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.connection.ConnectionEndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.connection.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.connectivity.context.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.connectivity.context.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.create.connectivity.service.input.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.create.connectivity.service.input.EndPointBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

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
                .add(new Uuid(uuid1), ad1)
                .add(new Uuid(uuid2), ad2)
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

        ReadOnlyTransaction tx2 = dataBroker.newReadOnlyTransaction();
        Context1 connCtx = tx2.read(LogicalDatastoreType.OPERATIONAL, TapiConnectivityServiceImpl.connectivityCtx).checkedGet().get();
        assertEquals(2, connCtx.getConnection().size());
        connCtx.getConnection().forEach(this::verifyConnection);

        assertEquals(1, connCtx.getConnectivityService().size());
        assertFalse(connCtx.getConnectivityService().get(0).getEndPoint().isEmpty());
        assertEquals("cs:" + servId, connCtx.getConnectivityService().get(0).getUuid().getValue());

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
        createConnectivityService();

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

    @Test
    public void testGetServiceListEmpty() throws InterruptedException, ExecutionException {
        //having
        //when
        RpcResult<GetConnectivityServiceListOutput> result =
                connectivityService.getConnectivityServiceList().get();
        //then
        assertTrue(result.isSuccessful());
        assertEquals(0, result.getResult().getService().size());
    }


    @Test
    public void testGetServiceList() throws TransactionCommitFailedException, InterruptedException, ExecutionException {
        //having
        createConnectivityService();

        //when
        RpcResult<GetConnectivityServiceListOutput> result =
                connectivityService.getConnectivityServiceList().get();

        //then
        assertTrue(result.isSuccessful());
        assertEquals(1, result.getResult().getService().size());
    }


    @Test
    public void testGetServiceDetailsNullInput() throws InterruptedException, ExecutionException {
        //having

        //when
        GetConnectivityServiceDetailsInputBuilder builder =
                new GetConnectivityServiceDetailsInputBuilder();
        RpcResult<GetConnectivityServiceDetailsOutput> result =
                connectivityService.getConnectivityServiceDetails(builder.build()).get();

        //then
        assertFalse(result.isSuccessful());
        assertEquals(1, result.getErrors().size());
    }

    @Test
    public void testGetServiceDetailsBadInput() throws InterruptedException, ExecutionException {
        //having

        //when
        GetConnectivityServiceDetailsInputBuilder builder =
                new GetConnectivityServiceDetailsInputBuilder();
        builder.setServiceIdOrName("missing-service");
        RpcResult<GetConnectivityServiceDetailsOutput> result =
                connectivityService.getConnectivityServiceDetails(builder.build()).get();

        //then
        assertFalse(result.isSuccessful());
        assertEquals(1, result.getErrors().size());
    }

    @Test
    public void testGetServiceDetails() throws InterruptedException, ExecutionException, TransactionCommitFailedException {
        //having
        createConnectivityService();

        //when
        GetConnectivityServiceDetailsInputBuilder builder =
                new GetConnectivityServiceDetailsInputBuilder();
        builder.setServiceIdOrName("some-service");
        RpcResult<GetConnectivityServiceDetailsOutput> result =
                connectivityService.getConnectivityServiceDetails(builder.build()).get();

        //then
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult().getService());
    }

    @Test
    public void getGetConnectionDetailsNullInput() throws InterruptedException, ExecutionException {
        //having

        //when
        GetConnectionDetailsInputBuilder builder =
                new GetConnectionDetailsInputBuilder();
        RpcResult<GetConnectionDetailsOutput> result =
                connectivityService.getConnectionDetails(builder.build()).get();

        //then
        assertFalse(result.isSuccessful());
        assertEquals(1, result.getErrors().size());
    }

    @Test
    public void getGetConnectionDetailsBadServiceName() throws InterruptedException, ExecutionException {
        //having

        //when
        GetConnectionDetailsInputBuilder builder =
                new GetConnectionDetailsInputBuilder();
        builder.setServiceIdOrName("missing-service");
        RpcResult<GetConnectionDetailsOutput> result =
                connectivityService.getConnectionDetails(builder.build()).get();

        //then
        assertFalse(result.isSuccessful());
        assertEquals(1, result.getErrors().size());
    }

    @Test
    public void getGetConnectionDetailsBadConnectionName() throws InterruptedException, ExecutionException {
        //having

        //when
        GetConnectionDetailsInputBuilder builder =
                new GetConnectionDetailsInputBuilder();
        builder.setConnectionIdOrName("missing-connection");
        RpcResult<GetConnectionDetailsOutput> result =
                connectivityService.getConnectionDetails(builder.build()).get();

        //then
        assertFalse(result.isSuccessful());
        assertEquals(1, result.getErrors().size());
    }

    @Test
    public void getGetConnectionDetailsByServiceName() throws InterruptedException, ExecutionException, TransactionCommitFailedException {
        //having
        createConnectivityService();

        //when
        GetConnectionDetailsInputBuilder builder =
                new GetConnectionDetailsInputBuilder();
        builder.setServiceIdOrName("some-service");
        RpcResult<GetConnectionDetailsOutput> result =
                connectivityService.getConnectionDetails(builder.build()).get();

        //then
        assertTrue(result.isSuccessful());
        assertNotNull(result.getResult().getConnection());
    }


    private void createConnectivityService() throws TransactionCommitFailedException {
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
    }


    private void verifyConnection(Connection connection) {
        assertFalse(connection.getConnectionEndPoint().isEmpty());
    }

    private List<EndPoint> eps(String ... uuids) {
        return Arrays.stream(uuids).map(uuid -> new EndPointBuilder()
                .setLocalId("e:" + uuid)
                .setRole(PortRole.SYMMETRIC)
                .setServiceInterfacePoint(new Uuid("sip:" + uuid))
                .build()).collect(Collectors.toList());
    }

    private org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.connectivity.context.ConnectivityService cs(String csId, Uuid connectionId) {
        return new ConnectivityServiceBuilder()
                .setUuid(new Uuid(csId))
                .setConnection(Collections.singletonList(connectionId))
                .build();
    }

    private org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.connectivity.context.Connection c(String nodeUuid, List<Uuid> route, String... neps) {
        ConnectionBuilder builder = new ConnectionBuilder()
                .setUuid(new Uuid("c:" + nodeUuid))
                .setContainerNode(new Uuid(nodeUuid))
                .setConnectionEndPoint(ceps(neps));

        if (!route.isEmpty()) {
            builder.setRoute(Collections.singletonList(new RouteBuilder()
                    .setConnectionEndPoint(route)
                    .setLocalId("route")
                    .build()
            ));
        }
        return builder.build();
    }

    private org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.connectivity.context.Connection c(String nodeUuid, String... neps) {
        return c(nodeUuid, Collections.emptyList(), neps);
    }

    private List<ConnectionEndPoint> ceps(String... neps) {
        return Arrays.stream(neps).map(nep -> new ConnectionEndPointBuilder()
                .setUuid(new Uuid("cep:" + nep))
//                .setTerminationDirection(TerminationDirection.Bidirectional)
                .setServerNodeEdgePoint(new Uuid(nep))
                .build()).collect(Collectors.toList());
    }

}
