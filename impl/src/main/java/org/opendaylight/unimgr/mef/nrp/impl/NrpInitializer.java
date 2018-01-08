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

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Context;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.ETH;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.context.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.context.TopologyKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

/**
 * @author bartosz.michalik@amartus.com
 */
public class NrpInitializer {
    private static final Logger LOG = LoggerFactory.getLogger(NrpInitializer.class);
    private final DataBroker dataBroker;


    public NrpInitializer(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void init() throws Exception {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        InstanceIdentifier<Context> ctxId = InstanceIdentifier.create(Context.class);
        CheckedFuture<? extends Optional<? extends DataObject>, ReadFailedException> result = tx.read(LogicalDatastoreType.OPERATIONAL, ctxId);

        Optional<? extends DataObject> context = result.checkedGet();

        if (! context.isPresent()) {
            LOG.info("initialize Presto NRP context");
            Context ctx = new ContextBuilder()
                    .setUuid(new Uuid(PRESTO_CTX))
                    .addAugmentation(org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.Context1.class, context())
                    .addAugmentation(org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.Context1.class, connCtx())
                    .build();
            tx.put(LogicalDatastoreType.OPERATIONAL, ctxId, ctx);
            try {
                tx.submit().checkedGet();
                LOG.debug("Presto context model created");
            } catch (TransactionCommitFailedException e) {
                LOG.error("Failed to create presto context model");
                throw new IllegalStateException("cannot create presto context", e);
            }
        }
    }

    private org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.Context1 context() {
        return new org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.Context1Builder()
                .setTopology(Arrays.asList(systemTopo(), extTopo()))
                .build();
    }

    private org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.Context1 connCtx() {
        return new org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.Context1Builder()
                .build();
    }

    private Topology extTopo() {
        Uuid topoId = new Uuid(PRESTO_EXT_TOPO);
        LOG.debug("Adding {}", PRESTO_EXT_TOPO);
        return new TopologyBuilder()
                .setLayerProtocolName(Collections.singletonList(ETH.class))
                .setUuid(topoId)
                .setKey(new TopologyKey(topoId))
                .setNode(Collections.singletonList(node("mef:presto-nrp-abstract-node")))
                .build();
    }

    private Node node(String uuid) {
        Uuid uid = new Uuid(uuid);
        return new NodeBuilder()
                .setLayerProtocolName(Collections.singletonList(ETH.class))
                .setEncapTopology(new Uuid(PRESTO_SYSTEM_TOPO))
                .setKey(new NodeKey(uid))
//                .setState()
                .setUuid(uid)
                .build();
    }

    private Topology systemTopo() {
        Uuid topoId = new Uuid(PRESTO_SYSTEM_TOPO);
        LOG.debug("Adding {}", PRESTO_SYSTEM_TOPO);
        return new TopologyBuilder()
                .setLayerProtocolName(Collections.singletonList(ETH.class))
                .setUuid(topoId)
                .setKey(new TopologyKey(topoId))
                .build();
    }


    public void close() {

    }
}
