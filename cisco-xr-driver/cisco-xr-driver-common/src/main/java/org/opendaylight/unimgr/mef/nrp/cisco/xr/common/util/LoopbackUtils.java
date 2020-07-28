/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util;

import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.topology.ext.rev180531.LoopbackAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Tools designated to support operations on loopback interfaces data
 *
 * @author krzysztof.bijakowski@amartus.com
 * @author marek.ryznar@amartus.com [modifications]
 */
public final class LoopbackUtils {

    private LoopbackUtils() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(LoopbackUtils.class);

    private static final String DEFAULT_LOOPBACK = "127.0.0.1";

    //First, loopback address must be added with netconf device on POST request
    public static Ipv4AddressNoZone getIpv4Address(ServicePort port, DataBroker dataBroker) {
        String loopback = null;
        NodeId nodeId = port.getNode();
        TopologyId topologyId = port.getTopology();
        Optional<Node> nodeOpt = MdsalUtils.readOptional(
                                                        dataBroker,
                                                        LogicalDatastoreType.CONFIGURATION,
                                                        getNodeIid(nodeId,topologyId));

        if (nodeOpt.isPresent()) {
            LoopbackAugmentation la = nodeOpt.get().augmentation(LoopbackAugmentation.class);

            if (la != null) {
                loopback = la.getLoopbackAddress().getIpv4Address().getValue();
            }
        }

        if (loopback == null) {
            LOG.warn("No loopback address found for {}", nodeId.getValue());
            loopback = DEFAULT_LOOPBACK;
        }

        return new Ipv4AddressNoZone(loopback);
    }

    public static String getDefaultLoopback() {
        return DEFAULT_LOOPBACK;
    }

    public static InstanceIdentifier<Node> getNodeIid(NodeId nodeId, TopologyId topologyId) {
        InstanceIdentifier<Node> nodeInstanceId = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(topologyId))
                .child(Node.class, new NodeKey(nodeId))
                .build();
        return nodeInstanceId;
    }
}
