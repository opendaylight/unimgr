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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.SplitHorizon;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.SplitHorizonBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.SegmentTypeVxlan;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class NetvirtUtils {

    private static final Logger logger = LoggerFactory.getLogger(NetvirtUtils.class);
    private static final long DEFAULT_MAC_TIMEOUT = 300;

    public static void createElanInstance(DataBroker dataBroker, String instanceName, boolean isEtree, Long segmentationId) {
        ElanInstanceBuilder einstBuilder = createElanInstance(instanceName, segmentationId);

        if (isEtree) {
            EtreeInstance etreeInstance = new EtreeInstanceBuilder().build();
            einstBuilder.addAugmentation(EtreeInstance.class, etreeInstance).build();
        }

        MdsalUtils.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION,
                getElanInstanceInstanceIdentifier(instanceName), einstBuilder.build());
    }

    public static void updateElanInstance(DataBroker dataBroker, String instanceName) {
        WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);
        updateElanInstance(instanceName, tx);
        MdsalUtils.commitTransaction(tx);
    }

    public static void updateElanInstance(String instanceName, WriteTransaction tx) {
        ElanInstanceBuilder einstBuilder = createElanInstance(instanceName);

        tx.merge(LogicalDatastoreType.CONFIGURATION, getElanInstanceInstanceIdentifier(instanceName),
                einstBuilder.build());
    }

    public static void updateElanInstance(String instanceName, WriteTransaction tx, Long segmentationId) {
        ElanInstanceBuilder einstBuilder = createElanInstance(instanceName);

        tx.merge(LogicalDatastoreType.CONFIGURATION, getElanInstanceInstanceIdentifier(instanceName),
                einstBuilder.build());
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

    public static void deleteElanInstance(String instanceName, WriteTransaction tx) {
        tx.delete(LogicalDatastoreType.CONFIGURATION, getElanInstanceInstanceIdentifier(instanceName));
    }

    public static void deleteElanInterface(DataBroker dataBroker, String interfaceName) {
        WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);
        deleteElanInterface(interfaceName, tx);
        MdsalUtils.commitTransaction(tx);
    }

    public static void deleteElanInterface(String interfaceName, WriteTransaction tx) {
        tx.delete(LogicalDatastoreType.CONFIGURATION, getElanInterfaceInstanceIdentifier(interfaceName));
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
        SplitHorizon sh = new SplitHorizonBuilder().setOverrideSplitHorizonProtection(true).build();
        ParentRefsBuilder parentRefsBuilder = new ParentRefsBuilder().setParentInterface(parentIfaceName);
        interfaceBuilder.setEnabled(true).setName(interfaceName).setType(L2vlan.class)
                .addAugmentation(SplitHorizon.class, sh)
                .addAugmentation(IfL2vlan.class, ifL2vlan).addAugmentation(ParentRefs.class, parentRefsBuilder.build());
        return interfaceBuilder.build();
    }

    private static ElanInstanceBuilder createElanInstance(String instanceName) {
        return createElanInstance(instanceName, Long.valueOf(Math.abs((short) instanceName.hashCode())));
    }

    private static ElanInstanceBuilder createElanInstance(String instanceName, Long segmentationId) {
        ElanInstanceBuilder einstBuilder = new ElanInstanceBuilder();
        einstBuilder.setElanInstanceName(instanceName);
        einstBuilder.setKey(new ElanInstanceKey(instanceName));
        einstBuilder.setMacTimeout(DEFAULT_MAC_TIMEOUT);
        einstBuilder.setSegmentationId(segmentationId);
        einstBuilder.setSegmentType(SegmentTypeVxlan.class);
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

    public static InstanceIdentifier<ElanInstance> getElanInstanceInstanceIdentifier(String instanceName) {
        return InstanceIdentifier.builder(ElanInstances.class)
                .child(ElanInstance.class, new ElanInstanceKey(instanceName)).build();
    }

    private static InstanceIdentifier<ElanInterface> getElanInterfaceInstanceIdentifier(String interfaceName) {
        return InstanceIdentifier.builder(ElanInterfaces.class)
                .child(ElanInterface.class, new ElanInterfaceKey(interfaceName)).build();
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

    public static void writeInterface(Interface iface, WriteTransaction tx) {
        String interfaceName = iface.getName();
        InstanceIdentifier<Interface> interfaceIdentifier = createInterfaceIdentifier(interfaceName);
        tx.put(LogicalDatastoreType.CONFIGURATION, interfaceIdentifier, iface, true);
    }

    public static void deleteInterface(String interfaceName, WriteTransaction tx) {
        InstanceIdentifier<Interface> interfaceIdentifier = createInterfaceIdentifier(interfaceName);
        tx.delete(LogicalDatastoreType.CONFIGURATION, interfaceIdentifier);
    }

    private static InstanceIdentifier<Interface> createInterfaceIdentifier(String interfaceName) {
        return InstanceIdentifier.builder(Interfaces.class).child(Interface.class, new InterfaceKey(interfaceName))
                .build();
    }

    public static void createElanInterface(DataBroker dataBroker, String instanceName, String interfaceName,
            EtreeInterfaceType etreeInterfaceType, boolean isEtree) {
        WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);
        createElanInterface(instanceName, interfaceName, etreeInterfaceType, isEtree, tx);
        MdsalUtils.commitTransaction(tx);
    }

    public static void createElanInterface(String instanceName, String interfaceName,
            EtreeInterfaceType etreeInterfaceType, boolean isEtree, WriteTransaction tx) {
        logger.info("Adding {} interface: {}", isEtree ? "etree" : "elan", interfaceName);

        if (isEtree) {
            NetvirtUtils.createEtreeInterface(instanceName, interfaceName, etreeInterfaceType, tx);
        } else {
            NetvirtUtils.createElanInterface(instanceName, interfaceName, tx);
        }
    }

    private static void createEtreeInterface(String instanceName, String interfaceName, EtreeInterfaceType type,
            WriteTransaction tx) {
        ElanInterfaceBuilder einterfaceBuilder = createEtreeInterface(instanceName, interfaceName, type);

        tx.put(LogicalDatastoreType.CONFIGURATION, getElanInterfaceInstanceIdentifier(interfaceName),
                einterfaceBuilder.build());
    }

    private static void createElanInterface(String instanceName, String interfaceName, WriteTransaction tx) {
        ElanInterfaceBuilder einterfaceBuilder = createElanInterface(instanceName, interfaceName);

        tx.put(LogicalDatastoreType.CONFIGURATION, getElanInterfaceInstanceIdentifier(interfaceName),
                einterfaceBuilder.build());
    }

    public static void safeSleep() {
        try {
            Thread.yield();
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }
    public static void safeSleep(short sec) {
        try {
            Thread.yield();
            Thread.sleep(1000*sec);
        } catch (InterruptedException e) {
        }
    }


}
