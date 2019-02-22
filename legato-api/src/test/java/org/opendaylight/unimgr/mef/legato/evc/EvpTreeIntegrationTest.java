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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
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

/**
 * @author OmS.awasthi@Xoriant.Com*
 */
@SuppressWarnings("deprecation")
@RunWith(PowerMockRunner.class)
@PrepareForTest({InstanceIdentifier.class, LogicalDatastoreType.class, LegatoUtils.class})
public class EvpTreeIntegrationTest {

    @Mock
    private LegatoServiceController legatoServiceController;
    @Mock
    private TapiConnectivityService prestoConnectivityService;
    @Mock
    private DataBroker dataBroker;
    @SuppressWarnings("rawtypes")
    @Mock
    private Appender mockAppender;
    @Mock
    private WriteTransaction transaction;
    @Mock
    private ReadOnlyTransaction readTxn;
    @SuppressWarnings("rawtypes")
    @Mock
    private CheckedFuture checkedFuture;
    private EndPointBuilder endPointBuilder;
    //private EndPointBuilder endPointBuilder2;
   // private EndPointBuilder endPointBuilder3;
    private Evc evc;
    private Evc evc1;
    private EVCDao evcDao;
   // private EvcBuilder evcBuilder;
    private ch.qos.logback.classic.Logger root;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {

        prestoConnectivityService =
                PowerMockito.mock(TapiConnectivityService.class, Mockito.CALLS_REAL_METHODS);
        legatoServiceController =
                PowerMockito.mock(LegatoServiceController.class, Mockito.CALLS_REAL_METHODS);
        PowerMockito.mock(LegatoUtils.class, Mockito.CALLS_REAL_METHODS);

        MemberModifier.field(LegatoServiceController.class, "dataBroker")
                .set(legatoServiceController, dataBroker);

        final List<EndPoint> endPointList = new ArrayList<EndPoint>();
        final List<VlanIdType> vlanList = new ArrayList<VlanIdType>();
        vlanList.add(new VlanIdType(Integer.parseInt(Constants.VLAN_ID)));

        endPointBuilder = new EndPointBuilder();
        endPointBuilder.setUniId(new Identifier45(Constants.UNI_ID1));
        endPointBuilder.setRole(EvcUniRoleType.Root);
        endPointBuilder.setCeVlans((new CeVlansBuilder().setCeVlan(vlanList)).build());
        endPointList.add(endPointBuilder.build());

        endPointBuilder = new EndPointBuilder();
        endPointBuilder.setUniId(new Identifier45(Constants.UNI_ID2));
        endPointBuilder.setRole(EvcUniRoleType.Leaf);
        endPointBuilder.setCeVlans((new CeVlansBuilder().setCeVlan(vlanList)).build());
        endPointList.add(endPointBuilder.build());
        
        endPointBuilder = new EndPointBuilder();
        endPointBuilder.setUniId(new Identifier45(Constants.UNI_ID3));
        endPointBuilder.setRole(EvcUniRoleType.Leaf);
        endPointBuilder.setCeVlans((new CeVlansBuilder().setCeVlan(vlanList)).build());
        endPointList.add(endPointBuilder.build());

        evc = (Evc) new EvcBuilder().setEvcId(new EvcIdType(Constants.EVC_ID_TYPE))
                .setEndPoints(new EndPointsBuilder().setEndPoint(endPointList).build())
                .setMaxFrameSize(new MaxFrameSizeType(Constants.MAXFRAME_SIZE_TYPE))
                .setEvcId(new EvcIdType(Constants.EVC_ID_TYPE))
                .setConnectionType(ConnectionType.RootedMultipoint)
                .setSvcType(MefServiceType.Evptree).build();

        evc1 = (Evc) new EvcBuilder().setSvcType(MefServiceType.Evpl).build();

        List<String> uniIdList = new ArrayList<String>();
        uniIdList.add(Constants.UNI_ID1);
        uniIdList.add(Constants.UNI_ID2);
        evcDao = new EVCDao();
        evcDao.setMaxFrameSize(Integer.valueOf(Constants.MAXFRAME_SIZE_TYPE));
        evcDao.setEvcId(Constants.EVC_ID_TYPE);
        evcDao.setConnectionType(ConnectionType.RootedMultipoint.getName());
        evcDao.setSvcType(MefServiceType.Evptree.getName());
        evcDao.setUniIdList(uniIdList);
        root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void createEvcTrue()
            throws ReadFailedException, InterruptedException, ExecutionException {
        assertNotNull(evc);
        MemberModifier.suppress(MemberMatcher.method(LegatoUtils.class, Constants.PARSE_NODES));
        PowerMockito.mockStatic(LegatoUtils.class, Mockito.CALLS_REAL_METHODS);
        when(LegatoUtils.parseNodes(evc)).thenReturn(evcDao);
        EVCDao evcDao1 = LegatoUtils.parseNodes(evc);
        assertEquals(ConnectionType.RootedMultipoint.getName(), evcDao1.getConnectionType());
        assertEquals(MefServiceType.Evptree.getName(), evcDao1.getSvcType());
        CreateConnectivityServiceInput input = LegatoUtils.buildCreateConnectivityServiceInput(
                evcDao1, Constants.VLAN_ID, evc.getEndPoints().getEndPoint());
        this.prestoConnectivityService.createConnectivityService(input);
        final RpcResult<CreateConnectivityServiceOutput> rpcResult = mock(RpcResult.class);
        final ListenableFuture<RpcResult<CreateConnectivityServiceOutput>> future = mock(ListenableFuture.class);
        when(future.get()).thenReturn(rpcResult);
        when(rpcResult.isSuccessful()).thenReturn(true);
        when(prestoConnectivityService.createConnectivityService(input)).thenReturn(future);

        Future<RpcResult<CreateConnectivityServiceOutput>> result =
                this.prestoConnectivityService.createConnectivityService(input);

        assertTrue(result.get().isSuccessful());

        final Optional<Evc> optEvc = mock(Optional.class);
        when(optEvc.isPresent()).thenReturn(true);
        when(optEvc.get()).thenReturn(evc);
        MemberModifier.suppress(MemberMatcher.method(LegatoUtils.class, Constants.READ_EVC,
                DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));

        final InstanceIdentifier<SubscriberServices> instanceIdentifier =
                InstanceIdentifier.builder(MefServices.class).child(CarrierEthernet.class)
                        .child(SubscriberServices.class).build();
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        when(LegatoUtils.readEvc(any(DataBroker.class), any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class))).thenReturn(optEvc);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(Evc.class));
        when(transaction.submit()).thenReturn(checkedFuture);
        assertEquals(true,
                LegatoUtils.updateEvcInOperationalDB(evc, instanceIdentifier, dataBroker));
        verify(transaction).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(Evc.class));
        verify(transaction).submit();
    }

    @Test
    public void createEvcfalse() throws ReadFailedException, ExecutionException {

        MemberModifier.suppress(MemberMatcher.method(LegatoUtils.class, Constants.PARSE_NODES));
        EVCDao evcDao1 = evcDao;
        evcDao1.setSvcType(MefServiceType.Evpl.getName());
        PowerMockito.mockStatic(LegatoUtils.class, Mockito.CALLS_REAL_METHODS);

        when(LegatoUtils.parseNodes(evc1)).thenReturn((EVCDao) evcDao1);
        EVCDao evcDao2 = LegatoUtils.parseNodes(evc1);
        assertEquals(Integer.valueOf(Constants.MAXFRAME_SIZE_TYPE), evcDao2.getMaxFrameSize());
        assertEquals(Constants.EVC_ID_TYPE, evcDao2.getEvcId());
        assertEquals(ConnectionType.RootedMultipoint.getName(), evcDao2.getConnectionType());
        assertNotEquals(MefServiceType.Evptree.getName(), evcDao2.getSvcType());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void updateEvc() throws InterruptedException, ExecutionException {

        assertNotNull(evc);
        MemberModifier.suppress(MemberMatcher.method(LegatoUtils.class, Constants.PARSE_NODES));
        PowerMockito.mockStatic(LegatoUtils.class, Mockito.CALLS_REAL_METHODS);
        when(LegatoUtils.parseNodes(evc)).thenReturn(evcDao);
        EVCDao evcDao1 = LegatoUtils.parseNodes(evc);
        assertEquals(ConnectionType.RootedMultipoint.getName(), evcDao1.getConnectionType());
        assertEquals(MefServiceType.Evptree.getName(), evcDao1.getSvcType());

        assertEquals(true,
                callUpdateConnectionService(LegatoUtils.buildUpdateConnectivityServiceInput(evcDao1,
                        evcDao1.getUniIdList().get(0) + "#" + Constants.VLAN_ID, Constants.UUID)));

        final InstanceIdentifier<?> evcKey = InstanceIdentifier.create(MefServices.class)
                .child(CarrierEthernet.class).child(SubscriberServices.class)
                .child(Evc.class, new EvcKey(new EvcIdType(evc.getEvcId())));

        MemberModifier.suppress(MemberMatcher.method(LegatoUtils.class, Constants.READ_EVC,
                DataBroker.class, LogicalDatastoreType.class, InstanceIdentifier.class));
        final Optional<Evc> optEvc = mock(Optional.class);
        when(LegatoUtils.readEvc(any(DataBroker.class), any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class))).thenReturn(optEvc);
        when(optEvc.isPresent()).thenReturn(true);
        when(optEvc.get()).thenReturn(evc);

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class));
        when(transaction.submit()).thenReturn(checkedFuture);
        assertEquals(true, LegatoUtils.deleteFromOperationalDB(evcKey, dataBroker));
        verify(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction).submit();

        final InstanceIdentifier<SubscriberServices> instanceIdentifier =
                InstanceIdentifier.builder(MefServices.class).child(CarrierEthernet.class)
                        .child(SubscriberServices.class).build();

        WriteTransaction transaction2 = Mockito.mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction2);
        doNothing().when(transaction2).put(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(Evc.class));
        when(transaction2.submit()).thenReturn(checkedFuture);
        assertEquals(true,
                LegatoUtils.updateEvcInOperationalDB(evc, instanceIdentifier, dataBroker));
        verify(transaction2).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(Evc.class));
        verify(transaction2).submit();
    }

    @Test
    public void updateEvcFalse() throws InterruptedException, ExecutionException {

        MemberModifier.suppress(MemberMatcher.method(LegatoUtils.class, Constants.PARSE_NODES));
        EVCDao evcDao1 = evcDao;
        evcDao1.setSvcType(MefServiceType.Evpl.getName());
        PowerMockito.mockStatic(LegatoUtils.class, Mockito.CALLS_REAL_METHODS);
        when(LegatoUtils.parseNodes(evc1)).thenReturn((EVCDao) evcDao1);
        EVCDao evcDao2 = LegatoUtils.parseNodes(evc1);
        assertEquals(Integer.valueOf(Constants.MAXFRAME_SIZE_TYPE), evcDao2.getMaxFrameSize());
        assertEquals(Constants.EVC_ID_TYPE, evcDao2.getEvcId());
        assertEquals(ConnectionType.RootedMultipoint.getName(), evcDao2.getConnectionType());
        assertNotEquals(MefServiceType.Evptree.getName(), evcDao2.getSvcType());
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
        when(transaction.submit()).thenReturn(checkedFuture);
        assertEquals(true, LegatoUtils.deleteFromOperationalDB(evcKey, dataBroker));
        verify(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction).submit();
        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains("Received a request to delete node");
            }
        }));

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void deleteEvcFalse() throws InterruptedException, ExecutionException {

        DeleteConnectivityServiceInput input = new DeleteConnectivityServiceInputBuilder()
                .setServiceIdOrName(Constants.UUID).build();
        assertEquals(true, callDeleteConnectionService(input));

        final InstanceIdentifier<?> evcKey = InstanceIdentifier.create(MefServices.class)
                .child(CarrierEthernet.class).child(SubscriberServices.class)
                .child(Evc.class, new EvcKey(new EvcIdType(Constants.EVC_ID_TYPE)));

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class));
        when(transaction.submit()).thenReturn(checkedFuture);
        assertEquals(true, LegatoUtils.deleteFromOperationalDB(evcKey, dataBroker));
        verify(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction).submit();
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
