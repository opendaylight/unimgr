/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.InterfaceHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.AttachmentCircuitHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.L2vpnHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.PseudowireHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.XConnectHelper;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.XconnectGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.XconnectGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.AttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.Pseudowires;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;

/**
 * Activator of VPLS-based L2 VPN using bridge connection on IOS-XR devices
 *
 * @author krzysztof.bijakowski@amartus.com
 */
public class L2vpnBridgeActivator extends AbstractL2vpnActivator {

    public L2vpnBridgeActivator(DataBroker dataBroker, MountPointService mountService) {
        super(mountService);
    }

    @Override
    protected InterfaceConfigurations activateInterface(FcPort port, FcPort neighbor, long mtu) {
        return new InterfaceHelper()
            .addInterface(port, Optional.absent(), true)
            .addInterface(neighbor, Optional.absent(), true)
            .build();
    }

    @Override
    protected Pseudowires activatePseudowire(FcPort neighbor) {
        return new PseudowireHelper().build();
    }

    @Override
    protected XconnectGroups activateXConnect(String outerName, String innerName, FcPort portA, FcPort portZ, Pseudowires pseudowires) {
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
}
