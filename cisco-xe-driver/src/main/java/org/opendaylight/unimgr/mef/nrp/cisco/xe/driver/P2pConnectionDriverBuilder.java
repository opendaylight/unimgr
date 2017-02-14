/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xe.driver;


import java.util.Optional;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverBuilder;
import org.opendaylight.unimgr.mef.nrp.cisco.xe.activator.P2pConnectionActivator;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivatorException;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.forwarding.constructs.ForwardingConstruct;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2pConnectionDriverBuilder implements ActivationDriverBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(P2pConnectionDriverBuilder.class);

    private static final String GROUP_NAME = "local";
    private static final String XE_TOPOLOGY_ID = "topology-cisco-xe";
    private P2pConnectionActivator activator;



    public P2pConnectionDriverBuilder(DataBroker dataBroker) {
        activator = new P2pConnectionActivator(dataBroker);
    }

    @Override
    public Optional<ActivationDriver> driverFor(FcPort port, BuilderContext _ctx) {
        //TODO: use SBI API to check connectivity
    	if(XE_TOPOLOGY_ID.equals(port.getTopology().getValue())){
            return Optional.of(getDriver());
    	}
        return Optional.empty();
    }

    @Override
    public Optional<ActivationDriver> driverFor(FcPort aPort, FcPort zPort, BuilderContext context) {
        //TODO: use SBI API to check connectivity
        if(XE_TOPOLOGY_ID.equals(aPort.getTopology().getValue()) && XE_TOPOLOGY_ID.equals(zPort.getTopology().getValue())){
            return Optional.of(getDriver());
        }
        return Optional.empty();
    }

    protected ActivationDriver getDriver() {
        final ActivationDriver driver = new ActivationDriver() {
            public FcPort aEnd;
            public FcPort zEnd;

            @Override
            public void commit() {
                //ignore for the moment
            }

            @Override
            public void rollback() {
                //ignore for the moment
            }

            @Override
            public void initialize(FcPort from, FcPort to, ForwardingConstruct ctx) {
                this.zEnd = to;
                this.aEnd = from;
            }

            @Override
            public void activate() throws TransactionCommitFailedException, ResourceActivatorException {
                String aEndNodeName = aEnd.getNode().getValue();
                activator.activate(aEndNodeName, GROUP_NAME, GROUP_NAME, aEnd, zEnd, 1522);
            }

            @Override
            public void deactivate() throws TransactionCommitFailedException, ResourceActivatorException {

                String aEndNodeName = aEnd.getNode().getValue();
                activator.deactivate(aEndNodeName, GROUP_NAME, GROUP_NAME, aEnd, zEnd, 1522);
            }

            @Override
            public int priority() {
                return 0;
            }
        };

        return driver;
    }
}
