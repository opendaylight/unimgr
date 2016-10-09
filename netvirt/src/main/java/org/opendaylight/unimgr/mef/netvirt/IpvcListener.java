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
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.ip.unis.IpUni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.Link;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.Ipvc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.ipvc.unis.Uni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.ipvc.unis.uni.EvcUniCeVlans;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.ipvc.unis.uni.evc.uni.ce.vlans.EvcUniCeVlan;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.EvcUniRoleType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.etree.rev160614.EtreeInterface.EtreeInterfaceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.Adjacencies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.AdjacenciesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.adjacency.list.Adjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.adjacency.list.AdjacencyBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class IpvcListener extends UnimgrDataTreeChangeListener<Ipvc> {
    private static final Logger log = LoggerFactory.getLogger(IpvcListener.class);
    private ListenerRegistration<IpvcListener> ipvcListenerRegistration;

    public IpvcListener(final DataBroker dataBroker) {
        super(dataBroker);

        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<Ipvc> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                    MefUtils.getIpvcInstanceIdentifier());
            ipvcListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
            log.info("IpvcDataTreeChangeListener created and registered");
        } catch (final Exception e) {
            log.error("Ipvc DataChange listener registration failed !", e);
            throw new IllegalStateException("Ipvc registration Listener failed.", e);
        }
    }

    @Override
    public void close() throws Exception {
        ipvcListenerRegistration.close();
    }

    @Override
    public void add(DataTreeModification<Ipvc> newDataObject) {
        if (newDataObject.getRootPath() != null && newDataObject.getRootNode() != null) {
            log.info("ipvc {} created", newDataObject.getRootNode().getIdentifier());
            addIpvc(newDataObject);
        }
    }

    @Override
    public void remove(DataTreeModification<Ipvc> removedDataObject) {
        if (removedDataObject.getRootPath() != null && removedDataObject.getRootNode() != null) {
            log.info("ipvc {} deleted", removedDataObject.getRootNode().getIdentifier());
            removeIpvc(removedDataObject);
        }
    }

    @Override
    public void update(DataTreeModification<Ipvc> modifiedDataObject) {
        if (modifiedDataObject.getRootPath() != null && modifiedDataObject.getRootNode() != null) {
            log.info("ipvc {} updated", modifiedDataObject.getRootNode().getIdentifier());
            updateIpvc(modifiedDataObject);
        }
    }

    private void addIpvc(DataTreeModification<Ipvc> newDataObject) {
        try {
            Ipvc data = newDataObject.getRootNode().getDataAfter();
            String instanceName = data.getIpvcId().getValue();

            log.info("Adding elan instance: " + instanceName);
            NetvirtUtils.createElanInstance(dataBroker, instanceName, false);

            log.info("Adding vpn instance: " + instanceName);
            NetvirtUtils.createVpnInstance(dataBroker, instanceName);

            // Create elan interfaces
            for (Uni uni : data.getUnis().getUni()) {
                createInterfaces(data, uni);
            }
        } catch (final Exception e) {
            log.error("Add ipvc failed !", e);
        }
    }

    private void updateIpvc(DataTreeModification<Ipvc> modifiedDataObject) {
        try {
            Ipvc original = modifiedDataObject.getRootNode().getDataBefore();
            Ipvc update = modifiedDataObject.getRootNode().getDataAfter();

            String instanceName = original.getIpvcId().getValue();

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
                        createInterfaces(original, uni);
                    }
                }
            } else if (originalUni != null && !originalUni.isEmpty()) {
                for (Uni uni : originalUni) {
                    removeElanInterface(instanceName, uni);
                }
            }
        } catch (final Exception e) {
            log.error("Update ipvc failed !", e);
        }
    }

    private void removeIpvc(DataTreeModification<Ipvc> removedDataObject) {
        try {
            Ipvc data = removedDataObject.getRootNode().getDataBefore();

            String instanceName = data.getIpvcId().getValue();

            for (Uni uni : data.getUnis().getUni()) {
                removeElanInterface(instanceName, uni);
            }

            log.info("Removing elan instance: " + instanceName);
            NetvirtUtils.deleteElanInstance(dataBroker, instanceName);
        } catch (final Exception e) {
            log.error("Remove ipvc failed !", e);
        }
    }

    private void createInterfaces(Ipvc data, Uni uni) {
        String instanceName = data.getIpvcId().getValue();        
        String interfaceName = uni.getIpUniId().getValue();
        
        createInterface(instanceName, uni, false);
        
        log.info("Adding vpn interface: " + interfaceName);
        NetvirtUtils.createVpnInterface(dataBroker, instanceName, interfaceName, createAdjacencies(uni));
    }
    
    private void createInterface(String instanceName, Uni uni, boolean isEtree) {
        IpvcUniUtils.addUni(dataBroker, uni);
        String interfaceName = uni.getUniId().getValue();
        EvcUniCeVlans evcUniCeVlans = uni.getEvcUniCeVlans();

        if (evcUniCeVlans != null && evcUniCeVlans.getEvcUniCeVlan() != null
                && !evcUniCeVlans.getEvcUniCeVlan().isEmpty()) {
            for (EvcUniCeVlan x : evcUniCeVlans.getEvcUniCeVlan()) {
                interfaceName = NetvirtUtils.getInterfaceNameForVlan(interfaceName, x.getVid().toString());
                
                NetvirtUtils.createInterface(dataBroker, instanceName, interfaceName, EtreeInterfaceType.Root, false);               
            }
        } else {
            NetvirtUtils.createInterface(dataBroker, instanceName, interfaceName, EtreeInterfaceType.Root, false);
        }
    }

    private Adjacencies createAdjacencies(Uni data) {
        String uniId = data.getIpUniId().getValue();
        Optional<org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni> value = MdsalUtils
                .read(dataBroker, LogicalDatastoreType.CONFIGURATION, MefUtils.getUniInstanceIdentifier(uniId));
        if (!value.isPresent()) {
            log.error("Couldn't find uni {} for ipvc-uni", uniId);
            throw new UnsupportedOperationException();
        }

        AdjacenciesBuilder builder = new AdjacenciesBuilder();
        List<Adjacency> list = new ArrayList<Adjacency>();

        org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni uni = value
                .get();

        if (uni.getIpUnis() != null) {
            for (IpUni ipUni : uni.getIpUnis().getIpUni()) {
                AdjacencyBuilder aBuilder = new AdjacencyBuilder();
                aBuilder.setIpAddress(MefUtils.ipPrefixToString(ipUni.getIpAddress()));
                aBuilder.setMacAddress(uni.getMacAddress().getValue());
                list.add(aBuilder.build());
            }
        }

        builder.setAdjacency(list);
        return builder.build();
    }

    private void removeElanInterface(String instanceName, Uni uni) {
        String uniId = uni.getIpUniId().getValue();
        String interfaceName = uniId;
        log.info("Removing elan interface: " + uniId);
        NetvirtUtils.deleteElanInterface(dataBroker, instanceName, interfaceName);
    }
}
