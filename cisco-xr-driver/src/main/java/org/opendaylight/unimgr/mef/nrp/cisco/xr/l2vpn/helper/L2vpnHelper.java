/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.InterfaceHelper;
//import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.five.L2vpnXrVersionSixDotFiveHelper;
//import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.one.L2vpnXrVersionSixDotOneHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.six.L2vpnXrVersionSixDotSixHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.three.L2vpnXrVersionSixDotThreeHelper;
//import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.three.L2vpnXrVersionSixDotThreeHelper;
import org.opendaylight.unimgr.utils.NetconfConstants;
/*
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev161215.PolicyManager;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpnBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.Database;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.DatabaseBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.BridgeDomainGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.XconnectGroups;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
*/
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ServiceType;


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

//    public static L2vpn build(XconnectGroups xconnectGroups) {
//        Database database = new DatabaseBuilder()
//            .setXconnectGroups(xconnectGroups)
//            .build();
//
//        return new L2vpnBuilder()
//            .setDatabase(database)
//            .build();
//    }
//
//    public static L2vpn build(BridgeDomainGroups bridgeDomainGroups) {
//        Database database = new DatabaseBuilder()
//            .setBridgeDomainGroups(bridgeDomainGroups)
//            .build();
//
//        return new L2vpnBuilder()
//            .setDatabase(database)
//            .build();
//    }

    
    public static void setL2vpnConfiguration(ServicePort port, String outerName, String innerName,
            ServicePort neighbor, boolean isExclusive, DataBroker dataBroker,
            ServiceType serviceType, WriteTransaction transaction) {
        if (InterfaceHelper.getNodeXRVersionMap().containsKey(port.getNode())) {
             String XRVersion = InterfaceHelper.getNodeXRVersionMap().get(port.getNode());
/*
             if(XRVersion.equals(NetconfConstants.XR_VERSION_SIX_ONE)) { 
                transaction.merge(LogicalDatastoreType.CONFIGURATION, L2vpnXrVersionSixDotOneHelper.getL2vpnId(),
                    L2vpnXrVersionSixDotOneHelper.addL2vpn(port, outerName, innerName, neighbor, isExclusive, dataBroker, serviceType));
             }*/
            
             if(XRVersion.equals(NetconfConstants.XR_VERSION_SIX_THREE)) { 
                transaction.merge(LogicalDatastoreType.CONFIGURATION, L2vpnXrVersionSixDotThreeHelper.getL2vpnId(),
                    L2vpnXrVersionSixDotThreeHelper.addL2vpn(port, outerName, innerName, neighbor, isExclusive, dataBroker, serviceType));
              }
             /*
             if(XRVersion.equals(NetconfConstants.XR_VERSION_SIX_FIVE)) { 
                transaction.merge(LogicalDatastoreType.CONFIGURATION, L2vpnXrVersionSixDotFiveHelper.getL2vpnId(),
                    L2vpnXrVersionSixDotFiveHelper.addL2vpn(port, outerName, innerName, neighbor, isExclusive, dataBroker, serviceType));
            }*/
             if(XRVersion.equals(NetconfConstants.XR_VERSION_SIX_SIX)) { 
                 transaction.merge(LogicalDatastoreType.CONFIGURATION, L2vpnXrVersionSixDotSixHelper.getL2vpnId(),
                     L2vpnXrVersionSixDotSixHelper.addL2vpn(port, outerName, innerName, neighbor, isExclusive, dataBroker, serviceType));
             }
        }
    }

}
