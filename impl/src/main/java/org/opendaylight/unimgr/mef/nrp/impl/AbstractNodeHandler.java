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
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.context.TopologyKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TopologyDataHandler listens to presto system topology and propagate significant changes to presto system topology.
 *
 * @author marek.ryznar@amartus.com
 */
public class AbstractNodeHandler implements DataTreeChangeListener<Topology> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractNodeHandler.class);
    private static final InstanceIdentifier<Topology> NRP_TOPOLOGY_SYSTEM_IID = InstanceIdentifier
            .create(Context.class)
            .augmentation(Context1.class)
            .child(Topology.class, new TopologyKey(new Uuid(TapiConstants.PRESTO_SYSTEM_TOPO)));
    private ListenerRegistration<AbstractNodeHandler> registration;

    private final DataBroker dataBroker;

    public AbstractNodeHandler(DataBroker dataBroker) {
        Objects.requireNonNull(dataBroker);
        this.dataBroker = dataBroker;
    }

    public void init() {
        registration = dataBroker
                .registerDataTreeChangeListener(
                        DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, NRP_TOPOLOGY_SYSTEM_IID), this);

        LOG.debug("AbstractNodeHandler registered: {}", registration);
    }

    public void close() {
        if (registration != null) {
            registration.close();
            LOG.debug("AbstractNodeHandler closed");
        }
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<Topology>> collection) {


        List<OwnedNodeEdgePoint> toUpdateNeps =
                collection.stream()
                        .map(DataTreeModification::getRootNode)
                        .flatMap(topo -> topo.getModifiedChildren().stream())
                        .flatMap(node -> node.getModifiedChildren().stream())
                        .filter(this::isNep)
                        .filter(this::checkIfUpdated)
                        .map(nep -> (OwnedNodeEdgePoint) nep.getDataAfter())
                        .collect(Collectors.toList());


        List<OwnedNodeEdgePoint> toDeleteNeps = collection.stream()
                .map(DataTreeModification::getRootNode)
                .flatMap(topo -> topo.getModifiedChildren().stream())
                .flatMap(node -> node.getModifiedChildren().stream())
                .filter(this::isNep)
                .filter(this::checkIfDeleted)
                .map(nep -> (OwnedNodeEdgePoint) nep.getDataBefore())
                .collect(Collectors.toList());

        final ReadWriteTransaction topoTx = dataBroker.newReadWriteTransaction();
        NrpDao dao = new NrpDao(topoTx);

        toUpdateNeps
                .forEach(dao::updateAbstractNep);

        toDeleteNeps
                .forEach(dao::deleteAbstractNep);

        Futures.addCallback(topoTx.commit(), new FutureCallback<CommitInfo>() {

            @Override
            public void onSuccess(@Nullable CommitInfo result) {
                LOG.info("Abstract TAPI node updated successful");
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOG.warn("Abstract TAPI node update failed due to an error", throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    private boolean isNep(DataObjectModification<?> dataObjectModificationNep) {
        return OwnedNodeEdgePoint.class.isAssignableFrom(dataObjectModificationNep.getDataType());
    }

    private boolean checkIfDeleted(DataObjectModification<?> dataObjectModificationNep) {
        OwnedNodeEdgePoint before = (OwnedNodeEdgePoint) dataObjectModificationNep.getDataBefore();
        OwnedNodeEdgePoint after = (OwnedNodeEdgePoint) dataObjectModificationNep.getDataAfter();

        if (before != null) {
            if (after == null) {
                return true;
            }
            if (hasSip(before)) {
                return ! hasSip(after);
            }
        }

        return false;
    }

    private boolean checkIfUpdated(DataObjectModification<?> dataObjectModificationNep) {
        OwnedNodeEdgePoint before = (OwnedNodeEdgePoint) dataObjectModificationNep.getDataBefore();
        OwnedNodeEdgePoint after = (OwnedNodeEdgePoint) dataObjectModificationNep.getDataAfter();
        if (after == null) {
            return false;
        }
        //added
        if (before == null) {
            return hasSip(after);
        }
        //updated
        return hasSip(after);

    }

    private boolean hasSip(OwnedNodeEdgePoint nep) {
        return nep.getMappedServiceInterfacePoint() != null && !nep.getMappedServiceInterfacePoint().isEmpty();
    }
}
