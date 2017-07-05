/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.common;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170531.Context;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170531.UniversalId;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170531.context.g.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170531.context.g.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170531.connectivity.context.g.Connection;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170531.connectivity.context.g.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170531.connectivity.context.g.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170531.connectivity.context.g.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170531.Context1;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170531.get.node.edge.point.details.output.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170531.node.g.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170531.node.g.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170531.node.g.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170531.topology.context.g.Topology;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170531.topology.context.g.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170531.topology.g.Node;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170531.topology.g.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170531.topology.g.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * @author bartosz.michalik@amartus.com
 */
public class NrpDao  {
    private static final Logger log = LoggerFactory.getLogger(NrpDao.class);
    private final ReadWriteTransaction tx;
    private final ReadTransaction rtx;


    public NrpDao(ReadWriteTransaction tx) {
        this.tx = tx;
        this.rtx = tx;
    }
    public NrpDao(ReadOnlyTransaction tx) {
        this.rtx = tx;
        this.tx =  null;
    }

    private Function<NodeEdgePoint, OwnedNodeEdgePoint> toNep = nep -> new OwnedNodeEdgePointBuilder(nep).build();

    public Node createSystemNode(String nodeId, List<OwnedNodeEdgePoint> neps) {
        verifyTx();
        UniversalId uuid = new UniversalId(nodeId);
        Node node = new NodeBuilder()
                .setKey(new NodeKey(uuid))
                .setUuid(uuid)
                .setOwnedNodeEdgePoint(neps)
                .build();
        tx.put(LogicalDatastoreType.OPERATIONAL, node(nodeId), node);
        return node;
    }

    private void verifyTx() {
        if(tx == null) throw new IllegalStateException("Top perform write operation read write transaction is needed");
    }

    /**
     * Update nep or add if it does not exist
     * @param nodeId node id
     * @param nep nep to update
     */
    public void updateNep(String nodeId, OwnedNodeEdgePoint nep) {
        updateNep(new UniversalId(nodeId), nep);
    }

    public void updateNep(UniversalId nodeId, OwnedNodeEdgePoint nep) {
        InstanceIdentifier<OwnedNodeEdgePoint> nodeIdent = node(nodeId).child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nep.getUuid()));
        tx.put(LogicalDatastoreType.OPERATIONAL, nodeIdent, nep);
    }

    public void removeNep(String nodeId, String nepId, boolean removeSips) {
        verifyTx();
        InstanceIdentifier<OwnedNodeEdgePoint> nepIdent = node(nodeId).child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(new UniversalId(nepId)));
        try {
            Optional<OwnedNodeEdgePoint> opt = tx.read(LogicalDatastoreType.OPERATIONAL, nepIdent).checkedGet();
            if(opt.isPresent()) {
                tx.delete(LogicalDatastoreType.OPERATIONAL,nepIdent);
                if(removeSips){
                    List<UniversalId> sips = opt.get().getMappedServiceInterfacePoint();
                    removeSips(sips == null ? null : sips.stream());
                }
            }
        } catch (ReadFailedException e) {
            log.error("Cannot read {} with id {}",OwnedNodeEdgePoint.class, nodeId);
        }
    }

    public void addSip(ServiceInterfacePoint sip) {
        verifyTx();
        tx.put(LogicalDatastoreType.OPERATIONAL,
        ctx().child(ServiceInterfacePoint.class, new ServiceInterfacePointKey(sip.getUuid())),
                sip);
    }

    public OwnedNodeEdgePoint readNep(String nodeId, String nepId) throws ReadFailedException {
        KeyedInstanceIdentifier<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> nepKey = node(nodeId).child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(new UniversalId(nepId)));
        return rtx.read(LogicalDatastoreType.OPERATIONAL, nepKey).checkedGet().orNull();
    }

    public boolean hasSip(String nepId) {
        UniversalId universalId = new UniversalId("sip:" + nepId);
        try {
            return rtx.read(LogicalDatastoreType.OPERATIONAL,
                    ctx().child(ServiceInterfacePoint.class, new ServiceInterfacePointKey(universalId))).checkedGet().isPresent();
        } catch (ReadFailedException e) {
            log.error("Cannot read sip with id {}", universalId.getValue());
        }
        return false;
    }

    public boolean hasNep(String nodeId, String nepId) throws ReadFailedException {
        return readNep(nodeId, nepId) != null;
    }

    public Topology getTopology(String uuid) throws ReadFailedException {
        Optional<Topology> topology = rtx.read(LogicalDatastoreType.OPERATIONAL, topo(uuid)).checkedGet();
        return topology.orNull();
    }

    public static InstanceIdentifier<Context> ctx() {
        return InstanceIdentifier.create(Context.class);
    }

    public static InstanceIdentifier<Topology> topo(String topoId) {
        return ctx()
                .augmentation(Context1.class)
                .child(Topology.class, new TopologyKey(new UniversalId(topoId)));
    }

    public static InstanceIdentifier<Node> node(String nodeId) {
        return node(new UniversalId(nodeId));
    }

    public static InstanceIdentifier<Node> node(UniversalId nodeId) {
        return topo(TapiConstants.PRESTO_SYSTEM_TOPO).child(Node.class, new NodeKey(nodeId));
    }

    public static InstanceIdentifier<Node> abstractNode() {
        return topo(TapiConstants.PRESTO_EXT_TOPO).child(Node.class, new NodeKey(new UniversalId(TapiConstants.PRESTO_ABSTRACT_NODE)));
    }

    public void removeSips(Stream<UniversalId>  uuids) {
        verifyTx();
        if(uuids == null) return ;
        uuids.forEach(sip -> {
            log.debug("removing ServiceInterfacePoint with id {}", sip);
            tx.delete(LogicalDatastoreType.OPERATIONAL, ctx().child(ServiceInterfacePoint.class, new ServiceInterfacePointKey(sip)));
        });
    }

    public void removeNode(String nodeId, boolean removeSips) {
        verifyTx();
        if(removeSips) {
            try {
                Optional<Node> opt = tx.read(LogicalDatastoreType.OPERATIONAL, node(nodeId)).checkedGet();
                if(opt.isPresent()) {
                    removeSips(opt.get().getOwnedNodeEdgePoint().stream().flatMap(nep -> nep.getMappedServiceInterfacePoint() == null ?
                            Stream.empty() : nep.getMappedServiceInterfacePoint().stream()
                    ));
                }
            } catch (ReadFailedException e) {
                log.error("Cannot read node with id {}", nodeId);
            }
        }

        tx.delete(LogicalDatastoreType.OPERATIONAL, node(nodeId));
    }

    public void updateAbstractNep(OwnedNodeEdgePoint nep){
        verifyTx();
        InstanceIdentifier<OwnedNodeEdgePoint> nodeIdent = abstractNode().child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nep.getUuid()));
        tx.merge(LogicalDatastoreType.OPERATIONAL, nodeIdent, nep);
    }

    public void deleteAbstractNep(OwnedNodeEdgePoint nep){
        verifyTx();
        InstanceIdentifier<OwnedNodeEdgePoint> nodeIdent = abstractNode().child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nep.getUuid()));
        tx.delete(LogicalDatastoreType.OPERATIONAL, nodeIdent);
    }

    public ConnectivityService getConnectivityService(UniversalId id) {
        try {
            return rtx.read(LogicalDatastoreType.OPERATIONAL, ctx().augmentation(org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170531.Context1.class).child(ConnectivityService.class, new ConnectivityServiceKey(id)))
                    .checkedGet().orNull();

        } catch (ReadFailedException e) {
            log.warn("reading connectivity service failed", e);
            return null;
        }
    }

    public ServiceInterfacePoint getSip(String sipId) throws ReadFailedException {
        KeyedInstanceIdentifier<ServiceInterfacePoint, ServiceInterfacePointKey> key = ctx().child(ServiceInterfacePoint.class, new ServiceInterfacePointKey(new UniversalId(sipId)));
        return rtx.read(LogicalDatastoreType.OPERATIONAL, key).checkedGet().orNull();
    }

    public ConnectivityService getConnectivityService(String id) {
        return getConnectivityService(new UniversalId(id));
    }

    public Connection getConnection(UniversalId connectionId) {
        try {
            return rtx.read(LogicalDatastoreType.OPERATIONAL, ctx().augmentation(org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170531.Context1.class).child(Connection.class, new ConnectionKey(connectionId)))
                    .checkedGet().orNull();

        } catch (ReadFailedException e) {
            log.warn("reading connectivity service failed", e);
            return null;
        }
    }
}
