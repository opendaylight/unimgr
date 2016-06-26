/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.netvirt;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.AbstractDataChangeListener;
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

public class EvcListener extends AbstractDataChangeListener<Evc> {

    private static final Logger log = LoggerFactory.getLogger(EvcListener.class);
    private DataBroker dataBroker;
    private ListenerRegistration<DataChangeListener> evcListenerRegistration;

    public EvcListener(final DataBroker dataBroker) {
        super(Evc.class);
        this.dataBroker = dataBroker;

        registerListener();
    }

    public void registerListener() {
        try {
            evcListenerRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, getWildCardPath(), EvcListener.this,
                    DataChangeScope.SUBTREE);
        } catch (final Exception e) {
            log.error("Evc DataChange listener registration failed !", e);
            throw new IllegalStateException("Evc registration Listener failed.", e);
        }
    }

    private InstanceIdentifier<Evc> getWildCardPath() {
        InstanceIdentifier<Evc> instanceIdentifier = InstanceIdentifier.create(MefServices.class).child(MefService.class).child(Evc.class);

        return instanceIdentifier;
    }

    @Override
    protected void remove(InstanceIdentifier<Evc> identifier, Evc del) {
        removeEvc(del);
    }

    private void removeEvc(Evc del) {
        try {
            String instanceName = del.getEvcId().getValue();

            log.info("Removing Elan Instance: " + instanceName);

            NetvirtUtils.deleteElanInstance(dataBroker, instanceName);
        } catch (final Exception e) {
            log.error("Remove evc failed !", e);
            throw new IllegalStateException("Remove evc failed.", e);
        }
    }

    private void removeUni(String instanceName, Uni uni) {
        String uniId = uni.getUniId().getValue();
        EvcUniCeVlans evcUniCeVlans = uni.getEvcUniCeVlans();

        if (evcUniCeVlans != null && !evcUniCeVlans.getEvcUniCeVlan().isEmpty()) {
            for (EvcUniCeVlan x : evcUniCeVlans.getEvcUniCeVlan()) {

                String interfaceName = NetvirtUtils.getInterfaceNameForVlan(uniId, x.getVid().toString());
                log.info("Removing elan interface: " + interfaceName);
                NetvirtUtils.deleteElanInterface(dataBroker, instanceName, interfaceName);
            }
        } else {
            log.info("Removing elan interface: " + uniId);
            NetvirtUtils.deleteElanInterface(dataBroker, instanceName, uniId);
        }
    }

    @Override
    protected void update(InstanceIdentifier<Evc> identifier, Evc original, Evc update) {
        try {
            String instanceName = update.getEvcId().getValue();

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
                        removeUni(instanceName, uni);
                    }
                }

                // Adding the new Uni which are presented in the updated List
                if (updateUni.size() > 0) {
                    for (Uni uni : updateUni) {
                        createUni(instanceName, uni);
                    }
                }
            } else if (originalUni != null && !originalUni.isEmpty()) {
                for (Uni uni : originalUni) {
                    removeUni(instanceName, uni);
                }
            }
        } catch (final Exception e) {
            log.error("Update evc failed !", e);
            throw new IllegalStateException("Update evc failed.", e);
        }
    }

    @Override
    protected void add(InstanceIdentifier<Evc> identifier, Evc add) {
        try {
            String instanceName = add.getEvcId().getValue();

            log.info("Adding elan instance: " + instanceName);
            NetvirtUtils.createElanInstance(dataBroker, instanceName);

            // Create elan interfaces
            for (Uni uni : add.getUnis().getUni()) {
                createUni(instanceName, uni);
            }
        } catch (final Exception e) {
            log.error("Add evc failed !", e);
            throw new IllegalStateException("Add evc failed.", e);
        }
    }

    private void createUni(String instanceName, Uni uni) {
        String uniId = uni.getUniId().getValue();
        EvcUniCeVlans evcUniCeVlans = uni.getEvcUniCeVlans();

        if (evcUniCeVlans != null && !evcUniCeVlans.getEvcUniCeVlan().isEmpty()) {
            for (EvcUniCeVlan x : evcUniCeVlans.getEvcUniCeVlan()) {

                String interfaceName = NetvirtUtils.getInterfaceNameForVlan(uniId, x.getVid().toString());

                log.info("Adding elan interface: " + interfaceName);
                NetvirtUtils.createElanInterface(dataBroker, instanceName, interfaceName);
            }
        } else {
            log.info("Adding elan interface: " + uniId);
            NetvirtUtils.createElanInterface(dataBroker, instanceName, uniId);
        }
    }
}
