/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.instances.ElanInstance;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElanListener extends UnimgrDataTreeChangeListener<ElanInstance> {

    private static final Logger log = LoggerFactory.getLogger(ElanListener.class);
    private ListenerRegistration<ElanListener> elanListenerRegistration;

    public ElanListener(final DataBroker dataBroker) {
        super(dataBroker);

        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<ElanInstance> dataTreeIid = new DataTreeIdentifier<>(
                    LogicalDatastoreType.CONFIGURATION, NetvirtUtils.getElanInstanceInstanceIdentifier());
            elanListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
            log.info("ElanDataTreeChangeListener created and registered");
        } catch (final Exception e) {
            log.error("Elan DataChange listener registration failed !", e);
            throw new IllegalStateException("Elan registration Listener failed.", e);
        }
    }

    @Override
    public void close() throws Exception {
        elanListenerRegistration.close();
    }

    @Override
    public void add(DataTreeModification<ElanInstance> newDataObject) {
        log.info("org.opendaylight.unimgr.mef.netvirt.ElanListener in add");
        ElanInstance instance = newDataObject.getRootNode().getDataAfter();

        String instanceName = instance.getElanInstanceName();
        if (!StringUtils.isNumericSpace(instanceName)) {
            instanceName = String.valueOf(instanceName.hashCode());
        }
        if (!MefUtils.EvcExists(dataBroker, instanceName)) {
            log.info("creating evc {}", instance);
            MefUtils.createEvcInstance(dataBroker, instanceName);
        } else {
            log.info("evc {} exists, skipping", instance);
        }

    }

    @Override
    public void remove(DataTreeModification<ElanInstance> removedDataObject) {
        log.info("org.opendaylight.unimgr.mef.netvirt.ElanListener in remove");
    }

    @Override
    public void update(DataTreeModification<ElanInstance> modifiedDataObject) {
        log.info("org.opendaylight.unimgr.mef.netvirt.ElanListener in update");
    }

}