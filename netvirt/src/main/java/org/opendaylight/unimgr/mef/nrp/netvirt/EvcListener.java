/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.netvirt;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.AbstractDataChangeListener;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.MefServices;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.MefService;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.Evc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.ElanInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.ElanInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.instances.ElanInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.instances.ElanInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.instances.ElanInstanceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.interfaces.ElanInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.interfaces.ElanInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.interfaces.ElanInterfaceKey;
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

    }

    @Override
    protected void update(InstanceIdentifier<Evc> identifier, Evc original, Evc update) {
    }

    @Override
    protected void add(InstanceIdentifier<Evc> identifier, Evc add) {

        try {
            String instanceName = add.getEvcId().getValue();
            NetvirtUtils.createElanInstance(dataBroker, instanceName);

            int i = 0;
            // Create elan interfaces
            for (Object uni : add.getUnis().getUni()) {
                i = NetvirtUtils.createElanInterface(dataBroker, instanceName, i);
            }
        } catch (final Exception e) {
            log.error("Add evc failed !", e);
            throw new IllegalStateException("Add evc failed.", e);
        }
    }
}
