/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.ElanInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.ElanInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.instances.ElanInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.instances.ElanInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.instances.ElanInstanceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.interfaces.ElanInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.interfaces.ElanInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.interfaces.ElanInterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NetvirtUtils {
    public final static String VLAN_SEPARATOR = ".";

    public static void createElanInstance(DataBroker dataBroker, String instanceName) {
        ElanInstanceBuilder einstBuilder = createElanInstance(instanceName);

        MdsalUtils.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION,
                getElanInstanceInstanceIdentifier(instanceName), einstBuilder.build());
    }

    public static void createElanInterface(DataBroker dataBroker, String instanceName, String interfaceName) {
        ElanInterfaceBuilder einterfaceBuilder = createElanInterface(instanceName, interfaceName);

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

    private static InstanceIdentifier<ElanInstance> getElanInstanceInstanceIdentifier(String instanceName) {
        return InstanceIdentifier.builder(ElanInstances.class)
                .child(ElanInstance.class, new ElanInstanceKey(instanceName)).build();
    }

    private static InstanceIdentifier<ElanInterface> getElanInterfaceInstanceIdentifier(String interfaceName) {
        return InstanceIdentifier.builder(ElanInterfaces.class)
                .child(ElanInterface.class, new ElanInterfaceKey(interfaceName)).build();
    }
}
