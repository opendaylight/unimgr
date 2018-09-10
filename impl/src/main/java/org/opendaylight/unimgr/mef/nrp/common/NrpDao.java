/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.common;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Stream;

import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint1;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint7;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.NodeAdiAugmentation;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.NodeAdiAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.NodeSvmAugmentation;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.NodeSvmAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.context.topology.node.ServiceVlanMap;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.ServiceInterfacePointRef;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ConnectionEndPointRef;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.OwnedNodeEdgePoint1Builder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.cep.list.ConnectionEndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connection.ConnectionEndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.EndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.OwnedNodeEdgePointRef;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.context.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Nrp data access methods to simplify interaction with model.
 * @author bartosz.michalik@amartus.com
 */
public class NrpDao  {
    private static final Logger LOG = LoggerFactory.getLogger(NrpDao.class);
    private static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang
            .tapi.connectivity.rev180307.Context1> CS_CTX
            = ctx().augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang
            .tapi.connectivity.rev180307.Context1.class);

    private final ReadWriteTransaction tx;
    private final ReadTransaction rtx;

    public NrpDao(ReadWriteTransaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }
        this.tx = tx;
        this.rtx = tx;
    }

    public NrpDao(ReadTransaction tx) {
        this.rtx = tx;
        this.tx =  null;
    }

    public Node createNode(String topologyId, String nodeId, LayerProtocolName name, List<OwnedNodeEdgePoint> neps) {
        return createNode(topologyId, nodeId, nodeId, name, neps);
    }

    public Node createNode(String topologyId, String nodeId, String activationDriverId,
                       LayerProtocolName name, List<OwnedNodeEdgePoint> neps) {
        return createNode(topologyId, nodeId, activationDriverId, name, neps,null);
    }

    public Node createNode(String topologyId, String nodeId, String activationDriverId,
                       LayerProtocolName name, List<OwnedNodeEdgePoint> neps, List<ServiceVlanMap> serviceVlanMapList) {
        verifyTx();
        assert tx != null;
        Uuid uuid = new Uuid(nodeId);

        NodeBuilder nb = new NodeBuilder()
                .withKey(new NodeKey(uuid))
                .setUuid(uuid)
                .setLayerProtocolName(Collections.singletonList(name))
                .setOwnedNodeEdgePoint(neps)
                .setCostCharacteristic(Collections.emptyList())
                .setLatencyCharacteristic(Collections.emptyList())
                .addAugmentation(NodeAdiAugmentation.class, new NodeAdiAugmentationBuilder()
                        .setActivationDriverId(activationDriverId).build());

        Node node = serviceVlanMapList == null ? nb.build() : nb.addAugmentation(NodeSvmAugmentation.class,
                new NodeSvmAugmentationBuilder().setServiceVlanMap(serviceVlanMapList).build()).build();
        tx.put(LogicalDatastoreType.OPERATIONAL, node(nodeId), node);
        return node;
    }

    /**
     * Update node or add if it does not exist.
     * @param node to be updated (or added)
     * <p>
     * Note: Please bare in mind that all external changes between reading/modifying
     *             the node given as parameter and writing it are silently lost
     * </p>
     */
    public void updateNode(Node node) {
        verifyTx();
        tx.put(LogicalDatastoreType.OPERATIONAL, node(node.getUuid()), node);
    }

    private void verifyTx() {
        if (tx == null) {
            throw new IllegalStateException("To perform write operation read write transaction is needed");
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
        InstanceIdentifier<OwnedNodeEdgePoint> nodeIdent = node(nodeId).child(OwnedNodeEdgePoint.class,
                new OwnedNodeEdgePointKey(nep.getUuid()));
        assert tx != null;
        tx.put(LogicalDatastoreType.OPERATIONAL, nodeIdent, nep);
    }

    public void removeNep(String nodeId, String nepId, boolean removeSips) {
        verifyTx();
        assert tx != null;
        InstanceIdentifier<OwnedNodeEdgePoint> nepIdent = node(nodeId).child(OwnedNodeEdgePoint.class,
                new OwnedNodeEdgePointKey(new Uuid(nepId)));
        try {
            Optional<OwnedNodeEdgePoint> opt = rtx.read(LogicalDatastoreType.OPERATIONAL, nepIdent).get();
            if (opt.isPresent()) {
                tx.delete(LogicalDatastoreType.OPERATIONAL,nepIdent);
                if (removeSips) {
                    Stream<Uuid> sips = opt.get().getMappedServiceInterfacePoint().stream()
                            .map(ServiceInterfacePointRef::getServiceInterfacePointId);
                    removeSips(sips);
                }
            }
        } catch (ExecutionException e) {
            LOG.error("Cannot read {} with id {}",OwnedNodeEdgePoint.class, nodeId);
        } catch (InterruptedException e) {
            LOG.error("Interrupted during read {} with id {}",OwnedNodeEdgePoint.class, nodeId);
        }
    }

    public void addSip(ServiceInterfacePoint sip) {
        verifyTx();
        assert tx != null;
        tx.put(LogicalDatastoreType.OPERATIONAL,
            ctx().child(ServiceInterfacePoint.class, new ServiceInterfacePointKey(sip.getUuid())),
                sip);
    }

    private Function<OwnedNodeEdgePointRef, KeyedInstanceIdentifier<OwnedNodeEdgePoint, OwnedNodeEdgePointKey>> toPath =
        ref -> topo(ref.getTopologyId())
                .child(Node.class, new NodeKey(new Uuid(ref.getNodeId())))
                .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(ref.getOwnedNodeEdgePointId()));

    public org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
            .connection.ConnectionEndPoint addConnectionEndPoint(OwnedNodeEdgePointRef ref, ConnectionEndPoint cep) {
        verifyTx();
        assert tx != null;
        OwnedNodeEdgePoint nep = null;
        try {
            nep = readNep(ref);
        } catch (ReadFailedException | InterruptedException | ExecutionException e) {
            LOG.warn("Error while reading NEP", e);
        }
        if (nep == null) {
            throw new IllegalArgumentException("Cannot find NEP for " + ref);
        }

        OwnedNodeEdgePoint1Builder builder;

        OwnedNodeEdgePoint1 aug = nep.augmentation(OwnedNodeEdgePoint1.class);
        if (aug == null) {
            builder = new OwnedNodeEdgePoint1Builder();
        } else {
            builder = new OwnedNodeEdgePoint1Builder(aug);
        }

        List<ConnectionEndPoint> cepList = builder.getConnectionEndPoint();
        if (cepList == null) {
            cepList = new LinkedList<>();
        }

        cepList.add(cep);
        builder.setConnectionEndPoint(cepList);

        nep = new OwnedNodeEdgePointBuilder(nep).addAugmentation(OwnedNodeEdgePoint1.class, builder.build()).build();
        tx.merge(LogicalDatastoreType.OPERATIONAL, toPath.apply(ref), nep);

        return new ConnectionEndPointBuilder(ref).setConnectionEndPointId(cep.getUuid()).build();
    }

    public OwnedNodeEdgePoint readNep(OwnedNodeEdgePointRef ref)
            throws ReadFailedException, InterruptedException, ExecutionException {

        KeyedInstanceIdentifier<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> nepKey = toPath.apply(ref);

        return rtx.read(LogicalDatastoreType.OPERATIONAL, nepKey).get().orElse(null);
    }

    public OwnedNodeEdgePoint readNep(String nodeId, String nepId) throws InterruptedException, ExecutionException {
        KeyedInstanceIdentifier<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> nepKey = node(nodeId)
                .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(new Uuid(nepId)));
        return rtx.read(LogicalDatastoreType.OPERATIONAL, nepKey).get().orElse(null);
    }

    public boolean hasSip(String nepId) {
        Uuid universalId = new Uuid("sip:" + nepId);
        try {
            return rtx.read(LogicalDatastoreType.OPERATIONAL,
                    ctx().child(ServiceInterfacePoint.class, new ServiceInterfacePointKey(universalId)))
                    .get().isPresent();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Cannot read sip with id {}", universalId.getValue());
        }
        return false;
    }

    public boolean hasNep(String nodeId, String nepId) throws InterruptedException, ExecutionException {
        return readNep(nodeId, nepId) != null;
    }

    public Topology getTopology(String uuid) throws InterruptedException, ExecutionException  {
        Optional<Topology> topology = rtx.read(LogicalDatastoreType.OPERATIONAL, topo(uuid)).get();
        return topology.orElse(null);
    }

    public Node getNode(String uuidTopo, String uuidNode) throws InterruptedException, ExecutionException  {
        Optional<Node> topology = rtx.read(LogicalDatastoreType.OPERATIONAL,
                node(new Uuid(uuidTopo), new Uuid(uuidNode))).get();
        return topology.orElse(null);
    }

    public Node getNode(Uuid uuidNode) throws InterruptedException, ExecutionException  {
        Optional<Node> topology = rtx.read(LogicalDatastoreType.OPERATIONAL, node(uuidNode)).get();
        return topology.orElse(null);
    }

    public static InstanceIdentifier<Context> ctx() {
        return InstanceIdentifier.create(Context.class);
    }

    public static InstanceIdentifier<Topology> topo(String topoId) {
        return topo(new Uuid(topoId));
    }

    public static InstanceIdentifier<Topology> topo(Uuid topoId) {
        return ctx()
                .augmentation(Context1.class)
                .child(Topology.class, new TopologyKey(topoId));
    }

    public static InstanceIdentifier<Node> node(String nodeId) {
        return node(new Uuid(nodeId));
    }

    public static InstanceIdentifier<Node> node(Uuid topologyId, Uuid nodeId) {
        return topo(topologyId).child(Node.class, new NodeKey(nodeId));
    }

    public static InstanceIdentifier<Node> node(Uuid nodeId) {
        return node(new Uuid(TapiConstants.PRESTO_SYSTEM_TOPO), nodeId);
    }

    public static InstanceIdentifier<Node> abstractNode() {
        return topo(TapiConstants.PRESTO_EXT_TOPO).child(Node.class,
                new NodeKey(new Uuid(TapiConstants.PRESTO_ABSTRACT_NODE)));
    }

    public void removeSip(Uuid uuid) {
        removeSips(Stream.of(uuid));
    }

    public void removeSips(Stream<Uuid>  uuids) {
        verifyTx();
        assert tx != null;
        if (uuids == null) {
            return;
        }
        uuids.forEach(sip -> {
            LOG.debug("removing ServiceInterfacePoint with id {}", sip);
            tx.delete(LogicalDatastoreType.OPERATIONAL, ctx().child(ServiceInterfacePoint.class,
                    new ServiceInterfacePointKey(sip)));
        });
    }

    public void removeNode(String nodeId, boolean removeSips) {
        verifyTx();
        if (removeSips) {
            try {
                Optional<Node> opt = rtx.read(LogicalDatastoreType.OPERATIONAL, node(nodeId)).get();
                if (opt.isPresent()) {
                    List<OwnedNodeEdgePoint> neps = opt.get().getOwnedNodeEdgePoint();
                    if (neps != null) {
                        removeSips(neps.stream().flatMap(nep -> nep.getMappedServiceInterfacePoint() == null
                                ? Stream.empty()
                                : nep.getMappedServiceInterfacePoint()
                                    .stream().map(ServiceInterfacePointRef::getServiceInterfacePointId)
                        ));
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Cannot read node with id {}", nodeId);
            }
        }
        assert tx != null;
        tx.delete(LogicalDatastoreType.OPERATIONAL, node(nodeId));
    }

    public void updateAbstractNep(OwnedNodeEdgePoint nep) {
        verifyTx();
        assert tx != null;
        InstanceIdentifier<OwnedNodeEdgePoint> nodeIdent = abstractNode().child(OwnedNodeEdgePoint.class,
                new OwnedNodeEdgePointKey(nep.getUuid()));
        tx.merge(LogicalDatastoreType.OPERATIONAL, nodeIdent, nep);
    }

    public void deleteAbstractNep(OwnedNodeEdgePoint nep) {
        verifyTx();
        assert tx != null;
        InstanceIdentifier<OwnedNodeEdgePoint> nodeIdent = abstractNode().child(OwnedNodeEdgePoint.class,
                new OwnedNodeEdgePointKey(nep.getUuid()));
        tx.delete(LogicalDatastoreType.OPERATIONAL, nodeIdent);
    }

    public List<ConnectivityService> getConnectivityServiceList() {
        try {
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.Context1 connectivity = rtx
                    .read(LogicalDatastoreType.OPERATIONAL, CS_CTX)
                    .get().orElse(null);
            return connectivity == null ? null : connectivity.getConnectivityService();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("reading connectivity services failed", e);
            return null;
        }
    }

    public ConnectivityService getConnectivityService(String idOrName) {
        ConnectivityService cs = getConnectivityService(new Uuid(idOrName));
        if (cs != null) {
            return cs;
        }

        List<ConnectivityService> csList = getConnectivityServiceList();
        if (csList != null) {
            return csList.stream()
                    .filter(child -> child.getName() != null && child.getName().stream()
                            .anyMatch(n -> idOrName.equals(n.getValue())))
                    .findFirst().orElse(null);

        }
        return null;
    }

    public ConnectivityService getConnectivityService(Uuid id) {
        try {
            return rtx.read(LogicalDatastoreType.OPERATIONAL, CS_CTX
                    .child(ConnectivityService.class, new ConnectivityServiceKey(id)))
                    .get().orElse(null);

        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("reading connectivity service failed", e);
            return null;
        }
    }

    public OwnedNodeEdgePoint getNepByCep(ConnectionEndPointRef ref) {
        KeyedInstanceIdentifier<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> nepPath =
                node(ref.getTopologyId(), ref.getNodeId())
                        .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(ref.getOwnedNodeEdgePointId()));

        try {
            return rtx.read(LogicalDatastoreType.OPERATIONAL, nepPath)
                    .get().orElse(null);

        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("reading NEP for ref " +  ref + " failed", e);
            return null;
        }
    }

    public ServiceInterfacePoint getSip(String sipId) throws InterruptedException, ExecutionException  {
        KeyedInstanceIdentifier<ServiceInterfacePoint, ServiceInterfacePointKey> key = ctx()
                .child(ServiceInterfacePoint.class, new ServiceInterfacePointKey(new Uuid(sipId)));
        return rtx.read(LogicalDatastoreType.OPERATIONAL, key).get().orElse(null);
    }

    public Connection getConnection(Uuid connectionId) {
        try {
            return rtx.read(LogicalDatastoreType.OPERATIONAL, CS_CTX.child(Connection.class,
                    new ConnectionKey(connectionId)))
                    .get().orElse(null);

        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("reading connectivity service failed", e);
            return null;
        }
    }

    public String getActivationDriverId(Uuid nodeUuid) throws InterruptedException, ExecutionException  {
        return getNode(nodeUuid).augmentation(NodeAdiAugmentation.class).getActivationDriverId();
    }

    public void removeConnection(Uuid connectionId) {
        Objects.requireNonNull(connectionId);
        verifyTx();
        assert tx != null;
        Connection connection = getConnection(connectionId);
        if (connection == null) {
            return;
        }

        for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
                .connection.ConnectionEndPoint cepRef : connection.getConnectionEndPoint()) {
            KeyedInstanceIdentifier<ConnectionEndPoint, ConnectionEndPointKey> cepKey =
                    node(cepRef.getTopologyId(), cepRef.getNodeId())
                    .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(cepRef.getOwnedNodeEdgePointId()))
                    .augmentation(OwnedNodeEdgePoint1.class).child(ConnectionEndPoint.class,
                            new ConnectionEndPointKey(cepRef.getConnectionEndPointId()));
            tx.delete(LogicalDatastoreType.OPERATIONAL,cepKey);
        }
        LOG.debug("removing connection {}", connectionId.getValue());
        tx.delete(LogicalDatastoreType.OPERATIONAL, CS_CTX.child(Connection.class, new ConnectionKey(connectionId)));
    }

    public ConnectivityService updateCsEndPoint(String serviceId,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
            .update.connectivity.service.input.EndPoint endPoint) throws InterruptedException, ExecutionException {
        Objects.requireNonNull(endPoint);
        Objects.requireNonNull(serviceId);
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
                .connectivity.service.EndPoint ep = new EndPointBuilder(endPoint).build();


        KeyedInstanceIdentifier<EndPoint, EndPointKey> epId = CS_CTX
                .child(ConnectivityService.class, new ConnectivityServiceKey(new Uuid(serviceId)))
                .child(EndPoint.class, new EndPointKey(endPoint.getLocalId()));

        tx.put(LogicalDatastoreType.OPERATIONAL, epId, ep);
        if (endPoint.augmentation(EndPoint7.class) != null) {
            tx.put(LogicalDatastoreType.OPERATIONAL, epId.augmentation(EndPoint1.class),
                    new EndPoint1Builder(endPoint.augmentation(EndPoint7.class)).build());
        }
        //XXX do we need to support name as well?
        ConnectivityService cs = getConnectivityService(serviceId);
        try {
            tx.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Problem with updating connectivity service endpoint", e);
            throw e;
        }

        return cs;
    }
}
