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
    private Boolean dataAvailable = false;
    private final Object lockDataAvailable = new Object();
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
            final DataTreeIdentifier<D> dataTreeIid = new DataTreeIdentifier<D>(logicalDatastoreType,
                    objectIdentifierId);
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
        synchronized (lockDataAvailable) {
            lockDataAvailable.notifyAll();
        }
    }

    @Override
    public void remove(DataTreeModification<D> removedDataObject) {
        if (removedDataObject.getRootPath() != null && removedDataObject.getRootNode() != null) {
            Log.info("data {} deleted", removedDataObject.getRootNode().getIdentifier());
        }
        synchronized (lockDataAvailable) {
            lockDataAvailable.notifyAll();
        }
    }

    @Override
    public void update(DataTreeModification<D> modifiedDataObject) {
        if (modifiedDataObject.getRootPath() != null && modifiedDataObject.getRootNode() != null) {
            Log.info("data {} updated", modifiedDataObject.getRootNode().getIdentifier());
        }
        synchronized (lockDataAvailable) {
            lockDataAvailable.notifyAll();
        }
    }

    private boolean dataAvailable() {
        if (getData() != null) {
            return true;
        }
        return false;
    }

    public boolean waitForData() {
        return waitForData(maxRetries);
    }

    public boolean waitForClean() {
        return waitForClean(maxRetries);
    }

    public Object getData() {
        Optional<D> objectInstance = MdsalUtils.read(dataBroker, logicalDatastoreType, objectIdentifierId);
        if (!objectInstance.isPresent()) {
            Log.debug("Data for {} doesn't exist, waiting more", objectIdentifierId);
            return null;
        }
        return getData.get(objectInstance.get());
    }

    public boolean waitForData(int retry) {
        synchronized (lockDataAvailable) {
            dataAvailable = dataAvailable();
            if (dataAvailable == true) {
                return true;
            } else if (retry <= 0) {
                return false;
            }
            safeWaitLock();
        }
        return waitForData(--retry);
    }

    public boolean waitForClean(int retry) {
        synchronized (lockDataAvailable) {
            dataAvailable = dataAvailable();
            if (dataAvailable == false) {
                return true;
            } else if (retry <= 0) {
                return false;
            }
            safeWaitLock();
        }
        return waitForClean(--retry);
    }

    private void safeWaitLock() {
        try {
            lockDataAvailable.wait(waitMillisec);
        } catch (InterruptedException e1) {
        }
    }
}
