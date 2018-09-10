/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl;

import static org.opendaylight.unimgr.mef.nrp.api.TapiConstants.PRESTO_CTX;
import static org.opendaylight.unimgr.mef.nrp.api.TapiConstants.PRESTO_EXT_TOPO;
import static org.opendaylight.unimgr.mef.nrp.api.TapiConstants.PRESTO_SYSTEM_TOPO;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.TopologyManager;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.EncapTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.context.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.context.TopologyKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FluentFuture;

/**
 * NrpInitializer is responsible for initial TAPI context related entities creation.
 * @author bartosz.michalik@amartus.com
 */
public class NrpInitializer implements TopologyManager {
    private static final Logger LOG = LoggerFactory.getLogger(NrpInitializer.class);
    private final DataBroker dataBroker;


    public NrpInitializer(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void init() throws Exception {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        InstanceIdentifier<Context> ctxId = InstanceIdentifier.create(Context.class);
        FluentFuture<Optional<Context>> result =
                tx.read(LogicalDatastoreType.OPERATIONAL, ctxId);

        Optional<? extends DataObject> context = result.get();

        if (! context.isPresent()) {
            LOG.info("initialize Presto NRP context");
            Context ctx = new ContextBuilder()
                    .setUuid(new Uuid(PRESTO_CTX))
                    .addAugmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                                .rev180307.Context1.class, context())
                    .addAugmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity
                                .rev180307.Context1.class, connCtx())
                    .build();
            tx.put(LogicalDatastoreType.OPERATIONAL, ctxId, ctx);
            tx.commit().get();
            LOG.debug("Presto context model created");
        }
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.Context1 context() {
        return new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.Context1Builder()
                .setTopology(Arrays.asList(systemTopo(), extTopo()))
                .build();
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.Context1 connCtx() {
        return new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.Context1Builder()
                .build();
    }

    private Topology extTopo() {
        Uuid topoId = new Uuid(PRESTO_EXT_TOPO);
        LOG.debug("Adding {}", PRESTO_EXT_TOPO);
        return new TopologyBuilder()
                .setLayerProtocolName(Collections.singletonList(LayerProtocolName.ETH))
                .setUuid(topoId)
                .withKey(new TopologyKey(topoId))
                .setNode(Collections.singletonList(node("mef:presto-nrp-abstract-node")))
                .build();
    }

    private Node node(String uuid) {
        Uuid uid = new Uuid(uuid);
        return new NodeBuilder()
                .setLayerProtocolName(Collections.singletonList(LayerProtocolName.ETH))
                .setEncapTopology(new EncapTopologyBuilder().setTopologyId(new Uuid(PRESTO_SYSTEM_TOPO)).build())
                .setCostCharacteristic(Collections.emptyList())
                .setLatencyCharacteristic(Collections.emptyList())
                .setUuid(uid)
                .build();
    }

    private Topology systemTopo() {
        Uuid topoId = new Uuid(PRESTO_SYSTEM_TOPO);
        LOG.debug("Adding {}", PRESTO_SYSTEM_TOPO);
        return new TopologyBuilder()
                .setLayerProtocolName(Collections.singletonList(LayerProtocolName.ETH))
                .setUuid(topoId)
                .build();
    }


    public void close() {

    }

    @Override
    public String getSystemTopologyId() {
        return PRESTO_SYSTEM_TOPO;
    }
}
