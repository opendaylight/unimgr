/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator;

import static org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.BandwidthProfileComposition.BwpApplicability.UNI;
import static org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.BandwidthProfileComposition.BwpDirection.EGRESS;
import static org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.BandwidthProfileComposition.BwpDirection.INGRESS;

import java.util.Optional;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.BandwidthProfileHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.InterfaceHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.AttachmentCircuitHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.L2vpnHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.PseudowireHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.XConnectHelper;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev161215.PolicyManager;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.XconnectGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.XconnectGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.AttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.Pseudowires;

/**
 * Activator of VPLS-based L2 VPN using bridge connection on IOS-XR devices
 *
 * @author krzysztof.bijakowski@amartus.com
 */
public class L2vpnLocalConnectActivator extends AbstractL2vpnActivator {

    private static final String GROUP_NAME = "local";

    public L2vpnLocalConnectActivator(DataBroker dataBroker, MountPointService mountService) {
        super(dataBroker, mountService);
    }

    @Override
    protected Optional<PolicyManager> activateQos(String name, ServicePort port) {
        return new BandwidthProfileHelper(port)
                .addPolicyMap(name, INGRESS, UNI)
                .addPolicyMap(name, EGRESS, UNI)
                .build();
    }

    @Override
    protected InterfaceConfigurations activateInterface(ServicePort port, ServicePort neighbor, long mtu) {
        return new InterfaceHelper()
            .addInterface(port, Optional.empty(), true)
            .addInterface(neighbor, Optional.empty(), true)
            .build();
    }

    @Override
    protected Pseudowires activatePseudowire(ServicePort neighbor) {
        return new PseudowireHelper().build();
    }

    @Override
    protected XconnectGroups activateXConnect(String outerName, String innerName, ServicePort portA, ServicePort portZ, Pseudowires pseudowires) {
        AttachmentCircuits attachmentCircuits = new AttachmentCircuitHelper()
            .addPort(portA)
            .addPort(portZ)
            .build();

        XconnectGroup xconnectGroup = new XConnectHelper()
            .appendXConnect(innerName, attachmentCircuits, pseudowires)
            .build(outerName);

        return XConnectHelper.createXConnectGroups(xconnectGroup);
    }

    @Override
    protected L2vpn activateL2Vpn(XconnectGroups xconnectGroups) {
        return L2vpnHelper.build(xconnectGroups);
    }

    @Override
    protected String getInnerName(String serviceId) {
        return GROUP_NAME;
    }

    @Override
    protected String getOuterName(String serviceId) {
        return GROUP_NAME;
    }
}
