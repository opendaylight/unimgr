/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.Link;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.Evc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.Uni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.uni.EvcUniCeVlans;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.uni.evc.uni.ce.vlans.EvcUniCeVlan;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.EvcType;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.EvcUniRoleType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.etree.rev160614.EtreeInterface.EtreeInterfaceType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

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
            boolean isEtree = data.getEvcType() == EvcType.RootedMultipoint;
            if (!data.isAdminStateEnabled()) {
                log.info("add - evc {} AdminStateEnabled false", data.getEvcId().getValue());
            } else {
                log.info("Adding {} instance: {}", isEtree ? "etree" : "elan", instanceName);
                NetvirtUtils.createElanInstance(dataBroker, instanceName, isEtree);
            }
            // Create interfaces
            if (data.getUnis() == null) {
                log.info("No UNI's in service {}, exiting", instanceName);
                return;
            }
            for (Uni uni : data.getUnis().getUni()) {
                if (!uni.isAdminStateEnabled()) {
                    log.info("uni {} AdminStateEnabled false", uni.getUniId().getValue());
                } else {
                    createInterface(instanceName, uni, isEtree);
                }
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
            boolean isEtree = update.getEvcType() == EvcType.RootedMultipoint;

            if (!update.isAdminStateEnabled()) {
                log.info("update - evc {} AdminStateEnabled false", update.getEvcId().getValue());
            }
            log.info("Updating {} instance: {}", isEtree ? "etree" : "elan", instanceName);

            List<Uni> originalUni = original.getUnis() != null ? original.getUnis().getUni() : Collections.emptyList();
            if (update == null || update.getUnis() == null) {
                log.info("update uni is null");
            }
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
                        if (!uni.isAdminStateEnabled()) {
                            log.info("uni {} AdminStateEnabled false", uni.getUniId().getValue());
                        } else {
                            removeElanInterface(instanceName, uni);
                        }
                    }
                }

                // Adding the new Uni which are presented in the updated List
                if (updateUni.size() > 0) {
                    for (Uni uni : updateUni) {
                        if (!uni.isAdminStateEnabled()) {
                            log.info("uni {} AdminStateEnabled false", uni.getUniId().getValue());
                        } else {
                            createInterface(instanceName, uni, isEtree);
                        }
                    }
                }
            } else if (originalUni != null && !originalUni.isEmpty()) {
                for (Uni uni : originalUni) {
                    if (!uni.isAdminStateEnabled()) {
                        log.info("uni {} AdminStateEnabled false", uni.getUniId().getValue());
                    } else {
                        removeElanInterface(instanceName, uni);
                    }
                }
            }
        } catch (final Exception e) {
            log.error("Update evc failed !", e);
        }
    }

    private void removeEvc(DataTreeModification<Evc> removedDataObject) {
        try {
            Evc data = removedDataObject.getRootNode().getDataBefore();
            if (!data.isAdminStateEnabled()) {
                log.info("remove - evc {} AdminStateEnabled false", data.getEvcId().getValue());
            }

            String instanceName = data.getEvcId().getValue();

            for (Uni uni : data.getUnis().getUni()) {
                if (!uni.isAdminStateEnabled()) {
                    log.info("uni {} AdminStateEnabled false", uni.getUniId().getValue());
                } else {

                    removeElanInterface(instanceName, uni);
                }
            }

            log.info("Removing elan instance: " + instanceName);
            NetvirtUtils.deleteElanInstance(dataBroker, instanceName);
        } catch (final Exception e) {
            log.error("Remove evc failed !", e);
        }
    }

    private void createInterface(String instanceName, Uni uni, boolean isEtree) {

        EvcUniUtils.addUni(dataBroker, uni);

        String uniId = uni.getUniId().getValue();

        Link link = EvcUniUtils.getLink(dataBroker, uni);
        String interfaceName = uniId;

        boolean result = waitForGeniusToUpdateInterface(interfaceName);
        if (!result) {
            log.error("State interface {} is not configured (missing ifIndex)", interfaceName);
            return;
        }

        EvcUniRoleType role = uni.getRole();

        EvcUniCeVlans evcUniCeVlans = uni.getEvcUniCeVlans();

        if (evcUniCeVlans != null && evcUniCeVlans.getEvcUniCeVlan() != null
                && !evcUniCeVlans.getEvcUniCeVlan().isEmpty()) {
            for (EvcUniCeVlan x : evcUniCeVlans.getEvcUniCeVlan()) {

                interfaceName = NetvirtUtils.getInterfaceNameForVlan(interfaceName, x.getVid().toString());

                log.info("Adding {} interface: {}", isEtree ? "etree" : "elan", interfaceName);

                if (isEtree) {
                    NetvirtUtils.createEtreeInterface(dataBroker, instanceName, interfaceName,
                            RoleToInterfaceType(role));
                } else {
                    NetvirtUtils.createElanInterface(dataBroker, instanceName, interfaceName);
                }
            }
        } else {
            log.info("Adding {} interface: {}", isEtree ? "etree" : "elan", interfaceName);
            if (isEtree) {
                NetvirtUtils.createEtreeInterface(dataBroker, instanceName, interfaceName, RoleToInterfaceType(role));
            } else {
                NetvirtUtils.createElanInterface(dataBroker, instanceName, interfaceName);
            }
        }
    }

    private boolean waitForGeniusToUpdateInterface(String interfaceName) {
        int retries = 10;

        while (retries > 0) {
            Optional<Interface> optional = MdsalUtils.read(dataBroker, LogicalDatastoreType.OPERATIONAL,
                    NetvirtUtils.getStateInterfaceIdentifier(interfaceName));

            if (!optional.isPresent()) {
                log.info("State interface {} doesn't exist", interfaceName);
                return false;
            }

            Interface stateInterface = optional.get();

            if (stateInterface.getIfIndex() != null) {
                log.info("State interface configured with ifIndex {}", stateInterface.getIfIndex());

                // Wait a bit, because if we continue too soon this will not
                // work.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }

                return true;
            }

            retries -= 1;
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
            }
        }

        return false;
    }

    private static EtreeInterfaceType RoleToInterfaceType(EvcUniRoleType role) {
        if (role == EvcUniRoleType.Root) {
            return EtreeInterfaceType.Root;
        } else {
            return EtreeInterfaceType.Leaf;
        }
    }

    private void removeElanInterface(String instanceName, Uni uni) {
        EvcUniUtils.removeUni(dataBroker, uni);

        String uniId = uni.getUniId().getValue();
        EvcUniCeVlans evcUniCeVlans = uni.getEvcUniCeVlans();

        Link link = EvcUniUtils.getLink(dataBroker, uni);
        String interfaceName = uniId;

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
