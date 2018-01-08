/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl;

import static org.junit.Assert.fail;
import static org.opendaylight.unimgr.mef.nrp.api.TapiConstants.PRESTO_SYSTEM_TOPO;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Context;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.ETH;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.context.attrs.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.ConnectivityServiceEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.create.connectivity.service.input.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.link.StateBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.context.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

/**
 * @author bartosz.michalik@amartus.com
 */
public abstract class AbstractTestWithTopo extends AbstractConcurrentDataBrokerTest {
    protected static final InstanceIdentifier NRP_ABSTRACT_NODE_IID = InstanceIdentifier
            .create(Context.class)
            .augmentation(Context1.class)
            .child(Topology.class, new TopologyKey(new Uuid(TapiConstants.PRESTO_EXT_TOPO)))
            .child(Node.class,new NodeKey(new Uuid(TapiConstants.PRESTO_ABSTRACT_NODE)));

    protected DataBroker dataBroker;

    @Before
    public void setupBroker() throws Exception {
        dataBroker = getDataBroker();
        new NrpInitializer(dataBroker).init();
    }

    protected  EndPoint ep(String nepId) {
        return ep(nepId, PortDirection.BIDIRECTIONAL);
    }

    protected EndPoint ep(String nepId, PortDirection pd) {
        ConnectivityServiceEndPoint ep = new EndPointBuilder()
                .setLocalId("ep_" + nepId)
                .setDirection(pd)
                .setServiceInterfacePoint(new Uuid("sip:" + nepId))
                .build();

        return new EndPoint(ep, null);
    }

    protected void l(ReadWriteTransaction tx, String nA, String nepA, String nB, String nepB, OperationalState state) {
        l(tx, nA, nepA, nB, nepB, state, ForwardingDirection.BIDIRECTIONAL);
    }

    protected void l(ReadWriteTransaction tx, String nA, String nepA, String nB, String nepB, OperationalState state, ForwardingDirection dir) {
        Uuid uuid = new Uuid(nepA + "-" + nepB);

        NrpDao dao = new NrpDao(tx);

        if(dao.hasSip(nepA)) {
            dao.removeSip(new Uuid("sip:" + nepA));
        }

        if(dao.hasSip(nepB)) {
            dao.removeSip(new Uuid("sip:" + nepB));
        }

        Link link = new LinkBuilder()
                .setUuid(uuid)
                .setKey(new LinkKey(uuid))
                .setDirection(dir)
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
        return n(tx, addSips, node, Arrays.stream(endpoints).map(i -> new Pair(i, PortDirection.BIDIRECTIONAL)));
    }

    protected Node n(ReadWriteTransaction tx, boolean addSips, String node, Stream<Pair> endpoints) {
        List<Pair> eps = endpoints.collect(Collectors.toList());
        NrpDao nrpDao = new NrpDao(tx);
        if (addSips) {
            eps.stream().map(e -> new ServiceInterfacePointBuilder()
                    .setUuid(new Uuid("sip:" + e.getId()))
                    .build())
                    .forEach(nrpDao::addSip);
        }

        return nrpDao.createSystemNode(node, eps.stream()
                .map(e-> {
                    OwnedNodeEdgePointBuilder builder = new OwnedNodeEdgePointBuilder()
                            .setLinkPortDirection(e.getDir())
                            .setUuid(new Uuid(e.getId()));
                    if (addSips) {
                        builder.setMappedServiceInterfacePoint(Collections.singletonList(new Uuid("sip:" + e.getId())));
                    }
                    return builder.build();
                }).collect(Collectors.toList()));
    }

    protected Node getAbstractNode() {

        try(ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction()) {
            Optional<Node> opt =
                    (Optional<Node>) tx.read(LogicalDatastoreType.OPERATIONAL,NRP_ABSTRACT_NODE_IID).checkedGet();
            if (opt.isPresent()) {
                return opt.get();
            } else {
                return null;
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }

        return null;
    }


    protected Node getAbstractNode(Predicate<Node> nodePredicate) {

        for(int i = 0; i < 5; ++i) {
            Node node = getAbstractNode();
            if(node != null && nodePredicate.test(node)) {
                return node;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        throw new IllegalStateException("No NEPs matching predicate");
    }



    protected static class Pair {
        private String id;
        private PortDirection dir;

        public Pair(String id, PortDirection dir) {
            this.id = id;
            this.dir = dir;
        }

        public String getId() {
            return id;
        }

        public PortDirection getDir() {
            return dir;
        }
    }

    protected Node n(ReadWriteTransaction tx, String node, String ... endpoints) {
        return n(tx,true, node, endpoints);
    }
}
