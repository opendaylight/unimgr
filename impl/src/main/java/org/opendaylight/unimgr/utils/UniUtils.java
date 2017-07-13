/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.utils;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.Speed;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed100M;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed100MBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10G;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10GBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10M;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10MBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed1G;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed1GBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class UniUtils {

    private static final Logger LOG = LoggerFactory.getLogger(UniUtils.class);

    private UniUtils() {
        throw new AssertionError("Instantiating utility class.");
    }

    /**
     * Creates and submit an UNI Node by using the Data contained in the UniAugmentation.
     * @param dataBroker The instance of the DataBroker to create transactions
     * @param uni The UNI's data
     * @return true if uni created
     */
    public static boolean createUniNode(DataBroker dataBroker, UniAugmentation uni) {
        final NodeId uniNodeId = new NodeId(createUniNodeId(uni.getIpAddress()));
        boolean result = false;
        try {
            final InstanceIdentifier<Node> uniNodeIid = UnimgrMapper.getUniNodeIid(uniNodeId);
            final NodeKey uniNodeKey = new NodeKey(uniNodeId);
            final Node nodeData = new NodeBuilder()
                                    .setNodeId(uniNodeId)
                                    .setKey(uniNodeKey)
                                    .addAugmentation(UniAugmentation.class, uni)
                                    .build();
            final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            transaction.put(LogicalDatastoreType.CONFIGURATION, uniNodeIid, nodeData);
            final CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
            future.checkedGet();
            result = true;
            LOG.info("Created and submitted a new Uni node {}", nodeData.getNodeId());
        } catch (final Exception e) {
            LOG.error("Exception while creating Uni Node, Uni Node Id: {}", uniNodeId, e);
        }
        return result;
    }

    /**
     * Creates an UNI node Id with an IP Address.
     * @param ipAddress The IP address of the UNI
     * @return A NodeId for a Specific UNI Node Id
     */
    public static NodeId createUniNodeId(IpAddress ipAddress) {
        return new NodeId(UnimgrConstants.UNI_PREFIX + ipAddress.getIpv4Address().getValue().toString());
    }

    /**
     * Search the Operation DataStore for a specific UNI.
     * @param dataBroker The dataBroker instance to create transactions
     * @param ipAddress The IP address of the UNI
     * @return An Optional UNI Node
     */
    public static Optional<Node> findUniNode(DataBroker dataBroker,
                                             IpAddress ipAddress) {
        final List<Node> uniNodes = getUniNodes(dataBroker);
        if (!uniNodes.isEmpty()) {
            for (final Node uniNode : uniNodes) {
                final UniAugmentation uniAugmentation = uniNode.getAugmentation(UniAugmentation.class);
                if (uniAugmentation.getIpAddress().equals(ipAddress)) {
                    LOG.info("Found Uni node");
                    return Optional.of(uniNode);
                }
            }
        }
        return Optional.absent();
    }

    /**
     * Retrieve a list of Uni Nodes from the Configuration DataStore.
     * @param dataBroker The dataBroker instance to create transactions
     * @return A list of Uni Nodes from the Config dataStore
     */
    public static List<Node> getUniNodes(DataBroker dataBroker) {
        final List<Node> uniNodes = new ArrayList<>();
        final InstanceIdentifier<Topology> topologyInstanceIdentifier = UnimgrMapper.getUniTopologyIid();
        final Topology topology = MdsalUtils.read(dataBroker,
                                 LogicalDatastoreType.CONFIGURATION,
                                 topologyInstanceIdentifier);
        if ((topology != null) && (topology.getNode() != null)) {
            for (final Node node : topology.getNode()) {
                final UniAugmentation uniAugmentation = node.getAugmentation(UniAugmentation.class);
                if (uniAugmentation != null) {
                    uniNodes.add(node);
                }
            }
        }
        return uniNodes;
    }

    /**
     * Retrieve a list of Uni Nodes on a specific DataStore.
     * @param dataBroker The dataBroker instance to create transactions
     * @param store The store to which to send the read request
     * @return A List of UNI Nodes.
     */
    public static List<Node> getUniNodes(DataBroker dataBroker,
                                         LogicalDatastoreType store) {
        final List<Node> uniNodes = new ArrayList<>();
        final InstanceIdentifier<Topology> topologyInstanceIdentifier = UnimgrMapper.getUniTopologyIid();
        final Topology topology = MdsalUtils.read(dataBroker,
                                 store,
                                 topologyInstanceIdentifier);
        if ((topology != null) && (topology.getNode() != null)) {
            for (final Node node : topology.getNode()) {
                final UniAugmentation uniAugmentation = node.getAugmentation(UniAugmentation.class);
                if (uniAugmentation != null) {
                    uniNodes.add(node);
                }
            }
        }
        return uniNodes;
    }

    /**
     * Retrieve a list of Unis on a specific DataStore.
     * @param dataBroker instance to create transactions
     * @param store to which send the read request
     * @return A List of Unis.
     */
    public static List<UniAugmentation> getUnis(DataBroker dataBroker,
                                         LogicalDatastoreType store) {
        final List<UniAugmentation> unis = new ArrayList<>();
        final InstanceIdentifier<Topology> topologyInstanceIdentifier = UnimgrMapper.getUniTopologyIid();
        final Topology topology = MdsalUtils.read(dataBroker,
                                 store,
                                 topologyInstanceIdentifier);
        if ((topology != null) && (topology.getNode() != null)) {
            for (final Node node : topology.getNode()) {
                final UniAugmentation uniAugmentation = node.getAugmentation(UniAugmentation.class);
                if (uniAugmentation != null) {
                    unis.add(uniAugmentation);
                }
            }
        }
        return unis;
    }

    /**
     * Retrieve a list of Unis on a specific DataStore.
     * @param dataBroker instance to create transactions
     * @param store to which send the read request
     * @param ipAddress of the required Uni
     * @return uni.
     */
    public static UniAugmentation getUni(DataBroker dataBroker,
                                         LogicalDatastoreType store, IpAddress ipAddress) {
        final InstanceIdentifier<Topology> topologyInstanceIdentifier = UnimgrMapper.getUniTopologyIid();
        final Topology topology = MdsalUtils.read(dataBroker,
                                 store,
                                 topologyInstanceIdentifier);
        if ((topology != null) && (topology.getNode() != null)) {
            for (final Node node : topology.getNode()) {
                final UniAugmentation uniAugmentation = node.getAugmentation(UniAugmentation.class);
                if ((uniAugmentation != null)
                        && uniAugmentation.getIpAddress().getIpv4Address().getValue().equals(
                                ipAddress.getIpv4Address().getValue())) {
                    return uniAugmentation;
                }
            }
        }
        return null;
    }

    /**
     * Updates a specific Uni Node on a specific DataStore type.
     * @param dataStore The datastore type
     * @param uniIID The UNI InstanceIdentifier
     * @param uni The Uni's data
     * @param ovsdbNode The Ovsdb Node
     * @param dataBroker The dataBroker instance to create transactions
     * @return true if uni is updated
     */
    public static boolean updateUniNode(LogicalDatastoreType dataStore,
                                     InstanceIdentifier<?> uniIID,
                                     UniAugmentation uni,
                                     Node ovsdbNode,
                                     DataBroker dataBroker) {
        final InstanceIdentifier<Node> ovsdbNodeIid = UnimgrMapper.getOvsdbNodeIid(ovsdbNode.getNodeId());
        final OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(ovsdbNodeIid);
        final UniAugmentationBuilder updatedUniBuilder = new UniAugmentationBuilder(uni);
        if (ovsdbNodeRef != null) {
            updatedUniBuilder.setOvsdbNodeRef(ovsdbNodeRef);
        }
        final Optional<Node> optionalNode = MdsalUtils.readNode(dataBroker,
                                               LogicalDatastoreType.CONFIGURATION,
                                               uniIID);
        if (optionalNode.isPresent()) {
            final Node node = optionalNode.get();
            final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            final NodeBuilder nodeBuilder = new NodeBuilder();
            nodeBuilder.setKey(node.getKey());
            nodeBuilder.setNodeId(node.getNodeId());
            nodeBuilder.addAugmentation(UniAugmentation.class, updatedUniBuilder.build());
            transaction.put(dataStore, uniIID.firstIdentifierOf(Node.class), nodeBuilder.build());
            transaction.submit();
            return true;
        }
        return false;
    }

    /**
     * Update a specific UNI node on a specific datastore type.
     * @param dataStore The datastore type
     * @param uniKey The UNI key
     * @param uni The Uni's data
     * @param ovsdbNodeIid The Ovsdb Node Instance Identifier
     * @param dataBroker The dataBroker instance to create transactions
     * @return true if uni is updated
     */
    public static boolean updateUniNode(LogicalDatastoreType dataStore,
                                     InstanceIdentifier<?> uniKey,
                                     UniAugmentation uni,
                                     InstanceIdentifier<?> ovsdbNodeIid,
                                     DataBroker dataBroker) {
        final OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(ovsdbNodeIid);
        final UniAugmentationBuilder updatedUniBuilder = new UniAugmentationBuilder(uni);
        if (ovsdbNodeRef != null) {
            updatedUniBuilder.setOvsdbNodeRef(ovsdbNodeRef);
        }
        final Optional<Node> optionalNode = MdsalUtils.readNode(dataBroker,
                                               LogicalDatastoreType.CONFIGURATION,
                                               uniKey);
        if (optionalNode.isPresent()) {
            final Node node = optionalNode.get();
            final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            final NodeBuilder nodeBuilder = new NodeBuilder();
            nodeBuilder.setKey(node.getKey());
            nodeBuilder.setNodeId(node.getNodeId());
            nodeBuilder.addAugmentation(UniAugmentation.class, updatedUniBuilder.build());
            transaction.put(dataStore, uniKey.firstIdentifierOf(Node.class), nodeBuilder.build());
            transaction.submit();
            return true;
        }
        return false;
    }

    /**
     * Convert Speed to string.
     * @param speedObject schema defined speed object
     * @return string representation
     */
    public static String getSpeed(Speed speedObject) {
        String speed = null;
        if (speedObject instanceof Speed10M) {
            // map to 10MB
            speed = "10000000";
        } else if (speedObject instanceof Speed100M) {
            // map to 20MB
            speed = "20000000";
        } else if (speedObject instanceof Speed1G) {
            // map to 30MB
            speed = "30000000";
        } else if (speedObject instanceof Speed10G) {
            // map to 40MB
            speed = "40000000";
        }
        return speed;
    }

    /**
     * Convert string to Speed.
     * @param speed string representation of speed
     * @return schema defined speed object
     */
    public static Speed getSpeed(final String speed) {
        Speed speedObject = null;
        if (speed.equals("10M")) {
            speedObject = new Speed10MBuilder().setSpeed10M(true)
                                               .build();
        } else if (speed.equals("100M")) {
            speedObject = new Speed100MBuilder().setSpeed100M(true)
                                                .build();
        } else if (speed.equals("1")) {
            speedObject = new Speed1GBuilder().setSpeed1G(true)
                                              .build();
        } else if (speed.equals("10")) {
            speedObject = new Speed10GBuilder().setSpeed10G(true)
                                               .build();
        }
        return speedObject;
    }
}
