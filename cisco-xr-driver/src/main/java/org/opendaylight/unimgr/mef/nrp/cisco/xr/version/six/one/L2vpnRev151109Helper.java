/*
 * Copyright (c) 2020 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.one;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util.LoopbackUtils;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.PseudowireGenerator;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.one.l2vpn.helper.AttachmentCircuitHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.one.l2vpn.helper.BridgeDomainAttachmentCircuitHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.one.l2vpn.helper.BridgeDomainHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.one.l2vpn.helper.BridgeDomainPseudowireHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.one.l2vpn.helper.PseudowireHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.one.l2vpn.helper.XConnectHelper;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpnBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.Database;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.DatabaseBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.BridgeDomainGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.XconnectGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.BridgeDomainGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.BdAttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.BdPseudowires;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.XconnectGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.AttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.Pseudowires;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ServiceType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class L2vpnRev151109Helper {

    public static InstanceIdentifier<L2vpn> getL2vpnId() {
        return InstanceIdentifier.builder(L2vpn.class).build();
    }

    public static L2vpn build(XconnectGroups xconnectGroups) {
        Database database = new DatabaseBuilder()
            .setXconnectGroups(xconnectGroups)
            .build();

        return new L2vpnBuilder()
            .setDatabase(database)
            .build();
    }

    public static L2vpn build(BridgeDomainGroups bridgeDomainGroups) {
        Database database = new DatabaseBuilder()
            .setBridgeDomainGroups(bridgeDomainGroups)
            .build();

        return new L2vpnBuilder()
            .setDatabase(database)
            .build();
    }

    public static L2vpn addL2vpn(ServicePort port, String outerName, String innerName, ServicePort neighbor,
    		boolean isExclusive, DataBroker dataBroker, ServiceType serviceType) {

    	if (serviceType.getName().equals(ServiceType.POINTTOPOINTCONNECTIVITY.getName())) {
    		Pseudowires pseudowires = activatePseudowire(neighbor, dataBroker);
    		XconnectGroups xconnectGroups = activateXConnect(outerName, innerName, port, neighbor, pseudowires, isExclusive);
    		return activateL2Vpn(xconnectGroups);
    	} else {
			BdPseudowires bdPseudowires = activateBdPseudowire(neighbor, dataBroker);
			BridgeDomainGroups bridgeDomainGroups = activateBridgeDomain(outerName,
                                                                    innerName,
                                                                    port,
                                                                    neighbor,
                                                                    bdPseudowires,
                                                                    isExclusive);
			return activateL2Vpn(bridgeDomainGroups);
        }
    }

    protected static Pseudowires activatePseudowire(ServicePort neighbor, DataBroker dataBroker) {
        return new PseudowireHelper()
                .addPseudowire(LoopbackUtils.getIpv4Address(neighbor, dataBroker), PseudowireGenerator.getPseudowireId()).build();
    }

    protected static XconnectGroups activateXConnect(String outerName, String innerName, ServicePort port,
            ServicePort neighbor, Pseudowires pseudowires, boolean isExclusive) {

       AttachmentCircuits attachmentCircuits = new AttachmentCircuitHelper()
               .addPort(port, isExclusive)
               .build();

       XconnectGroup xconnectGroup = new XConnectHelper()
               .appendXConnect(innerName, attachmentCircuits, pseudowires)
               .build(outerName);

       return XConnectHelper.createXConnectGroups(xconnectGroup);
   }

    protected static L2vpn activateL2Vpn(XconnectGroups xconnectGroups) {
        return build(xconnectGroups);
    }

    protected static BridgeDomainGroups activateBridgeDomain(String outerName, String innerName,
            ServicePort port, ServicePort neighbor, BdPseudowires bdPseudowires,
            boolean isExclusive) {

        BdAttachmentCircuits bdattachmentCircuits =
                                new BridgeDomainAttachmentCircuitHelper().addPort(port, isExclusive).build();
        BridgeDomainGroup bridgeDomainGroup = new BridgeDomainHelper()
                .appendBridgeDomain(innerName, bdattachmentCircuits, bdPseudowires)
                .build(outerName);

        return BridgeDomainHelper.createBridgeDomainGroups(bridgeDomainGroup);
    }

    protected static BdPseudowires activateBdPseudowire(ServicePort neighbor, DataBroker dataBroker) {
        return new BridgeDomainPseudowireHelper()
                .addBdPseudowire(LoopbackUtils.getIpv4Address(neighbor, dataBroker)).build();
    }

    protected static L2vpn activateL2Vpn(BridgeDomainGroups bridgeDomainGroups) {

        return build(bridgeDomainGroups);
    }
}
