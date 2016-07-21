/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.MefInterfaces;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.Unis;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.UniBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.UniKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.PhysicalLayers;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.PhysicalLayersBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.Links;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.LinksBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.Link;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.LinkKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.MefServices;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.MefService;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.Evc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.MefTopology;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.Devices;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.Device;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.DeviceBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.DeviceKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.device.Interfaces;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.device.InterfacesBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.device.interfaces.Interface;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.device.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.topology.rev150526.mef.topology.devices.device.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.DeviceRole;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.Identifier45;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.node.connector.statistics.and.port.number.map.NodeConnectorStatisticsAndPortNumberMap;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.CheckedFuture;

public final class MefUtils {
    private static final Logger logger = LoggerFactory.getLogger(MefUtils.class);
    private static boolean handleRemovedNodeConnectors = false;

    public static void handleNodeConnectorAdded(DataBroker dataBroker, String dpnId,
            FlowCapableNodeConnector nodeConnector) {

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();

        InstanceIdentifier interfacePath = getDeviceInterfaceInstanceIdentifier(dpnId, nodeConnector.getName());
        InterfaceBuilder interfaceBuilder = new InterfaceBuilder();
        interfaceBuilder.setPhy(new Identifier45(nodeConnector.getName()));
        DataObject deviceInterface = interfaceBuilder.build();

        tx.merge(LogicalDatastoreType.CONFIGURATION, interfacePath, deviceInterface, true);

        InstanceIdentifier uniPath = getUniInstanceIdentifier(nodeConnector.getName());
        UniBuilder uniBuilder = new UniBuilder();
        uniBuilder.setUniId(new Identifier45(nodeConnector.getName()));

        PhysicalLayersBuilder physicalLayersBuilder = new PhysicalLayersBuilder();
        LinksBuilder linksBuilder = new LinksBuilder();
        List<Link> links = new ArrayList();
        LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.setDevice(new Identifier45(dpnId));
        linkBuilder.setInterface(nodeConnector.getName());
        links.add(linkBuilder.build());
        linksBuilder.setLink(links);
        physicalLayersBuilder.setLinks(linksBuilder.build());
        uniBuilder.setPhysicalLayers(physicalLayersBuilder.build());
        DataObject uni = uniBuilder.build();

        tx.merge(LogicalDatastoreType.CONFIGURATION, uniPath, uni, true);
        CheckedFuture<Void, TransactionCommitFailedException> futures = tx.submit();

        try {
            futures.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error writing to datastore (path, data) : ({}, {}), ({}, {})", interfacePath, deviceInterface,
                    uniPath, uni);
            throw new RuntimeException(e.getMessage());
        }
    }

    public static void handleNodeConnectorRemoved(DataBroker dataBroker, String dpnId,
            FlowCapableNodeConnector nodeConnector) {

        if (!handleRemovedNodeConnectors) {
            return;
        }

        MdsalUtils.syncDelete(dataBroker, LogicalDatastoreType.CONFIGURATION,
                getDeviceInterfaceInstanceIdentifier(dpnId, nodeConnector.getName()));

        MdsalUtils.syncDelete(dataBroker, LogicalDatastoreType.CONFIGURATION,
                getUniLinkInstanceIdentifier(nodeConnector.getName(), dpnId, nodeConnector.getName()));
    }

    public static void handleNodeConnectorUpdated(DataBroker dataBroker, String dpnFromNodeConnectorId,
            FlowCapableNodeConnector original, FlowCapableNodeConnector update) {

    }

    public static InstanceIdentifier getDeviceInterfaceInstanceIdentifier(String deviceId, String interfaceId) {
        return InstanceIdentifier.builder(MefTopology.class).child(Devices.class)
                .child(Device.class, new DeviceKey(new Identifier45(deviceId))).child(Interfaces.class)
                .child(Interface.class, new InterfaceKey(new Identifier45(interfaceId))).build();
    }

    public static InstanceIdentifier<Uni> getUniInstanceIdentifier(String uniId) {
        return InstanceIdentifier.builder(MefInterfaces.class).child(Unis.class)
                .child(Uni.class, new UniKey(new Identifier45(uniId))).build();
    }

    public static InstanceIdentifier<org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.Uni> getEvcUniInstanceIdentifier(
            String uniId) {
        return InstanceIdentifier.create(MefServices.class).child(MefService.class).child(Evc.class)
                .child(org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.Unis.class)
                .child(org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.Uni.class,
                        new org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.UniKey(
                                new Identifier45(uniId)));

    }

    public static InstanceIdentifier getUniLinkInstanceIdentifier(String uniId, String deviceId, String interfaceId) {
        return InstanceIdentifier.builder(MefInterfaces.class).child(Unis.class)
                .child(Uni.class, new UniKey(new Identifier45(uniId))).child(PhysicalLayers.class).child(Links.class)
                .child(Link.class, new LinkKey(new Identifier45(deviceId), interfaceId)).build();
    }
}
