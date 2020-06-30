/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.connectivityservice;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverRepoService;
import org.opendaylight.unimgr.mef.nrp.api.RequestDecomposer;
import org.opendaylight.unimgr.mef.nrp.api.RequestValidator;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.unimgr.mef.nrp.impl.ConnectivityServiceIdResourcePool;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.DeleteConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.DeleteConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.GetConnectionDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.GetConnectionDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.GetConnectivityServiceDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.GetConnectivityServiceDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.GetConnectivityServiceListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.GetConnectivityServiceListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.TapiConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceOutput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TapiConnectivity RPC implementation.
 * @author bartosz.michalik@amartus.com
 */
public class TapiConnectivityServiceImpl implements TapiConnectivityService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(TapiConnectivityServiceImpl.class);
    static final InstanceIdentifier<Context1> CONNECTIVITY_CTX = NrpDao.ctx().augmentation(Context1.class);
    private ActivationDriverRepoService driverRepo;
    private RequestDecomposer decomposer;
    private RequestValidator validator;
    private DataBroker broker;
    private ConnectivityServiceIdResourcePool serviceIdPool;

    private ListeningExecutorService executor = null;


    public void init() {
        Objects.requireNonNull(driverRepo);
        Objects.requireNonNull(decomposer);
        Objects.requireNonNull(validator);
        Objects.requireNonNull(broker);
        Objects.requireNonNull(serviceIdPool);
        if (executor == null) {
            executor = MoreExecutors.listeningDecorator(
                    new ThreadPoolExecutor(4, 16,
                            30, TimeUnit.MINUTES,
                            new LinkedBlockingQueue<>()));
        }
        LOG.info("TapiConnectivityService initialized");
    }

    @Override
    public void close() throws Exception {
        executor.shutdown();
    }

    @Override
    public ListenableFuture<RpcResult<CreateConnectivityServiceOutput>> createConnectivityService(
            CreateConnectivityServiceInput input) {
        return executor.submit(new CreateConnectivityAction(this, input));
    }


    @Override
    public ListenableFuture<RpcResult<UpdateConnectivityServiceOutput>> updateConnectivityService(
            UpdateConnectivityServiceInput input) {
        return executor.submit(new UpdateConnectivityAction(this, input));
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectionDetailsOutput>> getConnectionDetails(
            GetConnectionDetailsInput input) {
        return executor.submit(new GetConnectionDetailsAction(this, input));
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectivityServiceDetailsOutput>> getConnectivityServiceDetails(
            GetConnectivityServiceDetailsInput input) {
        return executor.submit(new GetConnectivityDetailsAction(this, input));
    }

    @Override
    public ListenableFuture<RpcResult<DeleteConnectivityServiceOutput>> deleteConnectivityService(
            DeleteConnectivityServiceInput input) {
        return executor.submit(new DeleteConnectivityAction(this,input));

    }

    @Override
    public ListenableFuture<RpcResult<GetConnectivityServiceListOutput>> getConnectivityServiceList(
            GetConnectivityServiceListInput input) {
        return executor.submit(new ListConnectivityAction(this));
    }


    public void setValidator(RequestValidator validator) {
        this.validator = validator;
    }

    public void setDriverRepo(ActivationDriverRepoService driverRepo) {
        this.driverRepo = driverRepo;
    }

    public void setDecomposer(RequestDecomposer decomposer) {
        this.decomposer = decomposer;
    }

    public void setBroker(DataBroker broker) {
        this.broker = broker;
    }

    public void setExecutor(ListeningExecutorService executor) {
        this.executor = executor;
        if (executor != null) {
            throw new IllegalStateException();
        }
    }

    public void setServiceIdPool(ConnectivityServiceIdResourcePool serviceIdPool) {
        this.serviceIdPool = serviceIdPool;
    }

    ActivationDriverRepoService getDriverRepo() {
        return driverRepo;
    }

    RequestDecomposer getDecomposer() {
        return decomposer;
    }

    RequestValidator getValidator() {
        return validator;
    }

    DataBroker getBroker() {
        return broker;
    }

    ConnectivityServiceIdResourcePool getServiceIdPool() {
        return serviceIdPool;
    }
}
