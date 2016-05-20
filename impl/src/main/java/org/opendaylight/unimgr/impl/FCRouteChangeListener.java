/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.impl;

import org.mef.nrp.impl.ActivationDriverRepoService;
import org.mef.nrp.impl.ActivationDriverRepoServiceImpl;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.FcRouteList;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.fcroutelist.FcRoute;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * NRP top level change model listener
 * @author bartosz.michalik@amartus.com
 */
public class FCRouteChangeListener implements DataTreeChangeListener<FcRoute>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(FCRouteChangeListener.class);
    private final ListenerRegistration<FCRouteChangeListener> listener;
    private final FcRouteActivatorService routeActivator;

    private volatile ActivationDriverRepoService activationRepoService;

    public FCRouteChangeListener(DataBroker dataBroker) {
        final InstanceIdentifier<FcRoute> fwPath = getFwConstructsPath();
        final DataTreeIdentifier<FcRoute> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, fwPath);
        listener = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
        this.routeActivator = new FcRouteActivatorService();

        LOG.info("FCRouteChangeListener created and registered");
    }

    /**
     * Basic Implementation of DataTreeChange Listener to execute add, update or remove command
     * based on the data object modification type.
     */
    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<FcRoute>> collection) {
        //TODO add lock for concurrency support
        if(activationRepoService == null) {
            //TODO improve comment
            LOG.warn("ActivationDriverRepoService is not ready yet - ignoring request");
            return;
        }
        for (final DataTreeModification<FcRoute> change : collection) {
            final DataObjectModification<FcRoute> root = change.getRootNode();
            switch (root.getModificationType()) {
                case SUBTREE_MODIFIED:
                    update(change);
                    break;
                case WRITE:
                    //TO overcome whole subtree change event
                    boolean update = change.getRootNode().getDataBefore() != null;

                    if(update) {
                        update(change);
                    } else {
                        add(change);
                    }

                    break;
                case DELETE:
                    remove(change);
                    break;
            }
        }
    }

    public void add(DataTreeModification<FcRoute> newDataObject) {
        LOG.debug("FcRoute add event received");
        routeActivator.activate(newDataObject.getRootNode().getDataAfter());

    }

    public void remove(DataTreeModification<FcRoute> removedDataObject) {
        LOG.debug("FcRoute remove event received");
        routeActivator.deactivate(removedDataObject.getRootNode().getDataBefore());

    }

    public void update(DataTreeModification<FcRoute> modifiedDataObject) {
        LOG.debug("FcRoute update event received");
        //TODO for the moment transactional nature of this action is ignored :P
        routeActivator.deactivate(modifiedDataObject.getRootNode().getDataBefore());
        routeActivator.activate(modifiedDataObject.getRootNode().getDataAfter());
    }

    @Override
    public void close() throws Exception {
        listener.close();
    }

    public InstanceIdentifier<FcRoute> getFwConstructsPath() {
        final InstanceIdentifier<FcRoute> path = InstanceIdentifier
                .builder(FcRouteList.class)
                .child(FcRoute.class).build();

        return path;

    }

    public void setActivationDriverRepoService(ActivationDriverRepoService service) {
        this.activationRepoService = service;
        routeActivator.setActivationRepoService(service);
    }

    public void unsetActivationDriverRepoService() {
        this.activationRepoService = null;
        routeActivator.unsetActivationRepoService();
    }
}
