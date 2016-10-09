/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.globals.IfmConstants;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.VpnInstances;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.VpnInterfaces;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.instances.VpnInstance;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.instances.VpnInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.instances.VpnInstanceKey;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.interfaces.VpnInterface;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.interfaces.VpnInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.interfaces.VpnInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.etree.rev160614.EtreeInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.etree.rev160614.EtreeInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.etree.rev160614.EtreeInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.etree.rev160614.EtreeInterface.EtreeInterfaceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.etree.rev160614.EtreeInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.ElanInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.ElanInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.instances.ElanInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.instances.ElanInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.instances.ElanInstanceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.interfaces.ElanInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.interfaces.ElanInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.interfaces.ElanInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.Adjacencies;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class NetvirtUtils {
    private static final Logger logger = LoggerFactory.getLogger(MdsalUtils.class);
    
    public final static String VLAN_SEPARATOR = ".";

    public static void createElanInstance(DataBroker dataBroker, String instanceName, boolean isEtree) {
        ElanInstanceBuilder einstBuilder = createElanInstance(instanceName);

        if (isEtree) {
            EtreeInstance etreeInstance = new EtreeInstanceBuilder().build();
            einstBuilder.addAugmentation(EtreeInstance.class, etreeInstance).build();
        }

        MdsalUtils.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION,
                getElanInstanceInstanceIdentifier(instanceName), einstBuilder.build());
    }

    public static void createVpnInstance(DataBroker dataBroker, String instanceName) {
        VpnInstanceBuilder builder = new VpnInstanceBuilder();
        builder.setVpnInstanceName(instanceName);

        MdsalUtils.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION,
                getVpnInstanceInstanceIdentifier(instanceName), builder.build());
    }

    public static void createVpnInterface(DataBroker dataBroker, String instanceName, String interfaceName,
            Adjacencies adjancencies) {
        VpnInterfaceBuilder einterfaceBuilder = createVpnInterface(instanceName, interfaceName, adjancencies);

        MdsalUtils.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION,
                getVpnInterfaceInstanceIdentifier(interfaceName), einterfaceBuilder.build());
    }

    private static VpnInterfaceBuilder createVpnInterface(String instanceName, String interfaceName,
            Adjacencies adjacencies) {
        VpnInterfaceBuilder einterfaceBuilder = new VpnInterfaceBuilder();
        einterfaceBuilder.setVpnInstanceName(instanceName);
        einterfaceBuilder.setName(interfaceName);
        einterfaceBuilder.addAugmentation(Adjacencies.class, adjacencies);
        return einterfaceBuilder;
    }

    public static void createElanInterface(DataBroker dataBroker, String instanceName, String interfaceName) {
        ElanInterfaceBuilder einterfaceBuilder = createElanInterface(instanceName, interfaceName);

        MdsalUtils.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION,
                getElanInterfaceInstanceIdentifier(interfaceName), einterfaceBuilder.build());
    }

    public static void createEtreeInterface(DataBroker dataBroker, String instanceName, String interfaceName,
            EtreeInterfaceType type) {
        ElanInterfaceBuilder einterfaceBuilder = createEtreeInterface(instanceName, interfaceName, type);

        MdsalUtils.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION,
                getElanInterfaceInstanceIdentifier(interfaceName), einterfaceBuilder.build());
    }

    public static void updateElanInstance(DataBroker dataBroker, String instanceName) {

        ElanInstanceBuilder einstBuilder = createElanInstance(instanceName);

        MdsalUtils.syncUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION,
                getElanInstanceInstanceIdentifier(instanceName), einstBuilder.build());
    }

    public static void updateElanInterface(DataBroker dataBroker, String instanceName, String interfaceName) {
        ElanInterfaceBuilder einterfaceBuilder = createElanInterface(instanceName, interfaceName);

        MdsalUtils.syncUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION,
                getElanInterfaceInstanceIdentifier(interfaceName), einterfaceBuilder.build());
    }

    public static void deleteElanInstance(DataBroker dataBroker, String instanceName) {
        MdsalUtils.syncDelete(dataBroker, LogicalDatastoreType.CONFIGURATION,
                getElanInstanceInstanceIdentifier(instanceName));
    }

    public static void deleteElanInterface(DataBroker dataBroker, String instanceName, String interfaceName) {
        MdsalUtils.syncDelete(dataBroker, LogicalDatastoreType.CONFIGURATION,
                getElanInterfaceInstanceIdentifier(interfaceName));
    }

    public static Interface createTrunkInterface(String interfaceName, String parentIfaceName) {
        IfL2vlanBuilder ifL2vlanBuilder = new IfL2vlanBuilder();
        ifL2vlanBuilder.setL2vlanMode(IfL2vlan.L2vlanMode.Trunk);
        return createInterface(interfaceName, parentIfaceName, ifL2vlanBuilder.build());
    }

    public static Interface createTrunkMemberInterface(String interfaceName, String parentIfaceName, int vlanId) {
        IfL2vlanBuilder ifL2vlanBuilder = new IfL2vlanBuilder();
        ifL2vlanBuilder.setL2vlanMode(IfL2vlan.L2vlanMode.TrunkMember).setVlanId(new VlanId(vlanId));
        return createInterface(interfaceName, parentIfaceName, ifL2vlanBuilder.build());
    }

    private static Interface createInterface(String interfaceName, String parentIfaceName, IfL2vlan ifL2vlan) {
        InterfaceBuilder interfaceBuilder = new InterfaceBuilder();
        ParentRefsBuilder parentRefsBuilder = new ParentRefsBuilder().setParentInterface(parentIfaceName);
        interfaceBuilder.setEnabled(true).setName(interfaceName).setType(L2vlan.class)
                .addAugmentation(IfL2vlan.class, ifL2vlan).addAugmentation(ParentRefs.class, parentRefsBuilder.build());
        return interfaceBuilder.build();
    }

    public static String getInterfaceNameForVlan(String uniId, String vlanId) {
        return uniId + VLAN_SEPARATOR + vlanId;
    }

    private static ElanInstanceBuilder createElanInstance(String instanceName) {
        ElanInstanceBuilder einstBuilder = new ElanInstanceBuilder();
        einstBuilder.setElanInstanceName(instanceName);
        einstBuilder.setKey(new ElanInstanceKey(instanceName));
        return einstBuilder;
    }

    private static ElanInterfaceBuilder createElanInterface(String instanceName, String interfaceName) {
        ElanInterfaceBuilder einterfaceBuilder = new ElanInterfaceBuilder();
        einterfaceBuilder.setElanInstanceName(instanceName);
        einterfaceBuilder.setName(interfaceName);
        return einterfaceBuilder;
    }

    private static ElanInterfaceBuilder createEtreeInterface(String instanceName, String interfaceName,
            EtreeInterfaceType interfaceType) {
        ElanInterfaceBuilder einterfaceBuilder = new ElanInterfaceBuilder();
        einterfaceBuilder.setElanInstanceName(instanceName);
        einterfaceBuilder.setName(interfaceName);
        EtreeInterface etreeInterface = new EtreeInterfaceBuilder().setEtreeInterfaceType(interfaceType).build();
        einterfaceBuilder.addAugmentation(EtreeInterface.class, etreeInterface);
        return einterfaceBuilder;
    }

    public static InstanceIdentifier<ElanInstance> getElanInstanceInstanceIdentifier() {
        return InstanceIdentifier.builder(ElanInstances.class).child(ElanInstance.class).build();
    }

    public static InstanceIdentifier<ElanInterfaces> getElanInterfacesInstanceIdentifier() {
        return InstanceIdentifier.builder(ElanInterfaces.class).build();
    }

    private static InstanceIdentifier<ElanInstance> getElanInstanceInstanceIdentifier(String instanceName) {
        return InstanceIdentifier.builder(ElanInstances.class)
                .child(ElanInstance.class, new ElanInstanceKey(instanceName)).build();
    }

    private static InstanceIdentifier<ElanInterface> getElanInterfaceInstanceIdentifier(String interfaceName) {
        return InstanceIdentifier.builder(ElanInterfaces.class)
                .child(ElanInterface.class, new ElanInterfaceKey(interfaceName)).build();
    }

    public static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> getStateInterfaceIdentifier(
            String interfaceName) {
        InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> idBuilder = InstanceIdentifier
                .builder(InterfacesState.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.class,
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey(
                                interfaceName));
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> id = idBuilder
                .build();
        return id;
    }

    public static InstanceIdentifier<Interface> getInterfaceIdentifier(String interfaceName) {
        InstanceIdentifierBuilder<Interface> idBuilder = InstanceIdentifier.builder(Interfaces.class)
                .child(Interface.class, new InterfaceKey(interfaceName));
        InstanceIdentifier<Interface> id = idBuilder.build();
        return id;
    }

    public static Optional<Interface> getIetfInterface(DataBroker dataBroker, String interfaceName) {
        return MdsalUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION, getInterfaceIdentifier(interfaceName));
    }

    public static String getDeviceInterfaceName(String deviceName, String interfaceName) {
        return deviceName + IfmConstants.OF_URI_SEPARATOR + interfaceName;
    }

    public static void writeInterface(Interface iface, WriteTransaction tx) {
        String interfaceName = iface.getName();
        InstanceIdentifier<Interface> interfaceIdentifier = createInterfaceIdentifier(interfaceName);
        tx.put(LogicalDatastoreType.CONFIGURATION, interfaceIdentifier, iface, true);
    }

    private static InstanceIdentifier<VpnInstance> getVpnInstanceInstanceIdentifier(String instanceName) {
        return InstanceIdentifier.builder(VpnInstances.class).child(VpnInstance.class, new VpnInstanceKey(instanceName))
                .build();
    }

    private static InstanceIdentifier<VpnInterface> getVpnInterfaceInstanceIdentifier(String interfaceName) {
        return InstanceIdentifier.builder(VpnInterfaces.class)
                .child(VpnInterface.class, new VpnInterfaceKey(interfaceName)).build();
    }
    
    public static void write(Interface iface, WriteTransaction tx) {
        String interfaceName = iface.getName();
        InstanceIdentifier<Interface> interfaceIdentifier = createInterfaceIdentifier(interfaceName);
        tx.put(LogicalDatastoreType.CONFIGURATION, interfaceIdentifier, iface, true);
    }

    public static void delete(String interfaceName, WriteTransaction tx) {
        InstanceIdentifier<Interface> interfaceIdentifier = createInterfaceIdentifier(interfaceName);
        tx.delete(LogicalDatastoreType.CONFIGURATION, interfaceIdentifier);
    }
    
    public static boolean waitForGeniusToUpdateInterface(DataBroker dataBroker, String interfaceName) {
        int retries = 10;

        while (retries > 0) {
            Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> optional = MdsalUtils.read(dataBroker, LogicalDatastoreType.OPERATIONAL,
                    getStateInterfaceIdentifier(interfaceName));

            if (!optional.isPresent()) {
                logger.info("State interface {} doesn't exist", interfaceName);
            } else {
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface stateInterface = optional.get();

                if (stateInterface.getIfIndex() != null) {
                    logger.info("State interface configured with ifIndex {}", stateInterface.getIfIndex());

                    // Wait a bit, because if we continue too soon this will not
                    // work.
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }

                    return true;
                }
            }

            retries -= 1;
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
            }
        }

        return false;
    }
    
    private static InstanceIdentifier<Interface> createInterfaceIdentifier(String interfaceName) {
        return InstanceIdentifier.builder(Interfaces.class).child(Interface.class, new InterfaceKey(interfaceName))
                .build();
    }

    public static void createInterface(DataBroker dataBroker, String instanceName, String interfaceName, EtreeInterfaceType etreeInterfaceType, boolean isEtree) {
        boolean result = NetvirtUtils.waitForGeniusToUpdateInterface(dataBroker, interfaceName);
        if (!result) {
            logger.error("State interface {} is not configured (missing ifIndex)", interfaceName);
            return;
        }

        logger.info("Adding {} interface: {}", isEtree ? "etree" : "elan", interfaceName);

        if (isEtree) {
            NetvirtUtils.createEtreeInterface(dataBroker, instanceName, interfaceName,
                    etreeInterfaceType);
        } else {
            NetvirtUtils.createElanInterface(dataBroker, instanceName, interfaceName);
        }        
    }
}
