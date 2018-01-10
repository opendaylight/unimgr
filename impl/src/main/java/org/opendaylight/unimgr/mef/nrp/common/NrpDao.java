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
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Context;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.context.attrs.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.context.attrs.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.connectivity.context.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.context.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * @author bartosz.michalik@amartus.com
 */
public class NrpDao  {
    private static final Logger LOG = LoggerFactory.getLogger(NrpDao.class);
    private final ReadWriteTransaction tx;
    private final ReadTransaction rtx;


    public NrpDao(ReadWriteTransaction tx) {
        if(tx == null) throw new NullPointerException();
        this.tx = tx;
        this.rtx = tx;
    }
    public NrpDao(ReadOnlyTransaction tx) {
        this.rtx = tx;
        this.tx =  null;
    }

    public Node createSystemNode(String nodeId, List<OwnedNodeEdgePoint> neps) {
        verifyTx();
        Uuid uuid = new Uuid(nodeId);
        Node node = new NodeBuilder()
                .setKey(new NodeKey(uuid))
                .setUuid(uuid)
                .setOwnedNodeEdgePoint(neps)
                .build();
        tx.put(LogicalDatastoreType.OPERATIONAL, node(nodeId), node);
        return node;
    }

    private void verifyTx() {
        if (tx == null) {
            throw new IllegalStateException("Top perform write operation read write transaction is needed");
        }
    }

    /**
     * Update nep or add if it does not exist.
     * @param nodeId node id
     * @param nep nep to update
     */
    public void updateNep(String nodeId, OwnedNodeEdgePoint nep) {
        updateNep(new Uuid(nodeId), nep);
    }

    public void updateNep(Uuid nodeId, OwnedNodeEdgePoint nep) {
        verifyTx();
        InstanceIdentifier<OwnedNodeEdgePoint> nodeIdent = node(nodeId).child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nep.getUuid()));
        tx.put(LogicalDatastoreType.OPERATIONAL, nodeIdent, nep);
    }

    public void removeNep(String nodeId, String nepId, boolean removeSips) {
        verifyTx();
        InstanceIdentifier<OwnedNodeEdgePoint> nepIdent = node(nodeId).child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(new Uuid(nepId)));
        try {
            Optional<OwnedNodeEdgePoint> opt = tx.read(LogicalDatastoreType.OPERATIONAL, nepIdent).checkedGet();
            if (opt.isPresent()) {
                tx.delete(LogicalDatastoreType.OPERATIONAL,nepIdent);
                if (removeSips) {
                    List<Uuid> sips = opt.get().getMappedServiceInterfacePoint();
                    removeSips(sips == null ? null : sips.stream());
                }
            }
        } catch (ReadFailedException e) {
            LOG.error("Cannot read {} with id {}",OwnedNodeEdgePoint.class, nodeId);
        }
    }

    public void addSip(ServiceInterfacePoint sip) {
        verifyTx();
        tx.put(LogicalDatastoreType.OPERATIONAL,
            ctx().child(ServiceInterfacePoint.class, new ServiceInterfacePointKey(sip.getUuid())),
                sip);
    }

    public OwnedNodeEdgePoint readNep(String nodeId, String nepId) throws ReadFailedException {
        KeyedInstanceIdentifier<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> nepKey = node(nodeId).child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(new Uuid(nepId)));
        return rtx.read(LogicalDatastoreType.OPERATIONAL, nepKey).checkedGet().orNull();
    }

    public boolean hasSip(String nepId) {
        Uuid universalId = new Uuid("sip:" + nepId);
        try {
            return rtx.read(LogicalDatastoreType.OPERATIONAL,
                    ctx().child(ServiceInterfacePoint.class, new ServiceInterfacePointKey(universalId))).checkedGet().isPresent();
        } catch (ReadFailedException e) {
            LOG.error("Cannot read sip with id {}", universalId.getValue());
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
                .child(Topology.class, new TopologyKey(new Uuid(topoId)));
    }

    public static InstanceIdentifier<Node> node(String nodeId) {
        return node(new Uuid(nodeId));
    }

    public static InstanceIdentifier<Node> node(Uuid nodeId) {
        return topo(TapiConstants.PRESTO_SYSTEM_TOPO).child(Node.class, new NodeKey(nodeId));
    }

    public static InstanceIdentifier<Node> abstractNode() {
        return topo(TapiConstants.PRESTO_EXT_TOPO).child(Node.class, new NodeKey(new Uuid(TapiConstants.PRESTO_ABSTRACT_NODE)));
    }

    public void removeSip(Uuid uuid) {
        removeSips(Stream.of(uuid));
    }

    public void removeSips(Stream<Uuid>  uuids) {
        verifyTx();
        if (uuids == null) {
            return;
        }
        uuids.forEach(sip -> {
            LOG.debug("removing ServiceInterfacePoint with id {}", sip);
            tx.delete(LogicalDatastoreType.OPERATIONAL, ctx().child(ServiceInterfacePoint.class, new ServiceInterfacePointKey(sip)));
        });
    }

    public void removeNode(String nodeId, boolean removeSips) {
        verifyTx();
        if (removeSips) {
            try {
                Optional<Node> opt = tx.read(LogicalDatastoreType.OPERATIONAL, node(nodeId)).checkedGet();
                if (opt.isPresent()) {
                    removeSips(opt.get().getOwnedNodeEdgePoint().stream().flatMap(nep -> nep.getMappedServiceInterfacePoint() == null
                                                                                  ? Stream.empty()
                                                                                  : nep.getMappedServiceInterfacePoint().stream()
                    ));
                }
            } catch (ReadFailedException e) {
                LOG.error("Cannot read node with id {}", nodeId);
            }
        }

        tx.delete(LogicalDatastoreType.OPERATIONAL, node(nodeId));
    }

    public void updateAbstractNep(OwnedNodeEdgePoint nep) {
        verifyTx();
        InstanceIdentifier<OwnedNodeEdgePoint> nodeIdent = abstractNode().child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nep.getUuid()));
        tx.merge(LogicalDatastoreType.OPERATIONAL, nodeIdent, nep);
    }

    public void deleteAbstractNep(OwnedNodeEdgePoint nep) {
        verifyTx();
        InstanceIdentifier<OwnedNodeEdgePoint> nodeIdent = abstractNode().child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nep.getUuid()));
        tx.delete(LogicalDatastoreType.OPERATIONAL, nodeIdent);
    }

    public List<ConnectivityService> getConnectivityServiceList() {
        try {
            org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.Context1 connections = rtx.read(LogicalDatastoreType.OPERATIONAL,
                    ctx().augmentation(org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.Context1.class))
                    .checkedGet().orNull();
            return connections == null ? null : connections.getConnectivityService();
        } catch (ReadFailedException e) {
            LOG.warn("reading connectivity services failed", e);
            return null;
        }
    }

    public ConnectivityService getConnectivityService(Uuid id) {
        try {
            return rtx.read(LogicalDatastoreType.OPERATIONAL, ctx().augmentation(org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.Context1.class).child(ConnectivityService.class, new ConnectivityServiceKey(id)))
                    .checkedGet().orNull();

        } catch (ReadFailedException e) {
            LOG.warn("reading connectivity service failed", e);
            return null;
        }
    }

    public ServiceInterfacePoint getSip(String sipId) throws ReadFailedException {
        KeyedInstanceIdentifier<ServiceInterfacePoint, ServiceInterfacePointKey> key = ctx().child(ServiceInterfacePoint.class, new ServiceInterfacePointKey(new Uuid(sipId)));
        return rtx.read(LogicalDatastoreType.OPERATIONAL, key).checkedGet().orNull();
    }

    public ConnectivityService getConnectivityService(String id) {
        return getConnectivityService(new Uuid(id));
    }

    public Connection getConnection(Uuid connectionId) {
        try {
            return rtx.read(LogicalDatastoreType.OPERATIONAL, ctx().augmentation(org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.Context1.class).child(Connection.class, new ConnectionKey(connectionId)))
                    .checkedGet().orNull();

        } catch (ReadFailedException e) {
            LOG.warn("reading connectivity service failed", e);
            return null;
        }
    }
}
