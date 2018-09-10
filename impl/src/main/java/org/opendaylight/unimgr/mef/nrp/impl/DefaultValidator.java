/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.unimgr.mef.nrp.api.RequestValidator;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.LocalClass;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.update.connectivity.service.input.EndPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple default validator for RPC inputs.
 * @author bartosz.michalik@amartus.com
 */
public class DefaultValidator implements RequestValidator {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultValidator.class);

    private final  DataBroker dataBroker;

    public DefaultValidator(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public @Nonnull ValidationResult checkValid(CreateConnectivityServiceInput input) {
        LOG.debug("Validation for request started");
        ValidationResult fromInput = verifyPayloadCorrect(input);
        ValidationResult fromState = validateState(input);

        return fromState.merge(fromInput);
    }

    @Override
    public @Nonnull ValidationResult checkValid(UpdateConnectivityServiceInput input) {
        ConnectivityService cs = new NrpDao(dataBroker.newReadOnlyTransaction())
                .getConnectivityService(input.getServiceIdOrName());

        if (cs == null) {
            return new ValidationResult()
                    .problem(String.format("Connectivity service %s does not exists", input.getServiceIdOrName()));
        }

        EndPoint ep = input.getEndPoint();
        if (ep == null) {
            return new ValidationResult()
                    .problem(String.format("Endpoint not defined for %s", input.getServiceIdOrName()));
        }

        Optional<
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.EndPoint
                > epFromCs = cs.getEndPoint().stream()
                .filter(e -> e.getLocalId().equals(ep.getLocalId())).findFirst();
        if (! epFromCs.isPresent()) {
            return new ValidationResult()
                    .problem(String
                            .format("No endpoint with local id %1$s defined for %2$s",
                                    ep.getLocalId(), input.getServiceIdOrName()));
        }

        if (!epFromCs.get().getServiceInterfacePoint().equals(ep.getServiceInterfacePoint())) {
            return new ValidationResult()
                    .problem(String
                            .format("Sip mapping for endpoint %1$s is not matching for service %2$s",
                                    ep.getLocalId(), input.getServiceIdOrName()));
        }

        return new ValidationResult();
    }

    @Nonnull protected  ValidationResult validateState(CreateConnectivityServiceInput input) {
        // simple use case to validate port based service
        // more complex implementation could use caching techniques
        //TODO implement

        return new ValidationResult();
    }

    @Nonnull protected ValidationResult verifyPayloadCorrect(CreateConnectivityServiceInput input) {

        ValidationResult validationResult = new ValidationResult();

        if (input.getEndPoint() == null || input.getEndPoint().isEmpty()) {
            validationResult.problem("No endpoints specified for a connectivity service");
        } else {

            Set<String> allItems = new HashSet<>();
            Optional<String> firstDuplicate = input.getEndPoint().stream()
                    .filter(e -> e.getLocalId() != null)
                    .map(LocalClass::getLocalId)
                    .filter(s -> !allItems.add(s))
                    .findFirst();
            firstDuplicate.ifPresent(s -> validationResult.problem("A duplicate endpoint id: " + s));
        }



        return validationResult;
    }
}
