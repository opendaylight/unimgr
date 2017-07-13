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
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.UniBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.CeVlansBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.VlanToPort;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.VlanToPortBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.ce.vlans.CeVlan;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.ce.vlans.CeVlanBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.Link;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.VlanIdOrNoneType;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.VlanIdType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;

public class UniPortManager extends UnimgrDataTreeChangeListener<Uni> implements IUniPortManager {

    private static final Logger LOG = LoggerFactory.getLogger(UniPortManager.class);
    private ListenerRegistration<UniPortManager> uniListenerRegistration;
    private static int maxWaitRetries = 3;
    private static long noVlan = 0l;

    public UniPortManager(final DataBroker dataBroker) {
        super(dataBroker);

        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<Uni> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                    getInstanceIdentifier());
            uniListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
            LOG.info("UniPortListener created and registered");
        } catch (final Exception e) {
            LOG.error("UniPortListener registration failed !", e);
            throw new IllegalStateException("UniPortListener registration failed.", e);
        }
    }

    private InstanceIdentifier<Uni> getInstanceIdentifier() {
        return MefInterfaceUtils.getUniListInstanceIdentifier();
    }

    @Override
    public void close() throws Exception {
        uniListenerRegistration.close();
    }

    @Override
    public void add(DataTreeModification<Uni> newDataObject) {
        if (newDataObject.getRootPath() != null && newDataObject.getRootNode() != null) {
            LOG.info("uni node {} created", newDataObject.getRootNode().getIdentifier());
        }
        Uni confUni = newDataObject.getRootNode().getDataAfter();
        String uniId = confUni.getUniId().getValue();

        synchronized (uniId.intern()) {
            if (!checkOperUni(uniId)) {
                return;
            }
            addCheckUniPorts(confUni);
        }
    }

    @Override
    public void remove(DataTreeModification<Uni> removedDataObject) {
        if (removedDataObject.getRootPath() != null && removedDataObject.getRootNode() != null) {
            LOG.info("uni node {} deleted", removedDataObject.getRootNode().getIdentifier());
        }
        Uni confUni = removedDataObject.getRootNode().getDataBefore();
        String uniId = confUni.getUniId().getValue();
        synchronized (uniId.intern()) {
            if (!checkOperUni(uniId)) {
                return;
            }
            removeUniPorts(confUni);
        }
    }

    @Override
    public void update(DataTreeModification<Uni> modifiedDataObject) {
        if (modifiedDataObject.getRootPath() != null && modifiedDataObject.getRootNode() != null) {
            LOG.info("node connector {} updated", modifiedDataObject.getRootNode().getIdentifier());
        }
        Uni confUni = modifiedDataObject.getRootNode().getDataAfter();
        String uniId = confUni.getUniId().getValue();
        synchronized (uniId.intern()) {
            if (!checkOperUni(uniId)) {
                return;
            }
            removeCheckUniPorts(confUni);
            addCheckUniPorts(confUni);
        }
    }

    @Override
    public void updateOperUni(String uniId) {
        Uni confUni = MefInterfaceUtils.getUni(dataBroker, uniId, LogicalDatastoreType.CONFIGURATION);
        if (confUni == null) {
            LOG.debug("No UNI {} exists, nothing to update");
            return;
        }
        synchronized (uniId.intern()) {
            if (!checkOperUni(uniId)) {
                return;
            }
            LOG.info("UNI  {} ports updated", uniId);

            removeCheckUniPorts(confUni);
            addCheckUniPorts(confUni);
        }
    }

    @Override
    public void removeUniPorts(String uniId) {
        Uni confUni = MefInterfaceUtils.getUni(dataBroker, uniId, LogicalDatastoreType.CONFIGURATION);
        if (confUni == null) {
            LOG.debug("No UNI {} exists, nothing to update");
            return;
        }
        synchronized (uniId.intern()) {
            if (!checkOperUni(uniId)) {
                return;
            }
            removeUniPorts(confUni);
        }
    }

    private boolean checkOperUni(String uniId) {
        Uni operUni = MefInterfaceUtils.getUni(dataBroker, uniId, LogicalDatastoreType.OPERATIONAL);
        if (operUni == null) {
            LOG.info("Uni {} is not operational", uniId);
            return false;
        }
        return true;
    }

    private void addCheckUniPorts(Uni confUni) {
        WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);

        String uniId = confUni.getUniId().getValue();
        Link link = MefInterfaceUtils.getLink(dataBroker, uniId, LogicalDatastoreType.OPERATIONAL);
        String trunkInterface = MefInterfaceUtils.getInterfaceNameForVlan(uniId, null);
        String parentInterfaceName = MefInterfaceUtils.getTrunkParentName(link);
        List<VlanToPort> operVlanInterfaces = getOperTrunkInterfaces(uniId);
        if (!hasVlanPort(operVlanInterfaces, Long.valueOf(0))) {
            VlanToPort newOperVlanInterface = addTrunkInterface(trunkInterface, parentInterfaceName, tx);
            operVlanInterfaces.add(newOperVlanInterface);
        }

        List<CeVlan> ceVlans = confUni.getCeVlans() != null ? confUni.getCeVlans().getCeVlan()
                : Collections.emptyList();
        for (CeVlan ceVlan : ceVlans) {
            Long vlan = ceVlan.getVid().getValue().longValue();
            if (hasVlanPort(operVlanInterfaces, vlan)) {
                continue;
            }

            String trunkMemberName = MefInterfaceUtils.getInterfaceNameForVlan(uniId, vlan);
            VlanToPort newOperVlanInterface = addTrunkMemberInterface(trunkMemberName, trunkInterface, vlan, tx);
            operVlanInterfaces.add(newOperVlanInterface);
        }
        // set VlanMapping to Uni
        setOperTrunkInterfaces(uniId, operVlanInterfaces, tx);

        MdsalUtils.commitTransaction(tx);
    }

    private void removeCheckUniPorts(Uni confUni) {
        WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);

        List<CeVlan> ceVlans = confUni.getCeVlans() != null ? confUni.getCeVlans().getCeVlan()
                : Collections.emptyList();
        List<Long> vlansValue = ceVlans.stream().map(x -> x.getVid().getValue()).collect(Collectors.toList());

        String uniId = confUni.getUniId().getValue();
        List<VlanToPort> operVlanInterfaces = getOperTrunkInterfaces(uniId);

        for (VlanToPort oldPort : getOperTrunkInterfaces(uniId)) {
            Long oldVlan = oldPort.getVlan().getValue();
            if (!vlansValue.contains(oldVlan) && oldVlan != noVlan) {
                VlanToPort removedOperVlanInterface = removeTrunkInterface(oldPort.getVlanPortId(), oldVlan, tx);
                operVlanInterfaces.remove(removedOperVlanInterface);
            }
        }
        // set VlanMapping to Uni
        setOperTrunkInterfaces(uniId, operVlanInterfaces, tx);

        MdsalUtils.commitTransaction(tx);
    }

    private void removeUniPorts(Uni confUni) {
        WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);
        String uniId = confUni.getUniId().getValue();

        for (VlanToPort oldPort : getOperTrunkInterfaces(uniId)) {
            Long oldVlan = oldPort.getVlan().getValue();
            removeTrunkInterface(oldPort.getVlanPortId(), oldVlan, tx);
        }
        setOperTrunkInterfaces(uniId, new ArrayList<>(), tx);

        MdsalUtils.commitTransaction(tx);
    }

    private VlanToPort addTrunkInterface(String interfaceName, String parentInterfaceName, WriteTransaction tx) {
        LOG.info("Adding VLAN trunk {} ParentRef {}", interfaceName, parentInterfaceName);
        Interface trunkInterface = NetvirtUtils.createTrunkInterface(interfaceName, parentInterfaceName);
        NetvirtUtils.writeInterface(trunkInterface, tx);
        return createOperTrunkInterfaceMapping(Long.valueOf(0), trunkInterface.getName());
    }

    private VlanToPort addTrunkMemberInterface(String interfaceName, String parentInterfaceName, Long vlan,
            WriteTransaction tx) {
        LOG.info("Adding VLAN trunk member {} ParentRef {}", interfaceName, parentInterfaceName);
        Interface trunkInterface = NetvirtUtils.createTrunkMemberInterface(interfaceName, parentInterfaceName,
                vlan.intValue());
        NetvirtUtils.writeInterface(trunkInterface, tx);
        return createOperTrunkInterfaceMapping(vlan, trunkInterface.getName());
    }

    private VlanToPort removeTrunkInterface(String interfaceName, Long vlan, WriteTransaction tx) {
        LOG.info("Delete VLAN trunk {}", interfaceName);
        NetvirtUtils.deleteInterface(interfaceName, tx);
        return createOperTrunkInterfaceMapping(vlan, interfaceName);
    }

    private List<VlanToPort> getOperTrunkInterfaces(String operUniId) {
        InstanceIdentifier<Uni> identifier = MefInterfaceUtils.getUniInstanceIdentifier(operUniId);
        InstanceIdentifier<PortVlanMapping> path = identifier.augmentation(PortVlanMapping.class);
        Optional<PortVlanMapping> portVlanMapping = MdsalUtils.read(dataBroker, LogicalDatastoreType.OPERATIONAL, path);
        if (portVlanMapping.isPresent()) {
            return portVlanMapping.get().getVlanToPort();
        } else {
            return new ArrayList<>();
        }
    }

    private void setOperTrunkInterfaces(String operUniId, List<VlanToPort> vlanToPort, WriteTransaction tx) {
        InstanceIdentifier<Uni> identifier = MefInterfaceUtils.getUniInstanceIdentifier(operUniId);
        InstanceIdentifier<PortVlanMapping> path = identifier.augmentation(PortVlanMapping.class);

        PortVlanMappingBuilder portVlanMappingB = new PortVlanMappingBuilder();
        portVlanMappingB.setVlanToPort(vlanToPort);

        tx.put(LogicalDatastoreType.OPERATIONAL, path, portVlanMappingB.build());
    }

    private VlanToPort createOperTrunkInterfaceMapping(Long vlan, String interfaceName) {
        final Long vlanNotNull = replaceNull(vlan);

        VlanToPortBuilder vlanToPortBuilder = new VlanToPortBuilder();
        vlanToPortBuilder.setVlan(new VlanIdOrNoneType(vlanNotNull));
        vlanToPortBuilder.setVlanPortId(interfaceName);
        return vlanToPortBuilder.build();
    }

    private boolean hasVlanPort(List<VlanToPort> vlanInterfaces, Long vlan) {
        if (vlanInterfaces == null) {
            return false;
        }
        final Long vlanNotNull = replaceNull(vlan);

        if (vlanInterfaces.stream().filter(x -> x.getVlan().getValue().equals(vlanNotNull)).findAny().isPresent()) {
            return true;
        }
        return false;
    }

    private static final Long replaceNull(Long vlan) {
        if (vlan == null) {
            return Long.valueOf(0);
        }
        return vlan;
    }

    @Override
    public void addCeVlan(String uniId, Long vlanId) {
        if (getUniVlanInterfaceNoRetry(uniId, vlanId) != null) {
            LOG.debug("UNI {} Port for vlan {} exists already, nothing to update", uniId, vlanId);
            return;
        }
        synchronized (uniId.intern()) {
            Uni confUni = MefInterfaceUtils.getUni(dataBroker, uniId, LogicalDatastoreType.CONFIGURATION);
            if (confUni == null) {
                LOG.debug("No UNI {} exists, nothing to update");
                return;
            }
            if (!checkOperUni(uniId)) {
                return;
            }
            LOG.info("UNI  {} Vlan {} adding", uniId, vlanId);
            List<CeVlan> ceVlans = confUni.getCeVlans() != null ? confUni.getCeVlans().getCeVlan() : new ArrayList<>();
            CeVlanBuilder ceVlanBuilder = new CeVlanBuilder();
            ceVlanBuilder.setVid(new VlanIdType(vlanId));
            CeVlansBuilder ceVlansBuilder = confUni.getCeVlans() != null ? new CeVlansBuilder(confUni.getCeVlans())
                    : new CeVlansBuilder();
            ceVlans.add(ceVlanBuilder.build());
            ceVlansBuilder.setCeVlan(ceVlans);
            UniBuilder uniBuilder = new UniBuilder();
            uniBuilder.setUniId(confUni.getUniId());
            uniBuilder.setCeVlans(ceVlansBuilder.build());
            MdsalUtils.syncUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION,
                    MefInterfaceUtils.getUniInstanceIdentifier(uniId), uniBuilder.build());
        }
    }

    @Override
    public void removeCeVlan(String uniId, Long vlanId) {
        if (getUniVlanInterfaceNoRetry(uniId, vlanId) == null) {
            LOG.debug("No UNI {} Port for vlan {} dosn't exist already, nothing to delete", uniId, vlanId);
            return;
        }
        synchronized (uniId.intern()) {
            Uni confUni = MefInterfaceUtils.getUni(dataBroker, uniId, LogicalDatastoreType.CONFIGURATION);
            if (confUni == null) {
                LOG.debug("No UNI {} exists, nothing to update");
                return;
            }
            if (!checkOperUni(uniId)) {
                return;
            }
            LOG.info("UNI  {} Vlan {} deleting", uniId, vlanId);
            UniBuilder uniBuilder = new UniBuilder(confUni);

            if (vlanId != null && vlanId != noVlan) {
                List<CeVlan> ceVlans = confUni.getCeVlans() != null ? confUni.getCeVlans().getCeVlan()
                        : Collections.emptyList();
                CeVlanBuilder ceVlanBuilder = new CeVlanBuilder();
                ceVlanBuilder.setVid(new VlanIdType(vlanId));
                CeVlansBuilder ceVlansBuilder = new CeVlansBuilder(confUni.getCeVlans());
                ceVlans.remove(ceVlanBuilder.build());
                ceVlansBuilder.setCeVlan(ceVlans);
                uniBuilder.setCeVlans(ceVlansBuilder.build());
            } else {
                uniBuilder.setCeVlans(null);
            }
            MdsalUtils.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION,
                    MefInterfaceUtils.getUniInstanceIdentifier(uniId), uniBuilder.build());
        }

    }

    @Override
    public List<String> getUniVlanInterfaces(String uniId) {
        synchronized (uniId.intern()) {
            List<VlanToPort> vlanToPorts = getOperTrunkInterfaces(uniId);
            return vlanToPorts.stream().map(port -> port.getVlanPortId()).collect(Collectors.toList());
        }
    }

    @Override
    public String getUniVlanInterface(String uniId, Long vlanId) {
        Long vlanNotNull = replaceNull(vlanId);
        return getUniVlanInterfaceRetry(uniId, vlanNotNull, 0);
    }

    public String getUniVlanInterfaceNoRetry(String uniId, Long vlanId) {
        Long vlanNotNull = replaceNull(vlanId);
        return getUniVlanInterfaceRetry(uniId, vlanNotNull, maxWaitRetries);
    }

    private String getUniVlanInterfaceRetry(String uniId, Long vlanId, int retries) {
        LOG.trace("Retry {} to wait for uniId {} vlan {} interface", retries, uniId, vlanId);
        List<VlanToPort> vlanToPorts = getOperTrunkInterfaces(uniId);
        java.util.Optional<String> toReturn = vlanToPorts.stream()
                .filter(port -> port.getVlan().getValue().equals(vlanId)).map(port -> port.getVlanPortId()).findFirst();
        if (toReturn.isPresent()) {
            return toReturn.get();
        } else {
            if (retries >= maxWaitRetries) {
                return null;
            }
            NetvirtUtils.safeSleep();
            return getUniVlanInterfaceRetry(uniId, vlanId, ++retries);
        }
    }

    @Override
    public String getUniVlanInterfaceName(String uniId, Long vlanId) {
        return MefInterfaceUtils.getInterfaceNameForVlan(uniId, null);
    }
}
