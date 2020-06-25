/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.legato.evc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.unimgr.mef.legato.LegatoServiceController;
import org.opendaylight.unimgr.mef.legato.dao.EVCDao;
import org.opendaylight.unimgr.mef.legato.util.LegatoUtils;
import org.opendaylight.unimgr.mef.legato.utils.Constants;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.MefServices;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.CarrierEthernet;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.SubscriberServices;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.Evc;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.EvcBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.EvcKey;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.evc.EndPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.evc.end.points.EndPoint;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.evc.end.points.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.evc.end.points.end.point.CeVlansBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.ConnectionType;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.EvcIdType;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.EvcUniRoleType;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.Identifier45;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.MaxFrameSizeType;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.MefServiceType;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.VlanIdType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.DeleteConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.DeleteConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.DeleteConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.TapiConnectivityService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


/*
 * @author OmS.awasthi@Xoriant.Com
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({LegatoUtils.class})
public class EpTreeIntegrationTest {

    @Mock
    private LegatoServiceController legatoServiceController;
    @Mock
    private TapiConnectivityService prestoConnectivityService;
    @Mock
    private DataBroker dataBroker;
    @Mock
    private WriteTransaction transaction;
    @Mock
    private ReadTransaction readTxn;
    @SuppressWarnings("rawtypes")
    @Mock
    private FluentFuture fluentFuture;
    private EndPointBuilder endPointBuilder;
    private Evc evc;
    private EVCDao evcDao;

    @Before
    public void setUp() throws Exception {

        final List<VlanIdType> vlanIdTypes = new ArrayList<>();
        vlanIdTypes.add(new VlanIdType(301));

        CeVlansBuilder ceVlansBuilder = new CeVlansBuilder();
        ceVlansBuilder.setCeVlan(vlanIdTypes);

        final List<EndPoint> endPointList = new ArrayList<EndPoint>();
        endPointBuilder = new EndPointBuilder();
        endPointBuilder.setUniId(new Identifier45(Constants.UNI_ID1));
        endPointBuilder.setCeVlans(ceVlansBuilder.build());
        endPointBuilder.setRole(EvcUniRoleType.Root);
        endPointList.add(endPointBuilder.build());

        endPointBuilder = new EndPointBuilder();
        endPointBuilder.setUniId(new Identifier45(Constants.UNI_ID2));
        endPointBuilder.setCeVlans(ceVlansBuilder.build());
        endPointBuilder.setRole(EvcUniRoleType.Leaf);
        endPointList.add(endPointBuilder.build());

        endPointBuilder = new EndPointBuilder();
        endPointBuilder.setUniId(new Identifier45(Constants.UNI_ID3));
        endPointBuilder.setCeVlans(ceVlansBuilder.build());
        endPointBuilder.setRole(EvcUniRoleType.Leaf);
        endPointList.add(endPointBuilder.build());

        evc = (Evc) new EvcBuilder().setEvcId(new EvcIdType(Constants.EVC_ID_TYPE))
                .setEndPoints(new EndPointsBuilder().setEndPoint(endPointList).build())
                .setMaxFrameSize(new MaxFrameSizeType(Constants.MAXFRAME_SIZE_TYPE))
                .setEvcId(new EvcIdType(Constants.EVC_ID_TYPE))
                .setConnectionType(ConnectionType.RootedMultipoint)
                .setSvcType(MefServiceType.Eptree).build();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateService() throws ReadFailedException, InterruptedException, ExecutionException {

        assertNotNull(evc);
        evcDao = LegatoUtils.parseNodes(evc);

        PowerMockito.stub(PowerMockito.method(LegatoUtils.class, "parseNodes", Evc.class)).toReturn(evcDao);

        assertEquals(ConnectionType.RootedMultipoint.getName(), evcDao.getConnectionType());
        assertEquals(MefServiceType.Eptree.getName(), evcDao.getSvcType());

        CreateConnectivityServiceInput input = LegatoUtils.buildCreateConnectivityServiceInput(evcDao,
            String.valueOf(Constants.VLAN_ID), evc.getEndPoints().getEndPoint());

        final RpcResult<CreateConnectivityServiceOutput> rpcResult = mock(RpcResult.class);
        final ListenableFuture<RpcResult<CreateConnectivityServiceOutput>> future = mock(ListenableFuture.class);

        when(future.get()).thenReturn(rpcResult);
        when(rpcResult.isSuccessful()).thenReturn(true);
        when(prestoConnectivityService.createConnectivityService(input)).thenReturn(future);

        // when
        Future<RpcResult<CreateConnectivityServiceOutput>> result = this.prestoConnectivityService
            .createConnectivityService(input);
        // then
        assertTrue(result.get().isSuccessful());

        final Optional<Evc> optEvc = mock(Optional.class);
        when(optEvc.isPresent()).thenReturn(true);
        when(optEvc.get()).thenReturn(evc);

        final InstanceIdentifier<SubscriberServices> instanceIdentifier = InstanceIdentifier.builder(MefServices.class)
            .child(CarrierEthernet.class).child(SubscriberServices.class).build();

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);

        PowerMockito.stub(
                PowerMockito.method(LegatoUtils.class,
                        "readEvc", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class))
            .toReturn(optEvc);

        when(transaction.commit()).thenReturn(fluentFuture);

        assertEquals(true,LegatoUtils.updateEvcInOperationalDB(evc, instanceIdentifier, dataBroker));
        verify(transaction).put(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class),
                any(SubscriberServices.class));
        verify(transaction).commit();
    }

    @Test
    public void createEvcfalse() throws ReadFailedException, ExecutionException {

        assertNotNull(evc);
        evcDao = LegatoUtils.parseNodes(evc);
        evcDao.setSvcType(MefServiceType.Evptree.getName());

        PowerMockito.stub(PowerMockito.method(LegatoUtils.class, "parseNodes", Evc.class)).toReturn(evcDao);

        // then
        assertEquals(ConnectionType.RootedMultipoint.getName(), evcDao.getConnectionType());
        assertNotEquals(MefServiceType.Eptree.getName(), evcDao.getSvcType());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void updateEvc() throws InterruptedException, ExecutionException, ReadFailedException {

        // having
        assertNotNull(evc);
        evcDao = LegatoUtils.parseNodes(evc);

        PowerMockito.stub(PowerMockito.method(LegatoUtils.class, "parseNodes", Evc.class)).toReturn(evcDao);

        assertEquals(ConnectionType.RootedMultipoint.getName(), evcDao.getConnectionType());
        assertEquals(MefServiceType.Eptree.getName(), evcDao.getSvcType());
        DeleteConnectivityServiceInput deleteConnectivityServiceInput = new DeleteConnectivityServiceInputBuilder()
            .setServiceIdOrName(Constants.UUID).build();

        final RpcResult<DeleteConnectivityServiceOutput> rpcResult = mock(RpcResult.class);
        final ListenableFuture<RpcResult<DeleteConnectivityServiceOutput>> future = mock(ListenableFuture.class);

        when(future.get()).thenReturn(rpcResult);
        when(rpcResult.isSuccessful()).thenReturn(true);
        when(prestoConnectivityService.deleteConnectivityService(deleteConnectivityServiceInput)).thenReturn(future);

        // when
        Future<RpcResult<DeleteConnectivityServiceOutput>> result = this.prestoConnectivityService
            .deleteConnectivityService(deleteConnectivityServiceInput);

        // then
        assertTrue(result.get().isSuccessful());

        this.testCreateService();
    }

    @Test
    public void testUpdateServiceBadInput() throws InterruptedException, ExecutionException {

        assertNotNull(evc);
        evcDao = LegatoUtils.parseNodes(evc);
        evcDao.setSvcType(MefServiceType.Evptree.getName());

        PowerMockito.stub(PowerMockito.method(LegatoUtils.class, "parseNodes", Evc.class)).toReturn(evcDao);

        // then
        assertEquals(ConnectionType.RootedMultipoint.getName(), evcDao.getConnectionType());
        assertNotEquals(MefServiceType.Eptree.getName(), evcDao.getSvcType());
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testDeleteService() throws InterruptedException, ExecutionException {
        // having
        DeleteConnectivityServiceInput input = new DeleteConnectivityServiceInputBuilder()
                .setServiceIdOrName(Constants.UUID).build();

        final RpcResult<DeleteConnectivityServiceOutput> rpcResult = mock(RpcResult.class);
        final ListenableFuture<RpcResult<DeleteConnectivityServiceOutput>> future = mock(ListenableFuture.class);

        when(future.get()).thenReturn(rpcResult);
        when(rpcResult.isSuccessful()).thenReturn(true);
        when(prestoConnectivityService.deleteConnectivityService(input)).thenReturn(future);

        // when
        Future<RpcResult<DeleteConnectivityServiceOutput>> result = this.prestoConnectivityService
            .deleteConnectivityService(input);

        // then
        assertTrue(result.get().isSuccessful());

        final InstanceIdentifier<?> evcKey = InstanceIdentifier.create(MefServices.class)
                .child(CarrierEthernet.class).child(SubscriberServices.class)
                .child(Evc.class, new EvcKey(new EvcIdType(Constants.EVC_ID_TYPE)));

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        when(transaction.commit()).thenReturn(fluentFuture);

        assertEquals(true, LegatoUtils.deleteFromOperationalDB(evcKey, dataBroker));

        verify(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction).commit();
    }

    @Test
    public void testDeleteServiceBadInput() throws InterruptedException, ExecutionException {

        // having
        String uuid = "cs:162052f6bb1:73aaf0f6";

        // when
        DeleteConnectivityServiceInput input = new DeleteConnectivityServiceInputBuilder()
                .setServiceIdOrName(Constants.UUID).build();

        // then
        assertNotEquals(uuid, input.getServiceIdOrName());

    }

}
