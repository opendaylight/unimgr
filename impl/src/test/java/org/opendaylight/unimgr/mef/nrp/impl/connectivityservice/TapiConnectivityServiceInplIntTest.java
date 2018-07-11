/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.connectivityservice;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
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
import org.opendaylight.unimgr.mef.nrp.common.TapiUtils;
import org.opendaylight.unimgr.mef.nrp.impl.AbstractTestWithTopo;
import org.opendaylight.unimgr.mef.nrp.impl.ConnectivityServiceIdResourcePool;
import org.opendaylight.unimgr.mef.nrp.impl.decomposer.BasicDecomposer;
import org.opendaylight.unimgr.utils.ActivationDriverMocks;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.common.types.rev180321.PositiveInteger;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev180321.carrier.eth.connectivity.end.point.resource.CeVlanIdListAndUntagBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev180321.vlan.id.list.and.untag.VlanIdBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.*;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.connectivity.service.end.point.attrs.NrpCarrierEthConnectivityEndPointResource;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.connectivity.service.end.point.attrs.NrpCarrierEthConnectivityEndPointResourceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.*;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.*;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connection.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.end.point.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.create.connectivity.service.input.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.create.connectivity.service.input.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.Node;
import org.opendaylight.yangtools.yang.binding.AugmentationHolder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.opendaylight.unimgr.utils.PredicateMatcher.fromPredicate;


/**
 * @author bartosz.michalik@amartus.com
 */
public class TapiConnectivityServiceInplIntTest extends AbstractTestWithTopo {

    private ActivationDriver ad1;
    private ActivationDriver ad2;

    private String uuid1 = "uuid1";
    private String activationDriverId1 = "d1";
    private String activationDriverId2 = "d2";
    private TapiConnectivityServiceImpl connectivityService;
    private ConnectivityServiceIdResourcePool mockPool;

    @Before
    public void setUp() throws Exception {
        BasicDecomposer decomposer = new BasicDecomposer(dataBroker);

        ad1 = mock(ActivationDriver.class);
        ad2 = mock(ActivationDriver.class);
        ActivationDriverRepoService repo = ActivationDriverMocks.builder()
                .add(activationDriverId1 , ad1)
                .add(activationDriverId2 , ad2)
                .build();

        RequestValidator validator = mock(RequestValidator.class);
        when(validator.checkValid(any(CreateConnectivityServiceInput.class)))
                .thenReturn(new RequestValidator.ValidationResult());
        when(validator.checkValid(any(UpdateConnectivityServiceInput.class)))
                .thenReturn(new RequestValidator.ValidationResult());


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
        n(tx, new Uuid(uuid1), activationDriverId1, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");

        tx.commit().get();
        waitForAbstractNeps(5, TimeUnit.SECONDS, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");

        when(mockPool.getServiceId()).thenReturn(servId);


        //when
        RpcResult<CreateConnectivityServiceOutput> result = this.connectivityService
                .createConnectivityService(input).get();
        //then
        assertTrue(result.isSuccessful());
        verify(ad1, times(1)).activate();
        verify(ad1, times(1)).commit();
        verifyZeroInteractions(ad2);

        ReadOnlyTransaction tx2 = dataBroker.newReadOnlyTransaction();

        Context1 connCtx = tx2
                .read(LogicalDatastoreType.OPERATIONAL, TapiConnectivityServiceImpl.CONNECTIVITY_CTX).get().get();
        assertEquals(2, connCtx.getConnection().size());
        connCtx.getConnection().forEach(this::verifyConnection);

        assertEquals(1, connCtx.getConnectivityService().size());
        assertFalse(connCtx.getConnectivityService().get(0).getEndPoint().isEmpty());
        assertEquals("cs:" + servId, connCtx.getConnectivityService().get(0).getUuid().getValue());

        verifyMefAttributes(connCtx.getConnectivityService().get(0));
        verifyMefAttributes(result.getResult().getService());

        final Set<String> expected = input.getEndPoint().stream().map(LocalClass::getLocalId)
                .collect(Collectors.toSet());

        assertThat(result.getResult().getService().getEndPoint(),
                CoreMatchers.everyItem(fromPredicate(id -> expected.contains(id.getLocalId())))

        );

    }

    private void verifyMefAttributes(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
                                             .ConnectivityService connectivityService) {
        assertTrue(connectivityService.getEndPoint().stream().allMatch(ep -> toAugmentation(ep) != null));
    }

    @SuppressWarnings("unchecked")
    private NrpCarrierEthConnectivityEndPointResource toAugmentation(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang
                    .tapi.connectivity.rev180307.connectivity.service.EndPoint ep) {
        if(ep instanceof AugmentationHolder) {
            Optional<NrpConnectivityServiceEndPointAttrs> o = ((AugmentationHolder) ep).augmentations().values()
                    .stream()
                    .filter(a -> a instanceof NrpConnectivityServiceEndPointAttrs).findFirst();
            if(o.isPresent()) return o.get().getNrpCarrierEthConnectivityEndPointResource();
            return null;
        }

        if(ep.augmentation(EndPoint1.class) != null) return ep.augmentation(EndPoint1.class)
                .getNrpCarrierEthConnectivityEndPointResource();
        if(ep.augmentation(EndPoint3.class) != null) return ep.augmentation(EndPoint3.class)
                .getNrpCarrierEthConnectivityEndPointResource();
        if(ep.augmentation(EndPoint4.class) != null) return ep.augmentation(EndPoint4.class)
                .getNrpCarrierEthConnectivityEndPointResource();
        if(ep.augmentation(EndPoint5.class) != null) return ep.augmentation(EndPoint5.class)
                .getNrpCarrierEthConnectivityEndPointResource();
        if(ep.augmentation(EndPoint6.class) != null) return ep.augmentation(EndPoint5.class)
                .getNrpCarrierEthConnectivityEndPointResource();
        if(ep.augmentation(EndPoint8.class) != null) return ep.augmentation(EndPoint8.class)
                .getNrpCarrierEthConnectivityEndPointResource();

        return null;
    }

    @Test
    public void testMultiDriverActivation() throws Exception {
        //having
        final String servId = "service-id";
        final String uuid2 = "uuid2";
        final String uuid3 = "uuid3";

        CreateConnectivityServiceInput input = new CreateConnectivityServiceInputBuilder()
                .setEndPoint(eps(uuid1 + ":1", uuid3 + ":3"))
                .build();

        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, new Uuid(uuid1), activationDriverId1, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");
        n(tx, new Uuid(uuid2), activationDriverId2, uuid2 + ":1", uuid2 + ":2", uuid2 + ":3");
        n(tx, new Uuid(uuid3), activationDriverId2, uuid3 + ":1", uuid3 + ":2", uuid3 + ":3");
        l(tx, uuid1, uuid1 +":2", uuid2, uuid2 + ":1", OperationalState.ENABLED);
        l(tx, uuid2, uuid2 +":3", uuid3, uuid3 + ":2", OperationalState.ENABLED);

        when(mockPool.getServiceId()).thenReturn(servId);

        tx.commit().get();

        //when
        RpcResult<CreateConnectivityServiceOutput> result = this.connectivityService
                .createConnectivityService(input).get();
        //then
        assertTrue(result.isSuccessful());
        verify(ad1, times(1)).activate();
        verify(ad1, times(1)).commit();
        verify(ad2, times(2)).activate();
        verify(ad2, times(2)).commit();

        ReadOnlyTransaction tx2 = dataBroker.newReadOnlyTransaction();

        Context1 connCtx = tx2
                .read(LogicalDatastoreType.OPERATIONAL, TapiConnectivityServiceImpl.CONNECTIVITY_CTX).get().get();
        assertEquals(4, connCtx.getConnection().size());
        connCtx.getConnection().forEach(this::verifyConnection);

        assertEquals(1, connCtx.getConnectivityService().size());
        assertFalse(connCtx.getConnectivityService().get(0).getEndPoint().isEmpty());
        assertEquals("cs:" + servId, connCtx.getConnectivityService().get(0).getUuid().getValue());

    }

    @Test
    public void testNoServiceDeactivation() throws ExecutionException, InterruptedException {
        DeleteConnectivityServiceInput input = new DeleteConnectivityServiceInputBuilder()
                .setServiceIdOrName("some-service").build();
        RpcResult<DeleteConnectivityServiceOutput> result = connectivityService.deleteConnectivityService(input).get();
        assertFalse(result.isSuccessful());
    }


    @Test
    public void testServiceDeactivationWithSingleDriver() throws ExecutionException, InterruptedException,
            TransactionCommitFailedException, ReadFailedException, ResourceActivatorException {
        //having

        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, uuid1, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");
        tx.commit().get();

        waitForAbstractNeps(5, TimeUnit.SECONDS, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");

        createConnectivityService();

        //when
        DeleteConnectivityServiceInput input = new DeleteConnectivityServiceInputBuilder()
                .setServiceIdOrName("some-service").build();
        RpcResult<DeleteConnectivityServiceOutput> result = connectivityService.deleteConnectivityService(input).get();

        //then
        assertTrue(result.isSuccessful());
        ReadOnlyTransaction tx2 = dataBroker.newReadOnlyTransaction();
        Context1 connCtx = tx2
                .read(LogicalDatastoreType.OPERATIONAL, TapiConnectivityServiceImpl.CONNECTIVITY_CTX).get().get();
        verify(ad1).deactivate();
        verify(ad1).commit();
        assertEquals(0, connCtx.getConnection().size());
        assertEquals(0, connCtx.getConnectivityService().size());
        Node node1 = new NrpDao(tx2).getNode(TapiConstants.PRESTO_EXT_TOPO, TapiConstants.PRESTO_ABSTRACT_NODE);
        Node node2 = new NrpDao(tx2).getNode(TapiConstants.PRESTO_SYSTEM_TOPO, uuid1);
        long cEndPoints1 = node1.getOwnedNodeEdgePoint().stream()
                .map(nep -> nep.augmentation(OwnedNodeEdgePoint1.class)).filter(Objects::nonNull)
                .mapToLong(aug -> aug.getConnectionEndPoint().size()).sum();
        long cEndPoints2 = node2.getOwnedNodeEdgePoint().stream()
                .map(nep -> nep.augmentation(OwnedNodeEdgePoint1.class)).filter(Objects::nonNull)
                .mapToLong(aug -> aug.getConnectionEndPoint().size()).sum();
        assertEquals(0, cEndPoints1);
        assertEquals(0, cEndPoints2);
    }

    @Test
    public void testGetServiceListEmpty() throws InterruptedException, ExecutionException {
        //having
        //when
        RpcResult<GetConnectivityServiceListOutput> result =
                connectivityService.getConnectivityServiceList(
                        new GetConnectivityServiceListInputBuilder().build()).get();
        //then
        assertTrue(result.isSuccessful());
        assertEquals(0, result.getResult().getService().size());
    }

    private void waitForAbstractNeps(long time, TimeUnit units,String... uuid) {
        long timeMs = units.toMillis(time);

        final List<String> required = Arrays.asList(uuid);

        for(int i = 0; i < 5; ++i) {
            NrpDao nrpDao = new NrpDao(dataBroker.newReadOnlyTransaction());
            try {
                Node node = nrpDao.getNode(TapiConstants.PRESTO_EXT_TOPO, TapiConstants.PRESTO_ABSTRACT_NODE);
                if(node != null && node.getOwnedNodeEdgePoint() != null) {
                    long neps = node.getOwnedNodeEdgePoint().stream()
                            .map(nep -> nep.getUuid().getValue()).filter(required::contains).count();
                    if(neps == required.size()) return;
                }

                try {
                    Thread.sleep(timeMs/5);
                } catch (InterruptedException e) {
                    //
                }

            } catch (ReadFailedException _e) {
                //
            }
        }

        Assert.fail("NEPs are not added to abstract node " + Arrays.stream(uuid)
                .collect(Collectors.joining(",", "[", "]")));
    }

    @Test
    public void testGetServiceList() throws TransactionCommitFailedException, InterruptedException, ExecutionException {
        //having

        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, uuid1, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");
        tx.commit().get();

        waitForAbstractNeps(5, TimeUnit.SECONDS, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");

        createConnectivityService();

        //when
        RpcResult<GetConnectivityServiceListOutput> result =
                connectivityService.getConnectivityServiceList(
                        new GetConnectivityServiceListInputBuilder().build()).get();

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
    public void testGetServiceDetails()
            throws InterruptedException, ExecutionException, TransactionCommitFailedException {
        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, uuid1, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");
        tx.commit().get();

        waitForAbstractNeps(5, TimeUnit.SECONDS, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");

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
    public void getGetConnectionDetailsByServiceName()
            throws InterruptedException, ExecutionException, TransactionCommitFailedException {
        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, uuid1, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");
        tx.commit().get();

        waitForAbstractNeps(5, TimeUnit.SECONDS, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");

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


    private void createConnectivityService()
            throws TransactionCommitFailedException, InterruptedException, ExecutionException {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, new Uuid(uuid1), activationDriverId1, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");
        Connection system = c(tx, uuid1, uuid1 + ":1", uuid1 + ":2");
        Connection global = c(tx, TapiConstants.PRESTO_ABSTRACT_NODE,
                Collections.singletonList(system.getUuid()), uuid1 + ":1", uuid1 + ":2");
        cs(tx,"some-service", global);

        tx.commit().get();
    }


    private void verifyConnection(Connection connection) {
        assertNotNull("Connection " + connection.getUuid().getValue() + " should have at least 2 end points",
                connection.getConnectionEndPoint());
        assertFalse("Connection " + connection.getUuid().getValue() + " should have at least 2 end points",
                connection.getConnectionEndPoint().isEmpty());
    }

    private List<EndPoint> eps(String ... uuids) {

        EndPoint2Builder builder = new EndPoint2Builder();
        EndPoint2 epAugmentation = builder.setNrpCarrierEthConnectivityEndPointResource(new NrpCarrierEthConnectivityEndPointResourceBuilder()
                .setCeVlanIdListAndUntag(new CeVlanIdListAndUntagBuilder()
                        .setVlanId(Collections.singletonList(
                                new VlanIdBuilder().setVlanId(new PositiveInteger(100L)).build()))
                        .build()

                ).build()
        ).build();

        return Arrays.stream(uuids).map(uuid -> new EndPointBuilder()
                .setLocalId("e:" + uuid)
                .setRole(PortRole.SYMMETRIC)
                .setDirection(PortDirection.BIDIRECTIONAL)
                .setServiceInterfacePoint(TapiUtils.toSipRef(new Uuid("sip:" + uuid), ServiceInterfacePoint.class))
                .addAugmentation(EndPoint2.class, epAugmentation)
                .build()).collect(Collectors.toList());
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
            .connectivity.context.ConnectivityService cs(ReadWriteTransaction tx, String csId, Connection connection) {
        ConnectivityService cs = new ConnectivityServiceBuilder()
                .setUuid(new Uuid(csId))
                .setConnection(Collections.singletonList(connection.getUuid()))
                .setEndPoint(toEps(connection.getConnectionEndPoint()))
                .build();
        InstanceIdentifier<Context1> connectivityCtx = NrpDao.ctx().augmentation(Context1.class);
        tx.put(LogicalDatastoreType.OPERATIONAL, connectivityCtx.child(ConnectivityService.class,  new ConnectivityServiceKey(cs.getUuid())), cs);

        return cs;

    }

    private List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.EndPoint> toEps(List<ConnectionEndPoint> ceps) {
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.EndPointBuilder builder
                = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.EndPointBuilder();
        return ceps.stream().map(cep -> {
            ServiceInterfacePoint sipRef = TapiUtils.toSipRef(new Uuid("sip:" + cep.getOwnedNodeEdgePointId().getValue()), ServiceInterfacePoint.class);

            return builder.setServiceInterfacePoint(sipRef)
                    .setLocalId(cep.getConnectionEndPointId().getValue())
                    .build();
        }).collect(Collectors.toList());
    }

}
