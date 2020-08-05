/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper;

import java.util.Optional;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.InterfaceHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.five.InterfaceRev170907Helper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.five.L2vpnRev170626Helper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.one.InterfaceRev150730Helper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.one.L2vpnRev151109Helper;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev161215.PolicyManager;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpnBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.Database;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.DatabaseBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.BridgeDomainGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.XconnectGroups;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/*
 * Helper, supports configuration of L2VPN
 *
 * @author krzysztof.bijakowski@amartus.com
 */
public final class L2vpnHelper {

    private L2vpnHelper() {
    }

	/*
	 * public static InstanceIdentifier<L2vpn> getL2vpnId() { return
	 * InstanceIdentifier.builder(L2vpn.class).build(); }
	 */

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

    
    public static void setL2vpnConfiguration(ServicePort port, String outerName, String innerName,
            ServicePort neighbor, boolean isExclusive, DataBroker dataBroker, WriteTransaction transaction) {
        if (InterfaceHelper.getNodeInterfaceIdentifierList().containsKey(port.getNode().getValue())) {
             String identifierPath = InterfaceHelper.getNodeInterfaceIdentifierList().get(port.getNode().getValue());

     		if(identifierPath.contains(InterfaceRev150730Helper.getInterfaceConfigurationsId().getTargetType().getPackageName())) { 
 	    		transaction.merge(LogicalDatastoreType.CONFIGURATION, L2vpnRev151109Helper.getL2vpnId(),
 	    				L2vpnRev151109Helper.addL2vpn(port, outerName, innerName, neighbor, isExclusive, dataBroker));
            }

     		if(identifierPath.contains(InterfaceRev170907Helper.getInterfaceConfigurationsId().getTargetType().getPackageName())) { 
 	    		transaction.merge(LogicalDatastoreType.CONFIGURATION, L2vpnRev170626Helper.getL2vpnId(),
 	    				L2vpnRev170626Helper.addL2vpn(port, outerName, innerName, neighbor, isExclusive, dataBroker));
            }
        }
    }

}
