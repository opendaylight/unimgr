/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.unimgr.mef.nrp.api.RequestValidator;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.update.connectivity.service.input.EndPoint;

import java.util.Optional;

/**
 * @author bartosz.michalik@amartus.com
 */
public class DefaultValidator implements RequestValidator {

    private final  DataBroker dataBroker;

    public DefaultValidator(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public ValidationResult checkValid(CreateConnectivityServiceInput input) {
        return new ValidationResult();
    }

    @Override
    public ValidationResult checkValid(UpdateConnectivityServiceInput input) {
        ConnectivityService cs = new NrpDao(dataBroker.newReadOnlyTransaction()).getConnectivityService(input.getServiceIdOrName());

        if(cs == null) {
            return new ValidationResult().problem(String.format("Connectivity service %s does not exists", input.getServiceIdOrName()));
        }

        EndPoint ep = input.getEndPoint();
        if(ep == null) return new ValidationResult().problem(String.format("Endpoint not defined for %s", input.getServiceIdOrName()));

        Optional<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.EndPoint> epFromCs = cs.getEndPoint().stream()
                .filter(e -> e.getLocalId().equals(ep.getLocalId())).findFirst();
        if(! epFromCs.isPresent())
            return new ValidationResult().problem(String.format("No endpoint with local id %1$s defined for %2$s", ep.getLocalId(), input.getServiceIdOrName()));

        if(!epFromCs.get().getServiceInterfacePoint().equals(ep.getServiceInterfacePoint())) {
            return new ValidationResult().problem(String.format("Sip mapping for endpoint %1$s is not matching for service %2$s", ep.getLocalId(), input.getServiceIdOrName()));
        }

        return new ValidationResult();
    }
}
