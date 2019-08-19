/*
 * Copyright (c) 2019 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator;

import java.util.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.InterfaceHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.BridgeDomainAttachmentCircuitHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.BridgeDomainHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.BridgeDomainPseudowireHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.L2vpnHelper;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.BridgeDomainGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.BridgeDomainGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.BdAttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.BdPseudowires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arif.hussain@xoriant.com
 */
public class L2vpnBridgeDomainLocalActivator extends AbstractL2vpnBridgeDomainActivator {

    private static final Logger LOG = LoggerFactory.getLogger(L2vpnBridgeDomainLocalActivator.class);

    public L2vpnBridgeDomainLocalActivator(DataBroker dataBroker, MountPointService mountService) {
        super(dataBroker, mountService);
    }

    @Override
    protected BdPseudowires activateBdPseudowire(ServicePort neighbor) {
        return new BridgeDomainPseudowireHelper().build();
    }

    @Override
    protected BridgeDomainGroups activateBridgeDomain(String outerName, String innerName,
            ServicePort port, ServicePort neighbor, BdPseudowires bdPseudowires,
            boolean isExclusive) {

        BdAttachmentCircuits bdattachmentCircuits = new BridgeDomainAttachmentCircuitHelper()
                .addPort(port, isExclusive).addPort(neighbor, isExclusive).build();

        BridgeDomainGroup bridgeDomainGroup = new BridgeDomainHelper()
                .appendBridgeDomain(innerName, bdattachmentCircuits, bdPseudowires)
                .build(outerName);

        return BridgeDomainHelper.createBridgeDomainGroups(bridgeDomainGroup);
    }

    @Override
    protected L2vpn activateL2Vpn(BridgeDomainGroups bridgeDomainGroups) {
        return L2vpnHelper.build(bridgeDomainGroups);
    }

    @Override
    protected String getInnerName(String serviceId) {
        return replaceForbidenCharacters(serviceId);
    }

    @Override
    protected String getOuterName(String serviceId) {
        return replaceForbidenCharacters(serviceId);
    }

    @Override
    protected InterfaceConfigurations activateInterface(ServicePort port, ServicePort neighbor,
            long mtu, boolean isExclusive) {
        boolean setL2Transport = (isExclusive) ? true : false;

        return new InterfaceHelper()
            .addInterface(port, Optional.empty(), setL2Transport)
            .addInterface(neighbor, Optional.empty(), setL2Transport)
            .build();
    }

    @Override
    protected InterfaceConfigurations createSubInterface(ServicePort port, ServicePort portZ,
            long mtu) {
        return new InterfaceHelper()
                .addSubInterface(port, Optional.empty())
                .build();
    }

    /**
     * ASR 9000 can't accept colon in xconnect group name, so it have to be replaced with underscore.
     * If any other restriction will be found, this is a good place to change serviceId name.
     *
     * @param serviceId old service id
     * @return new service id
     */
    private String replaceForbidenCharacters(String serviceId) {
        return serviceId.replace(":","_");
    }

}
