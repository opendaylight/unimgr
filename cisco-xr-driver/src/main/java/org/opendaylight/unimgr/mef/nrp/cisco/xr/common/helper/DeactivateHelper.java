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
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.five.InterfaceRev170907Helper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.five.L2vpnRev170626Helper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.one.InterfaceRev150730Helper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.one.L2vpnRev151109Helper;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class DeactivateHelper {
	private static final Logger LOG = LoggerFactory.getLogger(DeactivateHelper.class);
	private static Map<String, String> nodeInterfaceIdentifierList = new HashMap<String, String>();

	public static void deativateXconnectConfiguration(ServicePort port, boolean isExclusive, String innerOuterName,
			EndPoint endpoint, List<String> dvls, WriteTransaction transaction) {
		nodeInterfaceIdentifierList = InterfaceHelper.getNodeInterfaceIdentifierList();

		if (nodeInterfaceIdentifierList.containsKey(port.getNode().getValue())) {
			String identifierPath = nodeInterfaceIdentifierList.get(port.getNode().getValue());

			if (identifierPath.contains(
					InterfaceRev150730Helper.getInterfaceConfigurationsId().getTargetType().getPackageName())) {
				
				LOG.info("Xconnect"+InterfaceRev150730Helper.getInterfaceConfigurationsId().getTargetType().getPackageName());
				
				if (!ServicePort.isSameDevice(endpoint, dvls)) {
					transaction.delete(LogicalDatastoreType.CONFIGURATION,
							L2vpnRev151109Helper.getXConnectIds(innerOuterName, innerOuterName));
				}
				transaction.delete(LogicalDatastoreType.CONFIGURATION,
						L2vpnRev151109Helper.getInterfaceConfigurationId(port, isExclusive));
			}

			if (identifierPath.contains(
					InterfaceRev170907Helper.getInterfaceConfigurationsId().getTargetType().getPackageName())) {
				
				LOG.info("Xconnect"+InterfaceRev170907Helper.getInterfaceConfigurationsId().getTargetType().getPackageName());
				if (!ServicePort.isSameDevice(endpoint, dvls)) {
					transaction.delete(LogicalDatastoreType.CONFIGURATION,
							L2vpnRev170626Helper.getXConnectIds(innerOuterName, innerOuterName));
				}
				transaction.delete(LogicalDatastoreType.CONFIGURATION,
						L2vpnRev170626Helper.getInterfaceConfigurationId(port, isExclusive));
			}
		}
	}

	public static void deativateBridgeDomainConfiguration(ServicePort port, boolean isExclusive, String innerOuterName,
			EndPoint endpoint, List<String> dvls, List<Uuid> inls, WriteTransaction transaction) {

		nodeInterfaceIdentifierList = InterfaceHelper.getNodeInterfaceIdentifierList();

		if (nodeInterfaceIdentifierList.containsKey(port.getNode().getValue())) {

			String identifierPath = nodeInterfaceIdentifierList.get(port.getNode().getValue());
			// InterfaceRev170907Helper

			if (identifierPath.contains(
					InterfaceRev150730Helper.getInterfaceConfigurationsId().getTargetType().getPackageName())) {
				LOG.info("Bridge Domain"+InterfaceRev150730Helper.getInterfaceConfigurationsId().getTargetType().getPackageName());
				if (!AbstractL2vpnBridgeDomainActivator.isSameInterface(endpoint, inls)) {
					if (!ServicePort.isSameDevice(endpoint, dvls)) {
						transaction.delete(LogicalDatastoreType.CONFIGURATION,
								L2vpnRev151109Helper.getBridgeDomainIds(innerOuterName, innerOuterName));

					}

					transaction.delete(LogicalDatastoreType.CONFIGURATION,
							L2vpnRev151109Helper.getInterfaceConfigurationId(port, isExclusive));

				}
			}

			if (identifierPath.contains(
					InterfaceRev170907Helper.getInterfaceConfigurationsId().getTargetType().getPackageName())) {
				LOG.info("Bridge Domain"+InterfaceRev170907Helper.getInterfaceConfigurationsId().getTargetType().getPackageName());
				if (!AbstractL2vpnBridgeDomainActivator.isSameInterface(endpoint, inls)) {

					if (!ServicePort.isSameDevice(endpoint, dvls)) {

						transaction.delete(LogicalDatastoreType.CONFIGURATION,
								L2vpnRev170626Helper.getBridgeDomainIds(innerOuterName, innerOuterName));

					}

					transaction.delete(LogicalDatastoreType.CONFIGURATION,
							L2vpnRev170626Helper.getInterfaceConfigurationId(port, isExclusive));

				}
			}
		}

	}
}
