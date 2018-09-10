/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.template.tapi;

import static org.opendaylight.unimgr.mef.nrp.api.TapiConstants.PRESTO_SYSTEM_TOPO;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.unimgr.mef.nrp.api.TopologyManager;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.unimgr.mef.nrp.common.TapiUtils;
import org.opendaylight.unimgr.mef.nrp.template.TemplateConstants;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.common.types.rev180321.NaturalNumber;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.ServiceInterfacePoint1;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.ServiceInterfacePoint1Builder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.sip.attrs.NrpCarrierEthEnniNResourceBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.sip.attrs.NrpCarrierEthInniNResourceBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.sip.attrs.NrpCarrierEthUniNResourceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.tapi.context.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.link.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.link.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.edge.point.MappedServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Template driver topology handler. It demonstrates how to add topology information to the system.
 * Real driver uses infrastructure related information to populate this model and is responsible to keep it in sync
 * in case of topology changes.
 * @author bartosz.michalik@amartus.com
 */
public class TopologyDataHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyDataHandler.class);
    private DataBroker dataBroker;
    private TopologyManager topologyManager;

    private enum SIPType { uni, enni, inni }

    public TopologyDataHandler(DataBroker dataBroker, TopologyManager topologyManager) {
        this.dataBroker = dataBroker;
        this.topologyManager = topologyManager;
    }

    public void init() {
        Objects.requireNonNull(dataBroker);
        LOG.info("Starting topology handler");
        // this is a static and simplistic topology push to the TAPI system topology

        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();

        try {

            // we have prepared an dao abstraction to make it easier to use some of the common interactions with
            // MD-SAL but you can use tx.put tx.merge etc. by yourself if you prefere to
            NrpDao nrpDao = new NrpDao(tx);

            Node node1 = nrpDao.createNode(topologyManager.getSystemTopologyId(), "node-id-1",
                    TemplateConstants.DRIVER_ID, LayerProtocolName.ETH, null);
            Node node2 = nrpDao.createNode(topologyManager.getSystemTopologyId(), "node-id-2",
                    TemplateConstants.DRIVER_ID, LayerProtocolName.ETH, null);

            //we are creating a list of NodeEdgePoints for the nodes no sips are added to the system
            List<OwnedNodeEdgePoint> node1Endpoints = createSomeEndpoints(node1.getUuid()
                    .getValue(), 1, 2, 5, 7);
            List<OwnedNodeEdgePoint> node2Endpoints = createSomeEndpoints(node2.getUuid()
                    .getValue(), 1, 2, 5, 7);
            nrpDao.updateNep(node1.getUuid().getValue(), node1Endpoints.get(0));
            nrpDao.updateNep(node2.getUuid().getValue(), node2Endpoints.get(0));
            createLink(tx,node1,node1Endpoints.get(0),node2,node2Endpoints.get(0));
            //add sip for one of these endpoints

            //create sid and add it to model
            ServiceInterfacePoint someSip1 = createSomeSip("some-sip-1", SIPType.uni);
            ServiceInterfacePoint someSip2 = createSomeSip("some-sip-2", SIPType.inni);
            ServiceInterfacePoint someSip3 = createSomeSip("some-sip-3", SIPType.enni);
            nrpDao.addSip(someSip1);
            nrpDao.addSip(someSip2);
            nrpDao.addSip(someSip3);

            //update an existing nep with mapping to sip

            MappedServiceInterfacePoint sipRef1 =
                    TapiUtils.toSipRef(new Uuid(someSip1.getUuid()), MappedServiceInterfacePoint.class);
            MappedServiceInterfacePoint sipRef2 =
                    TapiUtils.toSipRef(new Uuid(someSip2.getUuid()), MappedServiceInterfacePoint.class);
            MappedServiceInterfacePoint sipRef3 =
                    TapiUtils.toSipRef(new Uuid(someSip3.getUuid()), MappedServiceInterfacePoint.class);

            OwnedNodeEdgePoint updatedNep1 = new OwnedNodeEdgePointBuilder(node1Endpoints.get(1))
                    .setMappedServiceInterfacePoint(Collections.singletonList(sipRef1))
                    .build();

            OwnedNodeEdgePoint updatedNep2 = new OwnedNodeEdgePointBuilder(node1Endpoints.get(2))
                    .setMappedServiceInterfacePoint(Collections.singletonList(sipRef2))
                    .build();

            OwnedNodeEdgePoint updatedNep3 = new OwnedNodeEdgePointBuilder(node2Endpoints.get(3))
                    .setMappedServiceInterfacePoint(Collections.singletonList(sipRef3))
                    .build();

            nrpDao.updateNep(node1.getUuid().getValue(), updatedNep1);
            nrpDao.updateNep(node1.getUuid().getValue(), updatedNep2);
            nrpDao.updateNep(node2.getUuid().getValue(), updatedNep3);


            tx.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Adding nodes to system topology has failed", e);
        }

    }

    private ServiceInterfacePoint createSomeSip(String idx, SIPType type) {

        ServiceInterfacePoint1Builder sipBuilder = new ServiceInterfacePoint1Builder();


        switch (type) {
            case enni:
                sipBuilder.setNrpCarrierEthEnniNResource(new NrpCarrierEthEnniNResourceBuilder()
                    .setMaxFrameSize(new NaturalNumber(new Long(1024)))
                    .build()

                );
                break;
            case uni:
                sipBuilder.setNrpCarrierEthUniNResource(new NrpCarrierEthUniNResourceBuilder()
                        .setMaxFrameSize(new NaturalNumber(new Long(1024)))
                        .build()

                );
                break;
            case inni:
            default:
                sipBuilder.setNrpCarrierEthInniNResource(new NrpCarrierEthInniNResourceBuilder()
                        .setMaxFrameSize(new NaturalNumber(new Long(1024)))
                        .build()

                );
                break;
        }

        return new ServiceInterfacePointBuilder()
                .setUuid(new Uuid("sip" + ":" + TemplateConstants.DRIVER_ID + ":" + idx))
                .setLayerProtocolName(Collections.singletonList(LayerProtocolName.ETH))
                .addAugmentation(ServiceInterfacePoint1.class, sipBuilder.build())
                .build();
    }

    private List<OwnedNodeEdgePoint> createSomeEndpoints(String nodeId, int... indexes) {

        return Arrays.stream(indexes).mapToObj(idx -> new OwnedNodeEdgePointBuilder()
                .setUuid(new Uuid(nodeId + ":nep" + idx))
                .setLayerProtocolName(LayerProtocolName.ETH)
                .setLinkPortDirection(PortDirection.BIDIRECTIONAL)
                .setLinkPortRole(PortRole.SYMMETRIC)
                .setAdministrativeState(AdministrativeState.UNLOCKED)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setOperationalState(OperationalState.DISABLED)
                .build()).collect(Collectors.toList());
    }

    private void createLink(ReadWriteTransaction tx, Node n1, OwnedNodeEdgePoint onep1, Node n2,
                            OwnedNodeEdgePoint onep2) {
        Uuid uuid = new Uuid(onep1.getUuid().getValue() + onep2.getUuid().getValue());


        NodeEdgePointBuilder builder = new NodeEdgePointBuilder()
                .setTopologyId(new Uuid(TapiConstants.PRESTO_SYSTEM_TOPO));

        NodeEdgePoint nep1 = builder
                .setNodeId(n1.getUuid())
                .setOwnedNodeEdgePointId(onep1.getUuid())
                .build();
        NodeEdgePoint nep2 = builder
                .setNodeId(n2.getUuid())
                .setOwnedNodeEdgePointId(onep2.getUuid())
                .build();


        Link link = new LinkBuilder()
                .setUuid(uuid)
                .withKey(new LinkKey(uuid))
                .setDirection(ForwardingDirection.BIDIRECTIONAL)
                .setLayerProtocolName(Collections.singletonList(LayerProtocolName.ETH))
                .setNodeEdgePoint(Stream.of(nep1,nep2).collect(Collectors.toList()))
                .setOperationalState(OperationalState.ENABLED)
                .setTransitionedLayerProtocolName(Collections.emptyList())
                .setValidationMechanism(Collections.emptyList())
                .setCostCharacteristic(Collections.emptyList())
                .setLatencyCharacteristic(Collections.emptyList())
                .setRiskCharacteristic(Collections.emptyList())
                .build();

        tx.put(LogicalDatastoreType.OPERATIONAL,
                NrpDao.topo(PRESTO_SYSTEM_TOPO).child(Link.class, new LinkKey(uuid)), link);
    }

    public void close() {
        LOG.info("Closing topology handler");
    }

}
