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
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverBuilder;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivatorException;
import org.opendaylight.unimgr.mef.nrp.template.TemplateConstants;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.NrpConnectivityServiceAttrs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example driver builder
 * @author bartosz.michalik@amartus.com
 */
public class TemplateDriver implements ActivationDriverBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(TemplateDriver.class);
    @Override
    public Optional<ActivationDriver> driverFor(BuilderContext context) {
        // build a stateful driver
        // the API contract is following
        // 1. initialize is called
        // 2. activate or deactivate metod is called depending on NRP call (update will be supported soon)
        // 3. if activation/deactivation succeeds for all drivers the commit metod is called
        // 3a. if activation/deactivation fails for any driver rollback is called
        return Optional.of(new ActivationDriver() {

            public List<EndPoint> endpoints;
            public String serviceId;

            @Override
            public void commit() {
                LOG.info("commit was triggered for {}", serviceId);
            }

            @Override
            public void rollback() {
                LOG.info("rollback was triggered for {}", serviceId);
            }

            @Override
            public void initialize(List<EndPoint> endPoints, String serviceId, NrpConnectivityServiceAttrs context) {
                this.serviceId = serviceId;
                this.endpoints = endPoints;

                LOG.info("Driver initialized with: " + epsInfo());
            }

            @Override
            public void activate() throws TransactionCommitFailedException, ResourceActivatorException {
                // method can fail if you wish
                LOG.info("activate was triggered for {}", serviceId);
            }

            @Override
            public void deactivate() throws TransactionCommitFailedException, ResourceActivatorException {
                // method can fail if you wish
                LOG.info("dectivate was triggered for {}", serviceId);
            }

            @Override
            public void update() throws TransactionCommitFailedException, ResourceActivatorException {

            }

            @Override
            public int priority() {
                //if you would like to make your driver first on the list

                return 0;
            }

            private String epsInfo() {
                return endpoints.stream().map(e -> e.getNepRef().getNodeId().getValue() + ":"
                        + e.getNepRef().getOwnedNodeEdgePointId().getValue())
                        .collect(Collectors.joining(",", "[", "]"));
            }
        });
    }

    @Override
    public String getActivationDriverId() {
        return TemplateConstants.DRIVER_ID;
    }
}
