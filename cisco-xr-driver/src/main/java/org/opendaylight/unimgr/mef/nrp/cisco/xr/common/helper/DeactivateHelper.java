/*
 * Copyright (c) 2019 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator.AbstractL2vpnBridgeDomainActivator;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.five.L2vpnXrVersionSixDotFiveHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.one.L2vpnXrVersionSixDotOneHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.three.L2vpnXrVersionSixDotThreeHelper;
import org.opendaylight.unimgr.utils.NetconfConstants;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class DeactivateHelper {
	private static final Logger LOG = LoggerFactory.getLogger(DeactivateHelper.class);
	private static Map<NodeId, String> nodeXRVersionMap = new HashMap<NodeId, String>();

	public static void deativateXconnectConfiguration(ServicePort port, boolean isExclusive, String innerOuterName,
			EndPoint endpoint, List<String> dvls, WriteTransaction transaction) {
		nodeXRVersionMap = InterfaceHelper.getNodeXRVersionMap();

		if (nodeXRVersionMap.containsKey(port.getNode())) {
			String XRVersion = nodeXRVersionMap.get(port.getNode());

			if (XRVersion.equals(NetconfConstants.XR_VERSION_SIX_ONE)) {
				if (!ServicePort.isSameDevice(endpoint, dvls)) {
					transaction.delete(LogicalDatastoreType.CONFIGURATION,
							L2vpnXrVersionSixDotOneHelper.getXConnectIds(innerOuterName, innerOuterName));
				}
				transaction.delete(LogicalDatastoreType.CONFIGURATION,
						L2vpnXrVersionSixDotOneHelper.getInterfaceConfigurationId(port, isExclusive));
			}

			if (XRVersion.equals(NetconfConstants.XR_VERSION_SIX_THREE)) {
				if (!ServicePort.isSameDevice(endpoint, dvls)) {
					transaction.delete(LogicalDatastoreType.CONFIGURATION,
							L2vpnXrVersionSixDotThreeHelper.getXConnectIds(innerOuterName, innerOuterName));
				}
				transaction.delete(LogicalDatastoreType.CONFIGURATION,
						L2vpnXrVersionSixDotThreeHelper.getInterfaceConfigurationId(port, isExclusive));
			}

			if (XRVersion.equals(NetconfConstants.XR_VERSION_SIX_FIVE)) {
				if (!ServicePort.isSameDevice(endpoint, dvls)) {
					transaction.delete(LogicalDatastoreType.CONFIGURATION,
							L2vpnXrVersionSixDotFiveHelper.getXConnectIds(innerOuterName, innerOuterName));
				}
				transaction.delete(LogicalDatastoreType.CONFIGURATION,
						L2vpnXrVersionSixDotFiveHelper.getInterfaceConfigurationId(port, isExclusive));
			}
		}
	}

	public static void deativateBridgeDomainConfiguration(ServicePort port, boolean isExclusive, String innerOuterName,
			EndPoint endpoint, List<String> dvls, List<Uuid> inls, WriteTransaction transaction) {

		nodeXRVersionMap = InterfaceHelper.getNodeXRVersionMap();

		if (nodeXRVersionMap.containsKey(port.getNode())) {

			String XRVersion = nodeXRVersionMap.get(port.getNode());

			if(XRVersion.equals(NetconfConstants.XR_VERSION_SIX_ONE)) { 
				if (!AbstractL2vpnBridgeDomainActivator.isSameInterface(endpoint, inls)) {
					if (!ServicePort.isSameDevice(endpoint, dvls)) {
						transaction.delete(LogicalDatastoreType.CONFIGURATION,
								L2vpnXrVersionSixDotOneHelper.getBridgeDomainIds(innerOuterName, innerOuterName));
					}
					transaction.delete(LogicalDatastoreType.CONFIGURATION,
							L2vpnXrVersionSixDotOneHelper.getInterfaceConfigurationId(port, isExclusive));
				}
			}

			if(XRVersion.equals(NetconfConstants.XR_VERSION_SIX_THREE)) { 
				if (!AbstractL2vpnBridgeDomainActivator.isSameInterface(endpoint, inls)) {
					if (!ServicePort.isSameDevice(endpoint, dvls)) {
						transaction.delete(LogicalDatastoreType.CONFIGURATION,
								L2vpnXrVersionSixDotThreeHelper.getBridgeDomainIds(innerOuterName, innerOuterName));
					}
					transaction.delete(LogicalDatastoreType.CONFIGURATION,
							L2vpnXrVersionSixDotThreeHelper.getInterfaceConfigurationId(port, isExclusive));
				}
			}

			if(XRVersion.equals(NetconfConstants.XR_VERSION_SIX_FIVE)) { 
				if (!AbstractL2vpnBridgeDomainActivator.isSameInterface(endpoint, inls)) {
					if (!ServicePort.isSameDevice(endpoint, dvls)) {
						transaction.delete(LogicalDatastoreType.CONFIGURATION,
								L2vpnXrVersionSixDotFiveHelper.getBridgeDomainIds(innerOuterName, innerOuterName));
					}
					transaction.delete(LogicalDatastoreType.CONFIGURATION,
							L2vpnXrVersionSixDotFiveHelper.getInterfaceConfigurationId(port, isExclusive));
				}
			}
		}

	}
}
