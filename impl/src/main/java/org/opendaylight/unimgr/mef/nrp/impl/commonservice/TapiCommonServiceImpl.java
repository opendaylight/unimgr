/*
 * Copyright (c) 2018 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.impl.commonservice;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.NrpSipAttrs;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.ServiceInterfacePoint1;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.Sip1;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.Sip1Builder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.Sip2;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.Sip2Builder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.GetServiceInterfacePointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.GetServiceInterfacePointDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.GetServiceInterfacePointDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.GetServiceInterfacePointListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.GetServiceInterfacePointListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.GetServiceInterfacePointListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.TapiCommonService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.UpdateServiceInterfacePointInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.UpdateServiceInterfacePointOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TapiCommon RPC implementation.
 * @author bartosz.michalik@amartus.com
 */
public class TapiCommonServiceImpl implements TapiCommonService {
    private static final Logger LOG = LoggerFactory.getLogger(TapiCommonServiceImpl.class);
    private DataBroker broker;

    // TODO decide on strategy for executor service
    private ListeningExecutorService executor = null;

    public void init() {
        Objects.requireNonNull(broker);
        if (executor == null) {
            executor = MoreExecutors.listeningDecorator(
                new ThreadPoolExecutor(4, 16,
                    30, TimeUnit.MINUTES,
                    new LinkedBlockingQueue<>()));
        }
        LOG.info("TapiCommonServiceImpl initialized");
    }

    @Override
    public ListenableFuture<RpcResult<GetServiceInterfacePointDetailsOutput>> getServiceInterfacePointDetails(
            GetServiceInterfacePointDetailsInput input) {
        final String sip = input.getSipIdOrName();
        return executor.submit(() -> {
            NrpDao dao = new NrpDao(broker.newReadOnlyTransaction());
            try {
                ServiceInterfacePoint result = dao.getSip(sip);
                if (result == null) {
                    throw new IllegalArgumentException("Cannot find SIP for uuid " + sip);
                }
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307
                        .get.service._interface.point.details.output.SipBuilder sipBuilder
                        = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307
                        .get.service._interface.point.details.output.SipBuilder(result);
                NrpSipAttrs aug = result.augmentation(ServiceInterfacePoint1.class);
                if (aug != null) {
                    sipBuilder.addAugmentation(Sip1.class, new Sip1Builder(aug).build());
                }
                return RpcResultBuilder.success(


                        new GetServiceInterfacePointDetailsOutputBuilder()
                                .setSip(sipBuilder.build())
                                .build())
                        .build();

            } catch (IllegalArgumentException e) {
                return  RpcResultBuilder.<GetServiceInterfacePointDetailsOutput>failed()
                        .withError(RpcError.ErrorType.APPLICATION,
                        String.format("Cannot read SIP with uuid: %s", sip) ,e).build();
            }
        });
    }

    @Override
    public ListenableFuture<RpcResult<UpdateServiceInterfacePointOutput>> updateServiceInterfacePoint(
            UpdateServiceInterfacePointInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetServiceInterfacePointListOutput>> getServiceInterfacePointList(
            GetServiceInterfacePointListInput input) {
        return executor.submit(() -> {
            ReadTransaction rtx = broker.newReadOnlyTransaction();
            RpcResult<GetServiceInterfacePointListOutput> out = RpcResultBuilder
                    .success(new GetServiceInterfacePointListOutputBuilder().build()).build();
            List<ServiceInterfacePoint> sips;
            Optional<Context> ctx = rtx.read(LogicalDatastoreType.OPERATIONAL, NrpDao.ctx()).get();
            if (ctx.isPresent()) {
                sips = ctx.get().getServiceInterfacePoint();

                if (sips == null) {
                    sips = Collections.emptyList();
                }

                out = RpcResultBuilder.success(
                        new GetServiceInterfacePointListOutputBuilder()
                                .setSip(sips.stream().map(t -> {
                                    NrpSipAttrs nrpAug = t.augmentation(ServiceInterfacePoint1.class);
                                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang
                                            .tapi.common.rev180307
                                            .get.service._interface.point.list.output.SipBuilder sipBuilder
                                            = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang
                                            .tapi.common.rev180307
                                            .get.service._interface.point.list.output.SipBuilder(t);
                                    if (nrpAug != null) {
                                        sipBuilder.addAugmentation(Sip2.class, new Sip2Builder(nrpAug).build());
                                    }

                                    return sipBuilder.build();
                                }).collect(Collectors.toList())).build()
                ).build();
            }
            return out;
        });
    }

    public void setBroker(DataBroker broker) {
        this.broker = broker;
    }
}
