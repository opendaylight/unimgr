/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.driver;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverBuilder;
import org.opendaylight.unimgr.utils.CapabilitiesService;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator.L2vpnXconnectActivator;
import org.opendaylight.unimgr.mef.nrp.common.FixedServiceNaming;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.forwarding.constructs.ForwardingConstruct;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;

import java.util.Optional;

import static org.opendaylight.unimgr.utils.CapabilitiesService.NodeContext.NodeCapability.NETCONF;
import static org.opendaylight.unimgr.utils.CapabilitiesService.Capability.Mode.AND;
import static org.opendaylight.unimgr.utils.CapabilitiesService.NodeContext.NodeCapability.NETCONF_CISCO_IOX_IFMGR;
import static org.opendaylight.unimgr.utils.CapabilitiesService.NodeContext.NodeCapability.NETCONF_CISCO_IOX_L2VPN;

/**
 * Xconnect builder
 * @author bartosz.michalik@amartus.com
 */
public class L2vpnXconnectDriverBuilder implements ActivationDriverBuilder {

    private final DataBroker dataBroker;

    private final FixedServiceNaming namingProvider;

    private L2vpnXconnectActivator xconnectActivator;

    public L2vpnXconnectDriverBuilder(DataBroker dataBroker, MountPointService mountPointService) {
        this.dataBroker = dataBroker;
        xconnectActivator = new L2vpnXconnectActivator(dataBroker, mountPointService);
        namingProvider = new FixedServiceNaming();
    }

    @Override
    public Optional<ActivationDriver> driverFor(FcPort port, BuilderContext _ctx) {
        if (new CapabilitiesService(dataBroker).nodeByPort(port).isSupporting(AND, NETCONF, NETCONF_CISCO_IOX_IFMGR, NETCONF_CISCO_IOX_L2VPN)) {
            return Optional.of(getDriver());
        }

        return Optional.empty();
    }

    @Override
    public Optional<ActivationDriver> driverFor(FcPort aPort, FcPort zPort, BuilderContext context) {
        return Optional.empty();
    }

    protected ActivationDriver getDriver() {
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
            public void activate() throws TransactionCommitFailedException {
                String id = ctx.getUuid();
                long mtu = 1500;
                String outerName = namingProvider.getOuterName(id);
                String innerName = namingProvider.getInnerName(id);

                String aEndNodeName = aEnd.getNode().getValue();
                xconnectActivator.activate(aEndNodeName, outerName, innerName, aEnd, zEnd, mtu);

            }

            @Override
            public void deactivate() throws TransactionCommitFailedException {
                String id = ctx.getUuid();
                long mtu = 1500;
                String outerName = namingProvider.getOuterName(id);
                String innerName = namingProvider.getInnerName(id);

                String aEndNodeName = aEnd.getNode().getValue();
                xconnectActivator.deactivate(aEndNodeName, outerName, innerName, aEnd, zEnd, mtu);
            }

            @Override
            public int priority() {
                return 0;
            }
        };

        return driver;
    }
}
