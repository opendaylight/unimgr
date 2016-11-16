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
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.Subnets;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.subnets.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.OdlArputilService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SubnetListener extends UnimgrDataTreeChangeListener<Subnet> {
    private static final Logger Log = LoggerFactory.getLogger(SubnetListener.class);
    private ListenerRegistration<SubnetListener> subnetListenerRegistration;
    private final NotificationPublishService notificationPublishService;
    private final OdlArputilService arpUtilService;

    public SubnetListener(final DataBroker dataBroker, 
            final NotificationPublishService notPublishService, final OdlArputilService arputilService) {
        super(dataBroker);
        this.notificationPublishService = notPublishService;
        this.arpUtilService = arputilService;
        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<Subnet> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                    MefInterfaceUtils.getSubnetsListInstanceIdentifier());
            subnetListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
            Log.info("IpvcDataTreeChangeListener created and registered");
        } catch (final Exception e) {
            Log.error("Ipvc DataChange listener registration failed !", e);
            throw new IllegalStateException("Ipvc registration Listener failed.", e);
        }
    }

    @Override
    public void close() throws Exception {
        subnetListenerRegistration.close();
    }

    @Override
    public void add(DataTreeModification<Subnet> newDataObject) {
        if (newDataObject.getRootPath() != null && newDataObject.getRootNode() != null) {
            Log.info("subnet {} created", newDataObject.getRootNode().getIdentifier());
        }
    }

    @Override
    public void remove(DataTreeModification<Subnet> removedDataObject) {
        if (removedDataObject.getRootPath() != null && removedDataObject.getRootNode() != null) {
            Log.info("subnet {} deleted", removedDataObject.getRootNode().getIdentifier());
        }
    }

    @Override
    public void update(DataTreeModification<Subnet> modifiedDataObject) {
        if (modifiedDataObject.getRootPath() != null && modifiedDataObject.getRootNode() != null) {
            Log.info("subnet {} updated", modifiedDataObject.getRootNode().getIdentifier());
            Log.info("process as delete / create");
        }
    }
}

    
