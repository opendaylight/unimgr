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
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.PortVlanMapping;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.PortVlanMappingBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.VlanToPort;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.EvcElan;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.EvcElanBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.Evc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.ElanPorts;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.ElanPortsBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.Uni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.uni.EvcUniCeVlans;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.uni.evc.uni.ce.vlans.EvcUniCeVlan;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.EvcType;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.EvcUniRoleType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.etree.rev160614.EtreeInterface.EtreeInterfaceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class EvcListener extends UnimgrDataTreeChangeListener<Evc> {

    private static final Logger log = LoggerFactory.getLogger(EvcListener.class);
    private ListenerRegistration<EvcListener> evcListenerRegistration;
    private final IUniPortManager uniPortManager;

    public EvcListener(final DataBroker dataBroker, final UniPortManager uniPortManager) {
        super(dataBroker);
        this.uniPortManager = uniPortManager;
        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<Evc> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                    MefServicesUtils.getEvcsInstanceIdentifier());
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
            WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);
            Evc data = newDataObject.getRootNode().getDataAfter();
            
            String instanceName = data.getEvcId().getValue();
            boolean isEtree = data.getEvcType() == EvcType.RootedMultipoint;
            InstanceIdentifier<Evc> evcId = newDataObject.getRootPath().getRootIdentifier();
            
            synchronized (instanceName.intern()) {            
                NetvirtUtils.createElanInstance(dataBroker, instanceName, isEtree);

                // Create interfaces
                if (data.getUnis() == null) {
                    log.info("No UNI's in service {}, exiting", instanceName);
                    return;
                }
                List<String> uniElanInterfaces = Collections.emptyList();
                for (Uni uni : data.getUnis().getUni()) {
                    uniElanInterfaces = createUniElanInterfaces(instanceName, uni, isEtree);
                }
                setOperEvcElan(evcId, instanceName, uniElanInterfaces, tx);
                MdsalUtils.commitTransaction(tx);
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
                        removeElanInterface(instanceName, uni);
                    }
                }

                // Adding the new Uni which are presented in the updated List
                if (updateUni.size() > 0) {
                    for (Uni uni : updateUni) {
                        createUniElanInterfaces(instanceName, uni, isEtree);
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

    private List<String> createUniElanInterfaces(String instanceName, Uni uni, boolean isEtree) {
        EvcUniRoleType role = uni.getRole();
        EvcUniCeVlans evcUniCeVlans = uni.getEvcUniCeVlans();
        List<String> toReturn = new ArrayList<>();

        List<EvcUniCeVlan> evcUniCeVlan = (evcUniCeVlans != null && evcUniCeVlans.getEvcUniCeVlan() != null
                && !evcUniCeVlans.getEvcUniCeVlan().isEmpty()) ? evcUniCeVlans.getEvcUniCeVlan()
                        : Collections.emptyList();

                for (EvcUniCeVlan ceVlan : evcUniCeVlan) {
                    Long vlan = safeCastVlan(ceVlan.getVid());
                    uniPortManager.addCeVlan(uni.getUniId().getValue(), vlan);
                }

                // TODO :
                // Let to work for PortManager 
                if (evcUniCeVlan.isEmpty()) {
                    String interfaceName = uniPortManager.getUniVlanInterface(uni.getUniId().getValue(), Long.valueOf(0));
                    log.info("Creting elan interface for elan {} vlan {} interface {}", instanceName, 0, interfaceName);
                    NetvirtUtils.createElanInterface(dataBroker, instanceName, interfaceName, roleToInterfaceType(role),
                            isEtree);
                    toReturn.add(interfaceName);
                } else {
                    for (EvcUniCeVlan ceVlan : evcUniCeVlan) {
                        Long vlan = safeCastVlan(ceVlan.getVid());
                        String interfaceName = uniPortManager.getUniVlanInterface(uni.getUniId().getValue(), vlan);
                        log.info("Creting elan interface for elan {} vlan {} interface {}", instanceName, 0, interfaceName);
                        NetvirtUtils.createElanInterface(dataBroker, instanceName, interfaceName, roleToInterfaceType(role),
                                isEtree);
                        toReturn.add(interfaceName);

                    }
                }
                return toReturn;
    }

    // Expected from API is Long
    private Long safeCastVlan(Object vid) {
        if (!(vid instanceof Long)) {
            String errorMessage = String.format("vlan id %s cannot be cast to Long", vid);
            log.error(errorMessage);
            throw new UnsupportedOperationException(errorMessage);
        }
        return (Long) vid;
    }

    private static EtreeInterfaceType roleToInterfaceType(EvcUniRoleType role) {
        if (role == EvcUniRoleType.Root) {
            return EtreeInterfaceType.Root;
        } else {
            return EtreeInterfaceType.Leaf;
        }
    }

    private void removeElanInterface(String instanceName, Uni uni) {
        // EvcUniUtils.removeEvcUni(dataBroker, uni);

        String uniId = uni.getUniId().getValue();
        EvcUniCeVlans evcUniCeVlans = uni.getEvcUniCeVlans();
        String interfaceName = uniId;

        if (evcUniCeVlans != null && !evcUniCeVlans.getEvcUniCeVlan().isEmpty()) {
            for (EvcUniCeVlan x : evcUniCeVlans.getEvcUniCeVlan()) {

                // TODO :
                // interfaceName = NetvirtUtils.getInterfaceNameForVlan(uniId,
                // x.getVid().toString());
                log.info("Removing elan interface: " + interfaceName);
                NetvirtUtils.deleteElanInterface(dataBroker, instanceName, interfaceName);
            }
        } else {
            log.info("Removing elan interface: " + uniId);
            NetvirtUtils.deleteElanInterface(dataBroker, instanceName, interfaceName);
        }
    }

    private EvcElan getOperEvcElan(InstanceIdentifier<Evc> identifier) {
        InstanceIdentifier<EvcElan> path = identifier.augmentation(EvcElan.class);
        Optional<EvcElan> evcElan = MdsalUtils.read(dataBroker, LogicalDatastoreType.OPERATIONAL, path);
        if (evcElan.isPresent()) {
            return evcElan.get();
        } else {
            return null;
        }
    }

    private void setOperEvcElan(InstanceIdentifier<Evc> identifier, String elanId,  List<String> elanPortsStr, WriteTransaction tx) {
        InstanceIdentifier<EvcElan> path = identifier.augmentation(EvcElan.class);

        EvcElanBuilder evcElanBuilder = new EvcElanBuilder();
        evcElanBuilder.setElanId(elanId);

        List <ElanPorts>  elanPorts = elanPortsStr.stream().map(port -> {
            ElanPortsBuilder elanPortB = new ElanPortsBuilder();
            return elanPortB.setPortId(port).build();
        }).collect(Collectors.toList());

        evcElanBuilder.setElanPorts(elanPorts);
        tx.put(LogicalDatastoreType.OPERATIONAL, path, evcElanBuilder.build(), true);  
    }



}
