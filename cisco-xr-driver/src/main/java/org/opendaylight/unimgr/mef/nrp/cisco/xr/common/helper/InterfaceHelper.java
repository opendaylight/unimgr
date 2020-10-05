/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.five.InterfaceRev170907Helper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.five.L2vpnRev170626Helper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.one.InterfaceRev150730Helper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.one.L2vpnRev151109Helper;
import org.opendaylight.unimgr.utils.NetconfConstants;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurationsBuilder;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceModeEnum;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationBuilder;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations._interface.configuration.Mtus;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109.InterfaceConfiguration2;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109.InterfaceConfiguration2Builder;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109._interface.configurations._interface.configuration.EthernetServiceBuilder;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109._interface.configurations._interface.configuration.ethernet.service.Encapsulation;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109._interface.configurations._interface.configuration.ethernet.service.EncapsulationBuilder;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.datatypes.rev151109.Match;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.datatypes.rev151109.VlanTagOrAny;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.InterfaceConfiguration3;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.InterfaceConfiguration3Builder;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109._interface.configurations._interface.configuration.L2Transport;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109._interface.configurations._interface.configuration.L2TransportBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.LayerProtocolName;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortDirection;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortRole;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.Uint32;

import com.google.common.cache.LoadingCache;

/*
 * Helper, designated to support interface configuration
 *
 * @author krzysztof.bijakowski@amartus.com
 */
public class InterfaceHelper {
    private List<InterfaceConfiguration> configurations;
    private static Map<NodeId, String> nodeXRVersionMap = new HashMap<NodeId, String>();
    private static String gbPortPattern = ".*(GigabitEthernet|TenGigE)[^-]+$";
    private static String subInterfaceMTUOwner = "sub_vlan";

    public static InterfaceName getInterfaceName(ServicePort port) {
        String interfaceName = port.getTp().getValue();

        if (interfaceName.contains(":")) {
            interfaceName = interfaceName.split(":")[1];
        }

        return new InterfaceName(interfaceName);
    }
    
    public static String getGbPortPattern() {
        return gbPortPattern;
    }

    public static String getSubInterfaceMTUOwner() {
        return subInterfaceMTUOwner;
    }
    
    public static Map<NodeId, String> getNodeXRVersionMap() {
        return nodeXRVersionMap;
    }

    public static InterfaceName getSubInterfaceName(ServicePort port) {
        String interfaceName = port.getTp().getValue();

        if (interfaceName.contains(":")) {
            interfaceName = interfaceName.split(":")[1];
        }
        // adding vlan id with interface name
        interfaceName = interfaceName + "." + port.getVlanId();
        return new InterfaceName(interfaceName);
    }


//    public static InstanceIdentifier<InterfaceConfigurations> getInterfaceConfigurationsId() {
//        return InstanceIdentifier.builder(InterfaceConfigurations.class).build();
//    }

    public InterfaceHelper() {
        configurations = new LinkedList<>();
    }

//    public InterfaceHelper addInterface(ServicePort port, Optional<Mtus> mtus, boolean setL2Transport) {
//        return addInterface(getInterfaceName(port), mtus, setL2Transport);
//    }

//    public InterfaceHelper addInterface(InterfaceName name, Optional<Mtus> mtus, boolean setL2Transport) {
//        InterfaceConfigurationBuilder configurationBuilder = new InterfaceConfigurationBuilder();
//
//        configurationBuilder
//            .setInterfaceName(name)
//            .setActive(new InterfaceActive("act"));
//
//        if (mtus.isPresent()) {
//            configurationBuilder.setMtus(mtus.get());
//        }
//
//        if (setL2Transport) {
//            setL2Configuration(configurationBuilder);
//        }
//
//        configurations.add(configurationBuilder.build());
//        return this;
//    }
//
//    public InterfaceHelper addSubInterface(ServicePort port, Optional<Mtus> mtus) {
//        return addSubInterface(getSubInterfaceName(port), mtus, port);
//    }

//    public InterfaceHelper addSubInterface(InterfaceName name, Optional<Mtus> mtus, ServicePort port) {
//        InterfaceConfigurationBuilder configurationBuilder = new InterfaceConfigurationBuilder();
//
//        configurationBuilder
//            .setInterfaceName(name)
//            .setActive(new InterfaceActive("act"))
//            //.setShutdown(Boolean.FALSE)
//            .setDescription("Create sub interface through ODL")
//            .setInterfaceModeNonPhysical(InterfaceModeEnum.L2Transport);
//            // set ethernet service
//            setEthernetService(configurationBuilder, port);
//
//        if (mtus.isPresent()) {
//            configurationBuilder.setMtus(mtus.get());
//        }
//        configurations.add(configurationBuilder.build());
//
//        return this;
//    }

//    private void setEthernetService(InterfaceConfigurationBuilder configurationBuilder, ServicePort port) {
//        Encapsulation encapsulation = new EncapsulationBuilder()
//            .setOuterRange1Low(new VlanTagOrAny(Uint32.valueOf(port.getVlanId())))
//            .setOuterTagType(Match.MatchDot1q)
//            .build();
//
//        InterfaceConfiguration2 augmentation = new InterfaceConfiguration2Builder()
//                .setEthernetService(new EthernetServiceBuilder()
//                    .setEncapsulation(encapsulation)
//                    .build()
//                )
//                .build();
//
//        configurationBuilder.addAugmentation(InterfaceConfiguration2.class, augmentation);
//    }

    public InterfaceConfigurations build() {
        return new InterfaceConfigurationsBuilder()
            .setInterfaceConfiguration(configurations)
            .build();
    }

//    private void setL2Configuration(InterfaceConfigurationBuilder configurationBuilder) {
//        L2Transport l2transport = new L2TransportBuilder()
//                    .setEnabled(Empty.getInstance())
//                    .build();
//
//        InterfaceConfiguration3 augmentation = new InterfaceConfiguration3Builder()
//            .setL2Transport(l2transport)
//            .build();
//
//        configurationBuilder.addAugmentation(InterfaceConfiguration3.class, augmentation);
//    }

    public static List<OwnedNodeEdgePoint> toTp(Collection<Node> nodes, MountPointService mountService,
            LoadingCache<NodeKey, KeyedInstanceIdentifier<Node, NodeKey>> mountIds) {
        return nodes.stream().flatMap(cn -> {
            final NodeKey key = cn.key();
            try {
                KeyedInstanceIdentifier<Node, NodeKey> id = mountIds.get(key);
                Optional<MountPoint> mountPoint = mountService.getMountPoint(id);
                if (mountPoint.isPresent()) {
                      NetconfNode netconf = cn.augmentation(NetconfNode.class);
                      DataBroker deviceBroker = mountPoint.get().getService(DataBroker.class).get();
                      ReadTransaction tx = deviceBroker.newReadOnlyTransaction();
                      try {
                            if(InterfaceRev150730Helper.ports(tx).count() > 0 
                                && L2vpnRev151109Helper.checkL2vpnCapability(netconf)) {
                                nodeXRVersionMap.put(key.getNodeId(), NetconfConstants.XR_VERSION_SIX_ONE);
                                return InterfaceRev150730Helper.toTp(nodes, mountService, mountIds).stream();
                              }
                          }
                      catch (Exception e) {
                            // TODO Auto-generated catch block
                         }
                      try {
                            if (InterfaceRev170907Helper.ports(tx).count() > 0 
                                && L2vpnRev170626Helper.checkL2vpnCapability(netconf)) { 
                            	nodeXRVersionMap.put(key.getNodeId(), NetconfConstants.XR_VERSION_SIX_FIVE);
							    return InterfaceRev170907Helper.toTp(nodes, mountService, mountIds).stream();
							 }
                         }
                      catch (Exception e) {
                            // TODO Auto-generated catch block
                         }

                } else {
                }

            } catch (ExecutionException  e) {
            }
            return Stream.empty();
        }).collect(Collectors.toList());
    }

    public static void setInterfaceConfiguration(ServicePort port, long mtu, boolean setL2Transport, WriteTransaction transaction) {
        if (nodeXRVersionMap.containsKey(port.getNode())) {
            String XRVersion = nodeXRVersionMap.get(port.getNode());

            if(XRVersion.equals(NetconfConstants.XR_VERSION_SIX_ONE)) { 
        		transaction.merge(LogicalDatastoreType.CONFIGURATION, InterfaceRev150730Helper.getInterfaceConfigurationsId(),
	    				InterfaceRev150730Helper.addInterface(port, mtu, setL2Transport));
    		}
            if(XRVersion.equals(NetconfConstants.XR_VERSION_SIX_FIVE)) {
	    		transaction.merge(LogicalDatastoreType.CONFIGURATION, InterfaceRev170907Helper.getInterfaceConfigurationsId(),
	    				InterfaceRev170907Helper.addInterface(port, mtu, setL2Transport));
    		}
        }
    }

    public static void setSubInterfaceConfiguration(ServicePort port, WriteTransaction transaction, long mtu) {
        if (nodeXRVersionMap.containsKey(port.getNode())) {
            String XRVersion = nodeXRVersionMap.get(port.getNode());

            if(XRVersion.equals(NetconfConstants.XR_VERSION_SIX_ONE)) { 
    			transaction.merge(LogicalDatastoreType.CONFIGURATION, InterfaceRev150730Helper.getInterfaceConfigurationsId(),
    				InterfaceRev150730Helper.addSubInterface(port, mtu));
    		}
            if(XRVersion.equals(NetconfConstants.XR_VERSION_SIX_FIVE)) { 
    			transaction.merge(LogicalDatastoreType.CONFIGURATION, InterfaceRev170907Helper.getInterfaceConfigurationsId(),
    				InterfaceRev170907Helper.addSubInterface(port, mtu));
    		}
        }
    }

}
