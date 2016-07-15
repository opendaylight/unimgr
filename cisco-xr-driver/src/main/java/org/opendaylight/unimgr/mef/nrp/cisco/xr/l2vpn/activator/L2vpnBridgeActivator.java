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
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivator;
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
public class L2vpnBridgeActivator implements ResourceActivator {

    private L2vpnActivator l2vpnActivator;

    public L2vpnBridgeActivator(DataBroker dataBroker, MountPointService mountService) {
        this.l2vpnActivator = new L2vpnActivator(mountService, new L2vpnActivator.ActivationHandler() {
            @Override
            public InterfaceConfigurations handleInterfaceActivation(FcPort portA, FcPort portZ, long mtu) {
                return new InterfaceHelper()
                    .configure(portA, Optional.absent(), true)
                    .configure(portZ, Optional.absent(), true)
                    .getInterfaceConfigurations();
            }

            @Override
            public Pseudowires handlePseudowireActivation(FcPort neighbor) {
                return new PseudowireHelper().getPseudowires();
            }

            @Override
            public XconnectGroups handleXConnectActivation(String outerName, String innerName, FcPort portA, FcPort portZ, Pseudowires pseudowires) {
                AttachmentCircuits attachmentCircuits = new AttachmentCircuitHelper()
                    .configure(portA)
                    .configure(portZ)
                    .getAttachmentCircuits();

                XconnectGroup xconnectGroup = new XConnectHelper()
                    .configure(innerName, attachmentCircuits, pseudowires)
                    .getXconnectGroup(outerName);

                return XConnectHelper.createXconnectGroups(xconnectGroup);
            }

            @Override
            public L2vpn handleL2vpnActivation(XconnectGroups xconnectGroups) {
                return L2vpnHelper.configureL2vpn(xconnectGroups);
            }
        });
    }

    @Override
    public void activate(String nodeName, String outerName, String innerName, FcPort port, FcPort neighbor, long mtu) {
        l2vpnActivator.activate(nodeName, outerName, innerName, port, neighbor, mtu);
    }

    @Override
    public void deactivate(String nodeName, String outerName, String innerName, FcPort port, FcPort neighbor, long mtu) {
        l2vpnActivator.deactivate(nodeName, "local", innerName, port, neighbor, mtu);
    }
}
