/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.connectivityservice;

import java.util.Objects;
import java.util.concurrent.Callable;

import org.opendaylight.unimgr.mef.nrp.api.FailureResult;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.GetConnectivityServiceDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.GetConnectivityServiceDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.GetConnectivityServiceDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.get.connectivity.service.details.output.ServiceBuilder;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class GetConnectivityDetailsAction implements Callable<RpcResult<GetConnectivityServiceDetailsOutput>> {

    private final TapiConnectivityServiceImpl service;
    private final GetConnectivityServiceDetailsInput input;

    public GetConnectivityDetailsAction(TapiConnectivityServiceImpl service, GetConnectivityServiceDetailsInput input) {
        Objects.requireNonNull(service);
        this.service = service;
        this.input = input;
    }

    @Override
    public RpcResult<GetConnectivityServiceDetailsOutput> call() throws Exception {

        try {
            if (input.getServiceIdOrName() == null) {
                throw new FailureResult("get-connectivity-service-details requires a valid service-id-or-name");
            }

            NrpDao nrpDao = new NrpDao(service.getBroker().newReadOnlyTransaction());
            ConnectivityService value = nrpDao.getConnectivityService(input.getServiceIdOrName());

            if (value == null) {
                throw new FailureResult("There is no service with id {0}", input.getServiceIdOrName());
            }

            return RpcResultBuilder.success(
                    new GetConnectivityServiceDetailsOutputBuilder()
                    .setService(new ServiceBuilder(value).build())).build();

        } catch (FailureResult e) {
            return RpcResultBuilder
                    .<GetConnectivityServiceDetailsOutput>failed()
                    .withError(ErrorType.APPLICATION, e.getMessage())
                    .build();
        }
    }
}
