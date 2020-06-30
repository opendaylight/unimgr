/*
 * Copyright (c) 2017 Amartus and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.cep.list.ConnectionEndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connection.end.point.ParentNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connection.end.point.ParentNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.OwnedNodeEdgePointRef;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.link.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.context.Topology;


public class NrpDaoIntTest extends AbstractTestWithTopo {

    private String uuid1 = "uuid1";
    private Uuid topologyId = new Uuid(TapiConstants.PRESTO_SYSTEM_TOPO);

    private OwnedNodeEdgePointRef toRef(String nodeId, String nepId) {
        return new NodeEdgePointBuilder()
                .setTopologyId(topologyId)
                .setNodeId(new Uuid(nodeId))
                .setOwnedNodeEdgePointId(new Uuid(nepId))
                .build();
    }

    private ParentNodeEdgePoint parentNep(String nodeId, String nepId) {
        ParentNodeEdgePoint parentNep = new ParentNodeEdgePointBuilder()
                .setTopologyId(topologyId)
                .setNodeId(new Uuid(nodeId))
                .setOwnedNodeEdgePointId(new Uuid(nepId))
                .build();
        return parentNep;
    }

    @Test
    public void testAddCeps()
            throws ReadFailedException, InterruptedException, ExecutionException {

        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, uuid1, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");

        NrpDao nrpDao = new NrpDao(tx);
        OwnedNodeEdgePointRef nepRef = toRef(uuid1, uuid1 + ":1");

        ConnectionEndPointBuilder builder = new ConnectionEndPointBuilder()
                .setConnectionPortDirection(PortDirection.BIDIRECTIONAL)
                .setLayerProtocolName(LayerProtocolName.ETH)
                .setClientNodeEdgePoint(Collections.emptyList())
                .setParentNodeEdgePoint(Collections.singletonList(
                        parentNep(uuid1, uuid1 + ":1")));

        ConnectionEndPoint cep1 = builder
                .setUuid(new Uuid("c001:" + uuid1 + ":1"))
                .build();

        ConnectionEndPoint cep2 = builder
                .setUuid(new Uuid("c002:" + uuid1 + ":1"))
                .build();


        nrpDao.addConnectionEndPoint(nepRef, cep1);
        nrpDao.addConnectionEndPoint(nepRef, cep2);
        tx.commit().get();

        checkCeps(uuid1, uuid1 + ":1", 2);

    }

    @Test
    public void testOverride()
            throws ReadFailedException, InterruptedException, ExecutionException {

        ConnectionEndPointBuilder builder = new ConnectionEndPointBuilder()
                .setClientNodeEdgePoint(Collections.emptyList())
                .setParentNodeEdgePoint(Collections.singletonList(parentNep(uuid1, uuid1 + ":1")));

        OwnedNodeEdgePointRef nepRef = toRef(uuid1, uuid1 + ":1");

        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, uuid1, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");

        NrpDao nrpDao = new NrpDao(tx);
        ConnectionEndPoint cep1 = builder
                .setUuid(new Uuid("c001:" + uuid1 + ":1"))
                .setLifecycleState(LifecycleState.INSTALLED)
                .build();
        nrpDao.addConnectionEndPoint(nepRef, cep1);
        tx.commit().get();

        tx = dataBroker.newReadWriteTransaction();

        nrpDao = new NrpDao(tx);
        ConnectionEndPoint cep2 = builder
                .setUuid(new Uuid("c001:" + uuid1 + ":1"))
                .setLifecycleState(LifecycleState.PENDINGREMOVAL)
                .build();
        nrpDao.addConnectionEndPoint(nepRef, cep2);
        tx.commit().get();

        OwnedNodeEdgePoint1 aug = checkCeps(uuid1, uuid1 + ":1", 1);
        ConnectionEndPoint endPoint = aug.getConnectionEndPoint().get(0);
        Assert.assertEquals(LifecycleState.PENDINGREMOVAL, endPoint.getLifecycleState());

    }

    @Test
    public void testRemoveConnection()
            throws ReadFailedException, InterruptedException, ExecutionException {

        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, uuid1, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");
        Connection connection = c(tx, uuid1, uuid1 + ":1", uuid1 + ":2");
        tx.commit().get();

        //when
        tx = dataBroker.newReadWriteTransaction();
        NrpDao nrpDao = new NrpDao(tx);
        nrpDao.removeConnection(connection.getUuid());
        tx.commit().get();

        //then
        tx = dataBroker.newReadWriteTransaction();
        assertNull(new NrpDao(tx).getConnection(connection.getUuid()));

        Topology topology = tx.read(LogicalDatastoreType.OPERATIONAL, NrpDao.topo(TapiConstants.PRESTO_SYSTEM_TOPO))
                .get().get();

        assertFalse(topology.getNode().stream().flatMap(n -> n.getOwnedNodeEdgePoint().stream())
                .anyMatch(nep -> {
                    //find nep with at least single CEP
                    OwnedNodeEdgePoint1 aug = nep.augmentation(OwnedNodeEdgePoint1.class);
                    return aug != null && aug.getConnectionEndPoint() != null && aug.getConnectionEndPoint().size() > 0;
                })
        );

    }

    private OwnedNodeEdgePoint1 checkCeps(String nodeid, String nepid, int noCeps)
            throws InterruptedException, ExecutionException {

        ReadTransaction tx = dataBroker.newReadOnlyTransaction();

        OwnedNodeEdgePoint nep = new NrpDao(tx).readNep(nodeid, nepid);
        Assert.assertNotNull(nep);
        OwnedNodeEdgePoint1 aug = nep.augmentation(OwnedNodeEdgePoint1.class);
        Assert.assertNotNull(aug);
        Assert.assertEquals(noCeps, aug.getConnectionEndPoint().size());
        return aug;

    }
}
