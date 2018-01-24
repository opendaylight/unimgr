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
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.EvcElan;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.EvcElanBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.MefService;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.Evc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.ElanPorts;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.ElanPortsBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.Uni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.UniKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.uni.EvcUniCeVlans;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.uni.evc.uni.ce.vlans.EvcUniCeVlan;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.EvcType;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.EvcUniRoleType;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.RetailSvcIdType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.etree.rev160614.EtreeInterface.EtreeInterfaceType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;


public class EvcListener extends UnimgrDataTreeChangeListener<Evc> implements IUniAwareService {

    private static final Logger LOG = LoggerFactory.getLogger(EvcListener.class);
    private ListenerRegistration<EvcListener> evcListenerRegistration;
    private final IUniPortManager uniPortManager;
    private final UniQosManager uniQosManager;
    @SuppressWarnings("unused")
    private final UniAwareListener uniAwareListener;

    public EvcListener(final DataBroker dataBroker, final UniPortManager uniPortManager,
            final UniQosManager uniQosManager) {
        super(dataBroker);
        this.uniPortManager = uniPortManager;
        this.uniQosManager = uniQosManager;
        this.uniAwareListener = new UniAwareListener(dataBroker, this);
        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<Evc> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                    MefServicesUtils.getEvcsInstanceIdentifier());
            evcListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
            LOG.info("EvcDataTreeChangeListener created and registered");
        } catch (final Exception e) {
            LOG.error("Evc DataChange listener registration failed !", e);
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
            LOG.info("evc {} created", newDataObject.getRootNode().getIdentifier());
            addEvc(newDataObject);
        }
    }

    @Override
    public void remove(DataTreeModification<Evc> removedDataObject) {
        if (removedDataObject.getRootPath() != null && removedDataObject.getRootNode() != null) {
            LOG.info("evc {} deleted", removedDataObject.getRootNode().getIdentifier());
            removeEvc(removedDataObject);
        }
    }

    @Override
    public void update(DataTreeModification<Evc> modifiedDataObject) {
        if (modifiedDataObject.getRootPath() != null && modifiedDataObject.getRootNode() != null) {
            LOG.info("evc {} updated", modifiedDataObject.getRootNode().getIdentifier());
            updateEvc(modifiedDataObject);
        }
    }

    @Override
    public void connectUni(String uniId) {
        List<RetailSvcIdType> allEvcs = MefServicesUtils.getAllEvcsServiceIds(dataBroker);
        allEvcs = allEvcs != null ? allEvcs : Collections.emptyList();

        for (RetailSvcIdType evcSerId : allEvcs) {
            InstanceIdentifier<Evc> evcId = MefServicesUtils.getEvcInstanceIdentifier(evcSerId);
            Evc evc = MefServicesUtils.getEvc(dataBroker, evcId);
            if (evc == null) {
                LOG.error("Inconsistent data for svcId {}", evcSerId);
                continue;
            }

            String instanceName = evc.getEvcId().getValue();
            boolean isEtree = evc.getEvcType() == EvcType.RootedMultipoint;

            List<Uni> toConnect = new ArrayList<>();
            List<Uni> unis = evc.getUnis() != null ? evc.getUnis().getUni() : null;
            unis = unis != null ? unis : Collections.emptyList();
            for (Uni uni : unis) {
                if (uni.getUniId().getValue().equals(uniId)) {
                    LOG.info("Connecting Uni {} to svc id {}", uniId, evcSerId);
                    toConnect.add(uni);
                    break;
                }
            }

            EvcElan evcElan = getOperEvcElan(evcId);
            if (evcElan == null) {
                NetvirtUtils.createElanInstance(dataBroker, instanceName, isEtree, evc.getSegmentationId());
                evcElan = getOperEvcElan(evcId);
                if (evcElan == null) {
                    LOG.error("Evc {} has not been created as required. Nothing to reconnect", evcId);
                    return;
                }
            }

            for (Uni uni : toConnect) {
                createUniElanInterfaces(evcId, instanceName, uni, isEtree);
            }
            updateQos(toConnect);
        }
    }

    @Override
    public void disconnectUni(String uniId) {
        List<RetailSvcIdType> allEvcs = MefServicesUtils.getAllEvcsServiceIds(dataBroker);
        allEvcs = allEvcs != null ? allEvcs : Collections.emptyList();

        for (RetailSvcIdType evcSerId : allEvcs) {
            InstanceIdentifier<Evc> evcId = MefServicesUtils.getEvcInstanceIdentifier(evcSerId);
            Evc evc = MefServicesUtils.getEvc(dataBroker, evcId);
            if (evc == null) {
                LOG.error("Inconsistent data for svcId {}", evcSerId);
                continue;
            }

            String instanceName = evc.getEvcId().getValue();
            List<Uni> toDisconnect = new ArrayList<>();
            List<Uni> unis = evc.getUnis() != null ? evc.getUnis().getUni() : null;
            unis = unis != null ? unis : Collections.emptyList();
            for (Uni uni : unis) {
                if (uni.getUniId().getValue().equals(uniId)) {
                    LOG.info("Disconnecting Uni {} from svc id {}", uniId, evcSerId);
                    toDisconnect.add(uni);
                    break;
                }
            }

            EvcElan evcElan = getOperEvcElan(evcId);
            if (evcElan == null) {
                LOG.error("Evc {} has not been created as required. Nothing to disconnect", evcId);
                return;
            }

            updateQos(toDisconnect);
            for (Uni uni : toDisconnect) {
                removeUniElanInterfaces(evcId, instanceName, uni);
            }
        }

    }

    private void addEvc(DataTreeModification<Evc> newDataObject) {
        try {
            Evc data = newDataObject.getRootNode().getDataAfter();
            String instanceName = data.getEvcId().getValue();
            boolean isEtree = data.getEvcType() == EvcType.RootedMultipoint;
            InstanceIdentifier<Evc> evcId = newDataObject.getRootPath().getRootIdentifier();

            synchronized (instanceName.intern()) {
                NetvirtUtils.createElanInstance(dataBroker, instanceName, isEtree, data.getSegmentationId(),
                        data.getMacTimeout());

                // Create interfaces
                if (data.getUnis() == null) {
                    LOG.info("No UNI's in service {}, exiting", instanceName);
                    return;
                }
                for (Uni uni : data.getUnis().getUni()) {
                    createUniElanInterfaces(evcId, instanceName, uni, isEtree);
                }
                updateQos(data.getUnis().getUni());
            }
        } catch (final Exception e) {
            LOG.error("Add evc failed !", e);
        }
    }

    private void removeEvc(DataTreeModification<Evc> removedDataObject) {
        try {
            Evc data = removedDataObject.getRootNode().getDataBefore();
            InstanceIdentifier<Evc> evcId = removedDataObject.getRootPath().getRootIdentifier();
            List<Uni> uniToRemove = data.getUnis() != null && data.getUnis().getUni() != null ? data.getUnis().getUni()
                    : Collections.emptyList();

            synchronized (data.getEvcId().getValue().intern()) {
                updateQos(uniToRemove);
                EvcElan evcElan = getOperEvcElan(evcId);
                if (evcElan == null) {
                    LOG.error("Evc {} has not been created as required. Nothing to remove", data.getEvcId().getValue());
                    return;
                }

                String instanceName = evcElan.getElanId();

                for (Uni uni : uniToRemove) {
                    removeUniElanInterfaces(evcId, instanceName, uni);
                }

                LOG.info("Removing elan instance: " + instanceName);
                NetvirtUtils.deleteElanInstance(dataBroker, instanceName);
                removeOperEvcElan(evcId);
            }
        } catch (final Exception e) {
            LOG.error("Remove evc failed !", e);
        }
    }

    private void updateEvc(DataTreeModification<Evc> modifiedDataObject) {
        InstanceIdentifier<Evc> evcId = modifiedDataObject.getRootPath().getRootIdentifier();

        try {
            Evc original = modifiedDataObject.getRootNode().getDataBefore();
            Evc update = modifiedDataObject.getRootNode().getDataAfter();

            List<Uni> originalUni = original.getUnis() != null && original.getUnis().getUni() != null
                    ? original.getUnis().getUni() : Collections.emptyList();
            List<UniKey> originalUniIds = originalUni.stream().map(u -> u.getKey())
                    .collect(Collectors.toList());
            List<Uni> updateUni = update.getUnis() != null && update.getUnis().getUni() != null
                    ? update.getUnis().getUni() : Collections.emptyList();
            List<UniKey> updateUniIds = updateUni.stream().map(u -> u.getKey()).collect(Collectors.toList());

            synchronized (original.getEvcId().getValue().intern()) {

                String instanceName = original.getEvcId().getValue();
                boolean isEtree = update.getEvcType() == EvcType.RootedMultipoint;
                LOG.info("Updating {} instance: {}", isEtree ? "etree" : "elan", instanceName);

                // Changed Uni will be deleted / recreated
                List<Uni> uniToRemove = new ArrayList<>(originalUni);
                uniToRemove.removeIf(u -> updateUniIds.contains(u.getKey()));
                for (Uni uni : uniToRemove) {
                    removeUniElanInterfaces(evcId, instanceName, uni);
                }
                updateQos(uniToRemove);

                List<Uni> uniToCreate = new ArrayList<>(updateUni);
                uniToCreate.removeIf(u -> originalUniIds.contains(u.getKey()));
                uniToCreate.removeAll(originalUni);
                for (Uni uni : uniToCreate) {
                    createUniElanInterfaces(evcId, instanceName, uni, isEtree);
                }
                updateQos(uniToCreate);

                List<Uni> uniToUpdate = new ArrayList<>(updateUni);
                uniToUpdate.removeIf(u -> !originalUniIds.contains(u.getKey()));
                updateUnis(uniToUpdate);
            }
        } catch (final Exception e) {
            LOG.error("Update evc failed !", e);
        }
    }

    private void createUniElanInterfaces(InstanceIdentifier<Evc> evcId, String instanceName, Uni uni, boolean isEtree) {
        EvcUniRoleType role = uni.getRole();
        EvcUniCeVlans evcUniCeVlans = uni.getEvcUniCeVlans();

        List<EvcUniCeVlan> evcUniCeVlan = evcUniCeVlans != null && evcUniCeVlans.getEvcUniCeVlan() != null
                && !evcUniCeVlans.getEvcUniCeVlan().isEmpty() ? evcUniCeVlans.getEvcUniCeVlan()
                        : Collections.emptyList();

        for (EvcUniCeVlan ceVlan : evcUniCeVlan) {
            Long vlan = safeCastVlan(ceVlan.getVid());
            uniPortManager.addCeVlan(uni.getUniId().getValue(), vlan);
        }

        if (evcUniCeVlan.isEmpty()) {
            String interfaceName = uniPortManager.getUniVlanInterface(uni.getUniId().getValue(), Long.valueOf(0));
            if (interfaceName == null) {
                String errorMessage = String.format("Uni %s Interface for vlan %d is not operational ", uni.getUniId(),
                        0);
                LOG.error(errorMessage);
                throw new UnsupportedOperationException(errorMessage);
            }
            if (isOperEvcElanPort(evcId, interfaceName)) {
                LOG.info("elan interface for elan {} vlan {} interface {} exists already", instanceName, 0,
                        interfaceName);
                return;
            }
            LOG.info("Creting elan interface for elan {} vlan {} interface {}", instanceName, 0, interfaceName);
            NetvirtUtils.createElanInterface(dataBroker, instanceName, interfaceName, roleToInterfaceType(role),
                    isEtree);
            if (uni.isPortSecurityEnabled() && uni.getSecurityGroups() != null && !uni.getSecurityGroups().isEmpty()) {
                WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
                NetvirtUtils.addAclToInterface(interfaceName, uni.getSecurityGroups(), tx);
                MdsalUtils.commitTransaction(tx);
            }
            uniQosManager.mapUniPortBandwidthLimits(uni.getUniId().getValue(), interfaceName,
                    uni.getIngressBwProfile());
            setOperEvcElanPort(evcId, instanceName, interfaceName);
        } else {
            for (EvcUniCeVlan ceVlan : evcUniCeVlan) {
                Long vlan = safeCastVlan(ceVlan.getVid());
                String interfaceName = uniPortManager.getUniVlanInterface(uni.getUniId().getValue(), vlan);
                if (interfaceName == null) {
                    String errorMessage = String.format("Uni %s Interface for vlan %d is not operational ",
                            uni.getUniId(), 0);
                    LOG.error(errorMessage);
                    throw new UnsupportedOperationException(errorMessage);
                }
                if (isOperEvcElanPort(evcId, interfaceName)) {
                    LOG.info("elan interface for elan {} vlan {} interface {} exists already", instanceName, 0,
                            interfaceName);
                    return;
                }
                LOG.info("Creting elan interface for elan {} vlan {} interface {}", instanceName, 0, interfaceName);
                NetvirtUtils.createElanInterface(dataBroker, instanceName, interfaceName, roleToInterfaceType(role),
                        isEtree);
                if (uni.isPortSecurityEnabled() && uni.getSecurityGroups() != null && !uni.getSecurityGroups().isEmpty()) {
                    WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
                    NetvirtUtils.addAclToInterface(interfaceName, uni.getSecurityGroups(), tx);
                    MdsalUtils.commitTransaction(tx);                }
                uniQosManager.mapUniPortBandwidthLimits(uni.getUniId().getValue(), interfaceName,
                        uni.getIngressBwProfile());
                setOperEvcElanPort(evcId, instanceName, interfaceName);
            }
        }
    }

    private void removeUniElanInterfaces(InstanceIdentifier<Evc> evcId, String instanceName, Uni uni) {
        EvcUniCeVlans evcUniCeVlans = uni.getEvcUniCeVlans();

        List<EvcUniCeVlan> evcUniCeVlan = evcUniCeVlans != null && evcUniCeVlans.getEvcUniCeVlan() != null
                && !evcUniCeVlans.getEvcUniCeVlan().isEmpty() ? evcUniCeVlans.getEvcUniCeVlan()
                        : Collections.emptyList();

        if (evcUniCeVlan.isEmpty()) {
            String interfaceName = uniPortManager.getUniVlanInterface(uni.getUniId().getValue(), Long.valueOf(0));
            if (interfaceName == null || !isOperEvcElanPort(evcId, interfaceName)) {
                LOG.info("elan interface for elan {} vlan {} is not operational, nothing to remove", instanceName, 0,
                        interfaceName);
                interfaceName = uniPortManager.getUniVlanInterfaceName(uni.getUniId().getValue(), null);
            }
            removeElanInterface(evcId, uni.getUniId().getValue(), interfaceName);
        } else {
            for (EvcUniCeVlan ceVlan : evcUniCeVlan) {
                Long vlan = safeCastVlan(ceVlan.getVid());
                String interfaceName = uniPortManager.getUniVlanInterface(uni.getUniId().getValue(), vlan);
                if (interfaceName == null || !isOperEvcElanPort(evcId, interfaceName)) {
                    LOG.info("elan interface for elan {} vlan {} is not operational, nothing to remove", instanceName,
                            vlan, interfaceName);
                    interfaceName = uniPortManager.getUniVlanInterfaceName(uni.getUniId().getValue(), vlan);
                }
                removeElanInterface(evcId, uni.getUniId().getValue(), interfaceName);
            }
        }

        for (EvcUniCeVlan ceVlan : evcUniCeVlan) {
            Long vlan = safeCastVlan(ceVlan.getVid());
            uniPortManager.removeCeVlan(uni.getUniId().getValue(), vlan);
        }
    }

    private void removeElanInterface(InstanceIdentifier<Evc> identifier, String uniId, String interfaceName) {
        LOG.info("Removing elan interface: " + interfaceName);
        uniQosManager.unMapUniPortBandwidthLimits(uniId, interfaceName);
        NetvirtUtils.deleteElanInterface(dataBroker, interfaceName);

        EvcElan evcElan = getOperEvcElan(identifier);
        if (evcElan == null) {
            LOG.error("Removing non-operational Elan interface {}", interfaceName);
        }

        deleteOperEvcElanPort(identifier, interfaceName);
    }

    // Expected from API is Long
    private Long safeCastVlan(Object vid) {
        if (!(vid instanceof Long)) {
            String errorMessage = String.format("vlan id %s cannot be cast to Long", vid);
            LOG.error(errorMessage);
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

    private EvcElan getOperEvcElan(InstanceIdentifier<Evc> identifier) {
        InstanceIdentifier<EvcElan> path = identifier.augmentation(EvcElan.class);
        Optional<EvcElan> evcElan = MdsalUtils.read(dataBroker, LogicalDatastoreType.OPERATIONAL, path);
        if (evcElan.isPresent()) {
            return evcElan.get();
        } else {
            return null;
        }
    }

    private void removeOperEvcElan(InstanceIdentifier<Evc> identifier) {
        final InstanceIdentifier<MefService> serviceId = identifier.firstIdentifierOf(MefService.class);
        MdsalUtils.delete(dataBroker, LogicalDatastoreType.OPERATIONAL, serviceId);
    }

    private boolean isOperEvcElanPort(InstanceIdentifier<Evc> identifier, String elanPort) {
        EvcElan evcElan = getOperEvcElan(identifier);
        if (evcElan == null || evcElan.getElanPorts() == null) {
            return false;
        }
        List<ElanPorts> exPorts = evcElan.getElanPorts();
        return exPorts.stream().anyMatch(p -> p.getPortId().equals(elanPort));
    }

    private void setOperEvcElanPort(InstanceIdentifier<Evc> identifier, String elanName, String elanPort) {
        InstanceIdentifier<EvcElan> path = identifier.augmentation(EvcElan.class);
        EvcElan evcElan = getOperEvcElan(identifier);
        EvcElanBuilder evcElanBuilder = evcElan != null ? new EvcElanBuilder(evcElan) : new EvcElanBuilder();
        List<ElanPorts> exPorts = evcElan != null && evcElan.getElanPorts() != null ? evcElan.getElanPorts()
                : new ArrayList<>();

        ElanPortsBuilder portB = new ElanPortsBuilder();
        portB.setPortId(elanPort);
        exPorts.add(portB.build());
        evcElanBuilder.setElanId(elanName);
        evcElanBuilder.setElanPorts(exPorts);
        MdsalUtils.write(dataBroker, LogicalDatastoreType.OPERATIONAL, path, evcElanBuilder.build());
    }

    private void deleteOperEvcElanPort(InstanceIdentifier<Evc> identifier, String elanPort) {
        InstanceIdentifier<EvcElan> path = identifier.augmentation(EvcElan.class);
        EvcElan evcElan = getOperEvcElan(identifier);
        EvcElanBuilder evcElanBuilder = null;
        List<ElanPorts> exPorts = Collections.emptyList();
        if (evcElan != null) {
            evcElanBuilder = new EvcElanBuilder(evcElan);
            exPorts = evcElan.getElanPorts() != null ? evcElan.getElanPorts() : Collections.emptyList();
        } else {
            LOG.error("Deleting non-operational Elan port {}", elanPort);
            return;
        }
        List<ElanPorts> newList = exPorts.stream().filter(p -> !p.getPortId().equals(elanPort))
                .collect(Collectors.toList());
        evcElanBuilder.setElanPorts(newList);
        MdsalUtils.write(dataBroker, LogicalDatastoreType.OPERATIONAL, path, evcElanBuilder.build());
    }

    private void updateQos(List<Uni> uniToUpdate) {
        uniToUpdate.forEach(u -> uniQosManager.setUniBandwidthLimits(u.getUniId()));
    }

    private void updateUnis(List<Uni> uniToUpdate) {
        uniToUpdate.forEach(u -> uniQosManager.updateUni(u.getUniId(), u.getIngressBwProfile()));
        updateQos(uniToUpdate);
    }
}
