/*
 * Copyright (c) 2016 Microsemi and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.edgeassure;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverBuilder;
import org.opendaylight.unimgr.mef.nrp.common.FixedServiceNaming;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.forwarding.constructs.ForwardingConstruct;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;

import java.util.Optional;

/**
 * Fake driver builder;
 * @author sean.condon@microsemi.com
 */
public class EdgeAssureDriverBuilder implements ActivationDriverBuilder {

    private final FixedServiceNaming namingProvider;
    private final EdgeAssureActivator edgeAssureActivator;

    EdgeAssureDriverBuilder(DataBroker dataBroker, MountPointService mountService) {
        this.namingProvider = new FixedServiceNaming();
        edgeAssureActivator = new EdgeAssureActivator(dataBroker, mountService);
    }

    @Override
    public Optional<ActivationDriver> driverFor(FcPort port, BuilderContext context) {
        final ActivationDriver driver = new ActivationDriver() {
            public ForwardingConstruct ctx;
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
                this.ctx = ctx;
            }

            @Override
            public void activate() {
                String id = ctx.getUuid();
                long mtu = 1500;
                String outerName = namingProvider.getOuterName(id);
                String innerName = namingProvider.getInnerName(id);

                String aEndNodeName = aEnd.getNode().getValue();
                edgeAssureActivator.activate(aEndNodeName, outerName, innerName, aEnd, zEnd, mtu);

            }

            @Override
            public void deactivate() {
                String id = ctx.getUuid();
                long mtu = 1500;
                String outerName = namingProvider.getOuterName(id);
                String innerName = namingProvider.getInnerName(id);

                String aEndNodeName = aEnd.getNode().getValue();
                edgeAssureActivator.deactivate(aEndNodeName, outerName, innerName, aEnd, zEnd, mtu);
            }

            @Override
            public int priority() {
                return 0;
            }
        };

        return Optional.of(driver);
    }

    @Override
    public Optional<ActivationDriver> driverFor(FcPort aPort, FcPort zPort, BuilderContext context) {
        return Optional.empty();
    }
}
