/*
 * Copyright (c) 2018 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.impl.commonservice;


import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.*;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.*;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author bartosz.michalik@amartus.com
 */
public class TapiCommonServiceImpl implements TapiCommonService {
    private static final Logger LOG = LoggerFactory.getLogger(TapiCommonServiceImpl.class);
    private DataBroker broker;

    // TODO decide on strategy for executor service
    private ExecutorService executor = null;

    public void init() {
        Objects.requireNonNull(broker);
        if(executor == null) {
            executor = new ThreadPoolExecutor(4, 16,
                    30, TimeUnit.MINUTES,
                    new LinkedBlockingQueue<>());
        }
        LOG.info("TapiCommonServiceImpl initialized");
    }

    @Override
    public Future<RpcResult<GetServiceInterfacePointDetailsOutput>> getServiceInterfacePointDetails(GetServiceInterfacePointDetailsInput input) {
        final String sip = input.getSipIdOrName();
        return executor.submit(() -> {
            NrpDao dao = new NrpDao(broker.newReadOnlyTransaction());
            try {
                ServiceInterfacePoint result = dao.getSip(sip);
                if(result == null) throw new IllegalArgumentException("Cannot find SIP for uuid " + sip);
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.get.service._interface.point.details.output.SipBuilder sipBuilder
                        = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.get.service._interface.point.details.output.SipBuilder(result);
                NrpSipAttrs aug = result.getAugmentation(ServiceInterfacePoint1.class);
                if(aug != null)
                    sipBuilder.addAugmentation(Sip1.class, new Sip1Builder(aug).build());
                return RpcResultBuilder.success(


                        new GetServiceInterfacePointDetailsOutputBuilder()
                                .setSip(sipBuilder.build())
                                .build())
                        .build();

            } catch (ReadFailedException | IllegalArgumentException e) {
                return  RpcResultBuilder.<GetServiceInterfacePointDetailsOutput>failed().withError(RpcError.ErrorType.APPLICATION,
                        String.format("Cannot read SIP with uuid: %s", sip) ,e).build();
            }
        });
    }

    @Override
    public Future<RpcResult<Void>> updateServiceInterfacePoint(UpdateServiceInterfacePointInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<GetServiceInterfacePointListOutput>> getServiceInterfacePointList() {
        return executor.submit(() -> {
            ReadOnlyTransaction rtx = broker.newReadOnlyTransaction();
            RpcResult<GetServiceInterfacePointListOutput> out = RpcResultBuilder.success(new GetServiceInterfacePointListOutputBuilder().build()).build();
            try {
                List<ServiceInterfacePoint> sips;
                Optional<Context> ctx = rtx.read(LogicalDatastoreType.OPERATIONAL, NrpDao.ctx()).checkedGet();
                if(ctx.isPresent()) {
                    sips = ctx.get().getServiceInterfacePoint();

                    if(sips == null) sips = Collections.emptyList();

                    out = RpcResultBuilder.success(
                            new GetServiceInterfacePointListOutputBuilder()
                                    .setSip(sips.stream().map(t -> {
                                        NrpSipAttrs nrpAug = t.getAugmentation(ServiceInterfacePoint1.class);
                                        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.get.service._interface.point.list.output.SipBuilder sipBuilder
                                                = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.get.service._interface.point.list.output.SipBuilder(t);
                                        if(nrpAug != null)
                                            sipBuilder.addAugmentation(Sip2.class, new Sip2Builder(nrpAug).build());

                                        return sipBuilder.build();
                                    }).collect(Collectors.toList())).build()
                    ).build();
                }
            } catch (ReadFailedException e) {
                out = RpcResultBuilder.<GetServiceInterfacePointListOutput>failed().withError(RpcError.ErrorType.APPLICATION, "Cannot read SIPs" ,e).build();
            }
            return out;
        });
    }

    public void setBroker(DataBroker broker) {
        this.broker = broker;
    }
}
