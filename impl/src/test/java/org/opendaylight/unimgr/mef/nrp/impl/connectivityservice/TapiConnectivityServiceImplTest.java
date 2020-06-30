/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.connectivityservice;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverRepoService;
import org.opendaylight.unimgr.mef.nrp.api.FailureResult;
import org.opendaylight.unimgr.mef.nrp.api.RequestDecomposer;
import org.opendaylight.unimgr.mef.nrp.api.RequestValidator;
import org.opendaylight.unimgr.mef.nrp.api.Subrequrest;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivatorException;
import org.opendaylight.unimgr.mef.nrp.common.TapiUtils;
import org.opendaylight.unimgr.mef.nrp.impl.ConnectivityServiceIdResourcePool;
import org.opendaylight.unimgr.mef.nrp.impl.DefaultValidator;
import org.opendaylight.unimgr.utils.ActivationDriverMocks;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ServiceType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.end.point.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.create.connectivity.service.input.ConnConstraintBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.create.connectivity.service.input.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.create.connectivity.service.input.EndPointBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;


public class TapiConnectivityServiceImplTest {

    private ActivationDriver ad1;
    private ActivationDriver ad2;
    private ActivationDriver ad3;

    private Uuid uuid1 = new Uuid("uuid1");
    private Uuid uuid2 = new Uuid("uuid2");
    private String activationDriverId1 = "d1";
    private String activationDriverId2 = "d2";
    private TapiConnectivityServiceImpl connectivityService;
    private RequestDecomposer decomposer;
    private DataBroker broker;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        decomposer = mock(RequestDecomposer.class);
        RequestValidator validator = mock(RequestValidator.class);
        when(validator.checkValid(any(CreateConnectivityServiceInput.class)))
            .thenReturn(new RequestValidator.ValidationResult());
        when(validator.checkValid(any(UpdateConnectivityServiceInput.class)))
            .thenReturn(new RequestValidator.ValidationResult());

        ad1 = mock(ActivationDriver.class);
        ad2 = mock(ActivationDriver.class);
        ad3 = mock(ActivationDriver.class);
        String activationDriverId3 = "d3";
        ActivationDriverRepoService repo = ActivationDriverMocks.builder()
                .add(activationDriverId1, ad1)
                .add(activationDriverId2, ad2)
                .add(activationDriverId3, ad3)
                .build();

        connectivityService = new TapiConnectivityServiceImpl();
        connectivityService.setDriverRepo(repo);
        connectivityService.setDecomposer(decomposer);
        connectivityService.setValidator(validator);

        ReadWriteTransaction tx = mock(ReadWriteTransaction.class);
        when(tx.commit()).thenReturn(mock(FluentFuture.class));
        broker = mock(DataBroker.class);
        when(broker.newReadWriteTransaction()).thenReturn(tx);
        when(broker.newWriteOnlyTransaction()).thenReturn(tx);
        connectivityService.setBroker(broker);
        connectivityService.setServiceIdPool(new ConnectivityServiceIdResourcePool());
        connectivityService.init();
    }


    @Test
    public void emptyInputTest() {
        //having
        CreateConnectivityServiceInput empty = new CreateConnectivityServiceInputBuilder()
                .build();
        //when
        RpcResult<CreateConnectivityServiceOutput> result;
        try {
            result = this.connectivityService.createConnectivityService(empty).get();
            //then
            assertFalse(result.isSuccessful());
            verifyZeroInteractions(ad1);
            verifyZeroInteractions(ad2);
            verifyZeroInteractions(ad3);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
        }
    }

    @Test
    public void noPathTest() throws Exception {
        //having
        CreateConnectivityServiceInput input = input(2);
        configureDecomposerAnswer(eps -> null);

        //when
        RpcResult<CreateConnectivityServiceOutput> result =
                this.connectivityService.createConnectivityService(input).get();
        //then
        assertFalse(result.isSuccessful());
        verifyZeroInteractions(ad1);
        verifyZeroInteractions(ad2);
        verifyZeroInteractions(ad3);
    }

    @Test
    public void withLocalIds() {
        //having
        CreateConnectivityServiceInput input = input("a", "a", "b");
        connectivityService.setValidator(new DefaultValidator(broker));

        //when
        RpcResult<CreateConnectivityServiceOutput> result;
        try {
            result = this.connectivityService.createConnectivityService(input).get();
            //then
            assertFalse(result.isSuccessful());
            verifyZeroInteractions(ad1);
            verifyZeroInteractions(ad2);
            verifyZeroInteractions(ad3);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
        }
    }


    @Test
    public void failTwoDriversOneFailing() {
        //having
        CreateConnectivityServiceInput input = input(4);

        configureDecomposerAnswer(eps -> {
            Subrequrest s1 = new Subrequrest(uuid1, Arrays.asList(eps.get(0), eps.get(1)),activationDriverId1);
            Subrequrest s2 = new Subrequrest(uuid2, Arrays.asList(eps.get(2), eps.get(3)),activationDriverId2);

            return Arrays.asList(s1, s2);
        });

        try {
            doThrow(new ResourceActivatorException()).when(ad2).activate();
            //when
            RpcResult<CreateConnectivityServiceOutput> result =
                this.connectivityService.createConnectivityService(input).get();
            //then
            assertFalse(result.isSuccessful());
            verify(ad1).activate();
            verify(ad2).activate();
            verify(ad1).rollback();
            verify(ad2).rollback();
            verifyZeroInteractions(ad3);
        } catch (ResourceActivatorException e) {
            // TODO Auto-generated catch block
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
        }

    }

    @SuppressWarnings("checkstyle:emptyBlock")
    private void configureDecomposerAnswer(
            Function<List<org.opendaylight.unimgr.mef.nrp.api.EndPoint>, List<Subrequrest>> resp) {
        try {
            Mockito.when(decomposer.decompose(any(), any()))
                .thenAnswer(a -> {
                    List<org.opendaylight.unimgr.mef.nrp.api.EndPoint> eps = a.getArgument(0);
                    eps.forEach(e -> e.setNepRef(TapiUtils.toSysNepRef(new Uuid("node-id"), new Uuid("nep-id"))));
                    return resp.apply(eps);
                });
        } catch (FailureResult _f) { }
    }

    private CreateConnectivityServiceInput input(int count) {

        List<EndPoint> eps = IntStream.range(0, count).mapToObj(x -> ep("ep" + x)).collect(Collectors.toList());

        return new CreateConnectivityServiceInputBuilder()
                .setEndPoint(eps)
                .setConnConstraint(new ConnConstraintBuilder().setIsExclusive(true)
                .setServiceType(ServiceType.POINTTOPOINTCONNECTIVITY).build())
                .build();
    }

    private CreateConnectivityServiceInput input(String... localIds) {

        List<EndPoint> eps = Arrays.stream(localIds).map(this::ep).collect(Collectors.toList());

        return new CreateConnectivityServiceInputBuilder()
                .setEndPoint(eps)
                .build();
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
                .create.connectivity.service.input.EndPoint ep(String id) {
        return new EndPointBuilder()
                .setLocalId(id)
                .setRole(PortRole.SYMMETRIC)
                .setServiceInterfacePoint(
                        new ServiceInterfacePointBuilder()
                        .setServiceInterfacePointId(new Uuid(id)).build())
        .build();
    }

}
