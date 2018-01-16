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

import com.google.common.base.Optional;
import org.junit.Before;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.unimgr.mef.nrp.api.TopologyManager;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.unimgr.mef.nrp.common.TapiUtils;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.*;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.context.attrs.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.ConnectivityServiceEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.create.connectivity.service.input.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.link.*;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.risk.parameter.pac.RiskCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.*;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.context.*;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.topology.context.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.transfer.cost.pac.CostCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.transfer.timing.pac.LatencyCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.topology.rev171113.validation.pac.ValidationMechanismBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

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

        Link link = new LinkBuilder()
                .setUuid(uuid)
                .setKey(new LinkKey(uuid))
                .setDirection(dir)
                .setLayerProtocolName(Collections.singletonList(ETH.class))
                .setNode(toIds(nA, nB).collect(Collectors.toList()))
                .setNodeEdgePoint(toIds(nepA, nepB).collect(Collectors.toList()))
                .setState(new StateBuilder().setOperationalState(state).build())
                .setTransferCost(emptyCost())
                .setTransferTiming(emptyTiming())
                .setRiskParameter(emptyRisk())
                .setValidation(emptyValidation())
                .setLpTransition(emptyLpTransition())
                .build();

        tx.put(LogicalDatastoreType.OPERATIONAL, NrpDao.topo(PRESTO_SYSTEM_TOPO).child(Link.class, new LinkKey(uuid)), link);
        return link;
    }

    private LpTransition emptyLpTransition() {
        return new LpTransitionBuilder()
                .setTransitionedLayerProtocolName(Collections.singletonList("n/a"))
                .build();
    }

    private Validation emptyValidation() {
        return new ValidationBuilder()
                .setValidationMechanism(Collections.singletonList(
                        new ValidationMechanismBuilder()
                            .setValidationMechanism("empty")
                            .setLayerProtocolAdjacencyValidated("n/a")
                            .setValidationRobustness("n/a")
                        .build()
                ))
                .build();
    }

    private RiskParameter emptyRisk() {
        return new RiskParameterBuilder()
                .setRiskCharacteristic(Collections.singletonList(
                        new RiskCharacteristicBuilder()
                        .setRiskCharacteristicName("empty").build()
                )).build();

    }

    private TransferTiming emptyTiming() {
        return new TransferTimingBuilder()
                .setLatencyCharacteristic(Collections.singletonList(
                        new LatencyCharacteristicBuilder()
                            .setTrafficPropertyName("empty")
                            .setTrafficPropertyQueingLatency("0")
                        .build()
                )).build();
    }

    private TransferCost emptyCost() {
        return new TransferCostBuilder()
                .setCostCharacteristic(Collections.singletonList(
                        new CostCharacteristicBuilder()
                            .setCostName("empty")
                            .setCostAlgorithm("n/a")
                            .setCostValue("0")
                        .build()
                )).build();
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
                    .setLayerProtocol(Collections.singletonList(TapiUtils.toSipPN(ETH.class)))
                    .build())
                    .forEach(nrpDao::addSip);
        }

        return nrpDao.createNode(TapiConstants.PRESTO_SYSTEM_TOPO, node, ETH.class, eps.stream()
                .map(e-> {
                    OwnedNodeEdgePointBuilder builder = new OwnedNodeEdgePointBuilder()
                            .setLinkPortDirection(e.getDir())
                            .setLayerProtocol(Collections.singletonList(TapiUtils.toNepPN(ETH.class)))
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
                    tx.read(LogicalDatastoreType.OPERATIONAL,NRP_ABSTRACT_NODE_IID).checkedGet();
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
