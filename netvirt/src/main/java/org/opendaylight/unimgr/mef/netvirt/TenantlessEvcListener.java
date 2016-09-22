/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.Evc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.MefService;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.Uni;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class TenantlessEvcListener extends UnimgrDataTreeChangeListener<MefService> {

    private static final Logger log = LoggerFactory.getLogger(TenantlessEvcListener.class);
    private ListenerRegistration<TenantlessEvcListener> evcListenerRegistration;

    public TenantlessEvcListener(final DataBroker dataBroker) {
        super(dataBroker);

        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<MefService> dataTreeIid = new DataTreeIdentifier<>(
                    LogicalDatastoreType.CONFIGURATION, MefUtils.getMefServiceInstanceIdentifier());
            evcListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
            log.info("TenantlessEvcListener created and registered");
        } catch (final Exception e) {
            log.error("TenantlessEvcListener registration failed !", e);
            throw new IllegalStateException("Evc registration Listener failed.", e);
        }
    }

    @Override
    public void close() throws Exception {
        evcListenerRegistration.close();
    }

    @Override
    public void add(DataTreeModification<MefService> newDataObject) {
        if (newDataObject.getRootPath() != null && newDataObject.getRootNode() != null) {
            log.info("service {} created", newDataObject.getRootNode().getIdentifier());
            handleService(newDataObject.getRootNode().getDataAfter());
        }
    }

    @Override
    public void remove(DataTreeModification<MefService> removedDataObject) {
    }

    @Override
    public void update(DataTreeModification<MefService> modifiedDataObject) {
        if (modifiedDataObject.getRootPath() != null && modifiedDataObject.getRootNode() != null) {
            log.info("service {} updated", modifiedDataObject.getRootNode().getIdentifier());
            handleService(modifiedDataObject.getRootNode().getDataAfter());
        }
    }

    private void handleService(MefService service) {
        if (TenantEnhancerUtils.isServiceTenanted(service)) {
            log.info("Service {} is already connected to a Service", service.getSvcId().getValue());
            return;
        }
        Evc evc = service.getEvc();
        if (evc.getUnis() == null) {
            log.info("No UNI's in service {}, exiting", service.getSvcId().getValue());
            return;
        }
        List<Uni> unis = evc.getUnis().getUni();
        for (Uni uni : unis) {
            Optional<org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni> optonalUniInterface = MdsalUtils
                    .read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                            MefUtils.getUniInstanceIdentifier(uni.getUniId().getValue()));
            if (optonalUniInterface.isPresent()) {
                org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni uniInterface = optonalUniInterface
                        .get();
                if (TenantEnhancerUtils.isUniTenanted(uniInterface)) {
                    String tenant = uniInterface.getTenantId();
                    log.info("updating service {} with tenant {}", service.getSvcId().getValue(), tenant);
                    TenantEnhancerUtils.updateService(dataBroker, tenant, service);
                    return;
                }
            } else {
                log.info("Couldn't find uni {}", uni.getUniId());
            }
        }
    }
}