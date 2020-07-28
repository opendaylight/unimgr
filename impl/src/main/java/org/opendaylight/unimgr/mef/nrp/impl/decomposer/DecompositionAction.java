/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.decomposer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.api.FailureResult;
import org.opendaylight.unimgr.mef.nrp.api.Subrequrest;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.unimgr.mef.nrp.common.TapiUtils;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.NodeAdiAugmentation;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.ServiceInterfacePointRef;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.end.point.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.context.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Decompose request into sub-requests {@link Subrequrest} for drivers using graph path computation.
 * @author bartosz.michalik@amartus.com
 */
class DecompositionAction {
    private static final Logger LOG = LoggerFactory.getLogger(DecompositionAction.class);
    private final List<EndPoint> endpoints;
    private final DataBroker broker;
    private final HashMap<Uuid, Vertex> sipToNep = new HashMap<>();

    DecompositionAction(List<EndPoint> endpoints, DataBroker broker) {
        Objects.requireNonNull(endpoints);
        Objects.requireNonNull(broker);
        if (endpoints.size() < 2) {
            throw new IllegalArgumentException("there should be at least two endpoints defined");
        }
        this.endpoints = endpoints;
        this.broker = broker;
    }

    Function<ServiceInterfacePoint, Uuid> toUuid = s -> s == null ? null : s.getServiceInterfacePointId();

    List<Subrequrest> decompose() throws FailureResult {
        Graph<Vertex, DefaultEdge> graph = prepareData();



        Set<String> missingSips = endpoints.stream()
                .filter(e -> sipToNep.get(toUuid.apply(e.getEndpoint().getServiceInterfacePoint())) == null)
                .map(e -> toUuid.apply(e.getEndpoint().getServiceInterfacePoint()).getValue())
                .collect(Collectors.toSet());
        if (!missingSips.isEmpty()) {
            throw new FailureResult("Some service interface points not found in the system: "
                    + missingSips.stream().collect(Collectors.joining(",", "[", "]")));
        }

        List<Vertex> vertices = endpoints.stream().map(e -> {
            Vertex vertex = sipToNep.get(toUuid.apply(e.getEndpoint().getServiceInterfacePoint()));
            if ((vertex.dir == PortDirection.OUTPUT && e.getEndpoint().getDirection() != PortDirection.OUTPUT)
                    ||  (vertex.dir == PortDirection.INPUT && e.getEndpoint().getDirection() != PortDirection.INPUT)) {
                throw new IllegalArgumentException("Port direction for "
                        + e.getEndpoint().getLocalId() + " incompatible with NEP."
                        + "CEP " + e.getEndpoint().getDirection() + "  NEP " + vertex.dir);
            }
            return new Vertex(vertex, e.getEndpoint().getDirection());
        }).collect(Collectors.toList());

        assert vertices.size() > 1;

        Set<GraphPath<Vertex, DefaultEdge>> paths = new HashSet<>();

        Set<Vertex> inV = vertices.stream().filter(isInput).collect(Collectors.toSet());
        Set<Vertex> outV = vertices.stream().filter(isOutput).collect(Collectors.toSet());

        //do the verification whether it is possible to connect two nodes.
        inV.forEach(i ->
                outV.stream().filter(o -> i != o).forEach(o -> {
                    GraphPath<Vertex, DefaultEdge> path = DijkstraShortestPath.findPathBetween(graph, i, o);
                    if (path != null) {
                        LOG.debug("Couldn't find path between {} and  {}", i, o);
                    }
                    paths.add(path);
                })
        );

        if (paths.stream().anyMatch(Objects::isNull)) {
            LOG.info("At least single path between endpoints not found");
            return null;
        }

        List<Subrequrest> result = toSublists(paths);
        return result.isEmpty() ? null : result;
    }

    private List<Subrequrest> toSublists(Set<GraphPath<Vertex, DefaultEdge>> paths) {
        return paths.stream()
                .flatMap(gp -> gp.getVertexList().stream()).collect(Collectors.groupingBy(Vertex::getNodeUuid))
                .entrySet().stream()
                .map(e -> {
                    Set<EndPoint> fromVertexes = e.getValue().stream()
                            .map(this::toEndPoint).collect(Collectors.toSet());
                    return new Subrequrest(
                            e.getKey(),
                            new ArrayList<>(fromVertexes),
                            e.getValue().stream().findFirst().get().getActivationDriverId());
                }).collect(Collectors.toList());
    }

    private EndPoint toEndPoint(Vertex vertex) {
        EndPoint ep = endpoints.stream()
                .filter(e -> e.getEndpoint().getServiceInterfacePoint()
                        .getServiceInterfacePointId().equals(vertex.getSip()))
                .findFirst()
                .orElse(new EndPoint(null, null));
        Objects.requireNonNull(vertex.getUuid());
        ep.setNepRef(TapiUtils.toSysNepRef(vertex.getNodeUuid(), vertex.getUuid()));
        return ep;
    }

    private final Predicate<Vertex> isInput = v -> v.getDir() == PortDirection.BIDIRECTIONAL
            || v.getDir() == PortDirection.INPUT;
    private final Predicate<Vertex> isOutput = v -> v.getDir() == PortDirection.BIDIRECTIONAL
            || v.getDir() == PortDirection.OUTPUT;

    private void interconnectNode(Graph<Vertex, DefaultEdge> graph, List<Vertex> vertices) {
        vertices.forEach(graph::addVertex);
        Set<Vertex> inV = vertices.stream().filter(isInput).collect(Collectors.toSet());
        Set<Vertex> outV = vertices.stream().filter(isOutput).collect(Collectors.toSet());
        interconnect(graph, inV, outV);
    }

    private void interconnectLink(Graph<Vertex, DefaultEdge> graph, List<Vertex> vertices) {
        vertices.forEach(graph::addVertex);
        Set<Vertex> inV = vertices.stream().filter(isInput).collect(Collectors.toSet());
        Set<Vertex> outV = vertices.stream().filter(isOutput).collect(Collectors.toSet());
        interconnect(graph, outV, inV);


    }

    private void interconnect(Graph<Vertex, DefaultEdge> graph, Collection<Vertex> from, Collection<Vertex> to) {
        from.forEach(iV ->
                to.stream().filter(oV -> iV != oV).forEach(oV -> graph.addEdge(iV,oV)));
    }


    private Graph<Vertex, DefaultEdge> prepareData() throws FailureResult {
        ReadWriteTransaction tx = broker.newReadWriteTransaction();
        try {
            Topology topo = new NrpDao(tx).getTopology(TapiConstants.PRESTO_SYSTEM_TOPO);
            if (topo.getNode() == null) {
                throw new FailureResult("There are no nodes in {0} topology", TapiConstants.PRESTO_SYSTEM_TOPO);
            }

            Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

            topo.getNode().stream().map(this::nodeToGraph).forEach(vs -> {
                List<Vertex> vertices = vs.collect(Collectors.toList());
                vertices.forEach(v -> sipToNep.put(v.getSip(), v));
                interconnectNode(graph, vertices);
            });

            if (topo.getLink() != null) {
                topo.getLink().stream()
                    .filter(l -> OperationalState.ENABLED == l.getOperationalState())
                    .forEach(l -> {
                        //we probably need to take link bidir/unidir into consideration as well
                        List<Vertex> vertices = l.getNodeEdgePoint().stream()
                            .map(nep -> graph.vertexSet().stream()
                                    .filter(v -> v.getUuid().equals(nep.getOwnedNodeEdgePointId())).findFirst())
                            .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
                        interconnectLink(graph, vertices);
                    });
            }

            return graph;
        } catch (InterruptedException | ExecutionException e) {
            throw new FailureResult("Cannot read {0} topology - {1}", TapiConstants.PRESTO_SYSTEM_TOPO, e);
        }
    }

    private Stream<Vertex> nodeToGraph(Node node) {
        Uuid nodeUuid = node.getUuid();
        String activationDriverId = node.augmentation(NodeAdiAugmentation.class).getActivationDriverId();
               
        return node.getOwnedNodeEdgePoint().stream()
            .filter(ep -> ep.getLinkPortDirection() != null
                    && ep.getLinkPortDirection() != PortDirection.UNIDENTIFIEDORUNKNOWN)
            .map(nep -> {
                List<Uuid> sips = Collections.emptyList();
                if (nep.getMappedServiceInterfacePoint() != null) {
                    sips = nep.getMappedServiceInterfacePoint().stream()
                        .map(ServiceInterfacePointRef::getServiceInterfacePointId)
                        .collect(Collectors.toList());
                }

                if (sips.isEmpty()) {
                    return  new Vertex(nodeUuid, nep.getUuid(), null, nep.getLinkPortDirection(),activationDriverId);
                }
                if (sips.size() > 1) {
                    LOG.warn("NodeEdgePoint {} have multiple ServiceInterfacePoint mapped, selecting first one",
                            nep.getUuid());
                }
                return new Vertex(nodeUuid, nep.getUuid(), sips.get(0), nep.getLinkPortDirection(),activationDriverId);

            });
    }

    static class Vertex implements Comparable<Vertex> {

        private final Uuid nodeUuid;
        private final Uuid uuid;
        private final Uuid sip;
        private final String activationDriverId;
        private final PortDirection dir;

        Vertex(Vertex px, PortDirection csDir) {
            this.nodeUuid = px.nodeUuid;
            this.uuid = px.uuid;
            this.sip = px.sip;
            this.dir = csDir;
            this.activationDriverId = px.activationDriverId;
        }

        Vertex(Uuid nodeUuid, Uuid uuid, Uuid sip, PortDirection dir, String activationDriverId) {
            this.sip = sip;
            this.dir = dir;
            Objects.requireNonNull(nodeUuid);
            Objects.requireNonNull(uuid);
            Objects.requireNonNull(activationDriverId);

            this.nodeUuid = nodeUuid;
            this.uuid = uuid;
            this.activationDriverId = activationDriverId;
        }

        Uuid getNodeUuid() {
            return nodeUuid;
        }

        Uuid getUuid() {
            return uuid;
        }

        Uuid getSip() {
            return sip;
        }

        public String getActivationDriverId() {
            return activationDriverId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }
            Vertex vertex = (Vertex) other;
            return Objects.equals(uuid, vertex.uuid);
        }

        PortDirection getDir() {
            return dir;
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid);
        }

        @Override
        public int compareTo(Vertex other) {
            if (other == null) {
                return -1;
            }
            return uuid.getValue().compareTo(other.uuid.getValue());
        }

        @Override
        public String toString() {
            return "V{" + uuid.getValue() + '}';
        }
    }
}
