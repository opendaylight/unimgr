/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.legato.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.unimgr.mef.legato.dao.EVCDao;
import org.opendaylight.unimgr.mef.legato.util.LegatoUtils;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.MefGlobal;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.SlsProfiles;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.sls.profiles.Profile;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.sls.profiles.ProfileKey;
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
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.Identifier1024;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.Identifier45;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.MaxFrameSizeType;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.MefServiceType;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.VlanIdType;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.CreateConnectivityServiceInput1;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint2;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint7;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.UpdateConnectivityServiceInput1;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.connectivity.service.attrs.NrpCarrierEthConnectivityResource;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.connectivity.service.end.point.attrs.NrpCarrierEthConnectivityEndPointResource;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceInput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.util.concurrent.FluentFuture;

/*
 * @author Arif.Hussain@Xoriant.Com
 */

@SuppressWarnings("deprecation")
@RunWith(PowerMockRunner.class)
@PrepareForTest({LogicalDatastoreType.class, LegatoUtils.class, Optional.class})
public class LegatoUtilsTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();
    @Mock
    private DataBroker dataBroker;
    @Mock
    private WriteTransaction transaction;
    @SuppressWarnings("rawtypes")
    @Mock
    private FluentFuture checkedFuture;

    private EndPointBuilder endPointBuilder;
    private Evc evc;
    private EVCDao evcDao;

    private static final EvcIdType EVC_NODE_ID = new EvcIdType("EVC1");

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(LegatoUtils.class, Mockito.CALLS_REAL_METHODS);

        final List<VlanIdType> vlanIdTypes = new ArrayList<>();
        vlanIdTypes.add(new VlanIdType(301));

        CeVlansBuilder ceVlansBuilder = new CeVlansBuilder();
        ceVlansBuilder.setCeVlan(vlanIdTypes);

        final List<EndPoint> endPointList = new ArrayList<EndPoint>();
        endPointBuilder = new EndPointBuilder();
        endPointBuilder.setCeVlans(ceVlansBuilder.build());
        endPointBuilder.setUniId(new Identifier45(Constants.UNI_ID1));
        endPointBuilder.setRole(EvcUniRoleType.Root);

        endPointList.add(endPointBuilder.build());
        endPointBuilder = new EndPointBuilder();
        endPointBuilder.setCeVlans(ceVlansBuilder.build());
        endPointBuilder.setUniId(new Identifier45(Constants.UNI_ID2));
        endPointBuilder.setRole(EvcUniRoleType.Leaf);

        endPointList.add(endPointBuilder.build());
        endPointBuilder = new EndPointBuilder();
        endPointBuilder.setCeVlans(ceVlansBuilder.build());
        endPointBuilder.setUniId(new Identifier45(Constants.UNI_ID3));
        endPointBuilder.setRole(EvcUniRoleType.Leaf);
        endPointList.add(endPointBuilder.build());

        evc = (Evc) new EvcBuilder().setEvcId(new EvcIdType(Constants.EVC_ID_TYPE))
                .setEndPoints(new EndPointsBuilder().setEndPoint(endPointList).build())
                .setMaxFrameSize(new MaxFrameSizeType(Constants.MAXFRAME_SIZE_TYPE))
                .setEvcId(new EvcIdType(Constants.EVC_ID_TYPE))
                .setConnectionType(ConnectionType.MultipointToMultipoint)
                .setSvcType(MefServiceType.Eplan).build();
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testReadEvc() throws ReadFailedException, InterruptedException, ExecutionException {
        // having
        final InstanceIdentifier<Evc> evcID = InstanceIdentifier.create(MefServices.class)
                .child(CarrierEthernet.class).child(SubscriberServices.class)
                .child(Evc.class, new EvcKey(new EvcIdType(EVC_NODE_ID)));

        // when
        ReadTransaction readTransaction = mock(ReadTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readTransaction);
        FluentFuture<Optional<Evc>> nodeFuture = mock(FluentFuture.class);
        Optional<Evc> optNode = PowerMockito.mock(Optional.class);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(nodeFuture);
        when(nodeFuture.get()).thenReturn(optNode);

        // then
        Optional<Evc> expectedOpt =
                LegatoUtils.readEvc(dataBroker, LogicalDatastoreType.CONFIGURATION, evcID);
        verify(readTransaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        assertNotNull(expectedOpt);
        assertEquals(expectedOpt, optNode);
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testReadProfiles() throws ReadFailedException, InterruptedException, ExecutionException {

        // having
        final InstanceIdentifier<Profile> profileID =
                InstanceIdentifier.create(MefGlobal.class).child(SlsProfiles.class)
                        .child(Profile.class, new ProfileKey(new Identifier1024(Constants.ONE)));

        // when
        ReadTransaction readTransaction = mock(ReadTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readTransaction);
        FluentFuture<Optional<Profile>> nodeFuture =
                mock(FluentFuture.class);
        Optional<Profile> optNode = PowerMockito.mock(Optional.class);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(nodeFuture);
        when(nodeFuture.get()).thenReturn(optNode);

        // then
        Optional<Profile> expectedOpt =
                (Optional<Profile>) LegatoUtils.readProfile(Constants.SLS_PROFILES,
                        dataBroker, LogicalDatastoreType.CONFIGURATION, profileID);

        verify(readTransaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        assertNotNull(expectedOpt);
        assertEquals(expectedOpt, optNode);

    }


    @SuppressWarnings("unchecked")
    @Test
    public void testAddToOperationalDB() {
        // having
        final SlsProfiles slsProfile = mock(SlsProfiles.class);
        final InstanceIdentifier<SlsProfiles> instanceIdentifier = InstanceIdentifier
            .create(MefGlobal.class).child(SlsProfiles.class);

        // when
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).merge(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(Profile.class));
        when(transaction.commit()).thenReturn(checkedFuture);

        // then
        LegatoUtils.addToOperationalDB(slsProfile, instanceIdentifier, dataBroker);
        verify(transaction).merge(any(LogicalDatastoreType.class), any(),
                any());
        verify(transaction).commit();
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteFromOperationalDB() {
        // having
        final InstanceIdentifier<Evc> evcID = InstanceIdentifier.create(MefServices.class)
                .child(CarrierEthernet.class).child(SubscriberServices.class)
                .child(Evc.class, new EvcKey(new EvcIdType(EVC_NODE_ID)));

        // when
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class));
        when(transaction.commit()).thenReturn(checkedFuture);

        // then
        assertEquals(true, LegatoUtils.deleteFromOperationalDB(evcID, dataBroker));
        verify(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction).commit();
    }


    @Test
    @Ignore
    public void testBuildCreateConnectivityServiceInput() {
        // having
        assertNotNull(evc);
        evcDao = LegatoUtils.parseNodes(evc);

        MemberModifier.suppress(MemberMatcher.method(LegatoUtils.class, Constants.PARSE_NODES));
        when(LegatoUtils.parseNodes(evc)).thenReturn(evcDao);

        // when
        CreateConnectivityServiceInput future = mock(CreateConnectivityServiceInput.class);

        MemberModifier.suppress(MemberMatcher.method(LegatoUtils.class, Constants.CREATE_CONNECTIVITY_INPUT));
        when(LegatoUtils.buildCreateConnectivityServiceInput(evcDao,
            String.valueOf(Constants.VLAN_ID), evc.getEndPoints().getEndPoint()))
                        .thenReturn(future);
        // then
        assertNotNull(future);
        assertEquals(future, LegatoUtils.buildCreateConnectivityServiceInput(evcDao,
            String.valueOf(Constants.VLAN_ID), evc.getEndPoints().getEndPoint()));
    }


    @Test
    @Ignore
    public void testBuildUpdateConnectivityServiceInput() {

        // having
        assertNotNull(evc);
        evcDao = LegatoUtils.parseNodes(evc);

        MemberModifier.suppress(MemberMatcher.method(LegatoUtils.class, Constants.PARSE_NODES));
        when(LegatoUtils.parseNodes(evc)).thenReturn(evcDao);

        // when
        final UpdateConnectivityServiceInput input = mock(UpdateConnectivityServiceInput.class);

        MemberModifier.suppress(MemberMatcher.method(LegatoUtils.class, Constants.UPDATE_CONNECTIVITY_INPUT));
        when(LegatoUtils.buildUpdateConnectivityServiceInput(evcDao,
            evcDao.getUniIdList().get(0), Constants.UUID)).thenReturn(input);

        // then
        assertNotNull(input);
        assertEquals(input, LegatoUtils.buildUpdateConnectivityServiceInput(evcDao,
            evcDao.getUniIdList().get(0), Constants.UUID));
    }


    @Test
    @Ignore
    public void testBuildNrpCarrierEthConnectivityResource() {
        // having
        final NrpCarrierEthConnectivityResource nrpCarrierEthConnectivityResource =
            mock(NrpCarrierEthConnectivityResource.class);

        // when
        MemberModifier.suppress(MemberMatcher.method(LegatoUtils.class, Constants.NRP_CARRIER_ETH_CON_RESOURCE));
        when(LegatoUtils.buildNrpCarrierEthConnectivityResource(String.valueOf(Constants.MAXFRAME_SIZE_TYPE)))
            .thenReturn(nrpCarrierEthConnectivityResource);

        // then
        assertNotNull(nrpCarrierEthConnectivityResource);
        assertEquals(nrpCarrierEthConnectivityResource, LegatoUtils.buildNrpCarrierEthConnectivityResource(
                        String.valueOf(Constants.MAXFRAME_SIZE_TYPE)));
    }


    @Test
    @Ignore
    public void testBuildNrpCarrierEthConnectivityEndPointResource() {
        // having
        final NrpCarrierEthConnectivityEndPointResource input = mock(NrpCarrierEthConnectivityEndPointResource.class);

        // when
        MemberModifier.suppress(MemberMatcher.method(LegatoUtils.class,
            Constants.NRP_CARRIER_ETH_CON_ENDPOINT_RESOURCE));
        when(LegatoUtils.buildNrpCarrierEthConnectivityEndPointResource(
            String.valueOf(Constants.VLAN_ID))).thenReturn(input);

        // then
        assertNotNull(input);
        assertEquals(input, LegatoUtils.buildNrpCarrierEthConnectivityEndPointResource(
            String.valueOf(Constants.VLAN_ID)));
    }


    @Test
    @Ignore
    public void testBuildCreateEthConnectivityEndPointAugmentation() {
        // having
        final EndPoint2 createEndPoint = mock(EndPoint2.class);

        // when
        MemberModifier.suppress(MemberMatcher.method(LegatoUtils.class,
            Constants.CREATE_ETH_CON_ENDPOINT_AUGMENTATION));
        when(LegatoUtils.buildCreateEthConnectivityEndPointAugmentation(String.valueOf(Constants.VLAN_ID)))
            .thenReturn(createEndPoint);

        // then
        assertNotNull(createEndPoint);
        assertEquals(createEndPoint, LegatoUtils.buildCreateEthConnectivityEndPointAugmentation(
            String.valueOf(Constants.VLAN_ID)));
    }


    @Test
    @Ignore
    public void testBuildUpdateEthConnectivityEndPointAugmentation() {
        // having
        final EndPoint7 updateEndPoint = mock(EndPoint7.class);

        //when
        MemberModifier.suppress(MemberMatcher.method(LegatoUtils.class,
            Constants.UPDATE_ETH_CON_ENDPOINT_AUGMENTATION));
        when(LegatoUtils.buildUpdateEthConnectivityEndPointAugmentation(String.valueOf(Constants.VLAN_ID)))
            .thenReturn(updateEndPoint);

        // then
        assertNotNull(updateEndPoint);
        assertEquals(updateEndPoint, LegatoUtils.buildUpdateEthConnectivityEndPointAugmentation(
            String.valueOf(Constants.VLAN_ID)));
    }


    @Test
    @Ignore
    public void testBuildCreateConServiceAugmentation() {
        // having
        final CreateConnectivityServiceInput1 createConServInput = mock(CreateConnectivityServiceInput1.class);

        // when
        MemberModifier.suppress(MemberMatcher.method(LegatoUtils.class, Constants.CREATE_CON_SERVICE_AUGMENTATION));
        when(LegatoUtils.buildCreateConServiceAugmentation(String.valueOf(Constants.MAXFRAME_SIZE_TYPE)))
            .thenReturn(createConServInput);

        // then
        assertNotNull(createConServInput);
        assertEquals(createConServInput, LegatoUtils.buildCreateConServiceAugmentation(
            String.valueOf(Constants.MAXFRAME_SIZE_TYPE)));
    }


    @Test
    @Ignore
    public void testBuildUpdateConServiceAugmentation() {
        // having
        final UpdateConnectivityServiceInput1 updateConServInput = mock(UpdateConnectivityServiceInput1.class);

        // when
        MemberModifier.suppress(MemberMatcher.method(LegatoUtils.class, Constants.UPDATE_CON_SERVICE_AUGMENTATION));
        when(LegatoUtils.buildUpdateConServiceAugmentation(String.valueOf(Constants.MAXFRAME_SIZE_TYPE)))
                        .thenReturn(updateConServInput);

        // then
        assertNotNull(updateConServInput);
        assertEquals(updateConServInput, LegatoUtils.buildUpdateConServiceAugmentation(
            String.valueOf(Constants.MAXFRAME_SIZE_TYPE)));
    }

}
