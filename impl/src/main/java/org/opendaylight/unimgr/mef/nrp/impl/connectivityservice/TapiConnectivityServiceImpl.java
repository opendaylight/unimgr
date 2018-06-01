/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.connectivityservice;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.GetConnectivityServiceListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.TapiConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceOutput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author bartosz.michalik@amartus.com
 */
public class TapiConnectivityServiceImpl implements TapiConnectivityService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(TapiConnectivityServiceImpl.class);
    private ActivationDriverRepoService driverRepo;
    private RequestDecomposer decomposer;
    private RequestValidator validator;
    private DataBroker broker;
    private ConnectivityServiceIdResourcePool serviceIdPool;

    private ExecutorService executor = null;

    final static InstanceIdentifier<Context1> connectivityCtx = NrpDao.ctx().augmentation(Context1.class);


    public void init() {
        Objects.requireNonNull(driverRepo);
        Objects.requireNonNull(decomposer);
        Objects.requireNonNull(validator);
        Objects.requireNonNull(broker);
        Objects.requireNonNull(serviceIdPool);
        if(executor == null) {
            executor = new ThreadPoolExecutor(4, 16,
                    30, TimeUnit.MINUTES,
                    new LinkedBlockingQueue<>());
        }
        LOG.info("TapiConnectivityService initialized");
    }

    @Override
    public void close() throws Exception {
        executor.shutdown();
    }

    @Override
    public Future<RpcResult<CreateConnectivityServiceOutput>> createConnectivityService(CreateConnectivityServiceInput input) {
        return executor.submit(new CreateConnectivityAction(this, input));
    }


    @Override
    public Future<RpcResult<UpdateConnectivityServiceOutput>> updateConnectivityService(UpdateConnectivityServiceInput input) {
    	return executor.submit(new UpdateConnectivityAction(this, input));
    }

    @Override
    public Future<RpcResult<GetConnectionDetailsOutput>> getConnectionDetails(GetConnectionDetailsInput input) {
        return executor.submit(new GetConnectionDetailsAction(this, input));
    }

    @Override
    public Future<RpcResult<GetConnectivityServiceDetailsOutput>> getConnectivityServiceDetails(GetConnectivityServiceDetailsInput input) {
        return executor.submit(new GetConnectivityDetailsAction(this, input));
    }

    @Override
    public Future<RpcResult<DeleteConnectivityServiceOutput>> deleteConnectivityService(DeleteConnectivityServiceInput input) {
        return executor.submit(new DeleteConnectivityAction(this,input));

    }

    @Override
    public Future<RpcResult<GetConnectivityServiceListOutput>> getConnectivityServiceList() {
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

    public void setExecutor(ExecutorService executor) {
        if(executor != null) throw new IllegalStateException();
        this.executor = executor;
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
