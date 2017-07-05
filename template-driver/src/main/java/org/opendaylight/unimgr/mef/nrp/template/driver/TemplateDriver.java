/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.template.driver;

import java.util.List;
import java.util.Optional;

import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverBuilder;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivatorException;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp_interface.rev170531.NrpConnectivityServiceAttrsG;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170531.UniversalId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example driver builder
 * @author bartosz.michalik@amartus.com
 */
public class TemplateDriver implements ActivationDriverBuilder {
    private static final Logger log = LoggerFactory.getLogger(TemplateDriver.class);
    @Override
    public Optional<ActivationDriver> driverFor(BuilderContext context) {
        // build a stateful driver
        // the API contract is following
        // 1. initialize is called
        // 2. activate or deactivate metod is called depending on NRP call (update will be supported soon)
        // 3. if activation/deactivation succeeds for all drivers the commit metod is called
        // 3a. if activation/deactivation fails for any driver rollback is called
        return Optional.of(new ActivationDriver() {

            public String serviceId;

            @Override
            public void commit() {
                log.info("commit was triggered for {}", serviceId);
            }

            @Override
            public void rollback() {
                log.info("rollback was triggered for {}", serviceId);
            }

            @Override
            public void initialize(List<EndPoint> endPoints, String serviceId, NrpConnectivityServiceAttrsG context) {
                this.serviceId = serviceId;
            }

            @Override
            public void activate() throws TransactionCommitFailedException, ResourceActivatorException {
                // method can fail if you wish
                log.info("activate was triggered for {}", serviceId);
            }

            @Override
            public void deactivate() throws TransactionCommitFailedException, ResourceActivatorException {
                // method can fail if you wish
                log.info("adectivate was triggered for {}", serviceId);
            }

            @Override
            public int priority() {
                //if you would like to make your driver first on the list

                return 0;
            }
        });
    }

    @Override
    public UniversalId getNodeUuid() {
        return null;
    }
}
