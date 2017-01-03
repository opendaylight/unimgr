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
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

@SuppressWarnings("rawtypes")
public class DataWaitListener<D extends DataObject> extends UnimgrDataTreeChangeListener<D> {
    private static final Logger Log = LoggerFactory.getLogger(DataWaitListener.class);
    InstanceIdentifier<D> objectIdentifierId;
    private ListenerRegistration<DataWaitListener> dataWaitListenerRegistration;
    Boolean dataAvailable = false;
    private int maxRetries;
    LogicalDatastoreType logicalDatastoreType;
    DataWaitGetter<D> getData;
    private final long waitMillisec = 1000;
    

    public DataWaitListener(final DataBroker dataBroker, final InstanceIdentifier<D> objectIdentifierId,
            int maxRetiries, LogicalDatastoreType logicalDatastoreType, final DataWaitGetter<D> getData) {
        super(dataBroker);
        this.objectIdentifierId = objectIdentifierId;
        this.maxRetries = maxRetiries;
        this.logicalDatastoreType = logicalDatastoreType; 
        this.getData = getData;
        registerListener();
    }

    @SuppressWarnings("unchecked")
    public void registerListener() {
        try {
            final DataTreeIdentifier<D> dataTreeIid = new DataTreeIdentifier<D>(
                    LogicalDatastoreType.CONFIGURATION, objectIdentifierId);
            dataWaitListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
            Log.info("DataWaitListener created and registered");
        } catch (final Exception e) {
            Log.error("DataWaitListener DataChange listener registration failed !", e);
            throw new IllegalStateException("DataWaitListener registration Listener failed.", e);
        }
    }

    @Override
    public void close() throws Exception {
        dataWaitListenerRegistration.close();
    }

    @Override
    public void add(DataTreeModification<D> newDataObject) {
        if (newDataObject.getRootPath() != null && newDataObject.getRootNode() != null) {
            Log.info("data {} created", newDataObject.getRootNode().getIdentifier());
        }
        synchronized (dataAvailable) {
            dataAvailable.notifyAll();
        }
    }

    @Override
    public void remove(DataTreeModification<D> removedDataObject) {
    }

    @Override
    public void update(DataTreeModification<D> modifiedDataObject) {
        if (modifiedDataObject.getRootPath() != null && modifiedDataObject.getRootNode() != null) {
            Log.info("data {} updated", modifiedDataObject.getRootNode().getIdentifier());
        }
        synchronized (dataAvailable) {
            dataAvailable.notifyAll();
        }
    }

    private boolean dataAvailable() {
        Optional<D> objectInstance = MdsalUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                objectIdentifierId);
        if (!objectInstance.isPresent()) {
            Log.debug("Data for {} doesn't exist, waiting more", objectIdentifierId);
            return false;
        }  
        if (getData.get(objectInstance.get()) != null) {
            return true;
        }
        return false;
    }

    public boolean waitForData () {
        return waitForData(maxRetries);
    }
    
    
    public boolean waitForData(int retry) {
        synchronized (dataAvailable) {
            dataAvailable = dataAvailable();
            if (dataAvailable == true) {
                return true;
            } else if (retry <= 0) {
                return false;
            }
            try {
                dataAvailable.wait(waitMillisec);
            } catch (InterruptedException e1) {
            }
        }
        return waitForData(--retry);
    }
}
