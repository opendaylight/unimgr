/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.mef.nrp.cisco.xr;

import java.util.Optional;

import org.mef.nrp.impl.ActivationDriver;
import org.mef.nrp.impl.ActivationDriverBuilder;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.GFcPort;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.GForwardingConstruct;

/**
 * Provides drivers for binding two ports on the same node.
 * @author bartosz.michalik@amartus.com
 */
public class L2vpnBridgeDriverBuilder implements ActivationDriverBuilder, BindingAwareConsumer {

    private L2vpnBridgeActivator activator;

    @Override
    public void onSessionInitialized(BindingAwareBroker.ConsumerContext session) {
         DataBroker dataBroker = session.getSALService(DataBroker.class);
         MountPointService mountService = session.getSALService(MountPointService.class);
         activator = new L2vpnBridgeActivator(dataBroker, mountService);
    }

    @Override
    public Optional<ActivationDriver> driverFor(GFcPort port, BuilderContext _ctx) {
        return Optional.empty();
    }

    @Override
    public Optional<ActivationDriver> driverFor(GFcPort aPort, GFcPort zPort, BuilderContext context) {
        return Optional.of(getDriver());
    }

    protected ActivationDriver getDriver() {
        final ActivationDriver driver = new ActivationDriver() {
            public GForwardingConstruct ctx;
            public GFcPort aEnd;
            public GFcPort zEnd;

            @Override
            public void commit() {
                //ignore for the moment
            }

            @Override
            public void rollback() {
                //ignore for the moment
            }

            @Override
            public void initialize(GFcPort from, GFcPort to, GForwardingConstruct ctx) {
                this.zEnd = to;
                this.aEnd = from;
                this.ctx = ctx;
            }

            @Override
            public void activate() {
                String id = ctx.getUuid();
                long mtu = 1500;
                String outerName = "outer";
                String innerName = "inner";

                String aEndNodeName = aEnd.getLtpRefList().get(0).getValue().split(":")[0];
                activator.activate(aEndNodeName, outerName, innerName, aEnd, zEnd, mtu);
            }

            @Override
            public void deactivate() {
                String id = ctx.getUuid();
                long mtu = 1500;
                String outerName = "outer";
                String innerName = "inner";

                String aEndNodeName = aEnd.getLtpRefList().get(0).getValue().split(":")[0];
                activator.deactivate(aEndNodeName, outerName, innerName, aEnd, zEnd, mtu);
            }

            @Override
            public int priority() {
                return 0;
            }
        };
        return driver;
    }
}
