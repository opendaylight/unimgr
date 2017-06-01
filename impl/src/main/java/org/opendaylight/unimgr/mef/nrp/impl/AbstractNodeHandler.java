/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.impl;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.Context;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.UniversalId;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170227.Context1;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170227.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170227.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170227.topology.context.TopologyKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * TopologyDataHandler listens to presto system topology and propagate significant changes to presto system topology.
 *
 * @author marek.ryznar@amartus.com
 */
public class AbstractNodeHandler implements DataTreeChangeListener<Topology> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractNodeHandler.class);
    private static final InstanceIdentifier NRP_TOPOLOGY_SYSTEM_IID = InstanceIdentifier
            .create(Context.class)
            .augmentation(Context1.class)
            .child(Topology.class, new TopologyKey(new UniversalId(TapiConstants.PRESTO_SYSTEM_TOPO)));
    private ListenerRegistration<AbstractNodeHandler> registration;

    private final DataBroker dataBroker;

    public AbstractNodeHandler(DataBroker dataBroker){
        Objects.requireNonNull(dataBroker);
        this.dataBroker = dataBroker;
    }

    public void init(){
        registration = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, NRP_TOPOLOGY_SYSTEM_IID), this);
    }

    public void close(){
        if (registration!=null){
            registration.close();
        }
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<Topology>> collection) {

        List<OwnedNodeEdgePoint> toUpdateNeps =
                collection.stream()
                        .map(DataTreeModification::getRootNode)
                        .flatMap(topo -> topo.getModifiedChildren().stream())
                        .flatMap(node -> node.getModifiedChildren().stream())
                        .filter(this::checkIfUpdated)
                        .map(nep -> (OwnedNodeEdgePoint) nep.getDataAfter())
                        .collect(Collectors.toList());


        List<OwnedNodeEdgePoint> toDeleteNeps = collection.stream()
                .map(DataTreeModification::getRootNode)
                .flatMap(topo -> topo.getModifiedChildren().stream())
                .flatMap(node -> node.getModifiedChildren().stream())
                .filter(this::checkIfDeleted)
                .map(nep -> (OwnedNodeEdgePoint) nep.getDataBefore())
                .collect(Collectors.toList());

        final ReadWriteTransaction topoTx = dataBroker.newReadWriteTransaction();
        NrpDao dao = new NrpDao(topoTx);

        toUpdateNeps
                .forEach(dao::updateAbstractNep);

        toDeleteNeps
                .forEach(dao::deleteAbstractNep);

        Futures.addCallback(topoTx.submit(), new FutureCallback<Void>() {

            @Override
            public void onSuccess(@Nullable Void result) {
                LOG.info("Abstract TAPI node upadate successful");
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.warn("Abstract TAPI node upadate failed due to an error", t);
            }
        });
    }

    private boolean checkIfDeleted(DataObjectModification dataObjectModificationNep) {
        OwnedNodeEdgePoint b = (OwnedNodeEdgePoint) dataObjectModificationNep.getDataBefore();
        OwnedNodeEdgePoint a = (OwnedNodeEdgePoint) dataObjectModificationNep.getDataAfter();

        if(b != null) {
            if(a == null) return true;
            if(hasSip(b)) {
              return ! hasSip(a);
            }
        }

        return false;
    }

    private boolean checkIfUpdated(DataObjectModification dataObjectModificationNep){
        OwnedNodeEdgePoint before = (OwnedNodeEdgePoint) dataObjectModificationNep.getDataBefore();
        OwnedNodeEdgePoint after = (OwnedNodeEdgePoint) dataObjectModificationNep.getDataAfter();
        if(after == null) return false;
        //added
        if(before == null) {
            return hasSip(after);
        }
        //updated
        return hasSip(after);

    }

    private boolean hasSip(OwnedNodeEdgePoint nep) {
        return nep.getMappedServiceInterfacePoint() != null && !nep.getMappedServiceInterfacePoint().isEmpty();
    }
}
