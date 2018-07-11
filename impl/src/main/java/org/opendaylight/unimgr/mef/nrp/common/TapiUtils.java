/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.common;

import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.*;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.end.point.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.end.point.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.topology.constraint.IncludeNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.NodeRef;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.OwnedNodeEdgePointRef;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.link.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.edge.point.MappedServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.edge.point.MappedServiceInterfacePointBuilder;

/**
 * Utility methods.
 * @author bartosz.michalik@amartus.com
 */
public class TapiUtils {

    @SuppressWarnings("unchecked")
    public static <T extends ServiceInterfacePointRef> T toSipRef(Uuid uuid, Class<T> clazz) {
        if (ServiceInterfacePoint.class.isAssignableFrom(clazz)) {
            return (T) new ServiceInterfacePointBuilder().setServiceInterfacePointId(uuid).build();
        }
        if (MappedServiceInterfacePoint.class.isAssignableFrom(clazz)) {
            return (T) new MappedServiceInterfacePointBuilder().setServiceInterfacePointId(uuid).build();
        }
        return null;
    }

    public static NodeRef toNodeRef(Uuid topoUuid, Uuid nodeUuid) {
        return new IncludeNodeBuilder()
                .setTopologyId(topoUuid)
                .setNodeId(nodeUuid)
                .build();
    }

    public static OwnedNodeEdgePointRef toSysNepRef(Uuid nodeUuid, Uuid nepUuid) {
        return toNepRef(new Uuid(TapiConstants.PRESTO_SYSTEM_TOPO), nodeUuid, nepUuid);
    }

    public static OwnedNodeEdgePointRef toNepRef(Uuid topoUuid, Uuid nodeUuid, Uuid nepUuid) {
        return new NodeEdgePointBuilder()
                .setTopologyId(topoUuid)
                .setNodeId(nodeUuid)
                .setOwnedNodeEdgePointId(nepUuid)
                .build();
    }

    public static NodeRef toNodeRef(Uuid nodeUuid) {
        String topo = TapiConstants.PRESTO_ABSTRACT_NODE.equals(nodeUuid.getValue())
                ? TapiConstants.PRESTO_EXT_TOPO : TapiConstants.PRESTO_SYSTEM_TOPO;
        return toNodeRef(new Uuid(topo), nodeUuid);
    }
}
