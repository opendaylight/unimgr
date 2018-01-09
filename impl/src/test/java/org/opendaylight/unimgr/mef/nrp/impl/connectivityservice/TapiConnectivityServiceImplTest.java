/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.connectivityservice;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverRepoService;
import org.opendaylight.unimgr.mef.nrp.api.Constraints;
import org.opendaylight.unimgr.mef.nrp.api.FailureResult;
import org.opendaylight.unimgr.mef.nrp.api.RequestDecomposer;
import org.opendaylight.unimgr.mef.nrp.api.RequestValidator;
import org.opendaylight.unimgr.mef.nrp.api.Subrequrest;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivatorException;
import org.opendaylight.unimgr.mef.nrp.impl.ConnectivityServiceIdResourcePool;
import org.opendaylight.unimgr.utils.ActivationDriverMocks;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.CreateConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.CreateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.create.connectivity.service.input.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.create.connectivity.service.input.EndPointBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.util.concurrent.CheckedFuture;

/**
 * @author bartosz.michalik@amartus.com
 */
public class TapiConnectivityServiceImplTest {


    private ActivationDriver ad1;
    private ActivationDriver ad2;
    private ActivationDriver ad3;


    private Uuid uuid1 = new Uuid("uuid1");
    private Uuid uuid2 = new Uuid("uuid2");
    private Uuid uuid3 = new Uuid("uuid3");
    private TapiConnectivityServiceImpl connectivityService;
    private RequestDecomposer decomposer;
    private RequestValidator validator;
    private ReadWriteTransaction tx;

    @Before
    public void setUp() {
        ad1 = mock(ActivationDriver.class);
        ad2 = mock(ActivationDriver.class);
        ad3 = mock(ActivationDriver.class);
        ActivationDriverRepoService repo = ActivationDriverMocks.builder()
                .add(uuid1, ad1)
                .add(uuid2, ad2)
                .add(uuid3, ad3)
                .build();

        decomposer = mock(RequestDecomposer.class);
        validator = mock(RequestValidator.class);
        when(validator.checkValid(any())).thenReturn(new RequestValidator.ValidationResult());

        connectivityService = new TapiConnectivityServiceImpl();
        connectivityService.setDriverRepo(repo);
        connectivityService.setDecomposer(decomposer);
        connectivityService.setValidator(validator);

        tx = mock(ReadWriteTransaction.class);
        when(tx.submit()).thenReturn(mock(CheckedFuture.class));
        DataBroker broker = mock(DataBroker.class);


        when(broker.newWriteOnlyTransaction()).thenReturn(tx);
        connectivityService.setBroker(broker);
        connectivityService.setServiceIdPool(new ConnectivityServiceIdResourcePool());
        connectivityService.init();
    }


    @Test
    public void emptyInputTest() throws Exception {
        //having
        CreateConnectivityServiceInput empty = new CreateConnectivityServiceInputBuilder()
                .build();
        //when
        RpcResult<CreateConnectivityServiceOutput> result = this.connectivityService.createConnectivityService(empty).get();
        //then
        assertFalse(result.isSuccessful());
        verifyZeroInteractions(ad1);
        verifyZeroInteractions(ad2);
        verifyZeroInteractions(ad3);
    }

    @Test
    public void noPathTest() throws Exception {
        //having
        CreateConnectivityServiceInput input = input(2);
        configureDecomposerAnswer(eps -> null);

        //when
        RpcResult<CreateConnectivityServiceOutput> result = this.connectivityService.createConnectivityService(input).get();
        //then
        assertFalse(result.isSuccessful());
        verifyZeroInteractions(ad1);
        verifyZeroInteractions(ad2);
        verifyZeroInteractions(ad3);
    }

    @Test
    public void sucessfullTwoDrivers() throws ExecutionException, InterruptedException, ResourceActivatorException, TransactionCommitFailedException {
        //having
        CreateConnectivityServiceInput input = input(5);


        configureDecomposerAnswer(eps -> {
            Subrequrest s1 = new Subrequrest(uuid1, Arrays.asList(eps.get(0), eps.get(1), eps.get(2)));
            Subrequrest s3 = new Subrequrest(uuid3, Arrays.asList(eps.get(3), eps.get(4)));

            return Arrays.asList(s1, s3);
        });

        //when
        RpcResult<CreateConnectivityServiceOutput> result = this.connectivityService.createConnectivityService(input).get();
        //then
        assertTrue(result.isSuccessful());
        verify(ad1).activate();
        verify(ad3).activate();
        verify(ad1).commit();
        verify(ad3).commit();
        verifyZeroInteractions(ad2);
        //3x Connection (2 x system + 1 external) + ConnectivityService
        verify(tx,times(4)).put(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any());


    }


    @Test
    public void failTwoDriversOneFailing() throws ExecutionException, InterruptedException, ResourceActivatorException, TransactionCommitFailedException {
        //having
        CreateConnectivityServiceInput input = input(4);

        configureDecomposerAnswer(eps -> {
            Subrequrest s1 = new Subrequrest(uuid1, Arrays.asList(eps.get(0), eps.get(1)));
            Subrequrest s2 = new Subrequrest(uuid2, Arrays.asList(eps.get(2), eps.get(3)));

            return Arrays.asList(s1, s2);
        });

        doThrow(new ResourceActivatorException()).when(ad2).activate();

        //when
        RpcResult<CreateConnectivityServiceOutput> result = this.connectivityService.createConnectivityService(input).get();
        //then
        assertFalse(result.isSuccessful());
        verify(ad1).activate();
        verify(ad2).activate();
        verify(ad1).rollback();
        verify(ad2).rollback();
        verifyZeroInteractions(ad3);
    }


    private void configureDecomposerAnswer(Function<List<org.opendaylight.unimgr.mef.nrp.api.EndPoint>, List<Subrequrest>> resp) {
        try {
            when(decomposer.decompose(any(), any(Constraints.class)))
                .thenAnswer(a -> {
                    List<org.opendaylight.unimgr.mef.nrp.api.EndPoint> eps = a.getArgumentAt(0, List.class);
                    return resp.apply(eps);
                });
        } catch (FailureResult f) {
        }
    }

    private CreateConnectivityServiceInput input(int count) {

        List<EndPoint> eps = IntStream.range(0, count).mapToObj(x -> ep("ep" + x)).collect(Collectors.toList());

        return new CreateConnectivityServiceInputBuilder()
                .setEndPoint(eps)
                .build();
    }

    private org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.create.connectivity.service.input.EndPoint ep(String id) {
        return new EndPointBuilder()
                .setLocalId(id)
                .setRole(PortRole.SYMMETRIC)
//                .setServiceInterfacePoint()
        .build();
    }

}
