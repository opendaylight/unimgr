/*
 * Copyright (c) 2018 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.impl.topologytervice;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.*;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.get.link.details.output.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.get.node.details.output.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.get.topology.details.output.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.Node;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author bartosz.michalik@amartus.com
 */
public class TapiTopologyServiceImpl implements TapiTopologyService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TapiTopologyServiceImpl.class);
    private DataBroker broker;


    // TODO decide on strategy for executor service
    private ExecutorService executor = null;

    public void init() {
        Objects.requireNonNull(broker);
        if(executor == null) {
            executor = new ThreadPoolExecutor(4, 16,
                    30, TimeUnit.MINUTES,
                    new LinkedBlockingQueue<>());
        }
        LOG.info("TapiTopologyService initialized");
    }

    @Override
    public void close() throws Exception {
        if(!executor.isTerminated()) {
            executor.shutdownNow();
        }
    }

    public void setExecutor(ExecutorService executor) {
        if(executor != null) throw new IllegalStateException();
        this.executor = executor;
    }

    @Override
    public Future<RpcResult<GetNodeDetailsOutput>> getNodeDetails(GetNodeDetailsInput input) {
        return executor.submit(() -> {
           NrpDao nrpDao = new NrpDao(broker.newReadOnlyTransaction());

            Node node = nrpDao.getNode(input.getTopologyIdOrName(), input.getNodeIdOrName());
            if(node == null) return RpcResultBuilder.<GetNodeDetailsOutput>failed().withError(RpcError.ErrorType.APPLICATION,
                    String.format("No node for id: %s in topology %s", input.getNodeIdOrName(), input.getTopologyIdOrName())).build();

            GetNodeDetailsOutput output = new GetNodeDetailsOutputBuilder().setNode(new NodeBuilder(node).build()).build();
            return RpcResultBuilder.success(output).build();
        });
    }

    @Override
    public Future<RpcResult<GetNodeEdgePointDetailsOutput>> getNodeEdgePointDetails(GetNodeEdgePointDetailsInput input) {
        return null; //TODO not implemented
    }

    @Override
    public Future<RpcResult<GetLinkDetailsOutput>> getLinkDetails(GetLinkDetailsInput input) {
        return executor.submit(() -> {
            RpcResult<GetLinkDetailsOutput> out = RpcResultBuilder.<GetLinkDetailsOutput>failed().withError(RpcError.ErrorType.APPLICATION, "No link in topology").build();
            try {
                ReadOnlyTransaction rtx = broker.newReadOnlyTransaction();
                KeyedInstanceIdentifier<Link, LinkKey> linkId = NrpDao.topo(input.getTopologyIdOrName()).child(Link.class, new LinkKey(new Uuid(input.getLinkIdOrName())));
                Optional<Link> optional = rtx.read(LogicalDatastoreType.OPERATIONAL, linkId).checkedGet();
                if(optional.isPresent()) {
                    out = RpcResultBuilder
                            .success(new GetLinkDetailsOutputBuilder()
                                    .setLink(new LinkBuilder(optional.get()).build()).build())
                            .build();
                }
            } catch(ReadFailedException e) {
                out = RpcResultBuilder.<GetLinkDetailsOutput>failed().withError(RpcError.ErrorType.APPLICATION, String.format("Cannot read link %s",input.getLinkIdOrName()) ,e).build();
            }
            return out;
        });
    }

    private RpcResult<GetTopologyListOutput> getTopologies() {
        ReadOnlyTransaction rtx = broker.newReadOnlyTransaction();
        RpcResult<GetTopologyListOutput> out = RpcResultBuilder.success(new GetTopologyListOutputBuilder().build()).build();
        try {
            List<? extends Topology> topologies;
            Optional<Context1> ctx = rtx.read(LogicalDatastoreType.OPERATIONAL, NrpDao.ctx()
                    .augmentation(Context1.class)).checkedGet();
            if(ctx.isPresent()) {
                topologies = ctx.get().getTopology();

                out = RpcResultBuilder.success(
                        new GetTopologyListOutputBuilder()
                                .setTopology(topologies.stream().map(t -> new org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.get.topology.list.output.TopologyBuilder(t).build()).collect(Collectors.toList()))
                ).build();
            }
        } catch (ReadFailedException e) {
            out = RpcResultBuilder.<GetTopologyListOutput>failed().withError(RpcError.ErrorType.APPLICATION, "Cannot read topologies" ,e).build();
        }
        return out;
    }

    @Override
    public Future<RpcResult<GetTopologyListOutput>> getTopologyList() {
        return executor.submit(this::getTopologies);
    }

    @Override
    public Future<RpcResult<GetTopologyDetailsOutput>> getTopologyDetails(GetTopologyDetailsInput input) {
        return executor.submit(() -> {
            NrpDao nrpDao = new NrpDao(broker.newReadOnlyTransaction());
            org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.context.Topology topo = nrpDao.getTopology(input.getTopologyIdOrName());

            if(topo == null) return RpcResultBuilder.<GetTopologyDetailsOutput>failed().withError(RpcError.ErrorType.APPLICATION, String.format("No topology for id: %s", input.getTopologyIdOrName())).build();

            GetTopologyDetailsOutput result = new GetTopologyDetailsOutputBuilder()
                    .setTopology(new TopologyBuilder(topo).build())
                    .build();
            return RpcResultBuilder.success(result).build();
        });
    }

    public void setBroker(DataBroker broker) {
        this.broker = broker;
    }
}
