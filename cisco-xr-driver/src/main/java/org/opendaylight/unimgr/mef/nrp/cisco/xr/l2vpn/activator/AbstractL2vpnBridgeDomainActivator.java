/*
 * Copyright (c) 2019 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator;

import static org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort.toServicePort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivator;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivatorException;
import org.opendaylight.unimgr.utils.NetconfConstants;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev161215.PolicyManager;
/*import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev161215.PolicyManager;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.Database;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.BridgeDomainGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.BridgeDomainGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.BridgeDomainGroupKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.BridgeDomains;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.BdPseudowires;*/
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.BridgeDomainKey;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.bridge.domain.groups.BridgeDomainGroupKey;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.bridge.domain.table.BridgeDomains;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.BridgeDomain;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.BridgeDomainKey;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.BridgeDomainGroups;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.bridge.domain.groups.BridgeDomainGroup;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.bridge.domain.groups.BridgeDomainGroupKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.connectivity.service.end.point.attrs.NrpCarrierEthConnectivityEndPointResource;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ServiceType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * @author arif.hussain@xoriant.com
 */
public abstract class AbstractL2vpnBridgeDomainActivator implements ResourceActivator {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractL2vpnBridgeDomainActivator.class);
    private static final long MTU = 1500;
    private static List<Uuid> inls = new ArrayList<Uuid>();
    private static List<String> dvls = new ArrayList<String>();
    protected DataBroker dataBroker;
    protected MountPointService mountService;
    private static Map<String, String> nodeInterfaceIdentifierList = new HashMap<String, String>();
    protected AbstractL2vpnBridgeDomainActivator(DataBroker dataBroker,
            MountPointService mountService) {
        LOG.info(" L2vpn bridge domain activator initiated...");

        this.dataBroker = dataBroker;
        this.mountService = mountService;
        inls.clear();
        dvls.clear();
    }

    @Override
    public void activate(List<EndPoint> endPoints, String serviceId, boolean isExclusive, ServiceType serviceType)
            throws ResourceActivatorException, InterruptedException, ExecutionException {
        String innerOuterName = getInnerName(serviceId);
        ServicePort port = null;
        ServicePort neighbor = null;
        String portRole = null;
        String neighborRole = null;

        for (EndPoint endPoint : endPoints) {
            if (port == null) {
                port = toServicePort(endPoint, NetconfConstants.NETCONF_TOPOLODY_NAME);
                NrpCarrierEthConnectivityEndPointResource attrs = endPoint.getAttrs() == null ? null
                        : endPoint.getAttrs().getNrpCarrierEthConnectivityEndPointResource();
                if (attrs != null) {
                    port.setEgressBwpFlow(attrs.getEgressBwpFlow());
                    port.setIngressBwpFlow(attrs.getIngressBwpFlow());
                }
                portRole = endPoint.getEndpoint().getRole().name();
            } else {
                neighbor = toServicePort(endPoint, NetconfConstants.NETCONF_TOPOLODY_NAME);
                neighborRole = endPoint.getEndpoint().getRole().name();
            }
        }

        java.util.Optional<PolicyManager> qosConfig = activateQos(innerOuterName, port);
        //InterfaceConfigurations interfaceConfigurations = activateInterface(port, neighbor, MTU, isExclusive);
        /*BdPseudowires bdPseudowires = activateBdPseudowire(neighbor);
        BridgeDomainGroups bridgeDomainGroups = activateBridgeDomain(innerOuterName,
                                                                    innerOuterName,
                                                                    port,
                                                                    neighbor,
                                                                    bdPseudowires,
                                                                    isExclusive);
        L2vpn l2vpn = activateL2Vpn(bridgeDomainGroups);
        */

        // create sub interface for tag based service
        if (!isExclusive) {
            //InterfaceConfigurations subInterfaceConfigurations = createSubInterface(port, neighbor, MTU);
            //createSubInterface(port.getNode().getValue(), subInterfaceConfigurations, mountService);
        	createSubInterface(port, mountService, MTU);
        }

        if (serviceType != null && serviceType.getName().equals(ServiceType.ROOTEDMULTIPOINTCONNECTIVITY.getName())) {
            if (!(portRole != null && portRole.equals(PortRole.LEAF.getName())
                    && neighborRole != null && neighborRole.equals(PortRole.LEAF.getName()))) {
                //doActivate(port.getNode().getValue(), interfaceConfigurations, l2vpn, mountService, qosConfig);
                doActivate(port, mountService, qosConfig, MTU, isExclusive, innerOuterName, innerOuterName, neighbor, serviceType);
            }
        } else {
           //doActivate(port.getNode().getValue(), interfaceConfigurations, l2vpn, mountService, qosConfig);
            doActivate(port, mountService, qosConfig, MTU, isExclusive, innerOuterName, innerOuterName, neighbor, serviceType);
        }
    }

  /*  @Override
    public void deactivate(List<EndPoint> endPoints, String serviceId, boolean isExclusive, ServiceType serviceType)
            throws ResourceActivatorException, InterruptedException, ExecutionException {
        String innerOuterName = getInnerName(serviceId);
        ServicePort port = toServicePort(endPoints.stream().findFirst().get(), NetconfConstants.NETCONF_TOPOLODY_NAME);

              
        InstanceIdentifier<BridgeDomain> bridgeDomainId = deactivateBridgeDomain(innerOuterName, innerOuterName);
        InstanceIdentifier<InterfaceConfiguration> interfaceConfigurationId = deactivateInterface(port, isExclusive);
        doDeactivate(port,
                    bridgeDomainId,
                    interfaceConfigurationId,
                    isExclusive,
                    endPoints.stream().findFirst().get(),
                    dvls,
                    inls);
    }*/

   /* @Override
    public void deactivate(List<EndPoint> endPoints, String serviceId, boolean isExclusive, ServiceType serviceType)
            throws ResourceActivatorException, InterruptedException, ExecutionException {
        String innerOuterName = getInnerName(serviceId);
        ServicePort port = toServicePort(endPoints.stream().findFirst().get(), NetconfConstants.NETCONF_TOPOLODY_NAME);
        if (nodeInterfaceIdentifierList.containsKey(port.getNode().getValue())) {
            String identifierPath = nodeInterfaceIdentifierList.get(port.getNode().getValue());

      if(identifierPath.contains(InterfaceRev150730Helper.getInterfaceConfigurationsId().getTargetType().getPackageName())) { 
              
        InstanceIdentifier<BridgeDomain> bridgeDomainId = deactivateBridgeDomain(innerOuterName, innerOuterName);
        InstanceIdentifier<InterfaceConfiguration> interfaceConfigurationId = deactivateInterface(port, isExclusive);
        doDeactivate(port,
                    bridgeDomainId,
                    interfaceConfigurationId,
                    isExclusive,
                    endPoints.stream().findFirst().get(),
                    dvls,
                    inls);
    }else {
    	
    	InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.BridgeDomain> bridgeDomainId = deactivateBridgeDomainNew(innerOuterName, innerOuterName);
        InstanceIdentifier<InterfaceConfiguration> interfaceConfigurationId = deactivateInterface(port, isExclusive);
        doDeactivateNew(port,
                    bridgeDomainId,
                    interfaceConfigurationId,
                    isExclusive,
                    endPoints.stream().findFirst().get(),
                    dvls,
                    inls);
    	
    	
    	}
    }
        }
        */
    
    @Override
    public void deactivate(List<EndPoint> endPoints, String serviceId, boolean isExclusive, ServiceType serviceType)
            throws ResourceActivatorException, InterruptedException, ExecutionException {
        String innerOuterName = getInnerName(serviceId);
        ServicePort port = toServicePort(endPoints.stream().findFirst().get(), NetconfConstants.NETCONF_TOPOLODY_NAME);

              
       // InstanceIdentifier<BridgeDomain> bridgeDomainId = deactivateBridgeDomain(innerOuterName, innerOuterName);
      //  InstanceIdentifier<InterfaceConfiguration> interfaceConfigurationId = deactivateInterface(port, isExclusive);
        doDeactivate(port,
        		    serviceId,
                    isExclusive,
                    endPoints.stream().findFirst().get(),
                    dvls,
                    inls);
    }
    
    /*
     * Function is checking bridge domain configuration already deleted from XR-device.
     * @param endPoint
     * @param ls
     * @return boolean
     */
    public static boolean isSameInterface(EndPoint endPoint, List<Uuid> ls) {
        Uuid sip = endPoint.getEndpoint().getServiceInterfacePoint().getServiceInterfacePointId();

        if (ls.size() == 0) {
            ls.add(sip);
        } else if (ls.size() > 0) {
            List<Uuid> listWithoutDuplicates = ls.stream().distinct().collect(Collectors.toList());

            java.util.Optional<Uuid> preset =
                    listWithoutDuplicates.stream().filter(x -> x.equals(sip)).findFirst();

            if (preset.isPresent()) {
                return true;
            }
            ls.add(sip);
        }

        return false;
    }

    protected abstract java.util.Optional<PolicyManager> activateQos(String name, ServicePort port);

    protected abstract String getInnerName(String serviceId);

    protected abstract String getOuterName(String serviceId);

//    protected abstract InterfaceConfigurations activateInterface(
//                                                             ServicePort portA,
//                                                             ServicePort portZ,
//                                                             long mtu,
//                                                             boolean isExclusive);

    /*protected abstract void createSubInterface(
                                             String value,
                                             InterfaceConfigurations subInterfaceConfigurations,
                                             MountPointService mountService2)
            throws ResourceActivatorException, InterruptedException, ExecutionException;

    protected abstract InterfaceConfigurations createSubInterface(ServicePort portA, ServicePort portZ, long mtu);
*/

    protected abstract void createSubInterface(ServicePort port, MountPointService mountService2, long mtu)
			throws InterruptedException, ExecutionException;
    /*
    protected abstract BdPseudowires activateBdPseudowire(ServicePort neighbor);

    protected abstract BridgeDomainGroups activateBridgeDomain(
                                                            String outerName,
                                                            String innerName,
                                                            ServicePort port,
                                                            ServicePort neighbor,
                                                            BdPseudowires bdPseudowires,
                                                            boolean isExclusive);

    protected abstract L2vpn activateL2Vpn(BridgeDomainGroups bridgeDomainGroups);
*/

    /*protected abstract void doActivate(
                                    String node,
                                    InterfaceConfigurations interfaceConfigurations,
                                    L2vpn l2vpn,
                                    MountPointService mountService2,
                                    java.util.Optional<PolicyManager> qosConfig)
                                    throws ResourceActivatorException, InterruptedException, ExecutionException;
    */
    
    protected abstract void doActivate(ServicePort port, MountPointService mountService2,
			Optional<PolicyManager> qosConfig, long mtu, boolean isExclusive,
			String outerName, String innerName, ServicePort neighbor, ServiceType serviceType)
            throws ResourceActivatorException, InterruptedException, ExecutionException;

    protected abstract InstanceIdentifier<InterfaceConfiguration> deactivateInterface(
                                                                                  ServicePort port,
                                                                                  boolean isExclusive);

   /* private InstanceIdentifier<BridgeDomain> deactivateBridgeDomain(String outerName, String innerName) {

        return InstanceIdentifier.builder(L2vpn.class).child(Database.class)
                .child(BridgeDomainGroups.class)
                .child(BridgeDomainGroup.class, new BridgeDomainGroupKey(new CiscoIosXrString(outerName)))
                .child(BridgeDomains.class)
                .child(BridgeDomain.class, new BridgeDomainKey(new CiscoIosXrString(innerName)))
                .build();
    }
    */
   /* private InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.BridgeDomain> deactivateBridgeDomainNew(String outerName, String innerName) {

        return InstanceIdentifier.builder(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.L2vpn.class).child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.Database.class)
                .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.BridgeDomainGroups.class)
                .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.bridge.domain.groups.BridgeDomainGroup.class,
                new org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.bridge.domain.groups.BridgeDomainGroupKey(new CiscoIosXrString(outerName)))
                .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.bridge.domain.table.BridgeDomains.class)
                .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.BridgeDomain.class, 
                new org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.BridgeDomainKey(new CiscoIosXrString(innerName)))
                .build();
    }
*/
    /*protected abstract void doDeactivate(
                                       ServicePort port,
                                       InstanceIdentifier<BridgeDomain> bridgeDomainId,
                                       InstanceIdentifier<InterfaceConfiguration> interfaceConfigurationId,
                                       boolean isExclusive,
                                       EndPoint endPoint,
                                       List<String> dvls2,
                                       List<Uuid> inls2)
                                       throws ResourceActivatorException, InterruptedException, ExecutionException;
*/
    protected abstract void doDeactivate(
            ServicePort port,
            String serviceId,
            boolean isExclusive,
            EndPoint endPoint,
            List<String> dvls2,
            List<Uuid> inls2)
            throws ResourceActivatorException, InterruptedException, ExecutionException;
    
/*protected abstract void doDeactivateNew(
        ServicePort port,
        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.BridgeDomain>  bridgeDomainId,
        InstanceIdentifier<InterfaceConfiguration> interfaceConfigurationId,
        boolean isExclusive,
        EndPoint endPoint,
        List<String> dvls2,
        List<Uuid> inls2)
        throws ResourceActivatorException, InterruptedException, ExecutionException;
        */

}
