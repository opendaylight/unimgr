/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.transaction;

import java.util.List;
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.common.ResourceNotAvailableException;
import org.opendaylight.unimgr.mef.nrp.ovs.util.MdsalUtilsExt;
import org.opendaylight.unimgr.mef.nrp.ovs.util.NullAwareDatastoreGetter;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * Performs reading transactions related to openflow topology
 * during OvsDriver activation/deactivation.
 *
 * @author jakub.niezgoda@amartus.com
 */
public class TopologyTransaction {
    private DataBroker dataBroker;

    private static final String NODE_NOT_FOUND_ERROR_MESSAGE = "Node with port '%s' not found in OPERATIONAL data store.";
    private static final String LINKS_NOT_FOUND_ERROR_MESSAGE = "Links for node '%s' not found in OPERATIONAL data store.";
    private static final String TOPOLOGY_NOT_FOUND_ERROR_MESSAGE = "Topology '%s' not found in OPERATIONAL data store.";

    private static final String FLOW_TOPOLOGY_NAME = "flow:1";
    private static final String INTERSWITCH_LINK_ID_REGEX = "openflow:\\d+:\\d+";

    private static final Logger LOG = LoggerFactory.getLogger(TopologyTransaction.class);

    /**
     * Creates and initialize TopologyTransaction object
     *
     * @param dataBroker access to data tree store
     */
    public TopologyTransaction(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    /**
     * Returns list of nodes in openflow topology
     *
     * @return list of nodes
     */
    public List<NullAwareDatastoreGetter<Node>> readNodes() {
        return new NullAwareDatastoreGetter<Nodes>(MdsalUtils.readOptional(dataBroker, LogicalDatastoreType.OPERATIONAL, getNodesInstanceId())).collectMany(x -> x::getNode);
    }

    /**
     * Returns openflow node containing port portName
     *
     * @param portName node's port name
     * @return node
     * @throws ResourceNotAvailableException if node for the specified port name was not found
     */
    public Node readNode(String portName) throws ResourceNotAvailableException {
        for (NullAwareDatastoreGetter<Node> node : readNodes()) {
            if (node.get().isPresent()) {
                for (NodeConnector nodeConnector:node.get().get().getNodeConnector()) {
                    FlowCapableNodeConnector flowCapableNodeConnector
                            = nodeConnector.augmentation(FlowCapableNodeConnector.class);
                    if (portName.equals(flowCapableNodeConnector.getName())) {
                        return node.get().get();
                    }
                }
            }
        }

        LOG.warn(String.format(NODE_NOT_FOUND_ERROR_MESSAGE, portName));
        throw new ResourceNotAvailableException(String.format(NODE_NOT_FOUND_ERROR_MESSAGE, portName));
    }

    public Node readNodeOF(String ofportName) throws ResourceNotAvailableException {
        String ofNodeName = ofportName.split(":")[0]+":"+ofportName.split(":")[1];
        Nodes nodes = readOpenFLowTopology(dataBroker);
        if (nodes != null) {
            for (Node node: nodes.getNode()) {
                if (node.getId().getValue().equals(ofNodeName)) {
                    return node;
                }
            }
        } else {
            LOG.warn(String.format(NODE_NOT_FOUND_ERROR_MESSAGE, "OpenFlow"));
            throw new ResourceNotAvailableException(String.format(NODE_NOT_FOUND_ERROR_MESSAGE, "OpenFlow"));
        }

        LOG.warn(String.format(NODE_NOT_FOUND_ERROR_MESSAGE, ofportName));
        throw new ResourceNotAvailableException(String.format(NODE_NOT_FOUND_ERROR_MESSAGE, ofportName));
    }

    /**
     * Returns links associated with specified node
     *
     * @param node openflow node
     * @return list of links
     * @throws ResourceNotAvailableException if openflow topology or links for the specified node were not found
     */
    public List<Link> readLinks(Node node) throws ResourceNotAvailableException {
        Optional<Topology> flowTopology
                = MdsalUtilsExt.readTopology(dataBroker, LogicalDatastoreType.OPERATIONAL, FLOW_TOPOLOGY_NAME);

        if (flowTopology.isPresent()) {
            String nodeId = node.getId().getValue();
            List<Link> links = flowTopology.get().getLink()
                                           .stream()
                                           .filter(link -> link.getLinkId().getValue().startsWith(nodeId))
                                           .collect(Collectors.toList());
            if (links.isEmpty()) {
                LOG.warn(String.format(LINKS_NOT_FOUND_ERROR_MESSAGE, nodeId));
                throw new ResourceNotAvailableException(String.format(LINKS_NOT_FOUND_ERROR_MESSAGE, nodeId));
            } else {
                return links;
            }
        } else {
            LOG.warn(String.format(TOPOLOGY_NOT_FOUND_ERROR_MESSAGE, FLOW_TOPOLOGY_NAME));
            throw new ResourceNotAvailableException(String.format(TOPOLOGY_NOT_FOUND_ERROR_MESSAGE, FLOW_TOPOLOGY_NAME));
        }
    }

    /**
     * Returns interswitch links (links between openflow nodes) associated with specified node
     *
     * @param node openflow node
     * @return list of links
     * @throws ResourceNotAvailableException if openflow topology or links for the specified node were not found
     */
    public List<Link> readInterswitchLinks(Node node) throws ResourceNotAvailableException {
        List<Link> links = readLinks(node);
        return links.stream()
                    .filter(link -> link.getLinkId().getValue().matches(INTERSWITCH_LINK_ID_REGEX))
                    .collect(Collectors.toList());
    }

    private InstanceIdentifier<Nodes> getNodesInstanceId() {
        return InstanceIdentifier.builder(Nodes.class).build();
    }

    public static Nodes readOpenFLowTopology(DataBroker dataBroker) {
        InstanceIdentifier instanceIdentifier = InstanceIdentifier.builder(Nodes.class).build();
        return (Nodes) MdsalUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION,instanceIdentifier);
    }
}
