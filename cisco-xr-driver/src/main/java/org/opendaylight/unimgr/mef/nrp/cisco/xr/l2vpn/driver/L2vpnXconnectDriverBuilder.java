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
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivatorException;
import org.opendaylight.unimgr.mef.nrp.common.ResourceNotAvailableException;
import org.opendaylight.unimgr.utils.CapabilitiesService;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator.L2vpnXconnectActivator;
import org.opendaylight.unimgr.mef.nrp.common.FixedServiceNaming;
import org.opendaylight.unimgr.utils.DriverConstants;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp_interface.rev170227.NrpCreateConnectivityServiceAttrs;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.UniversalId;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.forwarding.constructs.ForwardingConstruct;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;

import java.util.List;
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
    public Optional<ActivationDriver> driverFor(BuilderContext context) {
        return Optional.of(getDriver());
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
            public void initialize(List<EndPoint> endPoints, String serviceId, NrpCreateConnectivityServiceAttrs context) {

            }

            @Override
            public void activate() throws TransactionCommitFailedException, ResourceActivatorException {
                xconnectActivator.activate(null,null);
            }

            @Override
            public void deactivate() throws TransactionCommitFailedException, ResourceActivatorException {
                xconnectActivator.deactivate(null,null);
            }

            @Override
            public int priority() {
                return 0;
            }
        };

        return driver;
    }

    @Override
    public UniversalId getNodeUuid() {
        return UniversalId.getDefaultInstance(DriverConstants.XR_NODE);
    }
}
