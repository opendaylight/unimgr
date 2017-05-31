/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.driver;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverBuilder;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivatorException;
import org.opendaylight.unimgr.mef.nrp.ovs.activator.OvsActivator;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp_interface.rev170227.NrpCreateConnectivityServiceAttrs;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.UniversalId;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;

import java.util.List;
import java.util.Optional;

/**
 * @author marek.ryznar@amartus.com
 */
public class OvsDriver implements ActivationDriverBuilder {

    private OvsActivator activator;
    private final DataBroker dataBroker;
    private static final String GROUP_NAME = "local";
    private static final long MTU_VALUE = 1522;

    public OvsDriver(DataBroker dataBroker){
        this.dataBroker = dataBroker;
        activator = new OvsActivator(dataBroker);
    }

    private ActivationDriver getDriver() {
        return new ActivationDriver() {
            private FcPort aEnd;
            private FcPort zEnd;
            private String uuid;

            @Override
            public void commit() {

            }

            @Override
            public void rollback() {

            }

            @Override
            public void initialize(List<EndPoint> endPoints, String serviceId, NrpCreateConnectivityServiceAttrs context) {

            }

            @Override
            public void activate() throws TransactionCommitFailedException, ResourceActivatorException {
                activator.activate(null,null);
            }

            @Override
            public void deactivate() throws TransactionCommitFailedException, ResourceActivatorException {
                activator.deactivate(null,null);
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
    public UniversalId getNodeUuid() {
        return null;
    }
}
