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
import org.opendaylight.genius.interfacemanager.globals.IfmConstants;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.MefInterfaces;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.Subnets;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.Unis;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.subnets.Subnet;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.subnets.SubnetKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.UniKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.IpUnis;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.PhysicalLayers;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.ip.unis.IpUni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.ip.unis.IpUniKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.Links;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.Link;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.LinkKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.MefTopology;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.Devices;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.Device;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.DeviceKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.device.Interfaces;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.device.interfaces.Interface;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.device.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.Identifier45;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public final class MefInterfaceUtils {
    private static final Logger logger = LoggerFactory.getLogger(MefInterfaceUtils.class);
    public final static String VLAN_SEPARATOR = ".";
    private final static String TRUNK_SUFFIX = "-trunk";

    public static InstanceIdentifier<Interface> getDeviceInterfaceInstanceIdentifier(String deviceId,
            String interfaceId) {
        return InstanceIdentifier.builder(MefTopology.class).child(Devices.class)
                .child(Device.class, new DeviceKey(new Identifier45(deviceId))).child(Interfaces.class)
                .child(Interface.class, new InterfaceKey(new Identifier45(interfaceId))).build();
    }

    public static InstanceIdentifier<Uni> getUniInstanceIdentifier(String uniId) {
        return InstanceIdentifier.builder(MefInterfaces.class).child(Unis.class)
                .child(Uni.class, new UniKey(new Identifier45(uniId))).build();
    }

    public static InstanceIdentifier<Uni> getUniListInstanceIdentifier() {
        return InstanceIdentifier.builder(MefInterfaces.class).child(Unis.class).child(Uni.class).build();
    }

    public static InstanceIdentifier<Link> getUniLinkInstanceIdentifier(String uniId, String deviceId,
            String interfaceId) {
        return InstanceIdentifier.builder(MefInterfaces.class).child(Unis.class)
                .child(Uni.class, new UniKey(new Identifier45(uniId))).child(PhysicalLayers.class).child(Links.class)
                .child(Link.class, new LinkKey(new Identifier45(deviceId), interfaceId)).build();
    }

    private static InstanceIdentifier<IpUni> getIpUniInstanceIdentifier(Identifier45 uniId, Identifier45 ipUniId) {
        return InstanceIdentifier.builder(MefInterfaces.class).child(Unis.class)
                .child(Uni.class, new UniKey(uniId)).child(IpUnis.class)
                .child(IpUni.class, new IpUniKey(ipUniId)).build();
    }

    public static InstanceIdentifier<Subnets> getSubnetListInstanceIdentifier() {
        return InstanceIdentifier.builder(MefInterfaces.class).child(Subnets.class).build();
    }

    public static InstanceIdentifier<Subnet> getSubnetsInstanceIdentifier() {
        return InstanceIdentifier.builder(MefInterfaces.class).child(Subnets.class).child(Subnet.class).build();
    }

    public static InstanceIdentifier<Subnet> getSubnetInstanceIdentifier(Identifier45 uniId, Identifier45 ipUniId,
            IpPrefix subnet) {
        return InstanceIdentifier.builder(MefInterfaces.class).child(Subnets.class)

                .child(Subnet.class, new SubnetKey(ipUniId, subnet, uniId)).build();
    }

    public static Link getLink(DataBroker dataBroker, String uniId, LogicalDatastoreType datastoreType) {
        Uni uni = getUni(dataBroker, uniId, datastoreType);

        if (uni == null) {
            logger.error("A matching Uni doesn't exist for EvcUni {}", uniId);
            return null;
        }

        PhysicalLayers physicalLayers = uni.getPhysicalLayers();
        if (physicalLayers == null) {
            logger.warn("Uni {} is missing PhysicalLayers", uniId);
            return null;
        }

        Links links = physicalLayers.getLinks();
        if (links == null || links.getLink() == null) {
            logger.warn("Uni {} is has no links", uniId);
            return null;
        }

        Link link = links.getLink().get(0);
        return link;
    }

    public static Uni getUni(DataBroker dataBroker, String uniId, LogicalDatastoreType datastoreType) {
        Optional<Uni> optional = MdsalUtils.read(dataBroker, datastoreType,
                MefInterfaceUtils.getUniInstanceIdentifier(uniId));

        if (!optional.isPresent()) {
            logger.debug("A matching Uni doesn't exist {}", uniId);
            return null;
        }

        return optional.get();
    }

    public static IpUni getIpUni(DataBroker dataBroker, Identifier45 uniId, Identifier45 ipUniId,
            LogicalDatastoreType datastoreType) {
        Optional<IpUni> optional = MdsalUtils.read(dataBroker, datastoreType,
                MefInterfaceUtils.getIpUniInstanceIdentifier(uniId, ipUniId));

        if (!optional.isPresent()) {
            logger.error("A matching IpUni doesn't exist Uni {} IpUni {}", uniId, ipUniId);
            return null;
        }

        return optional.get();
    }

    public static Interface getInterface(DataBroker dataBroker, String deviceId, String uniId,
            LogicalDatastoreType datastoreType) {
        InstanceIdentifier<Interface> interfacePath = MefInterfaceUtils.getDeviceInterfaceInstanceIdentifier(deviceId,
                uniId);
        Optional<Interface> optional = MdsalUtils.read(dataBroker, datastoreType, interfacePath);

        if (!optional.isPresent()) {
            logger.debug("A matching Uni doesn't exist {}", uniId);
            return null;
        }

        return optional.get();
    }

    public static String getInterfaceNameForVlan(String interfaceName, Long vlan) {
        final StringBuilder s = new StringBuilder();
        s.append(interfaceName);
        if (vlan != null) {
            s.append(VLAN_SEPARATOR).append(vlan);
        }
        s.append(TRUNK_SUFFIX);

        return getUUidFromString(s.toString());
    }

    private static String getUUidFromString(String key) {
        return java.util.UUID.nameUUIDFromBytes(key.getBytes()).toString();
    }

    public static String getTrunkParentName(Link link) {
        String interfaceName = link.getInterface().toString();
        return interfaceName;
    }

    public static String getInterfaceName(Link link, String uniId) {
        String device = link.getDevice().getValue();
        return getDeviceInterfaceName(device, uniId);
    }

    public static String getDeviceInterfaceName(String deviceName, String interfaceName) {
        return deviceName + IfmConstants.OF_URI_SEPARATOR + interfaceName;
    }
}
