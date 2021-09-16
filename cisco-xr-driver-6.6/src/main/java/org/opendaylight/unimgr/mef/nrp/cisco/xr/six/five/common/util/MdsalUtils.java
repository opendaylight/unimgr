/*
 * Copyright (c) 2018 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.common.util;

import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.common.ServicePort;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/*
 * @author bartosz.michalik@amartus.com
 */
public final class MdsalUtils {

    private MdsalUtils() {
    }

    /*
     * Read a TerminationPoint from datastore used in given FcPort.
     * @param dataBroker The dataBroker instance to create transactions
     * @param store The datastore type.
     * @param topo TopologyId
     * @param ep FcPort data
     * @return An Optional TerminationPoint instance
     */
    public static Optional<TerminationPoint> readTerminationPoint(
                                                                DataBroker dataBroker,
                                                                LogicalDatastoreType store,
                                                                TopologyId topo,
                                                                EndPoint ep) {
        ServicePort servicePort = ServicePort.toServicePort(ep, topo);
        return readTerminationPoint(
                                dataBroker,
                                store,
                                servicePort.getTopology(),
                                servicePort.getNode(),
                                servicePort.getTp());
    }

    public static Optional<TerminationPoint> readTerminationPoint(
                                                                DataBroker dataBroker,
                                                                LogicalDatastoreType store,
                                                                TopologyId topologyId,
                                                                NodeId nodeId,
                                                                TpId tpId) {
        InstanceIdentifier<TerminationPoint> tpIid = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(topologyId))
                .child(Node.class, new NodeKey(nodeId))
                .child(TerminationPoint.class, new TerminationPointKey(tpId))
                .build();

        return org.opendaylight.unimgr.utils.MdsalUtils.readOptional(dataBroker, store, tpIid);
    }

}
