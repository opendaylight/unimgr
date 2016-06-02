/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.impl;

import java.util.Collection;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverRepoService;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.FcRouteList;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.fcroutelist.FcRoute;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NRP top level change model listener
 * @author bartosz.michalik@amartus.com
 */
public class FcRouteChangeListener implements DataTreeChangeListener<FcRoute>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(FcRouteChangeListener.class);
    private final ListenerRegistration<FcRouteChangeListener> listener;
    private final FcRouteActivatorService routeActivator;

    private final ActivationDriverRepoService activationRepoService;

    public FcRouteChangeListener(DataBroker dataBroker, ActivationDriverRepoService activationRepoService) {
        this.activationRepoService = activationRepoService;
        routeActivator = new FcRouteActivatorService(activationRepoService);

        final InstanceIdentifier<FcRoute> fwPath = getFwConstructsPath();
        final DataTreeIdentifier<FcRoute> dataTreeIid =
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, fwPath);
        listener = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);

        LOG.info("FcRouteChangeListener created and registered");
    }

    /**
     * Basic Implementation of DataTreeChange Listener to execute add, update or remove command
     * based on the data object modification type.
     */
    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<FcRoute>> collection) {
        //TODO add lock for concurrency support
        if (activationRepoService == null) {
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

                    if (update) {
                        update(change);
                    } else {
                        add(change);
                    }

                    break;
                case DELETE:
                    remove(change);
                    break;
                default:
                    break;
            }
        }
    }

    protected void add(DataTreeModification<FcRoute> newDataObject) {
        //TODO: Refine the logged addition
        LOG.debug("FcRoute add event received {}", newDataObject);
        routeActivator.activate(newDataObject.getRootNode().getDataAfter());

    }

    protected void remove(DataTreeModification<FcRoute> removedDataObject) {
        //TODO: Refine the logged removal
        LOG.debug("FcRoute remove event received {}", removedDataObject);
        routeActivator.deactivate(removedDataObject.getRootNode().getDataBefore());

    }

    protected void update(DataTreeModification<FcRoute> modifiedDataObject) {
        //TODO: Refine the logged modification
        LOG.debug("FcRoute update event received {}", modifiedDataObject);

        //TODO for the moment transactional nature of this action is ignored :P
        routeActivator.deactivate(modifiedDataObject.getRootNode().getDataBefore());
        routeActivator.activate(modifiedDataObject.getRootNode().getDataAfter());
    }

    @Override
    public void close() {
        listener.close();
    }

    private InstanceIdentifier<FcRoute> getFwConstructsPath() {
        final InstanceIdentifier<FcRoute> path = InstanceIdentifier
                .builder(FcRouteList.class)
                .child(FcRoute.class).build();

        return path;
    }
}
