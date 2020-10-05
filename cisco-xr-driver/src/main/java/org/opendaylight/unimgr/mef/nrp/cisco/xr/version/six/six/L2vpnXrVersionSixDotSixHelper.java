/*
 * Copyright (c) 2020 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.six;

import java.util.concurrent.ExecutionException;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.InterfaceHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util.LoopbackUtils;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.PseudowireGenerator;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.six.l2vpn.helper.AttachmentCircuitHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.six.l2vpn.helper.PseudowireHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.six.l2vpn.helper.XConnectHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.six.l2vpn.helper.BridgeDomainAttachmentCircuitHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.six.l2vpn.helper.BridgeDomainHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.six.l2vpn.helper.BridgeDomainPseudowireHelper;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.ifmgr.cfg.rev170907.InterfaceActive;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.ifmgr.cfg.rev170907.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.l2vpn.cfg.rev180615.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.l2vpn.cfg.rev180615.L2vpnBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.l2vpn.cfg.rev180615.bridge.domain.table.BridgeDomains;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.l2vpn.cfg.rev180615.bridge.domain.table.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.l2vpn.cfg.rev180615.bridge.domain.table.bridge.domains.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.l2vpn.cfg.rev180615.bridge.domain.table.bridge.domains.bridge.domain.BdAttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.l2vpn.cfg.rev180615.bridge.domain.table.bridge.domains.bridge.domain.BdPseudowires;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.l2vpn.cfg.rev180615.l2vpn.Database;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.l2vpn.cfg.rev180615.l2vpn.DatabaseBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.l2vpn.cfg.rev180615.l2vpn.database.BridgeDomainGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.l2vpn.cfg.rev180615.l2vpn.database.XconnectGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.l2vpn.cfg.rev180615.l2vpn.database.bridge.domain.groups.BridgeDomainGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.l2vpn.cfg.rev180615.l2vpn.database.bridge.domain.groups.BridgeDomainGroupKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.l2vpn.cfg.rev180615.l2vpn.database.xconnect.groups.XconnectGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.l2vpn.cfg.rev180615.l2vpn.database.xconnect.groups.XconnectGroupKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.l2vpn.cfg.rev180615.l2vpn.database.xconnect.groups.xconnect.group.P2pXconnects;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.l2vpn.cfg.rev180615.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnect;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.l2vpn.cfg.rev180615.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnectKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.l2vpn.cfg.rev180615.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.AttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._6.l2vpn.cfg.rev180615.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.Pseudowires;
/*import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.ifmgr.cfg.rev170907.InterfaceActive;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.ifmgr.cfg.rev170907.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.L2vpnBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.bridge.domain.table.BridgeDomains;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.bridge.domain.BdAttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.bridge.domain.BdPseudowires;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.l2vpn.Database;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.l2vpn.DatabaseBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.l2vpn.database.BridgeDomainGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.l2vpn.database.XconnectGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.l2vpn.database.bridge.domain.groups.BridgeDomainGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.l2vpn.database.bridge.domain.groups.BridgeDomainGroupKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.l2vpn.database.xconnect.groups.XconnectGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.l2vpn.database.xconnect.groups.XconnectGroupKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.l2vpn.database.xconnect.groups.xconnect.group.P2pXconnects;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnect;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnectKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.AttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.Pseudowires;
*/import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ServiceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class L2vpnXrVersionSixDotSixHelper {

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
    public static InstanceIdentifier<P2pXconnect> getXConnectIds(String innerName,String outerName)
    {
    	return InstanceIdentifier.builder(L2vpn.class).child(Database.class).child(XconnectGroups.class)
				.child(XconnectGroup.class, new XconnectGroupKey(new CiscoIosXrString(outerName)))
     			.child(P2pXconnects.class).child(P2pXconnect.class, new P2pXconnectKey(new CiscoIosXrString(innerName)))
				.build();
    }

    public static InstanceIdentifier<BridgeDomain> getBridgeDomainIds(String innerName, String outerName)
    {
    	 return InstanceIdentifier.builder(L2vpn.class).child(Database.class)
                 .child(BridgeDomainGroups.class)
                 .child(BridgeDomainGroup.class, new BridgeDomainGroupKey(new CiscoIosXrString(outerName)))
                 .child(BridgeDomains.class)
                 .child(BridgeDomain.class, new BridgeDomainKey(new CiscoIosXrString(innerName)))
                 .build();
    }
    
    public static InstanceIdentifier<InterfaceConfiguration> getInterfaceConfigurationId(ServicePort port, boolean isExclusive)
    {
    	return InstanceIdentifier
                .builder(
                        InterfaceConfigurations.class)
                .child(InterfaceConfiguration.class,
                        new InterfaceConfigurationKey(new InterfaceActive("act"),
                                isExclusive == true ? InterfaceHelper.getInterfaceName(port)
                                        : InterfaceHelper.getSubInterfaceName(port)))
                .build();
    }

    public static boolean checkL2vpnCapability(NetconfNode netconf) throws InterruptedException, ExecutionException {
        return netconf
        .getAvailableCapabilities()
        .getAvailableCapability()
        .stream()
        .anyMatch(capability -> capability.getCapability().equals("(http://cisco.com/ns/yang/Cisco-IOS-XR-l2vpn-cfg?revision=2018-06-15)Cisco-IOS-XR-l2vpn-cfg"));
        
     }
    
}
