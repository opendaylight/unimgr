/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.RetailSvcIdType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.unimgr.unimgr.dhcp.rev161214.UnimgrDhcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.unimgr.unimgr.dhcp.rev161214.unimgr.dhcp.UnimgrServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.unimgr.unimgr.dhcp.rev161214.unimgr.dhcp.unimgr.services.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.unimgr.unimgr.dhcp.rev161214.unimgr.dhcp.unimgr.services.network.UnimgrAllocationPool;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpAllocationPoolListener extends UnimgrDataTreeChangeListener<UnimgrAllocationPool> {
    private static final Logger Log = LoggerFactory.getLogger(DhcpAllocationPoolListener.class);
    private ListenerRegistration<DhcpAllocationPoolListener> subnetListenerRegistration;

    public DhcpAllocationPoolListener(final DataBroker dataBroker) {
        super(dataBroker);
        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<UnimgrAllocationPool> dataTreeIid = new DataTreeIdentifier<>(
                    LogicalDatastoreType.CONFIGURATION,
                    InstanceIdentifier.builder(UnimgrDhcp.class).child(UnimgrServices.class).child(Network.class)
                            .child(UnimgrAllocationPool.class).build());
            subnetListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
            Log.info("DhcpAllocationPoolListener created and registered");
        } catch (final Exception e) {
            Log.error("DhcpAllocationPoolListener registration failed !", e);
            throw new IllegalStateException("DhcpAllocationPoolListener registration Listener failed.", e);
        }
    }

    @Override
    public void close() throws Exception {
        subnetListenerRegistration.close();
    }

    @Override
    public void add(DataTreeModification<UnimgrAllocationPool> newDataObject) {
        if (newDataObject.getRootPath() != null && newDataObject.getRootNode() != null) {
            Log.info("Dhcp Allocation Pool {} created", newDataObject.getRootNode().getIdentifier());
            addDhcpAllocationPool(newDataObject);
        }
    }

    @Override
    public void remove(DataTreeModification<UnimgrAllocationPool> removedDataObject) {
        if (removedDataObject.getRootPath() != null && removedDataObject.getRootNode() != null) {
            Log.info("Dhcp Allocation Pool {} deleted", removedDataObject.getRootNode().getIdentifier());
            removeDhcpAllocationPool(removedDataObject);
        }
    }

    @Override
    public void update(DataTreeModification<UnimgrAllocationPool> modifiedDataObject) {
        if (modifiedDataObject.getRootPath() != null && modifiedDataObject.getRootNode() != null) {
            Log.info("subnet {} updated", modifiedDataObject.getRootNode().getIdentifier());
            Log.info("process as delete / create");
        }
    }

    private void addDhcpAllocationPool(DataTreeModification<UnimgrAllocationPool> newDataObject) {
        String networkId = getAllocationPoolNetworkIdFromDataTreeMod(newDataObject);
        RetailSvcIdType svcId = getAllocationPoolSvcIdFromDataTreeMod(newDataObject);
        NetvirtUtils.createDhcpAllocationPool(dataBroker, newDataObject.getRootNode().getDataAfter(), networkId, svcId);
    }

    private void removeDhcpAllocationPool(DataTreeModification<UnimgrAllocationPool> removedDataObject) {
        String networkId = getAllocationPoolNetworkIdFromDataTreeMod(removedDataObject);
        RetailSvcIdType svcId = getAllocationPoolSvcIdFromDataTreeMod(removedDataObject);
        IpPrefix subnet = getAllocationPoolSubnetFromDataTreeMod(removedDataObject);
        NetvirtUtils.removeDhcpAllocationPool(dataBroker, networkId, svcId, subnet);
    }

    private IpPrefix getAllocationPoolSubnetFromDataTreeMod(
            DataTreeModification<UnimgrAllocationPool> dataObject) {
        return dataObject.getRootPath().getRootIdentifier().firstKeyOf(UnimgrAllocationPool.class)
                .getSubnet();
    }

    private RetailSvcIdType getAllocationPoolSvcIdFromDataTreeMod(
            DataTreeModification<UnimgrAllocationPool> dataObject) {
        return dataObject.getRootPath().getRootIdentifier().firstKeyOf(UnimgrServices.class).getSvcId();
    }

    private String getAllocationPoolNetworkIdFromDataTreeMod(
            DataTreeModification<UnimgrAllocationPool> dataObject) {
        return dataObject.getRootPath().getRootIdentifier().firstKeyOf(Network.class).getNetworkId();
    }

}
