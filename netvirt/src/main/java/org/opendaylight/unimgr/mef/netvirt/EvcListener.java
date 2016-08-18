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

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.Link;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.MefServices;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.MefService;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.Evc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.Uni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.uni.EvcUniCeVlans;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.uni.evc.uni.ce.vlans.EvcUniCeVlan;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvcListener extends UnimgrDataTreeChangeListener<Evc> {

    private static final Logger log = LoggerFactory.getLogger(EvcListener.class);
    private ListenerRegistration<EvcListener> evcListenerRegistration;

    public EvcListener(final DataBroker dataBroker) {
        super(dataBroker);

        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<Evc> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                    MefUtils.getEvcInstanceIdentifier());
            evcListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
            log.info("EvcDataTreeChangeListener created and registered");
        } catch (final Exception e) {
            log.error("Evc DataChange listener registration failed !", e);
            throw new IllegalStateException("Evc registration Listener failed.", e);
        }
    }

    @Override
    public void close() throws Exception {
        evcListenerRegistration.close();
    }

    @Override
    public void add(DataTreeModification<Evc> newDataObject) {
        if (newDataObject.getRootPath() != null && newDataObject.getRootNode() != null) {
            log.info("evc {} created", newDataObject.getRootNode().getIdentifier());
            addEvc(newDataObject);
        }
    }

    @Override
    public void remove(DataTreeModification<Evc> removedDataObject) {
        if (removedDataObject.getRootPath() != null && removedDataObject.getRootNode() != null) {
            log.info("evc {} deleted", removedDataObject.getRootNode().getIdentifier());
            removeEvc(removedDataObject);
        }
    }

    @Override
    public void update(DataTreeModification<Evc> modifiedDataObject) {
        if (modifiedDataObject.getRootPath() != null && modifiedDataObject.getRootNode() != null) {
            log.info("evc {} updated", modifiedDataObject.getRootNode().getIdentifier());
            updateEvc(modifiedDataObject);
        }
    }

    private void addEvc(DataTreeModification<Evc> newDataObject) {
        try {
            Evc data = newDataObject.getRootNode().getDataAfter();
            String instanceName = data.getEvcId().getValue();

            log.info("Adding elan instance: " + instanceName);
            NetvirtUtils.createElanInstance(dataBroker, instanceName);

            // Create elan interfaces
            for (Uni uni : data.getUnis().getUni()) {
                createElanInterface(instanceName, uni);
            }
        } catch (final Exception e) {
            log.error("Add evc failed !", e);
        }
    }

    private void updateEvc(DataTreeModification<Evc> modifiedDataObject) {
        try {
            Evc original = modifiedDataObject.getRootNode().getDataBefore();
            Evc update = modifiedDataObject.getRootNode().getDataAfter();

            String instanceName = original.getEvcId().getValue();

            log.info("Updating elan instance: " + instanceName);

            List<Uni> originalUni = original.getUnis().getUni();
            List<Uni> updateUni = update.getUnis().getUni();
            if (updateUni != null && !updateUni.isEmpty()) {
                List<Uni> existingClonedUni = new ArrayList<>();
                if (originalUni != null && !originalUni.isEmpty()) {
                    existingClonedUni.addAll(0, originalUni);
                    originalUni.removeAll(updateUni);
                    updateUni.removeAll(existingClonedUni);
                    // removing the Uni which are not presented in the updated
                    // List
                    for (Uni uni : originalUni) {
                        removeElanInterface(instanceName, uni);
                    }
                }

                // Adding the new Uni which are presented in the updated List
                if (updateUni.size() > 0) {
                    for (Uni uni : updateUni) {
                        createElanInterface(instanceName, uni);
                    }
                }
            } else if (originalUni != null && !originalUni.isEmpty()) {
                for (Uni uni : originalUni) {
                    removeElanInterface(instanceName, uni);
                }
            }
        } catch (final Exception e) {
            log.error("Update evc failed !", e);
        }
    }

    private void removeEvc(DataTreeModification<Evc> removedDataObject) {
        try {
            Evc data = removedDataObject.getRootNode().getDataBefore();

            String instanceName = data.getEvcId().getValue();

            for (Uni uni : data.getUnis().getUni()) {
                removeElanInterface(instanceName, uni);
            }

            log.info("Removing elan instance: " + instanceName);
            NetvirtUtils.deleteElanInstance(dataBroker, instanceName);
        } catch (final Exception e) {
            log.error("Remove evc failed !", e);
        }
    }

    private void createElanInterface(String instanceName, Uni uni) {
        EvcUniUtils.addUni(dataBroker, uni);

        String uniId = uni.getUniId().getValue();

        Link link = EvcUniUtils.getLink(dataBroker, uni);
        String interfaceName = EvcUniUtils.getInterfaceName(link, uniId);

        EvcUniCeVlans evcUniCeVlans = uni.getEvcUniCeVlans();

        if (evcUniCeVlans != null && !evcUniCeVlans.getEvcUniCeVlan().isEmpty()) {
            for (EvcUniCeVlan x : evcUniCeVlans.getEvcUniCeVlan()) {

                interfaceName = NetvirtUtils.getInterfaceNameForVlan(interfaceName, x.getVid().toString());

                log.info("Adding elan interface: " + interfaceName);
                NetvirtUtils.createElanInterface(dataBroker, instanceName, interfaceName);
            }
        } else {
            log.info("Adding elan interface: " + interfaceName);
            NetvirtUtils.createElanInterface(dataBroker, instanceName, interfaceName);
        }
    }

    private void removeElanInterface(String instanceName, Uni uni) {
        EvcUniUtils.removeUni(dataBroker, uni);

        String uniId = uni.getUniId().getValue();
        EvcUniCeVlans evcUniCeVlans = uni.getEvcUniCeVlans();

        Link link = EvcUniUtils.getLink(dataBroker, uni);
        String interfaceName = EvcUniUtils.getInterfaceName(link, uniId);

        if (evcUniCeVlans != null && !evcUniCeVlans.getEvcUniCeVlan().isEmpty()) {
            for (EvcUniCeVlan x : evcUniCeVlans.getEvcUniCeVlan()) {

                interfaceName = NetvirtUtils.getInterfaceNameForVlan(uniId, x.getVid().toString());
                log.info("Removing elan interface: " + interfaceName);
                NetvirtUtils.deleteElanInterface(dataBroker, instanceName, interfaceName);
            }
        } else {
            log.info("Removing elan interface: " + uniId);
            NetvirtUtils.deleteElanInterface(dataBroker, instanceName, interfaceName);
        }
    }
}
