/*
 * Copyright (c) 2019 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.one.l2vpn.helper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.InterfaceHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator.AbstractL2vpnBridgeDomainActivator;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator.TransactionActivator;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.five.InterfaceRev170907Helper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.one.InterfaceRev150730Helper;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeactivateHelper {
	private static final Logger LOG = LoggerFactory.getLogger(DeactivateHelper.class);
	private static Map<String, String> nodeInterfaceIdentifierList = new HashMap<String, String>();
	
	public static void deativateXconnectConfiguration(ServicePort port,boolean isExclusive, String innerOuterName, EndPoint endpoint,List<String> dvls,WriteTransaction transaction) {
		nodeInterfaceIdentifierList=InterfaceHelper.getNodeInterfaceIdentifierList();
		LOG.info("********************************IN deativateXconnectConfiguration******************************");
		 if (nodeInterfaceIdentifierList.containsKey(port.getNode().getValue())) {
	            String identifierPath = nodeInterfaceIdentifierList.get(port.getNode().getValue());
	            LOG.info("********************************IN deativateXconnectConfiguration******************1");
	            //InterfaceRev170907Helper
	    		if(identifierPath.contains(InterfaceRev150730Helper.getInterfaceConfigurationsId().getTargetType().getPackageName())) { 
	    			 LOG.info("********************************IN deativateXconnectConfiguration******************2");
	                if (!ServicePort.isSameDevice(endpoint, dvls)) {
	                	LOG.info("********************************IN deativateXconnectConfiguration******************3"+ InterfaceRev150730Helper.getXConnectIds(innerOuterName, innerOuterName));
	                    transaction.delete(LogicalDatastoreType.CONFIGURATION, InterfaceRev150730Helper.getXConnectIds(innerOuterName, innerOuterName));
	                    LOG.info("********************************IN deativateXconnectConfiguration******************4");
	                }
	                LOG.info("********************************IN deativateXconnectConfiguration******************5");
	                transaction.delete(LogicalDatastoreType.CONFIGURATION, InterfaceRev150730Helper.getInterfaceConfigurationId(port, isExclusive));
	                LOG.info("********************************IN deativateXconnectConfiguration******************6");
	    		}

	    		if(identifierPath.contains(InterfaceRev170907Helper.getInterfaceConfigurationsId().getTargetType().getPackageName())) { 
	    			LOG.info("********************************IN deativateXconnectConfiguration******************7");
		                if (!ServicePort.isSameDevice(endpoint, dvls)) {
		                	LOG.info("********************************IN deativateXconnectConfiguration******************8=="+InterfaceRev170907Helper.getXConnectIds(innerOuterName, innerOuterName));
		                    transaction.delete(LogicalDatastoreType.CONFIGURATION, InterfaceRev170907Helper.getXConnectIds(innerOuterName, innerOuterName));
		                    LOG.info("********************************IN deativateXconnectConfiguration******************9");
		                }
		                LOG.info("********************************IN deativateXconnectConfiguration******************10=="+InterfaceRev170907Helper.getInterfaceConfigurationId(port, isExclusive));
		                transaction.delete(LogicalDatastoreType.CONFIGURATION,InterfaceRev170907Helper.getInterfaceConfigurationId(port, isExclusive));
		                LOG.info("********************************IN deativateXconnectConfiguration******************11");
		    		}
	    		}
	      
		
		
	}
	public static void deativateBridgeDomainConfiguration(ServicePort port,boolean isExclusive, String innerOuterName, EndPoint endpoint,List<String> dvls, List<Uuid> inls,WriteTransaction transaction) {
		LOG.info("********************************IN deativateBridgeDomainConfiguration******************************");
		nodeInterfaceIdentifierList=InterfaceHelper.getNodeInterfaceIdentifierList();
		LOG.info("********************************IN deativateBridgeDomainConfiguration******************************1");
		 if (nodeInterfaceIdentifierList.containsKey(port.getNode().getValue())) {
			 LOG.info("********************************IN deativateBridgeDomainConfiguration******************************2");
	            String identifierPath = nodeInterfaceIdentifierList.get(port.getNode().getValue());
	            //InterfaceRev170907Helper
	            LOG.info("********************************IN deativateBridgeDomainConfiguration******************************3");
	    		if(identifierPath.contains(InterfaceRev150730Helper.getInterfaceConfigurationsId().getTargetType().getPackageName())) { 
	    			LOG.info("********************************IN deativateBridgeDomainConfiguration******************************4");

	    			if (!AbstractL2vpnBridgeDomainActivator.isSameInterface(endpoint, inls)) {
	    				LOG.info("********************************IN deativateBridgeDomainConfiguration******************************5");
	                    if (!ServicePort.isSameDevice(endpoint, dvls)) {
	                    	LOG.info("********IN deativateBridgeDomainConfiguration******************IDs"+InterfaceRev150730Helper.getBridgeDomainIds(innerOuterName, innerOuterName));
	                        transaction.delete(LogicalDatastoreType.CONFIGURATION,InterfaceRev150730Helper.getBridgeDomainIds(innerOuterName, innerOuterName));
	                        LOG.info("********************************IN deativateBridgeDomainConfiguration******************************7");
	                    }
	                    LOG.info("********************************IN deativateBridgeDomainConfiguration******************************8");
	                    transaction.delete(LogicalDatastoreType.CONFIGURATION, InterfaceRev150730Helper.getInterfaceConfigurationId(port, isExclusive));
	                    LOG.info("********************************IN deativateBridgeDomainConfiguration******************************9");
	                }
	    		}

	    		if(identifierPath.contains(InterfaceRev170907Helper.getInterfaceConfigurationsId().getTargetType().getPackageName())) { 
	    			LOG.info("********************************IN deativateBridgeDomainConfiguration******************************10");
	    			if(identifierPath.contains(InterfaceRev150730Helper.getInterfaceConfigurationsId().getTargetType().getPackageName())) { 
	    				LOG.info("********************************IN deativateBridgeDomainConfiguration******************************11");

	    				if (!AbstractL2vpnBridgeDomainActivator.isSameInterface(endpoint, inls)) {
	    					LOG.info("********************************IN deativateBridgeDomainConfiguration******************************12");
	    	                if (!ServicePort.isSameDevice(endpoint, dvls)) {
	    	                	LOG.info("********************************IN deativateBridgeDomainConfiguration******************************13"+InterfaceRev170907Helper.getBridgeDomainIds(innerOuterName, innerOuterName));
	    	                    transaction.delete(LogicalDatastoreType.CONFIGURATION, InterfaceRev170907Helper.getBridgeDomainIds(innerOuterName, innerOuterName));
	    	                    LOG.info("********************************IN deativateBridgeDomainConfiguration******************************14");
	    	                }
	    	                LOG.info("********************************IN deativateBridgeDomainConfiguration******************************15");
	    	                transaction.delete(LogicalDatastoreType.CONFIGURATION, InterfaceRev170907Helper.getInterfaceConfigurationId(port, isExclusive));
	    	                LOG.info("********************************IN deativateBridgeDomainConfiguration******************************16");
	    	            }
		    		}
	    		}
	        }
		
		
	}

}
