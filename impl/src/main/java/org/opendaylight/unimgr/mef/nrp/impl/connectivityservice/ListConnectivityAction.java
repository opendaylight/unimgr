/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.connectivityservice;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.GetConnectivityServiceListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.GetConnectivityServiceListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.get.connectivity.service.list.output.Service;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.get.connectivity.service.list.output.ServiceBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class ListConnectivityAction implements Callable<RpcResult<GetConnectivityServiceListOutput>> {

    private final TapiConnectivityServiceImpl service;

    ListConnectivityAction(TapiConnectivityServiceImpl service) {
        Objects.requireNonNull(service);
        this.service = service;
    }

    @Override
    public RpcResult<GetConnectivityServiceListOutput> call() {
        List<Service> services = new ArrayList<>();

        NrpDao nrpDao = new NrpDao(service.getBroker().newReadOnlyTransaction());
        List<ConnectivityService> connectivityServices = nrpDao.getConnectivityServiceList();
        if (connectivityServices != null) {
            for (ConnectivityService cs : connectivityServices) {
                services.add(new ServiceBuilder(cs).build());
            }
        }

        return RpcResultBuilder.success(
                new GetConnectivityServiceListOutputBuilder()
                .setService(services)).build();
    }
}
