/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.PhysicalLayers;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.Links;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.Link;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.MefServices;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.MefService;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.Evc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.Unis;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.Uni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.uni.EvcUniCeVlans;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.uni.evc.uni.ce.vlans.EvcUniCeVlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;

public class EvcUniListener extends UnimgrDataTreeChangeListener<Uni> {
    private static final Logger logger = LoggerFactory.getLogger(EvcUniListener.class);

    private ListenerRegistration<EvcUniListener> uniListenerRegistration;

    public EvcUniListener(final DataBroker dataBroker) {
        super(dataBroker);

        registerListener();
    }

    @Override
    public void add(DataTreeModification<Uni> newDataObject) {
        if (newDataObject.getRootPath() != null && newDataObject.getRootNode() != null) {
            logger.info("uni {} created", newDataObject.getRootNode().getIdentifier());
            addUni(newDataObject);
        }
    }

    @Override
    public void remove(DataTreeModification<Uni> removedDataObject) {
        if (removedDataObject.getRootPath() != null && removedDataObject.getRootNode() != null) {
            logger.info("uni {} deleted", removedDataObject.getRootNode().getIdentifier());
            removeUni(removedDataObject);
        }
    }

    @Override
    public void update(DataTreeModification<Uni> modifiedDataObject) {
        if (modifiedDataObject.getRootPath() != null && modifiedDataObject.getRootNode() != null) {
            logger.info("uni {} updated", modifiedDataObject.getRootNode().getIdentifier());
            updateUni(modifiedDataObject);
        }
    }

    protected void removeUni(DataTreeModification<Uni> removedDataObject) {
        try {
            Uni data = removedDataObject.getRootNode().getDataBefore();

            String uniId = data.getUniId().getValue();
            WriteTransaction tx = createTransaction();
            logger.info("Removing VLAN trunk {}", uniId);
            delete(uniId, tx);

            Optional<List<EvcUniCeVlan>> ceVlansOptional = getCeVlans(data);
            if (!ceVlansOptional.isPresent()) {
                return;
            }

            removeTrunkMemberInterfaces(uniId, ceVlansOptional.get(), tx);
            commitTransaction(tx);
        } catch (final Exception e) {
            logger.error("Remove uni failed !", e);
        }
    }

    protected void updateUni(DataTreeModification<Uni> modifiedDataObject) {
        try {
            Uni original = modifiedDataObject.getRootNode().getDataBefore();
            Uni update = modifiedDataObject.getRootNode().getDataAfter();

            String uniId = update.getUniId().getValue();
            WriteTransaction tx = createTransaction();
            String origTrunkParentName = getTrunkParentName(original);
            String updatedTrunkParentName = getTrunkParentName(update);

            if (!Objects.equal(origTrunkParentName, updatedTrunkParentName)) {
                addTrunkInterface(uniId, updatedTrunkParentName, tx);
            }

            Set<EvcUniCeVlan> origCeVlans = Sets.newHashSet(getCeVlans(original).or(Collections.emptyList()));
            Set<EvcUniCeVlan> updatedCeVlans = Sets.newHashSet(getCeVlans(update).or(Collections.emptyList()));
            Iterable<EvcUniCeVlan> removedCeVlans = Sets.difference(origCeVlans, updatedCeVlans);
            Iterable<EvcUniCeVlan> addedCeVlans = Sets.difference(updatedCeVlans, origCeVlans);
            removeTrunkMemberInterfaces(uniId, removedCeVlans, tx);
            addTrunkMemberInterfaces(uniId, addedCeVlans, tx);
            commitTransaction(tx);
        } catch (final Exception e) {
            logger.error("Update uni failed !", e);
        }

    }

    protected void addUni(DataTreeModification<Uni> newDataObject) {
        try {
            Uni data = newDataObject.getRootNode().getDataAfter();

            String uniId = data.getUniId().getValue();
            WriteTransaction tx = createTransaction();
            addTrunkInterface(uniId, getTrunkParentName(data), tx);

            Optional<List<EvcUniCeVlan>> ceVlansOptional = getCeVlans(data);
            if (ceVlansOptional.isPresent()) {
                addTrunkMemberInterfaces(uniId, ceVlansOptional.get(), tx);
            }

            commitTransaction(tx);
        } catch (final Exception e) {
            logger.error("Add uni failed !", e);
        }

    }

    private void addTrunkInterface(String interfaceName, String parentInterfaceName, WriteTransaction tx) {
        logger.info("Adding VLAN trunk {} ParentRef {}", interfaceName, parentInterfaceName);
        Interface trunkInterface = NetvirtUtils.createTrunkInterface(interfaceName, parentInterfaceName);
        write(trunkInterface, tx);
    }

    private void addTrunkMemberInterfaces(String parentInterfaceName, Iterable<EvcUniCeVlan> ceVlans,
            WriteTransaction tx) {
        for (EvcUniCeVlan ceVlan : ceVlans) {
            Long vlanId = (Long) ceVlan.getVid();
            String interfaceName = NetvirtUtils.getInterfaceNameForVlan(parentInterfaceName, vlanId.toString());
            logger.info("Adding VLAN trunk-member {} ParentRef {}", interfaceName, parentInterfaceName);
            Interface trunkMemberInterface = NetvirtUtils.createTrunkMemberInterface(interfaceName, parentInterfaceName,
                    vlanId.intValue());
            write(trunkMemberInterface, tx);
        }
    }

    private void removeTrunkMemberInterfaces(String parentInterfaceName, Iterable<EvcUniCeVlan> ceVlans,
            WriteTransaction tx) {
        for (EvcUniCeVlan ceVlan : ceVlans) {
            Long vlanId = (Long) ceVlan.getVid();
            String interfaceName = NetvirtUtils.getInterfaceNameForVlan(parentInterfaceName, vlanId.toString());
            logger.info("Removing VLAN trunk-member {}", interfaceName);
            delete(interfaceName, tx);
        }
    }

    private InstanceIdentifier<Interface> createInterfaceIdentifier(String interfaceName) {
        return InstanceIdentifier.builder(Interfaces.class).child(Interface.class, new InterfaceKey(interfaceName))
                .build();
    }

    private WriteTransaction createTransaction() {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        return tx;
    }

    private void commitTransaction(WriteTransaction tx) {
        try {
            CheckedFuture<Void, TransactionCommitFailedException> futures = tx.submit();
            futures.get();
        } catch (Exception e) {
            logger.error("failed to commit transaction due to exception ", e);
        }
    }

    private void write(Interface iface, WriteTransaction tx) {
        String interfaceName = iface.getName();
        InstanceIdentifier<Interface> interfaceIdentifier = createInterfaceIdentifier(interfaceName);
        tx.put(LogicalDatastoreType.CONFIGURATION, interfaceIdentifier, iface, true);
    }

    private void delete(String interfaceName, WriteTransaction tx) {
        InstanceIdentifier<Interface> interfaceIdentifier = createInterfaceIdentifier(interfaceName);
        tx.delete(LogicalDatastoreType.CONFIGURATION, interfaceIdentifier);
    }

    private String getTrunkParentName(Uni evcUni) {
        
        Optional<org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni> optional = MdsalUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION, MefUtils.getUniInstanceIdentifier(evcUni.getUniId().getValue()));
        
        if (!optional.isPresent())
        {
            logger.error("A matching Uni doesn't exist for EvcUni {}", evcUni.getUniId());
            return null;
        }
        
        org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni uni = optional.get();
               
        PhysicalLayers physicalLayers = uni.getPhysicalLayers();
        if (physicalLayers == null) {
            logger.warn("Uni {} is missing PhysicalLayers", evcUni.getUniId());
            return null;
        }

        Links links = physicalLayers.getLinks();
        if (links == null || links.getLink() == null) {
            logger.warn("Uni {} is has no links", evcUni.getUniId());
            return null;
        }

        Link link = links.getLink().get(0);
        String deviceName = link.getDevice().getValue();
        String interfaceName = link.getInterface().toString();
        return getDeviceInterfaceName(deviceName, interfaceName);
    }

    private String getDeviceInterfaceName(String deviceName, String interfaceName) {
        // FIXME need to use a hack until genius will start generating
        // interfaces including device name
        return interfaceName;
        // return deviceName + "#" + interfaceName;
    }

    private Optional<List<EvcUniCeVlan>> getCeVlans(Uni uni) {
        EvcUniCeVlans ceVlans = uni.getEvcUniCeVlans();
        if (ceVlans == null) {
            return Optional.absent();
        }

        return Optional.fromNullable(ceVlans.getEvcUniCeVlan());
    }

    private void registerListener() {
        try {
            final DataTreeIdentifier<Uni> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                    getUniTopologyPath());
            uniListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
            logger.info("UniDataTreeChangeListener created and registered");
        } catch (final Exception e) {
            logger.error("Uni DataChange listener registration failed !", e);
            throw new IllegalStateException("Uni registration Listener failed.", e);
        }
    }

    private InstanceIdentifier<Uni> getUniTopologyPath() {
        return InstanceIdentifier.create(MefServices.class).child(MefService.class).child(Evc.class).child(Unis.class)
                .child(Uni.class);
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub

    }
}