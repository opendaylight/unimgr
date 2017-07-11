/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.connectivityservice;

import java.util.concurrent.Callable;

import org.opendaylight.unimgr.mef.nrp.api.FailureResult;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170531.UniversalId;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170531.GetConnectionDetailsInput;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170531.GetConnectionDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170531.GetConnectionDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170531.connectivity.context.g.Connection;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170531.connectivity.context.g.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170531.get.connection.details.output.ConnectionBuilder;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class GetConnectionDetailsAction implements Callable<RpcResult<GetConnectionDetailsOutput>> {

    private final TapiConnectivityServiceImpl service;
    private final GetConnectionDetailsInput input;

    public GetConnectionDetailsAction(TapiConnectivityServiceImpl service, GetConnectionDetailsInput input) {
        this.service = service;
        this.input = input;
    }

    @Override
    public RpcResult<GetConnectionDetailsOutput> call() throws Exception {

        try {
            NrpDao nrpDao = new NrpDao(service.getBroker().newReadOnlyTransaction());

            String connectionId = input.getConnectionIdOrName();
            if (connectionId == null) {
                String serviceId = input.getServiceIdOrName();
                if (serviceId == null) {
                    throw new FailureResult("Cannot fetch connection without id.");
                }

                ConnectivityService cs = nrpDao.getConnectivityService(input.getServiceIdOrName());
                if (cs == null) {
                    throw new FailureResult("There is no service with id {0}", input.getServiceIdOrName());
                }
                connectionId = cs.getConnection().get(0).getValue();
            }

            Connection connection = nrpDao.getConnection(new UniversalId(connectionId));
            if (connection == null) {
                throw new FailureResult("There is no connection with id {0}", connectionId);
            }

            return RpcResultBuilder.success(
                    new GetConnectionDetailsOutputBuilder()
                    .setConnection(new ConnectionBuilder(connection).build())
                    .build()).build();

        } catch (FailureResult e) {
            return RpcResultBuilder
                    .<GetConnectionDetailsOutput>failed()
                    .withError(ErrorType.APPLICATION, e.getMessage())
                    .build();
        }

    }

}
