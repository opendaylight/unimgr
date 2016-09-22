/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.MefServices;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.MefService;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class TenantUniListener extends UnimgrDataTreeChangeListener<Uni> {

    private static final Logger log = LoggerFactory.getLogger(TenantUniListener.class);
    private ListenerRegistration<TenantUniListener> evcListenerRegistration;

    public TenantUniListener(final DataBroker dataBroker) {
        super(dataBroker);

        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<Uni> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                    MefUtils.getUniListInterfaceInstanceIdentifier());
            evcListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
            log.info("TenantUniListener created and registered");
        } catch (final Exception e) {
            log.error("TenantUniListener registration failed !", e);
            throw new IllegalStateException("Evc registration Listener failed.", e);
        }
    }

    @Override
    public void close() throws Exception {
        evcListenerRegistration.close();
    }

    @Override
    public void add(DataTreeModification<Uni> newDataObject) {
        log.info("received add Uni notification");
        handleUniChanged(newDataObject.getRootNode().getDataAfter());
    }

    @Override
    public void remove(DataTreeModification<Uni> removedDataObject) {
    }

    @Override
    public void update(DataTreeModification<Uni> modifiedDataObject) {
        log.info("received update Uni notification");
        handleUniChanged(modifiedDataObject.getRootNode().getDataAfter());
    }

    private void handleUniChanged(Uni uni) {
        if (!TenantEnhancerUtils.isUniTenanted(uni)) {
            return;
        }

        String tenant = uni.getTenantId();

        Optional<MefServices> optionalServices = MdsalUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                MefUtils.getMefServicesInstanceIdentifier());
        if (!optionalServices.isPresent()) {
            return;
        }
        for (MefService service : optionalServices.get().getMefService()) {
            for (org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.Uni serviceUni : service
                    .getEvc().getUnis().getUni()) {
                if (!TenantEnhancerUtils.isServiceTenanted(service) && serviceUni.getUniId().equals(uni.getUniId())) {
                    log.info("instance identifier is {}", MefUtils.getMefServiceInstanceIdentifier(service.getSvcId()));
                    TenantEnhancerUtils.updateService(dataBroker, tenant, service);
                }
            }
        }
    }
}
