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
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniAwareListener extends UnimgrDataTreeChangeListener<Uni> {
    private static final Logger Log = LoggerFactory.getLogger(UniAwareListener.class);
    private ListenerRegistration<UniAwareListener> uniListenerRegistration;
    IUniAwareService serviceSubscribe;

    public UniAwareListener(final DataBroker dataBroker, final IUniAwareService serviceSubscribe) {
        super(dataBroker);
        this.serviceSubscribe = serviceSubscribe;
        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<Uni> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                    MefInterfaceUtils.getUniListInstanceIdentifier());
            uniListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
            Log.info("UniAwareListener created and registered for service {}", serviceSubscribe);
        } catch (final Exception e) {
            Log.error("UniAwareListener registration failed !", e);
            throw new IllegalStateException("UniAwareListener failed.", e);
        }
    }

    @Override
    public void close() throws Exception {
        uniListenerRegistration.close();
    }

    @Override
    public void add(DataTreeModification<Uni> newDataObject) {
        if (newDataObject.getRootPath() != null && newDataObject.getRootNode() != null) {
            Log.info("Uni {} is operational", newDataObject.getRootNode().getIdentifier());
            // Uni Id is same in interface and service
            serviceSubscribe.connectUni(newDataObject.getRootNode().getDataAfter().getUniId().getValue());
        }
    }

    @Override
    public void remove(DataTreeModification<Uni> removedDataObject) {
        if (removedDataObject.getRootPath() != null && removedDataObject.getRootNode() != null) {
            Log.info("Remove Uni {} from operational", removedDataObject.getRootNode().getIdentifier());
            // Uni Id is same in interface and service
            serviceSubscribe.disconnectUni(removedDataObject.getRootNode().getDataBefore().getUniId().getValue());
        }
    }

    @Override
    public void update(DataTreeModification<Uni> modifiedDataObject) {
    }
}
