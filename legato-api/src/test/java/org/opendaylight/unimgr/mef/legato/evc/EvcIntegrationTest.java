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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.legato.LegatoServiceController;
import org.opendaylight.unimgr.mef.legato.dao.EVCDao;
import org.opendaylight.unimgr.mef.legato.util.LegatoUtils;
import org.opendaylight.unimgr.mef.legato.utils.Constants;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivatorException;
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
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.evc.cos.names.CosNameKey;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.DeleteConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.DeleteConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.TapiConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceOutput;
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

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;

/**
 * @author Arif.Hussain@Xoriant.Com
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({LogicalDatastoreType.class, LegatoUtils.class, Optional.class})
public class EvcIntegrationTest {

    @Mock private LegatoServiceController legatoServiceController;
    @Mock private TapiConnectivityService prestoConnectivityService;
    @Mock private DataBroker dataBroker;
    @SuppressWarnings("rawtypes")
    @Mock private Appender mockAppender;
    @Mock private WriteTransaction transaction;
    @Mock private ReadTransaction readTxn;
    @SuppressWarnings("rawtypes")
    @Mock private FluentFuture checkedFuture;
    private EndPointBuilder endPointBuilder1;
    private EndPointBuilder endPointBuilder2;
    private Evc evc;
    private ch.qos.logback.classic.Logger root;


    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        prestoConnectivityService =
                PowerMockito.mock(TapiConnectivityService.class, Mockito.CALLS_REAL_METHODS);
        legatoServiceController =
                PowerMockito.mock(LegatoServiceController.class, Mockito.CALLS_REAL_METHODS);
        PowerMockito.mockStatic(LegatoUtils.class, Mockito.CALLS_REAL_METHODS);

        MemberModifier.field(LegatoServiceController.class, "dataBroker")
                .set(legatoServiceController, dataBroker);

        CosNameBuilder builder = new CosNameBuilder();
        builder.setName(new Identifier1024(Constants.COSNAME));
        builder.withKey(new CosNameKey(new Identifier1024(Constants.COSNAME)));

        final List<CosName> cosNameList = new ArrayList<CosName>();
        cosNameList.add(builder.build());

        CosNamesBuilder cosNamesBuilder = new CosNamesBuilder();
        cosNamesBuilder.setCosName(cosNameList);

        final List<VlanIdType> vlanIdTypes = new ArrayList<>();
        vlanIdTypes.add(new VlanIdType(Constants.VLAN_ID_TYPE));

        CeVlansBuilder ceVlansBuilder = new CeVlansBuilder();
        ceVlansBuilder.setCeVlan(vlanIdTypes);

        endPointBuilder1 = new EndPointBuilder();
        endPointBuilder1.setCeVlans(ceVlansBuilder.build());
        endPointBuilder1.setUniId(new Identifier45(Constants.UNI_ID1));

        endPointBuilder2 = new EndPointBuilder();
        endPointBuilder2.setCeVlans(ceVlansBuilder.build());
        endPointBuilder2.setUniId(new Identifier45(Constants.UNI_ID2));

        final List<EndPoint> endPointList = new ArrayList<EndPoint>();
        endPointList.add(endPointBuilder1.build());
        endPointList.add(endPointBuilder2.build());

        evc = (Evc) new EvcBuilder().withKey(new EvcKey(new EvcIdType(Constants.EVC_ID_TYPE)))
                .setMaxFrameSize(new MaxFrameSizeType(Constants.MAXFRAME_SIZE_TYPE))
                .setEvcId(new EvcIdType(Constants.EVC_ID_TYPE))
                .setSvcType(MefServiceType.Epl)
                .setConnectionType(ConnectionType.PointToPoint).setCosNames(cosNamesBuilder.build())
                .setEndPoints(new EndPointsBuilder().setEndPoint(endPointList).build()).build();

        root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

    }


    @SuppressWarnings("unchecked")
    @Test
    public void createEvc() throws InterruptedException, ExecutionException, ResourceActivatorException {

        try {
            assertNotNull(evc);
            final EVCDao evcDao = LegatoUtils.parseNodes(evc);
            Future<RpcResult<CreateConnectivityServiceOutput>> result =
                    this.prestoConnectivityService.createConnectivityService(
                            LegatoUtils.buildCreateConnectivityServiceInput(evcDao));
            assertTrue(result.get().isSuccessful());

            final InstanceIdentifier<?> evcKey = InstanceIdentifier.create(MefServices.class)
                    .child(CarrierEthernet.class).child(SubscriberServices.class)
                    .child(Evc.class, new EvcKey(new EvcIdType(evc.getEvcId())));

            final Optional<Evc> optEvc = PowerMockito.mock(Optional.class);
            when(optEvc.isPresent()).thenReturn(true);
            when(optEvc.get()).thenReturn(evc);
            MemberModifier.suppress(MemberMatcher.method(LegatoUtils.class, Constants.READ_EVC,
                    DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
            when(LegatoUtils.readEvc(any(DataBroker.class), any(LogicalDatastoreType.class),
                    evcKey)).thenReturn(optEvc);

            final InstanceIdentifier<SubscriberServices> instanceIdentifier =
                    InstanceIdentifier.builder(MefServices.class).child(CarrierEthernet.class)
                            .child(SubscriberServices.class).build();

            doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                    any(InstanceIdentifier.class), any(Evc.class));
            when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
            assertEquals(true, LegatoUtils.updateEvcInOperationalDB(evc, instanceIdentifier, dataBroker));
            verify(transaction).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                    any(Evc.class));
            verify(transaction).commit();

        } catch (Exception ex) {
        }

    }


    @SuppressWarnings("unchecked")
    @Test
    @Ignore
    public void updateEvc() throws InterruptedException, ExecutionException {

        assertNotNull(evc);
        final EVCDao evcDao = LegatoUtils.parseNodes(evc);
        assertEquals(true,
                callUpdateConnectionService(LegatoUtils.buildUpdateConnectivityServiceInput(evcDao,
                        evcDao.getUniList().get(0), Constants.UUID)));

        final InstanceIdentifier<?> evcKey = InstanceIdentifier.create(MefServices.class)
                .child(CarrierEthernet.class).child(SubscriberServices.class)
                .child(Evc.class, new EvcKey(new EvcIdType(evc.getEvcId())));

        MemberModifier.suppress(MemberMatcher.method(LegatoUtils.class, Constants.READ_EVC,
                DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        final Optional<Evc> optEvc = PowerMockito.mock(Optional.class);
        when(LegatoUtils.readEvc(any(DataBroker.class), any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class))).thenReturn(optEvc);
        when(optEvc.isPresent()).thenReturn(true);
        when(optEvc.get()).thenReturn(evc);

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class));
        when(transaction.commit()).thenReturn(checkedFuture);
        assertEquals(true, LegatoUtils.deleteFromOperationalDB(evcKey, dataBroker));
        verify(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction).commit();

        final InstanceIdentifier<SubscriberServices> instanceIdentifier =
                InstanceIdentifier.builder(MefServices.class).child(CarrierEthernet.class)
                        .child(SubscriberServices.class).build();

        WriteTransaction transaction2 = Mockito.mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction2);
        doNothing().when(transaction2).put(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(Evc.class));
        when(transaction2.commit()).thenReturn(checkedFuture);
        assertEquals(true, LegatoUtils.updateEvcInOperationalDB(evc, instanceIdentifier, dataBroker));
        verify(transaction2).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(Evc.class));
        verify(transaction2).commit();
    }

    private boolean callUpdateConnectionService(
            UpdateConnectivityServiceInput updateConnectivityServiceInput) {
        try {
            Future<RpcResult<UpdateConnectivityServiceOutput>> response =
                    this.prestoConnectivityService
                            .updateConnectivityService(updateConnectivityServiceInput);

            return true;

        } catch (Exception ex) {
            return false;
        }
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void deleteEvc() throws InterruptedException, ExecutionException {

        DeleteConnectivityServiceInput input = new DeleteConnectivityServiceInputBuilder()
                .setServiceIdOrName(Constants.UUID).build();
        assertEquals(true, callDeleteConnectionService(input));

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

    private boolean callDeleteConnectionService(
            DeleteConnectivityServiceInput deleteConnectivityServiceInput) {
        try {
            this.prestoConnectivityService
                    .deleteConnectivityService(deleteConnectivityServiceInput);
            return true;

        } catch (Exception ex) {
            return false;
        }
    }


}
