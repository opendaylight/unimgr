/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.tapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.TopologyManager;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.unimgr.mef.nrp.common.ResourceNotAvailableException;
import org.opendaylight.unimgr.mef.nrp.common.TapiUtils;
import org.opendaylight.unimgr.mef.nrp.ovs.transaction.TopologyTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.tapi.context.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.edge.point.MappedServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * TopologyDataHandler listens to ovsdb topology and propagate significant changes to presto ext topology.
 *
 * @author bartosz.michalik@amartus.com
 */
public class TopologyDataHandler implements DataTreeChangeListener<Node> {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyDataHandler.class);
    private static final String OVS_NODE = "ovs-node";
    private static final String OVS_DRIVER_ID = "ovs-driver";
    private static final String DELIMETER = ":";
    private static final InstanceIdentifier<Topology> OVSDB_TOPO_IID = InstanceIdentifier
            .create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(new Uri("ovsdb:1"))));
    private ListenerRegistration<TopologyDataHandler> registration;
    private TopologyTransaction topologyTransaction;
    private DataObjectModificationQualifier dataObjectModificationQualifier;

    private final TopologyManager topologyManager;

    private final DataBroker dataBroker;
    private final int MAX_RETRIALS = 5;

    public TopologyDataHandler(DataBroker dataBroker, TopologyManager topologyManager) {
        Objects.requireNonNull(dataBroker);
        this.dataBroker = dataBroker;
        this.topologyManager = topologyManager;
        topologyTransaction = new TopologyTransaction(dataBroker);
    }

    public void init() {
        initializeWithRetrial(MAX_RETRIALS);
    }

    private void initializeWithRetrial(int retrialCouter) {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();

        NrpDao dao = new NrpDao(tx);

        dao.createNode(topologyManager
                .getSystemTopologyId(), OVS_NODE, OVS_DRIVER_ID, LayerProtocolName.ETH, null, new ArrayList<>());

        Futures.addCallback(tx.commit(), new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(@Nullable CommitInfo result) {
                LOG.info("Node {} created", OVS_NODE);
                registerOvsdbTreeListener();
            }

            @Override
            public void onFailure(Throwable t) {
                if (retrialCouter != 0) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) { }
                    if (retrialCouter != MAX_RETRIALS) {
                        LOG.debug("Retrying initialization of {} for {} time",
                                OVS_NODE, MAX_RETRIALS - retrialCouter + 1);
                    }
                    initializeWithRetrial(retrialCouter - 1);
                } else {
                    LOG.error("No node created due to the error", t);
                }

            }
        }, MoreExecutors.directExecutor());
    }


    private void registerOvsdbTreeListener() {
        dataObjectModificationQualifier = new DataObjectModificationQualifier(dataBroker);
        InstanceIdentifier<Node> nodeId = OVSDB_TOPO_IID.child(Node.class);
        registration = dataBroker
                .registerDataTreeChangeListener(
                        DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, nodeId), this);
    }

    public void close() {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();

        NrpDao dao = new NrpDao(tx);
        dao.removeNode(OVS_NODE, true);

        Futures.addCallback(tx.commit(), new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(@Nullable CommitInfo result) {
                LOG.info("Node {} deleted", OVS_NODE);
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("No node deleted due to the error", t);
            }
        }, MoreExecutors.directExecutor());

        if (registration != null) {
            LOG.info("closing netconf tree listener");
            registration.close();
        }
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<Node>> collection) {
        final Map<TerminationPoint,String> toAddMap = new HashMap<>();
        final Map<TerminationPoint,String> toDeleteMap = new HashMap<>();
        final Map<TerminationPoint,String> toUpdateMap = new HashMap<>();

        List<DataObjectModification<? extends DataObject>> nodes = collection.stream()
                .map(DataTreeModification::getRootNode)
                .collect(Collectors.toList());

        dataObjectModificationQualifier.checkNodes(nodes,toAddMap,toUpdateMap,toDeleteMap);

        executeDbAction(addAction,toAddMap);
        executeDbAction(updateAction,toUpdateMap);
        executeDbAction(deleteAction,toDeleteMap);
    }

    BiConsumer<Map<TerminationPoint,String>,NrpDao> updateAction = (map,dao) -> {
        map.entrySet()
                .forEach(entry -> {
                    String nepId = OVS_NODE + DELIMETER
                            + getFullPortName(entry.getValue(), entry.getKey().getTpId().getValue());
                    OwnedNodeEdgePoint nep;
                    if (dao.hasSip(nepId)) {
                        nep = createNep(nepId);
                        dao.updateNep(OVS_NODE,nep);
                    } else {
                        addEndpoint(dao,nepId);
                    }
                });
    };

    BiConsumer<Map<TerminationPoint,String>,NrpDao> deleteAction = (map,dao) -> {
        map.entrySet()
                .forEach(entry -> {
                    String nepId = OVS_NODE + DELIMETER
                            + getFullPortName(entry.getValue(), entry.getKey().getTpId().getValue());
                    dao.removeNep(OVS_NODE,nepId,true);
                });
    };

    BiConsumer<Map<TerminationPoint,String>,NrpDao> addAction = (map,dao) -> {
        List<OwnedNodeEdgePoint> newNeps = getNewNeps(map);
        newNeps.forEach(nep -> addEndpoint(dao,nep.key().getUuid().getValue()));
    };

    private void executeDbAction(BiConsumer<Map<TerminationPoint,String>,NrpDao> action,
                                 Map<TerminationPoint,String> map) {
        if (map.isEmpty()) {
            return ;
        }
        final ReadWriteTransaction topoTx = dataBroker.newReadWriteTransaction();
        NrpDao dao = new NrpDao(topoTx);

        action.accept(map,dao);

        Futures.addCallback(topoTx.commit(), new FutureCallback<CommitInfo>() {

            @Override
            public void onSuccess(@Nullable CommitInfo result) {
                LOG.debug("Ovs TAPI node action executed successfully");
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.warn("Ovs TAPI node action execution failed due to an error", t);
            }
        }, MoreExecutors.directExecutor());
    }

    private String getFullPortName(String switchName, String portName) {
        return switchName + DELIMETER + portName;
    }

    private void addEndpoint(NrpDao dao, String nepName) {
        ServiceInterfacePoint sip = createSip(nepName);

        dao.addSip(sip);
        dao.updateNep(OVS_NODE, nep(nepName, sip.getUuid()));
    }

    private OwnedNodeEdgePoint nep(String nepName, Uuid sipUuid) {
        Uuid uuid = new Uuid(nepName);
        return new OwnedNodeEdgePointBuilder()
                .setUuid(uuid)
                .withKey(new OwnedNodeEdgePointKey(uuid))
                .setLinkPortDirection(PortDirection.BIDIRECTIONAL)
                .setLinkPortRole(PortRole.SYMMETRIC)
                .setLayerProtocolName(LayerProtocolName.ETH)
                .setMappedServiceInterfacePoint(
                        Collections.singletonList(TapiUtils.toSipRef(sipUuid, MappedServiceInterfacePoint.class)))
                .build();
    }

    private ServiceInterfacePoint createSip(String nep) {
        Uuid uuid = new Uuid("sip" + DELIMETER + nep);
        return new ServiceInterfacePointBuilder()
                .setUuid(uuid)
//                .setKey(new ServiceInterfacePointKey(uuid))
                .setLayerProtocolName(Collections.singletonList(LayerProtocolName.ETH))
                .setLifecycleState(LifecycleState.INSTALLED)
                .build();
    }

    private OwnedNodeEdgePoint createNep(String nepId) {
        OwnedNodeEdgePointBuilder tpBuilder = new OwnedNodeEdgePointBuilder();
        Uuid tpId = new Uuid(OVS_NODE + DELIMETER + nepId);
        return tpBuilder
                .setUuid(tpId)
                .withKey(new OwnedNodeEdgePointKey(tpId))
                .build();
    }

    private List<OwnedNodeEdgePoint> getNewNeps(Map<TerminationPoint,String> toAddMap) {
        return toAddMap.entrySet().stream()
                .map(entry -> createNep(getFullPortName(entry.getValue(),entry.getKey().getTpId().getValue())))
                .collect(Collectors.toList());
    }

    //TODO: write better implementation
    private boolean isNep(TerminationPoint terminationPoint) {
        OvsdbTerminationPointAugmentation ovsdbTerminationPoint
                = terminationPoint.augmentation(OvsdbTerminationPointAugmentation.class);
        if (ovsdbTerminationPoint == null
                || (ovsdbTerminationPoint.getInterfaceType() != null
                && ovsdbTerminationPoint.getInterfaceType().equals(InterfaceTypeInternal.class))) {
            return false;
        }

        if (ovsdbTerminationPoint.getOfport() == null) {
            return false;
        }

        String ofPortNumber = ovsdbTerminationPoint.getOfport().toString();
        try {
            org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node
                    = topologyTransaction.readNode(terminationPoint.getTpId().getValue());
            String ofPortName = node.getId().getValue() + ":" + ofPortNumber;
            List<Link> links = topologyTransaction.readLinks(node);
            return !links.stream()
                    .anyMatch(link -> link.getSource().getSourceTp().getValue().equals(ofPortName));
        } catch (ResourceNotAvailableException e) {
            LOG.warn(e.getMessage());
        }
        return false;
    }

    public static String getDriverId() {
        return OVS_DRIVER_ID;
    }

}