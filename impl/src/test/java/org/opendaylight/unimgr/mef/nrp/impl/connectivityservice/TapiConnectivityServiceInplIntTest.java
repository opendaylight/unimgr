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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.opendaylight.unimgr.utils.PredicateMatcher.fromPredicate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
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
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint1;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint2;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint2Builder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint3;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint4;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint5;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint6;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint8;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.NrpConnectivityServiceEndPointAttrs;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.connectivity.service.end.point.attrs.NrpCarrierEthConnectivityEndPointResource;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.connectivity.service.end.point.attrs.NrpCarrierEthConnectivityEndPointResourceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.LocalClass;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.DeleteConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.DeleteConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.DeleteConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.GetConnectionDetailsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.GetConnectionDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.GetConnectivityServiceDetailsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.GetConnectivityServiceDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.GetConnectivityServiceListInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.GetConnectivityServiceListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceInput;
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

        ad1 = mock(ActivationDriver.class);
        ad2 = mock(ActivationDriver.class);

        RequestValidator validator = mock(RequestValidator.class);
        when(validator.checkValid(any(CreateConnectivityServiceInput.class)))
                .thenReturn(new RequestValidator.ValidationResult());
        when(validator.checkValid(any(UpdateConnectivityServiceInput.class)))
                .thenReturn(new RequestValidator.ValidationResult());

        mockPool = mock(ConnectivityServiceIdResourcePool.class);

        connectivityService = new TapiConnectivityServiceImpl();
        ActivationDriverRepoService repo = ActivationDriverMocks.builder()
                .add(activationDriverId1 , ad1)
                .add(activationDriverId2 , ad2)
                .build();
        connectivityService.setDriverRepo(repo);
        connectivityService.setDecomposer(new BasicDecomposer(dataBroker));
        connectivityService.setValidator(validator);
        connectivityService.setBroker(getDataBroker());
        connectivityService.setServiceIdPool(mockPool);
        connectivityService.init();
    }

    @Test
    public void testSingleDriverActivation() throws Exception {
        //having
        final String servId = "service-id";

        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, new Uuid(uuid1), activationDriverId1, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");

        tx.commit().get();
        waitForAbstractNeps(5, TimeUnit.SECONDS, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");

        when(mockPool.getServiceId()).thenReturn(servId);

        //when
        CreateConnectivityServiceInput input = new CreateConnectivityServiceInputBuilder()
                .setEndPoint(eps(uuid1 + ":1", uuid1 + ":2"))
                .build();

        RpcResult<CreateConnectivityServiceOutput> result = this.connectivityService
                .createConnectivityService(input).get();

        //then
        assertTrue(result.isSuccessful());
        verify(ad1, times(1)).activate();
        verify(ad1, times(1)).commit();
        verifyZeroInteractions(ad2);

        ReadTransaction tx2 = dataBroker.newReadOnlyTransaction();

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

    private void verifyMefAttributes(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
                .ConnectivityService serviceToVerify) {
        assertTrue(serviceToVerify.getEndPoint().stream().allMatch(ep -> toAugmentation(ep) != null));
    }

    @SuppressWarnings("unchecked")
    private NrpCarrierEthConnectivityEndPointResource toAugmentation(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang
                    .tapi.connectivity.rev180307.connectivity.service.EndPoint ep) {
        if (ep instanceof AugmentationHolder) {
            Optional<NrpConnectivityServiceEndPointAttrs> optional =
                    ((AugmentationHolder) ep).augmentations().values()
                    .stream()
                    .filter(a -> a instanceof NrpConnectivityServiceEndPointAttrs).findFirst();
            if (optional.isPresent()) {
                return optional.get().getNrpCarrierEthConnectivityEndPointResource();
            }
            return null;
        }

        if (ep.augmentation(EndPoint1.class) != null) {
            return ep.augmentation(EndPoint1.class).getNrpCarrierEthConnectivityEndPointResource();
        }
        if (ep.augmentation(EndPoint3.class) != null) {
            return ep.augmentation(EndPoint3.class).getNrpCarrierEthConnectivityEndPointResource();
        }
        if (ep.augmentation(EndPoint4.class) != null) {
            return ep.augmentation(EndPoint4.class).getNrpCarrierEthConnectivityEndPointResource();
        }
        if (ep.augmentation(EndPoint5.class) != null) {
            return ep.augmentation(EndPoint5.class).getNrpCarrierEthConnectivityEndPointResource();
        }
        if (ep.augmentation(EndPoint6.class) != null) {
            return ep.augmentation(EndPoint5.class).getNrpCarrierEthConnectivityEndPointResource();
        }
        if (ep.augmentation(EndPoint8.class) != null) {
            return ep.augmentation(EndPoint8.class).getNrpCarrierEthConnectivityEndPointResource();
        }

        return null;
    }

    @Test
    public void testMultiDriverActivation() throws Exception {
        //having
        final String servId = "service-id";
        final String uuid2 = "uuid2";
        final String uuid3 = "uuid3";

        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, new Uuid(uuid1), activationDriverId1, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");
        n(tx, new Uuid(uuid2), activationDriverId2, uuid2 + ":1", uuid2 + ":2", uuid2 + ":3");
        n(tx, new Uuid(uuid3), activationDriverId2, uuid3 + ":1", uuid3 + ":2", uuid3 + ":3");
        l(tx, uuid1, uuid1 + ":2", uuid2, uuid2 + ":1", OperationalState.ENABLED);
        l(tx, uuid2, uuid2 + ":3", uuid3, uuid3 + ":2", OperationalState.ENABLED);

        when(mockPool.getServiceId()).thenReturn(servId);

        tx.commit().get();

        //when
        CreateConnectivityServiceInput input = new CreateConnectivityServiceInputBuilder()
                .setEndPoint(eps(uuid1 + ":1", uuid3 + ":3"))
                .build();

        RpcResult<CreateConnectivityServiceOutput> result = this.connectivityService
                .createConnectivityService(input).get();
        //then
        assertTrue(result.isSuccessful());
        verify(ad1, times(1)).activate();
        verify(ad1, times(1)).commit();
        verify(ad2, times(2)).activate();
        verify(ad2, times(2)).commit();

        ReadTransaction tx2 = dataBroker.newReadOnlyTransaction();

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
            ReadFailedException, ResourceActivatorException {
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

        ReadTransaction rtx = dataBroker.newReadOnlyTransaction();
        Optional<Context> opt =
                rtx.read(LogicalDatastoreType.OPERATIONAL, NrpDao.ctx()).get();
        Context c = opt.get();
        Context1 connCtx = c.augmentation(Context1.class);

        // Semantics changed to augmentation being removed along with last child?
        assertNull(connCtx);
//      assertEquals(0, connCtx.getConnection().size());
//      assertEquals(0, connCtx.getConnectivityService().size());

        verify(ad1).deactivate();
        verify(ad1).commit();
        Node node1 = new NrpDao(rtx).getNode(TapiConstants.PRESTO_EXT_TOPO, TapiConstants.PRESTO_ABSTRACT_NODE);
        Node node2 = new NrpDao(rtx).getNode(TapiConstants.PRESTO_SYSTEM_TOPO, uuid1);
        long countEndPoints1 = node1.getOwnedNodeEdgePoint().stream()
                .map(nep -> nep.augmentation(OwnedNodeEdgePoint1.class)).filter(Objects::nonNull)
                .mapToLong(aug -> aug.getConnectionEndPoint().size()).sum();
        long countEndPoints2 = node2.getOwnedNodeEdgePoint().stream()
                .map(nep -> nep.augmentation(OwnedNodeEdgePoint1.class)).filter(Objects::nonNull)
                .mapToLong(aug -> aug.getConnectionEndPoint().size()).sum();
        assertEquals(0, countEndPoints1);
        assertEquals(0, countEndPoints2);
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

        for (int i = 0; i < 5; ++i) {
            NrpDao nrpDao = new NrpDao(dataBroker.newReadOnlyTransaction());
            try {
                Node node = nrpDao.getNode(TapiConstants.PRESTO_EXT_TOPO, TapiConstants.PRESTO_ABSTRACT_NODE);
                if (node != null && node.getOwnedNodeEdgePoint() != null) {
                    long neps = node.getOwnedNodeEdgePoint().stream()
                            .map(nep -> nep.getUuid().getValue()).filter(required::contains).count();
                    if (neps == required.size()) {
                        return;
                    }
                }

                try {
                    Thread.sleep(timeMs / 5);
                } catch (InterruptedException e) {
                    //
                }

            } catch (InterruptedException | ExecutionException _e) {
                //
            }
        }

        Assert.fail("NEPs are not added to abstract node " + Arrays.stream(uuid)
                .collect(Collectors.joining(",", "[", "]")));
    }

    @Test
    public void testGetServiceList() throws InterruptedException, ExecutionException {
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
            throws InterruptedException, ExecutionException {
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
            throws InterruptedException, ExecutionException {
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
            throws InterruptedException, ExecutionException {
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
        EndPoint2 epAugmentation = builder.setNrpCarrierEthConnectivityEndPointResource(
                new NrpCarrierEthConnectivityEndPointResourceBuilder()
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
                .setServiceInterfacePoint(TapiUtils.toSipRef(
                        new Uuid("sip:" + uuid), ServiceInterfacePoint.class))
                .addAugmentation(EndPoint2.class, epAugmentation)
                .build()).collect(Collectors.toList());
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
            .connectivity.context.ConnectivityService cs(
                    ReadWriteTransaction tx, String csId, Connection connection) {
        ConnectivityService cs = new ConnectivityServiceBuilder()
                .setUuid(new Uuid(csId))
                .setConnection(Collections.singletonList(connection.getUuid()))
                .setEndPoint(toEps(connection.getConnectionEndPoint()))
                .build();
        InstanceIdentifier<Context1> connectivityCtx = NrpDao.ctx().augmentation(Context1.class);
        tx.put(LogicalDatastoreType.OPERATIONAL,
                connectivityCtx.child(
                        ConnectivityService.class,  new ConnectivityServiceKey(cs.getUuid())), cs);

        return cs;

    }

    private List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
                .connectivity.service.EndPoint>
            toEps(List<ConnectionEndPoint> ceps) {

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
            .connectivity.service.EndPointBuilder builder
                = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
                    .connectivity.service.EndPointBuilder();
        return ceps.stream().map(cep -> {
            ServiceInterfacePoint sipRef =
                    TapiUtils.toSipRef(
                            new Uuid("sip:" + cep.getOwnedNodeEdgePointId().getValue()),
                            ServiceInterfacePoint.class);

            return builder.setServiceInterfacePoint(sipRef)
                    .setLocalId(cep.getConnectionEndPointId().getValue())
                    .build();
        }).collect(Collectors.toList());
    }

}
