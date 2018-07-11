/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl;

import com.google.common.base.Optional;
import org.junit.Before;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.unimgr.mef.nrp.common.TapiUtils;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.common.types.rev180321.NaturalNumber;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.common.types.rev180321.PositiveInteger;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.common.types.rev180321.VlanId;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev180321.carrier.eth.connectivity.end.point.resource.CeVlanIdListAndUntag;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev180321.carrier.eth.connectivity.end.point.resource.CeVlanIdListAndUntagBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev180321.vlan.id.list.and.untag.VlanIdBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint2Builder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.NrpConnectivityServiceEndPointAttrs;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.ServiceInterfacePoint1;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.ServiceInterfacePoint1Builder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.connectivity.service.end.point.attrs.NrpCarrierEthConnectivityEndPointResourceBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.sip.attrs.NrpCarrierEthInniNResourceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.*;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.tapi.context.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ConnectivityServiceEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connection.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connection.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.end.point.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.create.connectivity.service.input.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.OwnedNodeEdgePointRef;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.link.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.link.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.edge.point.MappedServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.*;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.context.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.fail;
import static org.opendaylight.unimgr.mef.nrp.api.TapiConstants.PRESTO_SYSTEM_TOPO;

/**
 * @author bartosz.michalik@amartus.com
 */
public abstract class AbstractTestWithTopo extends AbstractConcurrentDataBrokerTest {
    protected static final InstanceIdentifier<Node> NRP_ABSTRACT_NODE_IID = InstanceIdentifier
            .create(Context.class)
            .augmentation(Context1.class)
            .child(Topology.class, new TopologyKey(new Uuid(TapiConstants.PRESTO_EXT_TOPO)))
            .child(Node.class,new NodeKey(new Uuid(TapiConstants.PRESTO_ABSTRACT_NODE)));

    protected DataBroker dataBroker;

    protected NrpInitializer topologyManager;

    @Before
    public void setupBroker() throws Exception {
        dataBroker = getDataBroker();
        topologyManager = new NrpInitializer(dataBroker);
        topologyManager.init();
        new AbstractNodeHandler(dataBroker).init();
    }

    protected  EndPoint ep(String nepId) {
        return ep(nepId, PortDirection.BIDIRECTIONAL);
    }

    protected EndPoint ep(String nepId, PortDirection pd) {

        ConnectivityServiceEndPoint ep = new EndPointBuilder()
                .setLocalId("ep_" + nepId)
                .setDirection(pd)
                .setServiceInterfacePoint(TapiUtils.toSipRef(new Uuid("sip:" + nepId), ServiceInterfacePoint.class))
                .build();

        return new EndPoint(ep, someAttributes());
    }

    CeVlanIdListAndUntag toVlanList(long... vlans) {
        List<org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev180321.vlan.id.list.and.untag.VlanId> vlanList = Arrays.stream(vlans).mapToObj(vlan -> new VlanIdBuilder().setVlanId(new PositiveInteger(vlan)).build())
                .collect(Collectors.toList());
        return new CeVlanIdListAndUntagBuilder().setVlanId(vlanList).build();

    }

    private NrpConnectivityServiceEndPointAttrs someAttributes() {
        return new EndPoint2Builder()
                .setNrpCarrierEthConnectivityEndPointResource(
                    new NrpCarrierEthConnectivityEndPointResourceBuilder()
                        .setCeVlanIdListAndUntag(toVlanList(1004L)
                        ).build()
                ).build();
    }

    protected Link l(ReadWriteTransaction tx, String nA, String nepA, String nB, String nepB, OperationalState state) {
        return l(tx, nA, nepA, nB, nepB, state, ForwardingDirection.BIDIRECTIONAL);
    }

    protected Link l(ReadWriteTransaction tx, String nA, String nepA, String nB, String nepB, OperationalState state, ForwardingDirection dir) {
        Uuid uuid = new Uuid(nepA + "-" + nepB);

        NrpDao dao = new NrpDao(tx);

        if(dao.hasSip(nepA)) {
            dao.removeSip(new Uuid("sip:" + nepA));
        }

        if(dao.hasSip(nepB)) {
            dao.removeSip(new Uuid("sip:" + nepB));
        }

        NodeEdgePointBuilder builder = new NodeEdgePointBuilder().setTopologyId(new Uuid(TapiConstants.PRESTO_SYSTEM_TOPO));

        NodeEdgePoint nepRefA = builder
                .setNodeId(new Uuid(nA))
                .setOwnedNodeEdgePointId(new Uuid(nepA))
                .build();

        NodeEdgePoint nepRefB = builder
                .setNodeId(new Uuid(nB))
                .setOwnedNodeEdgePointId(new Uuid(nepB))
                .build();


        Link link = new LinkBuilder()
                .setUuid(uuid)
                .withKey(new LinkKey(uuid))
                .setDirection(dir)
                .setLayerProtocolName(Collections.singletonList(LayerProtocolName.ETH))
                .setOperationalState(state)
                .setTransitionedLayerProtocolName(Collections.emptyList())
                .setCostCharacteristic(Collections.emptyList())
                .setLatencyCharacteristic(Collections.emptyList())
                .setRiskCharacteristic(Collections.emptyList())
                .setValidationMechanism(Collections.emptyList())
                .setNodeEdgePoint(Stream.of(nepRefA, nepRefB).collect(Collectors.toList()))

                .build();

        tx.put(LogicalDatastoreType.OPERATIONAL, NrpDao.topo(PRESTO_SYSTEM_TOPO).child(Link.class, new LinkKey(uuid)), link);
        return link;
    }

    protected Node n(ReadWriteTransaction tx, boolean addSips, Uuid node, String activationDriverId, String ... endpoints) {
        return n(tx, addSips, node.getValue(), activationDriverId, Arrays.stream(endpoints).map(i -> new Pair(i, PortDirection.BIDIRECTIONAL)));
    }

    protected Node n(ReadWriteTransaction tx, boolean addSips, String node, String activationDriverId, Stream<Pair> endpoints) {
        List<Pair> eps = endpoints.collect(Collectors.toList());
        NrpDao nrpDao = new NrpDao(tx);
        if (addSips) {

            ServiceInterfacePoint1Builder sipBuilder = new ServiceInterfacePoint1Builder();
            sipBuilder.setNrpCarrierEthInniNResource(new NrpCarrierEthInniNResourceBuilder()
                    .setMaxFrameSize(new NaturalNumber(2048L))
            .build());

            eps.stream().map(e -> new ServiceInterfacePointBuilder()
                    .setUuid(new Uuid("sip:" + e.getId()))
                    .setLayerProtocolName(Collections.singletonList(LayerProtocolName.ETH))
                    .addAugmentation(ServiceInterfacePoint1.class, sipBuilder.build())
                    .build())
                    .forEach(nrpDao::addSip);
        }

        return nrpDao.createNode(TapiConstants.PRESTO_SYSTEM_TOPO, node, activationDriverId, LayerProtocolName.ETH, eps.stream()
                .map(e-> {
                    OwnedNodeEdgePointBuilder builder = new OwnedNodeEdgePointBuilder()
                            .setLinkPortDirection(e.getDir())
                            .setLayerProtocolName(LayerProtocolName.ETH)
                            .setUuid(new Uuid(e.getId()));
                    if (addSips) {
                        builder.setMappedServiceInterfacePoint(Collections.singletonList(TapiUtils.toSipRef(new Uuid("sip:" + e.getId()), MappedServiceInterfacePoint.class)));
                    }
                    return builder.build();
                }).collect(Collectors.toList()));
    }

    protected org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.Connection c(ReadWriteTransaction tx, String nodeUuid, List<Uuid> route, String... neps) {
        ConnectionBuilder builder = new ConnectionBuilder()
                .setUuid(new Uuid("c:" + nodeUuid))
                .setConnectionEndPoint(ceps(tx, nodeUuid, neps));

        if (!route.isEmpty()) {
            builder.setRoute(Collections.singletonList(new RouteBuilder()
                    .setConnectionEndPoint(route)
                    .setLocalId("route")
                    .build()
            ));
        }

        Connection connection = builder.build();
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.Context1> connectivityCtx = NrpDao.ctx().augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.Context1.class);
        tx.put(LogicalDatastoreType.OPERATIONAL, connectivityCtx.child(Connection.class,  new ConnectionKey(connection.getUuid())), connection);

        return connection;
    }


    protected org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.Connection c(ReadWriteTransaction tx, String nodeUuid, String... neps) {
        return c(tx, nodeUuid, Collections.emptyList(), neps);
    }

    private List<ConnectionEndPoint> ceps(ReadWriteTransaction tx, String nodeId, String... neps) {
        NrpDao nrpDao = new NrpDao(tx);

        return  Arrays.stream(neps).map(nep -> {
            OwnedNodeEdgePointRef nepRef = toRef(nodeId, nep);
            return nrpDao.addConnectionEndPoint(nepRef, dummyCep(nepRef));

        }).collect(Collectors.toList());
    }

    private OwnedNodeEdgePointRef toRef(String nodeId, String nepId) {
        return new NodeEdgePointBuilder()
                .setTopologyId(new Uuid(TapiConstants.PRESTO_ABSTRACT_NODE.equals(nodeId) ?
                        TapiConstants.PRESTO_EXT_TOPO : TapiConstants.PRESTO_SYSTEM_TOPO ))
                .setNodeId(new Uuid(nodeId))
                .setOwnedNodeEdgePointId(new Uuid(nepId))
                .build();
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.cep.list.ConnectionEndPoint dummyCep(OwnedNodeEdgePointRef nepRef) {
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.cep.list.ConnectionEndPointBuilder builder
                = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.cep.list.ConnectionEndPointBuilder();

        return builder
                .setUuid(new Uuid("cep:" + nepRef.getOwnedNodeEdgePointId().getValue()))
                .setConnectionPortRole(PortRole.SYMMETRIC)
                .setTerminationDirection(TerminationDirection.BIDIRECTIONAL)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setOperationalState(OperationalState.ENABLED)
                .build();
    }


    protected Node getAbstractNode() {

        try(ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction()) {
            Optional<Node> opt =
                    tx.read(LogicalDatastoreType.OPERATIONAL,NRP_ABSTRACT_NODE_IID).get();
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
        return n(tx,true, new Uuid(node), node, endpoints);
    }

    protected Node n(ReadWriteTransaction tx, Uuid node, String activationDriverId, String ... endpoints) {
        return n(tx,true, node, activationDriverId, endpoints);
    }
}
