/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.utils;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MdsalUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MdsalUtils.class);

    private MdsalUtils() {
        throw new AssertionError("Instantiating utility class.");
    }

    /**
     * Read a specific datastore type and return a DataObject as a casted
     * class type Object.
     * @param dataBroker The dataBroker instance to create transactions
     * @param store The store type to query
     * @param path The generic path to query
     * @return The DataObject as a casted Object
     */
    public static <D extends org.opendaylight.yangtools.yang.binding.DataObject> D read(
            DataBroker dataBroker,
            final LogicalDatastoreType store,
            final InstanceIdentifier<D> path)  {
        D result = null;
        final ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        Optional<D> optionalDataObject;
        final CheckedFuture<Optional<D>, ReadFailedException> future = transaction.read(store, path);
        try {
            optionalDataObject = future.checkedGet();
            if (optionalDataObject.isPresent()) {
                result = optionalDataObject.get();
            } else {
                LOG.debug("{}: Failed to read {}",
                        Thread.currentThread().getStackTrace()[1], path);
            }
        } catch (final ReadFailedException e) {
            LOG.warn("Failed to read {} ", path, e);
        }
        transaction.close();
        return result;
    }

    /**
     * Read a specific datastore type and return a optional of DataObject.
     * @param dataBroker The dataBroker instance to create transactions
     * @param store The store type to query
     * @param path The generic path to query
     * @return Read object optional
     */
    public static <D extends org.opendaylight.yangtools.yang.binding.DataObject> Optional<D> readOptional(
            DataBroker dataBroker,
            final LogicalDatastoreType store,
            final InstanceIdentifier<D> path) {

        final ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        Optional<D> optionalDataObject = Optional.absent();
        final CheckedFuture<Optional<D>, ReadFailedException> future = transaction.read(store, path);
        try {
            optionalDataObject = future.checkedGet();
        } catch (final ReadFailedException e) {
            LOG.warn("Failed to read {} ", path, e);
        }

        transaction.close();
        return optionalDataObject;
    }

    /**
     * Read a specific node from the Operational Data store by default.
     * @param dataBroker The dataBroker instance to create transactions
     * @param genericNode The Instance Identifier of the Node
     * @return The Optional Node instance
     */
    public static final Optional<Node> readNode(DataBroker dataBroker,
                                                InstanceIdentifier<?> genericNode) {
        final ReadTransaction read = dataBroker.newReadOnlyTransaction();
        final InstanceIdentifier<Node> nodeIid = genericNode.firstIdentifierOf(Node.class);
        final CheckedFuture<Optional<Node>, ReadFailedException> nodeFuture =
                                                              read.read(LogicalDatastoreType.OPERATIONAL,
                                                                        nodeIid);
        try {
            return nodeFuture.checkedGet();
        } catch (final ReadFailedException e) {
            LOG.error("Unable to read node with Iid {}", nodeIid, e);
        }
        return Optional.absent();
    }

    /**
     * Read a specific node from a specific data store type.
     * @param dataBroker The dataBroker instance to create transactions
     * @param store The data store type
     * @param genericNode The Instance Identifier of a specific Node
     * @return An Optional Node instance
     */
    public static final Optional<Node> readNode(DataBroker dataBroker,
                                                LogicalDatastoreType store,
                                                InstanceIdentifier<?> genericNode) {
        final ReadTransaction read = dataBroker.newReadOnlyTransaction();
        final InstanceIdentifier<Node> nodeIid = genericNode.firstIdentifierOf(Node.class);
        final CheckedFuture<Optional<Node>, ReadFailedException> nodeFuture = read
                .read(store, nodeIid);
        try {
            return nodeFuture.checkedGet();
        } catch (final ReadFailedException e) {
            LOG.info("Unable to read node with Iid {}", nodeIid, e);
        }
        return Optional.absent();
    }

    /**
     * Generic function to delete a node on a specific dataStore.
     * @param dataBroker The instance of the data broker to create transactions.
     * @param genericNode The instance identifier of a generic node
     * @param store The dataStore where to send and submit the delete call.
     * @return <code>true</code> if success
     */
    public static boolean deleteNode(DataBroker dataBroker,
                                  InstanceIdentifier<?> genericNode,
                                  LogicalDatastoreType store) {
        LOG.info("Received a request to delete node {}", genericNode);
        boolean result = false;
        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.delete(store, genericNode);
        try {
            transaction.submit().checkedGet();
            result = true;
        } catch (final TransactionCommitFailedException e) {
            LOG.error("Unable to remove node with Iid {} from store {}", genericNode, store, e);
        }
        return result;
    }

    /**
     * Read a specific Link from a specific datastore.
     * @param dataBroker The dataBroker instance to create transactions
     * @param store The datastore type.
     * @param genericNode The Instance Identifier of the Link
     * @return An Optional Link instance
     */
    public static final Optional<Link> readLink(DataBroker dataBroker,
                                                LogicalDatastoreType store,
                                                InstanceIdentifier<?> genericNode) {
        final ReadTransaction read = dataBroker.newReadOnlyTransaction();
        final InstanceIdentifier<Link> linkIid = genericNode.firstIdentifierOf(Link.class);
        final CheckedFuture<Optional<Link>, ReadFailedException> linkFuture = read.read(store, linkIid);
        try {
            return linkFuture.checkedGet();
        } catch (final ReadFailedException e) {
            LOG.info("Unable to read node with Iid {}", linkIid, e);
        }
        return Optional.absent();
    }

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
        final CheckedFuture<Optional<Topology>, ReadFailedException> topologyFuture = read.read(store, topologyInstanceId);

        try {
            return topologyFuture.checkedGet();
        } catch (final ReadFailedException e) {
            LOG.info("Unable to read topology with Iid {}", topologyInstanceId, e);
        }
        return Optional.absent();
    }
}
