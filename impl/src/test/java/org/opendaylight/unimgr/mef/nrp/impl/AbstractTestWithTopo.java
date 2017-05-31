/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl;

import org.junit.Before;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.OperationalState;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.UniversalId;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.context.attrs.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170227.ConnectivityServiceEndPoint;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170227.create.connectivity.service.input.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170227.link.StateBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170227.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170227.topology.Link;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170227.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170227.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170227.topology.Node;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opendaylight.unimgr.mef.nrp.api.TapiConstants.PRESTO_SYSTEM_TOPO;

/**
 * @author bartosz.michalik@amartus.com
 */
public abstract class AbstractTestWithTopo extends AbstractDataBrokerTest {


    protected DataBroker dataBroker;

    @Before
    public void setupBroker() throws Exception {
        dataBroker = getDataBroker();
        new NrpInitializer(dataBroker).init();
    }

    protected EndPoint ep(String nepId) {
        ConnectivityServiceEndPoint ep = new EndPointBuilder()
                .setLocalId("ep_" + nepId)
                .setServiceInterfacePoint(new UniversalId("sip:" + nepId))
                .build();

        return new EndPoint(ep, null);
    }
    protected void l(ReadWriteTransaction tx, String nA, String nepA, String nB, String nepB, OperationalState state) {
        UniversalId uuid = new UniversalId(nepA + "-" + nepB);
        Link link = new LinkBuilder()
                .setUuid(uuid)
                .setKey(new LinkKey(uuid))
                .setDirection(ForwardingDirection.Bidirectional)
                .setLayerProtocolName(Collections.singletonList(LayerProtocolName.Eth))
                .setNode(toIds(nA, nB).collect(Collectors.toList()))
                .setNodeEdgePoint(toIds(nepA, nepB).collect(Collectors.toList()))
                .setState(new StateBuilder().setOperationalState(state).build())
                .build();

        tx.put(LogicalDatastoreType.OPERATIONAL, NrpDao.topo(PRESTO_SYSTEM_TOPO).child(Link.class, new LinkKey(uuid)), link);
    }

    protected Stream<UniversalId> toIds(String ... uuids) {
        return toIds(Arrays.stream(uuids));
    }

    protected Stream<UniversalId> toIds(Stream<String> uuids) {
        return uuids.map(UniversalId::new);
    }

    protected Node n(ReadWriteTransaction tx, boolean addSips, String node, String ... endpoints) {
        NrpDao nrpDao = new NrpDao(tx);
        if(addSips) Arrays.stream(endpoints).map(e -> new ServiceInterfacePointBuilder()
                .setUuid(new UniversalId("sip:" + e))
                .build())
                .forEach(nrpDao::addSip);


        return nrpDao.createSystemNode(node, Arrays.stream(endpoints)
                .map(e-> {
                    OwnedNodeEdgePointBuilder builder = new OwnedNodeEdgePointBuilder().setUuid(new UniversalId(e));
                    if(addSips) {
                        builder.setMappedServiceInterfacePoint(Collections.singletonList(new UniversalId("sip:"+e)));
                    }
                    return builder.build();
                }).collect(Collectors.toList()));
    }

    protected Node n(ReadWriteTransaction tx, String node, String ... endpoints) {
        return n(tx,true, node, endpoints);
    }
}
