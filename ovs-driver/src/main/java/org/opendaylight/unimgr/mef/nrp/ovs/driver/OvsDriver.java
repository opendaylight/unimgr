/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.driver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverBuilder;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivatorException;
import org.opendaylight.unimgr.mef.nrp.common.ResourceNotAvailableException;
import org.opendaylight.unimgr.mef.nrp.ovs.activator.OvsActivator;
import org.opendaylight.unimgr.mef.nrp.ovs.tapi.TopologyDataHandler;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.NrpConnectivityServiceAttrs;

/**
 * Ovs driver builder.
 * @author marek.ryznar@amartus.com
 */
public class OvsDriver implements ActivationDriverBuilder {

    private OvsActivator activator;

    public OvsDriver(DataBroker dataBroker) {
        activator = new OvsActivator(dataBroker);
    }


    private ActivationDriver getDriver() {
        return new ActivationDriver() {
            List<EndPoint> endPoints;
            String serviceId;

            @Override
            public void commit() {

            }

            @Override
            public void rollback() {

            }

            @Override
            public void initialize(List<EndPoint> endPoints, String serviceId, NrpConnectivityServiceAttrs context) {
                this.endPoints = new ArrayList<>(endPoints);
                this.serviceId = serviceId;
            }

            @Override
            public void activate() throws TransactionCommitFailedException, ResourceNotAvailableException {
                activator.activate(endPoints,serviceId);
            }

            @Override
            public void update() throws TransactionCommitFailedException, ResourceActivatorException {
                activator.update(endPoints,serviceId);
            }

            @Override
            public void deactivate() throws TransactionCommitFailedException, ResourceNotAvailableException {
                activator.deactivate(endPoints,serviceId);
            }

            @Override
            public int priority() {
                return 0;
            }


        };
    }

    @Override
    public Optional<ActivationDriver> driverFor(BuilderContext context) {
        return Optional.ofNullable(getDriver());
    }

    @Override
    public String getActivationDriverId() {
        return TopologyDataHandler.getDriverId();
    }
}
