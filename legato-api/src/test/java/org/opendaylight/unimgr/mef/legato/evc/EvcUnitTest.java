/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.legato.evc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.unimgr.mef.legato.dao.EVCDao;
import org.opendaylight.unimgr.mef.legato.util.LegatoUtils;
import org.opendaylight.unimgr.mef.legato.utils.Constants;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.MefServices;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.CarrierEthernet;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.SubscriberServices;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.Evc;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.EvcBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.EvcKey;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.evc.CosNamesBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.evc.EndPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.evc.cos.names.CosName;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.evc.cos.names.CosNameBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.evc.end.points.EndPoint;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.evc.end.points.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.evc.end.points.end.point.CeVlansBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.ConnectionType;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.EvcIdType;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.Identifier1024;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.Identifier45;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.MaxFrameSizeType;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.MefServiceType;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.VlanIdType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.DeleteConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.DeleteConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.DeleteConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.TapiConnectivityService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;


/*
 * @author Arif.Hussain@Xoriant.Com
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({LegatoUtils.class})
public class EvcUnitTest {

    @Mock  private TapiConnectivityService prestoConnectivityService;
    @Mock  private DataBroker dataBroker;
    @SuppressWarnings("rawtypes")
    @Mock  private FluentFuture checkedFuture;
    @SuppressWarnings("rawtypes")
    @Mock  private Appender mockAppender;
    @Mock  private WriteTransaction transaction;
    @Mock  private ReadTransaction readTxn ;
    private EndPointBuilder endPointBuilder;
    private EVCDao evcDao;
    private Evc evc;
    private ch.qos.logback.classic.Logger root;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {

        CosNameBuilder builder = new CosNameBuilder();
        builder.setName(new Identifier1024(Constants.COSNAME));

        final List<CosName> cosNameList = new ArrayList<CosName>();
        cosNameList.add(builder.build());

        CosNamesBuilder cosNamesBuilder = new CosNamesBuilder();
        cosNamesBuilder.setCosName(cosNameList);

        final List<VlanIdType> vlanIdTypes = new ArrayList<>();
        vlanIdTypes.add(new VlanIdType(Constants.VLAN_ID));

        CeVlansBuilder ceVlansBuilder = new CeVlansBuilder();
        ceVlansBuilder.setCeVlan(vlanIdTypes);

        final List<EndPoint> endPointList = new ArrayList<EndPoint>();
        endPointBuilder = new EndPointBuilder();
        endPointBuilder.setCeVlans(ceVlansBuilder.build());
        endPointBuilder.setUniId(new Identifier45(Constants.UNI_ID1));
        endPointList.add(endPointBuilder.build());

        endPointBuilder = new EndPointBuilder();
        endPointBuilder.setCeVlans(ceVlansBuilder.build());
        endPointBuilder.setUniId(new Identifier45(Constants.UNI_ID2));
        endPointList.add(endPointBuilder.build());

        evc = (Evc) new EvcBuilder().setEvcId(new EvcIdType(Constants.EVC_ID_TYPE))
                .setMaxFrameSize(new MaxFrameSizeType(Constants.MAXFRAME_SIZE_TYPE))
                .setEvcId(new EvcIdType(Constants.EVC_ID_TYPE)).setSvcType(MefServiceType.Epl)
                .setConnectionType(ConnectionType.PointToPoint).setCosNames(cosNamesBuilder.build())
                .setEndPoints(new EndPointsBuilder().setEndPoint(endPointList).build()).build();

        root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateEvc() throws ReadFailedException {

        // having
        assertNotNull(evc);
        evcDao = LegatoUtils.parseNodes(evc);

        PowerMockito.stub(PowerMockito.method(LegatoUtils.class, "parseNodes", Evc.class)).toReturn(evcDao);

        // when
        CreateConnectivityServiceInput future = mock(CreateConnectivityServiceInput.class);

        MemberModifier.suppress(MemberMatcher.method(LegatoUtils.class, Constants.CREATE_CONNECTIVITY_INPUT));
        PowerMockito.mockStatic(LegatoUtils.class, Mockito.CALLS_REAL_METHODS);

        PowerMockito.stub(PowerMockito.method(
                LegatoUtils.class, "buildCreateConnectivityServiceInput", EVCDao.class, String.class, List.class))
        .toReturn(future);

        // then
        final Optional<Evc> optEvc = mock(Optional.class);
        when(optEvc.isPresent()).thenReturn(true);
        when(optEvc.get()).thenReturn(evc);

        final InstanceIdentifier<SubscriberServices> instanceIdentifier = InstanceIdentifier.builder(MefServices.class)
            .child(CarrierEthernet.class)
            .child(SubscriberServices.class).build();

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);

        PowerMockito.stub(
                PowerMockito.method(LegatoUtils.class,
                        "readEvc", DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class))
        .toReturn(optEvc);

        when(transaction.commit()).thenReturn(checkedFuture);

        assertEquals(true, LegatoUtils.updateEvcInOperationalDB(evc, instanceIdentifier, dataBroker));
        verify(transaction).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(SubscriberServices.class));
        verify(transaction).commit();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateEvc() throws InterruptedException, ExecutionException, ReadFailedException {

        // having
        assertNotNull(evc);
        evcDao = LegatoUtils.parseNodes(evc);

        PowerMockito.stub(PowerMockito.method(LegatoUtils.class, "parseNodes", Evc.class)).toReturn(evcDao);

        assertEquals(ConnectionType.PointToPoint.getName(), evcDao.getConnectionType());
        assertEquals(MefServiceType.Epl.getName(), evcDao.getSvcType());
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

        this.testCreateEvc();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testDeleteEvc() throws Exception {
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
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class));
        when(transaction.commit()).thenReturn(checkedFuture);

        assertEquals(true, LegatoUtils.deleteFromOperationalDB(evcKey, dataBroker));

        verify(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction).commit();
        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains("Received a request to delete node");
            }
        }));

    }

}
