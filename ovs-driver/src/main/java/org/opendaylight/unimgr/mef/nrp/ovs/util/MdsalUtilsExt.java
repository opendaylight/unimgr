/*
 * Copyright (c) 2018 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.ovs.util;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FluentFuture;

/**
 * Utility functions.
 * @author bartosz.michalik@amartus.com
 */
public class MdsalUtilsExt {
    private static final Logger LOG = LoggerFactory.getLogger(MdsalUtilsExt.class);

    /**
     * Read a specific Link from a specific datastore.
     * @param dataBroker The dataBroker instance to create transactions
     * @param store The datastore type.
     * @param topologyName The topology name.
     * @return An Optional Link instance
     */
    public static final Optional<Topology> readTopology(DataBroker dataBroker,
                                                        LogicalDatastoreType store,
                                                        String topologyName) {
        final ReadTransaction read = dataBroker.newReadOnlyTransaction();
        final TopologyId topologyId = new TopologyId(topologyName);
        InstanceIdentifier<Topology> topologyInstanceId
                = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(topologyId))
                .build();
        final FluentFuture<Optional<Topology>> topologyFuture =
                read.read(store, topologyInstanceId);

        try {
            return topologyFuture.get();
        } catch (final InterruptedException | ExecutionException e) {
            LOG.info("Unable to read topology with Iid {}", topologyInstanceId, e);
        }
        return Optional.empty();
    }
}
