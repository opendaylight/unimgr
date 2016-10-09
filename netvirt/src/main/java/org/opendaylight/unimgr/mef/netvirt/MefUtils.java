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
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.MefInterfaces;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.Unis;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.UniKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.IpUnis;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.PhysicalLayers;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.ip.unis.IpUni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.ip.unis.IpUniKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.Links;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.Link;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.LinkKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.MefServices;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.MefService;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.MefServiceKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.Evc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.EvcBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.Ipvc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.MefTopology;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.Devices;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.Device;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.DeviceKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.device.Interfaces;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.device.interfaces.Interface;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.device.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.EvcIdType;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.EvcType;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.Identifier45;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.RetailSvcIdType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public final class MefUtils {
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(MefUtils.class);

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

    public static InstanceIdentifier<Uni> getUniListInterfaceInstanceIdentifier() {
        return InstanceIdentifier.builder(MefInterfaces.class).child(Unis.class).child(Uni.class).build();
    }

    public static InstanceIdentifier<org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.Unis> getUnisInstanceIdentifier(
            String evcId) {
        return InstanceIdentifier.builder(MefServices.class)
                .child(MefService.class, new MefServiceKey(RetailSvcIdType.getDefaultInstance(evcId))).child(Evc.class)
                .child(org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.Unis.class)
                .build();
    }

    public static InstanceIdentifier<org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.Uni> getEvcUniInstanceIdentifier(
            String uniId) {
        return InstanceIdentifier.builder(MefServices.class).child(MefService.class).child(Evc.class)
                .child(org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.Unis.class)
                .child(org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.Uni.class,
                        new org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.UniKey(
                                new Identifier45(uniId)))
                .build();
    }

    public static InstanceIdentifier<Link> getUniLinkInstanceIdentifier(String uniId, String deviceId,
            String interfaceId) {
        return InstanceIdentifier.builder(MefInterfaces.class).child(Unis.class)
                .child(Uni.class, new UniKey(new Identifier45(uniId))).child(PhysicalLayers.class).child(Links.class)
                .child(Link.class, new LinkKey(new Identifier45(deviceId), interfaceId)).build();
    }

    public static InstanceIdentifier<IpUni> getIpUniInstanceIdentifier(String uniId, String ipUniId) {
        return InstanceIdentifier.builder(MefInterfaces.class).child(Unis.class)
                .child(Uni.class, new UniKey(new Identifier45(uniId))).child(IpUnis.class)
                .child(IpUni.class, new IpUniKey(new Identifier45(ipUniId))).build();
    }

    public static InstanceIdentifier<Evc> getEvcInstanceIdentifier() {
        return getMefServiceInstanceIdentifier().child(Evc.class);
    }

    public static InstanceIdentifier<MefService> getMefServiceInstanceIdentifier() {
        return InstanceIdentifier.create(MefServices.class).child(MefService.class);
    }

    public static InstanceIdentifier<MefService> getMefServiceInstanceIdentifier(RetailSvcIdType retailSvcIdType) {
        return InstanceIdentifier.create(MefServices.class).child(MefService.class, new MefServiceKey(retailSvcIdType));
    }

    public static InstanceIdentifier<MefServices> getMefServicesInstanceIdentifier() {
        return InstanceIdentifier.create(MefServices.class);
    }

    public static void createEvcInstance(DataBroker dataBroker, String instanceName) {
        Evc einst = createEvcInstance(instanceName);

        MdsalUtils.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION,
                getEvcInstanceInstanceIdentifier(instanceName), einst);
    }

    private static InstanceIdentifier<Evc> getEvcInstanceInstanceIdentifier(String instanceName) {
        return InstanceIdentifier.builder(MefServices.class)
                .child(MefService.class, new MefServiceKey(RetailSvcIdType.getDefaultInstance(instanceName)))
                .child(Evc.class).build();
    }

    private static InstanceIdentifier<org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.Uni> getEvcUniInstanceIdentifier(
            String serviceName, String uniId) {
        return InstanceIdentifier.builder(MefServices.class)
                .child(MefService.class, new MefServiceKey(RetailSvcIdType.getDefaultInstance(serviceName)))
                .child(Evc.class)
                .child(org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.Unis.class)
                .child(org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.Uni.class, //
                        new org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.UniKey(
                                Identifier45.getDefaultInstance(uniId)))
                .build();
    }

    private static Evc createEvcInstance(String instanceName) {
        EvcBuilder evcBuilder = new EvcBuilder();
        evcBuilder.setAdminStateEnabled(false);
        evcBuilder.setEvcId(EvcIdType.getDefaultInstance(instanceName));
        evcBuilder.setEvcType(EvcType.MultipointToMultipoint);
        return evcBuilder.build();
    }

    public static Boolean EvcExists(DataBroker dataBroker, String instanceName) {
        Optional<Evc> evc = getEvc(dataBroker, instanceName);
        return evc.isPresent();
    }

    public static Boolean isEvcAdminStateEnabled(DataBroker dataBroker, String instanceName) {
        Optional<Evc> evc = getEvc(dataBroker, instanceName);
        return evc.isPresent() && evc.get().isAdminStateEnabled();

    }

    public static Optional<Evc> getEvc(DataBroker dataBroker, String instanceName) {
        Optional<Evc> evc = MdsalUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                getEvcInstanceInstanceIdentifier(instanceName));
        return evc;
    }

    public static Boolean EvcUniExists(DataBroker dataBroker, String instanceName, String uniId) {
        logger.info("searching for uni id {} in service {}", uniId, instanceName);
        Optional<org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.Uni> uni = MdsalUtils
                .read(dataBroker, LogicalDatastoreType.CONFIGURATION, getEvcUniInstanceIdentifier(instanceName, uniId));
        if (uni.isPresent()) {
            logger.info("found uni");
        } else {
            logger.info("no uni");
        }
        return uni.isPresent();
    }

    public static String getTrunkParentName(Link link) {
        String interfaceName = link.getInterface().toString();
        return interfaceName;
    }

    public static String getInterfaceName(Link link, String uniId) {
        String device = link.getDevice().getValue();
        return NetvirtUtils.getDeviceInterfaceName(device, uniId);
    }

    public static InstanceIdentifier<Ipvc> getIpvcInstanceIdentifier() {
        return InstanceIdentifier.create(MefServices.class).child(MefService.class).child(Ipvc.class);
    }

    public static String ipPrefixToString(IpPrefix ipAddress) {
        if (ipAddress.getIpv4Prefix() != null) {
            return ipAddress.getIpv4Prefix().getValue();
        }

        return ipAddress.getIpv6Prefix().getValue();
    }

    public static String ipAddressToString(IpAddress ipAddress) {
        if (ipAddress.getIpv4Address() != null) {
            return ipAddress.getIpv4Address().getValue();
        }

        return ipAddress.getIpv6Address().getValue();
    }

}
