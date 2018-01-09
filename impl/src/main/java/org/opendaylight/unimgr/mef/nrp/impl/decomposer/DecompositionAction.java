/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.decomposer;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.api.FailureResult;
import org.opendaylight.unimgr.mef.nrp.api.Subrequrest;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.context.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author bartosz.michalik@amartus.com
 */
public class DecompositionAction {
    private static final Logger LOG = LoggerFactory.getLogger(DecompositionAction.class);
    private final List<EndPoint> endpoints;
    private final DataBroker broker;
    private HashMap<Uuid, Vertex> sipToNep = new HashMap<>();

    public DecompositionAction(List<EndPoint> endpoints, DataBroker broker) {
        Objects.requireNonNull(endpoints);
        Objects.requireNonNull(broker);
        if (endpoints.size() < 2) {
            throw new IllegalArgumentException("there should be at least two endpoints defined");
        }
        this.endpoints = endpoints;
        this.broker = broker;
    }

    List<Subrequrest> decompose() throws FailureResult {
        Graph<Vertex, DefaultEdge> graph = prepareData();

        List<Vertex> vertexes = endpoints.stream().map(e -> sipToNep.get(e.getEndpoint().getServiceInterfacePoint())).collect(Collectors.toList());

        assert vertexes.size() > 1;

        if (vertexes.size() > 2) {
            throw new IllegalStateException("currently only point to point is supported");
        }

        GraphPath<Vertex, DefaultEdge> path = DijkstraShortestPath.findPathBetween(graph, vertexes.get(0), vertexes.get(1));

        if (path == null) {
            return null;
        }

        return path.getVertexList().stream().collect(Collectors.groupingBy(v -> v.getNodeUuid()))
                .entrySet().stream().map(e -> {
                    return new Subrequrest(e.getKey(), e.getValue().stream().map(v -> toEndPoint(v)).collect(Collectors.toList()));
                }).collect(Collectors.toList());
    }

    private EndPoint toEndPoint(Vertex v) {
        EndPoint ep = endpoints.stream().filter(e -> e.getEndpoint().getServiceInterfacePoint().equals(v.getSip())).findFirst()
                .orElse(new EndPoint(null, null));
        ep.setSystemNepUuid(v.getUuid());
        return ep;
    }

    private void connected(Graph<Vertex, DefaultEdge> graph, List<Vertex> vertices) {
        for (int i = 0; i < vertices.size(); ++i) {
            Vertex f = vertices.get(i);
            //its OK if the vertex is added in internal loop nothing will happen
            graph.addVertex(f);
            for (int j = i + 1; j < vertices.size(); ++j) {
                Vertex t = vertices.get(j);
                graph.addVertex(t);
                graph.addEdge(f,t);
            }
        }
    }

    protected Graph<Vertex, DefaultEdge> prepareData() throws FailureResult {
        ReadWriteTransaction tx = broker.newReadWriteTransaction();
        try {
            Topology topo = new NrpDao(tx).getTopology(TapiConstants.PRESTO_SYSTEM_TOPO);
            if (topo.getNode() == null) {
                throw new FailureResult("There are no nodes in {0} topology", TapiConstants.PRESTO_SYSTEM_TOPO);
            }

            Graph<Vertex, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);
            topo.getNode().stream().map(this::nodeToGraph).forEach(vs -> {
                List<Vertex> vertices = vs.collect(Collectors.toList());
                vertices.forEach(v -> sipToNep.put(v.getSip(), v));
                connected(graph, vertices);
            });

            if (topo.getLink() != null) {
                topo.getLink().stream()
                        .filter(l -> l.getState() != null && OperationalState.ENABLED == l.getState().getOperationalState())
                        .forEach(l -> {
                    List<Vertex> vertices = l.getNodeEdgePoint().stream()
                            .map(nep -> graph.vertexSet().stream().filter(v -> v.getUuid().equals(nep)).findFirst())
                            .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
                    connected(graph, vertices);
                });
            }

            return graph;
        } catch (ReadFailedException e) {
            throw new FailureResult("Cannot read {0} topology", TapiConstants.PRESTO_SYSTEM_TOPO);
        }
    }

    protected Stream<Vertex> nodeToGraph(Node n) {
        Uuid nodeUuid = n.getUuid();
        return n.getOwnedNodeEdgePoint().stream().map(nep -> {
            List<Uuid> sips = nep.getMappedServiceInterfacePoint();
            if (sips == null || sips.isEmpty()) {
                return  new Vertex(nodeUuid, nep.getUuid(), null);
            }
            if (sips.size() > 1) {
                LOG.warn("NodeEdgePoint {} have multiple ServiceInterfacePoint mapped, selecting first one", nep.getUuid());
            }
            return new Vertex(nodeUuid, nep.getUuid(), sips.get(0));

        });
    }

    public class Vertex implements Comparable<Vertex> {

        private final Uuid nodeUuid;
        private final Uuid uuid;
        private final Uuid sip;

        public Vertex(Uuid nodeUuid, Uuid uuid, Uuid sip) {
            this.sip = sip;
            Objects.requireNonNull(nodeUuid);
            Objects.requireNonNull(uuid);
            this.nodeUuid = nodeUuid;
            this.uuid = uuid;
        }

        public Uuid getNodeUuid() {
            return nodeUuid;
        }

        public Uuid getUuid() {
            return uuid;
        }

        public Uuid getSip() {
            return sip;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Vertex vertex = (Vertex) o;
            return Objects.equals(uuid, vertex.uuid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid);
        }

        @Override
        public int compareTo(Vertex o) {
            if (o == null) {
                return -1;
            }
            return uuid.getValue().compareTo(o.uuid.getValue());
        }

        @Override
        public String toString() {
            return "V{" + uuid.getValue() + '}';
        }
    }
}
