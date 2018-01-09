/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl;

import static org.opendaylight.unimgr.mef.nrp.api.TapiConstants.PRESTO_SYSTEM_TOPO;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.ETH;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.context.attrs.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.ConnectivityServiceEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.create.connectivity.service.input.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.link.StateBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.Node;

import com.google.common.base.Optional;

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
                .setServiceInterfacePoint(new Uuid("sip:" + nepId))
                .build();

        return new EndPoint(ep, null);
    }

    protected void l(ReadWriteTransaction tx, String nA, String nepA, String nB, String nepB, OperationalState state) {
        Uuid uuid = new Uuid(nepA + "-" + nepB);
        Link link = new LinkBuilder()
                .setUuid(uuid)
                .setKey(new LinkKey(uuid))
                .setDirection(ForwardingDirection.BIDIRECTIONAL)
                .setLayerProtocolName(Collections.singletonList(ETH.class))
                .setNode(toIds(nA, nB).collect(Collectors.toList()))
                .setNodeEdgePoint(toIds(nepA, nepB).collect(Collectors.toList()))
                .setState(new StateBuilder().setOperationalState(state).build())
                .build();

        tx.put(LogicalDatastoreType.OPERATIONAL, NrpDao.topo(PRESTO_SYSTEM_TOPO).child(Link.class, new LinkKey(uuid)), link);
    }

    protected Stream<Uuid> toIds(String ... uuids) {
        return toIds(Arrays.stream(uuids));
    }

    protected Stream<Uuid> toIds(Stream<String> uuids) {
        return uuids.map(Uuid::new);
    }

    protected Node n(ReadWriteTransaction tx, boolean addSips, String node, String ... endpoints) {
        NrpDao nrpDao = new NrpDao(tx);
        if (addSips) {
            Arrays.stream(endpoints).map(e -> new ServiceInterfacePointBuilder()
                .setUuid(new Uuid("sip:" + e))
                .build())
                .forEach(nrpDao::addSip);
        }

        return nrpDao.createSystemNode(node, Arrays.stream(endpoints)
                .map(e-> {
                    OwnedNodeEdgePointBuilder builder = new OwnedNodeEdgePointBuilder().setUuid(new Uuid(e));
                    if (addSips) {
                        builder.setMappedServiceInterfacePoint(Collections.singletonList(new Uuid("sip:" + e)));
                    }
                    return builder.build();
                }).collect(Collectors.toList()));
    }

    protected Node n(ReadWriteTransaction tx, String node, String ... endpoints) {
        return n(tx,true, node, endpoints);
    }
}
