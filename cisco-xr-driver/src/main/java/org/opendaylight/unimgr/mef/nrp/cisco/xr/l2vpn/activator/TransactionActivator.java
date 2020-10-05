/*
 * Copyright (c) 2019 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.FixedServiceNaming;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.MountPointHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.DeactivateHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.InterfaceHelper;
//import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util.MtuUtils;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.L2vpnHelper;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._1.infra.policymgr.cfg.rev161215.PolicyManager;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations._interface.configuration.Mtus;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev161215.PolicyManager;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TransactionActivator {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionActivator.class);
   

    // for now QoS is ignored
    /*protected void activate(String nodeName, InterfaceConfigurations interfaceConfigurations, L2vpn l2vpn,
            MountPointService mountService,java.util.Optional<PolicyManager> qosConfig)
            throws InterruptedException, ExecutionException {

        Optional<DataBroker> optional = MountPointHelper.getDataBroker(mountService, nodeName);
        if (!optional.isPresent()) {
            LOG.error("Could not retrieve MountPoint for {}", nodeName);
            return;
        }

        WriteTransaction transaction = optional.get().newWriteOnlyTransaction();
        transaction.merge(LogicalDatastoreType.CONFIGURATION, InterfaceHelper.getInterfaceConfigurationsId(),
            interfaceConfigurations);
        transaction.merge(LogicalDatastoreType.CONFIGURATION, L2vpnHelper.getL2vpnId(), l2vpn);
        transaction.commit().get();
    }*/
    
    protected void activate(ServicePort port, MountPointService mountService, 
    		Optional<PolicyManager> qosConfig, long mtu, boolean isExclusive, String outerName,
			String innerName, ServicePort neighbor, DataBroker dataBroker, ServiceType serviceType)
            throws InterruptedException, ExecutionException {

        Optional<DataBroker> optional = MountPointHelper.getDataBroker(mountService, port.getNode().getValue());
        if (!optional.isPresent()) {
            LOG.error("Could not retrieve MountPoint for {}", port.getNode().getValue());
            return;
        }

        // Enable L2Trasportation for port based service
        boolean setL2Transport = (isExclusive) ? true : false;
        WriteTransaction transaction = optional.get().newWriteOnlyTransaction();
        //transaction.merge(LogicalDatastoreType.CONFIGURATION, InterfaceHelper.getInterfaceConfigurationsId(),
         //   interfaceConfigurations);
        InterfaceHelper.setInterfaceConfiguration(port, mtu, setL2Transport, transaction);

        //transaction.merge(LogicalDatastoreType.CONFIGURATION, L2vpnHelper.getL2vpnId(), l2vpn);
        L2vpnHelper.setL2vpnConfiguration(port, outerName, innerName, neighbor, isExclusive, dataBroker, serviceType, transaction);
        
        transaction.commit().get();
    }

/*
    protected void activateSubInterface(String nodeName, InterfaceConfigurations interfaceConfigurations,
            MountPointService mountService) throws InterruptedException, ExecutionException {

        Optional<DataBroker> optional = MountPointHelper.getDataBroker(mountService, nodeName);
        if (!optional.isPresent()) {
            LOG.error("Could not retrieve MountPoint for {}", nodeName);
            return;
        }

        WriteTransaction transaction = optional.get().newWriteOnlyTransaction();
        transaction.merge(LogicalDatastoreType.CONFIGURATION,
                            InterfaceHelper.getInterfaceConfigurationsId(),
                            interfaceConfigurations);
        transaction.commit().get();
    }
    */

    protected void activateSubInterface(ServicePort port, MountPointService mountService, long mtu) throws InterruptedException, ExecutionException {

        Optional<DataBroker> optional = MountPointHelper.getDataBroker(mountService, port.getNode().getValue());
        if (!optional.isPresent()) {
            LOG.error("Could not retrieve MountPoint for {}", port.getNode().getValue());
            return;
        }

        WriteTransaction transaction = optional.get().newWriteOnlyTransaction();
/*        transaction.merge(LogicalDatastoreType.CONFIGURATION,
                            InterfaceHelper.getInterfaceConfigurationsId(),
                            interfaceConfigurations);
                            */
        InterfaceHelper.setSubInterfaceConfiguration(port, transaction, mtu);
        transaction.commit().get();
    }

 /*protected void deactivate(ServicePort port, InstanceIdentifier<?> ids,
            InstanceIdentifier<InterfaceConfiguration> interfaceConfigurationId, boolean isExclusive,
            EndPoint endpoint, MountPointService mountService, List<String> dvls, List<Uuid> inls)
            throws InterruptedException, ExecutionException {

        Optional<DataBroker> optional = MountPointHelper.getDataBroker(mountService, port.getNode().getValue());
        if (!optional.isPresent()) {
            LOG.error("Could not retrieve MountPoint for {}", port.getNode().getValue());
            return;
        }

        WriteTransaction transaction = optional.get().newWriteOnlyTransaction();

        if (ids.getTargetType().equals(
                org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109
                .l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnect.class)) {

            if (!ServicePort.isSameDevice(endpoint, dvls)) {
                transaction.delete(LogicalDatastoreType.CONFIGURATION, ids);
            }
            transaction.delete(LogicalDatastoreType.CONFIGURATION, interfaceConfigurationId);
        } else {

            if (!AbstractL2vpnBridgeDomainActivator.isSameInterface(endpoint, inls)) {
                if (!ServicePort.isSameDevice(endpoint, dvls)) {
                    transaction.delete(LogicalDatastoreType.CONFIGURATION, ids);
                }
                transaction.delete(LogicalDatastoreType.CONFIGURATION, interfaceConfigurationId);
            }
        }
        transaction.commit().get();
    }
*/
    
    protected void deactivateBridgeDomain(ServicePort port, String serviceId, boolean isExclusive,
            EndPoint endpoint, MountPointService mountService, List<String> dvls, List<Uuid> inls)
            throws InterruptedException, ExecutionException {
        Optional<DataBroker> optional = MountPointHelper.getDataBroker(mountService, port.getNode().getValue());
        if (!optional.isPresent()) {
            LOG.error("Could not retrieve MountPoint for {}", port.getNode().getValue());
            return;
        }
        WriteTransaction transaction = optional.get().newWriteOnlyTransaction();
        String innerOuterName = new FixedServiceNaming().replaceForbidenCharacters(serviceId);
        LOG.info("********************************IN Deactivate Bridge Domain******************************3"+innerOuterName);
        DeactivateHelper.deativateBridgeDomainConfiguration(port,isExclusive,innerOuterName,endpoint,dvls,inls,transaction); 
        transaction.commit().get();
        LOG.info("*******************************After Running Transectctioins Commit *****************************");
    }
   
      
    protected void deactivateXConnect(ServicePort port, String serviceId, boolean isExclusive,
            EndPoint endpoint, MountPointService mountService, List<String> dvls, List<Uuid> inls)
            throws InterruptedException, ExecutionException {
        Optional<DataBroker> optional = MountPointHelper.getDataBroker(mountService, port.getNode().getValue());
        if (!optional.isPresent()) {
            LOG.error("Could not retrieve MountPoint for {}", port.getNode().getValue());
            return;
        }
        WriteTransaction transaction = optional.get().newWriteOnlyTransaction();
        String innerOuterName = new FixedServiceNaming().replaceForbidenCharacters(serviceId);
      //  DeactivateHelper.deativateBridgeDomainConfiguration(port,interfaceConfigurationId,innerOuterName,endpoint,dvls,inls,transaction);
        DeactivateHelper.deativateXconnectConfiguration(port,isExclusive,innerOuterName,endpoint,dvls,transaction);
        transaction.commit().get();
        LOG.info("*******************************After Running Transectctioins Commit *****************************");
    }                                                                                                                              
}
