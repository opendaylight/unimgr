/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.cisco.xr;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.unimgr.mef.nrp.api.TopologyManager;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.InterfaceHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util.XrCapabilitiesService;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.driver.XrDriverBuilder;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.unimgr.mef.nrp.common.TapiUtils;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.tapi.context.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.edge.point.MappedServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util.XrCapabilitiesService.NodeCapability.*;
import static org.opendaylight.unimgr.utils.CapabilitiesService.Capability.Mode.AND;

/**
 * @author bartosz.michalik@amartus.com
 */
public class TopologyDataHandler implements DataTreeChangeListener<Node> {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyDataHandler.class);
    public static final InstanceIdentifier<Topology> NETCONF_TOPO_IID =
            InstanceIdentifier
                    .create(NetworkTopology.class)
                    .child(Topology.class,
                            new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));

    private final int MAX_RETRIALS = 5;

    private final  TopologyManager topologyManager;

    LoadingCache<NodeKey, KeyedInstanceIdentifier<Node, NodeKey>> mountIds = CacheBuilder.newBuilder()
            .maximumSize(20)
            .build(
                    new CacheLoader<NodeKey, KeyedInstanceIdentifier<Node, NodeKey>>() {
                        public KeyedInstanceIdentifier<Node, NodeKey> load(final NodeKey key) {
                            return NETCONF_TOPO_IID.child(Node.class, key);
                        }
                    });


    private final DataBroker dataBroker;
    private final MountPointService mountService;


    private ListenerRegistration<TopologyDataHandler> registration;
    private XrCapabilitiesService capabilitiesService;


    public TopologyDataHandler(TopologyManager topologyManager, DataBroker dataBroker, MountPointService mountService) {
        this.topologyManager = topologyManager;
        Objects.requireNonNull(dataBroker);
        Objects.requireNonNull(mountService);
        this.dataBroker = dataBroker;
        this.mountService = mountService;
    }

    public void init() {
        LOG.debug("initializing topology handler for {}", XrDriverBuilder.XR_NODE);
        initializeWithRetrial(MAX_RETRIALS);
    }

    private void initializeWithRetrial(int retrialCouter) {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();

        NrpDao dao = new NrpDao(tx);
        dao.createNode(topologyManager.getSystemTopologyId(), XrDriverBuilder.XR_NODE, LayerProtocolName.ETH, null);

        Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                LOG.info("Node {} created", XrDriverBuilder.XR_NODE);
                capabilitiesService = new XrCapabilitiesService(dataBroker);
                registerNetconfTreeListener();
            }

            @Override
            public void onFailure(Throwable t) {
                if(retrialCouter != 0) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException _e) { }
                    if(retrialCouter != MAX_RETRIALS) {
                        LOG.debug("Retrying initialization of {} for {} time", XrDriverBuilder.XR_NODE, MAX_RETRIALS - retrialCouter + 1);
                    }
                    initializeWithRetrial(retrialCouter - 1);
                } else {
                    LOG.error("No node created due to the error", t);
                }

            }
        }, MoreExecutors.directExecutor());
    }

    public void close() {
        if (registration != null) {
            LOG.info("closing netconf tree listener");
            registration.close();
        }

    }

    private void registerNetconfTreeListener() {

        InstanceIdentifier<Node> nodeId = NETCONF_TOPO_IID.child(Node.class);

        registration = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, nodeId), this);
        LOG.info("netconf tree listener registered");
    }



    Function<DataObjectModification<Node>, Node> addedNode = mod -> (mod.getModificationType() == DataObjectModification.ModificationType.WRITE || mod.getModificationType() == DataObjectModification.ModificationType.SUBTREE_MODIFIED) ?
            mod.getDataAfter() : null;

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<Node>> changes) {

        List<Node> addedNodes = changes.stream().map(DataTreeModification::getRootNode)
                .map(addedNode::apply)
                .filter(n -> {
                    if (n == null) return false;
                    return capabilitiesService.node(n).isSupporting(AND, NETCONF, NETCONF_CISCO_IOX_IFMGR, NETCONF_CISCO_IOX_L2VPN);
                }).collect(Collectors.toList());
        try {
            onAddedNodes(addedNodes);
        } catch(Exception e) {
            //TODO improve error handling
            LOG.error("error while processing new Cisco nodes", e);
        }
    }

    private void onAddedNodes(@Nonnull Collection<Node> added) throws ReadFailedException {
        if (added.isEmpty()) return;
        LOG.debug("found {} added XR nodes", added.size());

        final ReadWriteTransaction topoTx = dataBroker.newReadWriteTransaction();
        NrpDao dao = new NrpDao(topoTx);
        toTp(added).forEach(nep -> {

            ServiceInterfacePoint sip = new ServiceInterfacePointBuilder()
                    .setUuid(new Uuid("sip:" + nep.getUuid().getValue()))
//                    .setState(St)
                    .setLayerProtocolName(Collections.singletonList(LayerProtocolName.ETH))
                    .build();
            dao.addSip(sip);
            MappedServiceInterfacePoint sipRef = TapiUtils.toSipRef(sip.getUuid(), MappedServiceInterfacePoint.class);
            nep = new OwnedNodeEdgePointBuilder(nep).setMappedServiceInterfacePoint(Collections.singletonList(sipRef)).build();
            LOG.trace("Adding nep {} to {} node", nep.getUuid(), XrDriverBuilder.XR_NODE);
            dao.updateNep(XrDriverBuilder.XR_NODE, nep);
        });

        Futures.addCallback(topoTx.submit(), new FutureCallback<Void>() {

            @Override
            public void onSuccess(@Nullable Void result) {
                LOG.debug("TAPI node upadate successful");
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.warn("TAPI node upadate failed due to an error", t);
            }
        });
    }

    //simplyfied version of selecting
    private Pattern gbPort = Pattern.compile(".*(GigabitEthernet|TenGigE)[^.]+$");

    final Predicate<InterfaceConfiguration> isNep = ic -> {
        final String name = ic.getKey().getInterfaceName().getValue();
        return gbPort.matcher(name).matches();
    };

    private List<OwnedNodeEdgePoint> toTp(Collection<Node> nodes) {
        OwnedNodeEdgePointBuilder tpBuilder = new OwnedNodeEdgePointBuilder();
        return nodes.stream().flatMap(cn -> {
            final NodeKey key = cn.getKey();
            try {
                KeyedInstanceIdentifier<Node, NodeKey> id = mountIds.get(key);
                Optional<MountPoint> mountPoint = mountService.getMountPoint(id);
                if (mountPoint.isPresent()) {
                    DataBroker deviceBroker = mountPoint.get().getService(DataBroker.class).get();
                    LOG.debug(deviceBroker.toString());
                    List<OwnedNodeEdgePoint> tps;
                    try(ReadOnlyTransaction tx = deviceBroker.newReadOnlyTransaction()) {
                        tps = ports(tx)
                                .filter(i -> {
                                    boolean shutdown = i != null && i.isShutdown() != null && i.isShutdown();
                                    return !shutdown;
                                })
                                .filter(isNep::test)
                                .map(i -> {
                                    InterfaceConfigurationKey ikey = i.getKey();
                                    LOG.debug("found {} interface", ikey);

                                    Uuid tpId = new Uuid(cn.getNodeId().getValue() + ":" + ikey.getInterfaceName().getValue());
                                    return tpBuilder
                                            .setUuid(tpId)
                                            .setKey(new OwnedNodeEdgePointKey(tpId))
                                            .setLinkPortDirection(PortDirection.BIDIRECTIONAL)
                                            .setLinkPortRole(PortRole.SYMMETRIC)
                                            .setLayerProtocolName(LayerProtocolName.ETH)
                                            .build();
                                }).collect(Collectors.toList());

                    }

                    return tps.stream();

                } else {
                    LOG.warn("no mount point for {}", key);
                }

            } catch (Exception e) {
                LOG.warn("error while processing " + key, e);
            }
            return Stream.empty();
        }).collect(Collectors.toList());
    }

    private Stream<InterfaceConfiguration> ports(ReadOnlyTransaction tx) throws ReadFailedException {
        Optional<InterfaceConfigurations> interfaces = tx.read(LogicalDatastoreType.OPERATIONAL, InterfaceHelper.getInterfaceConfigurationsId()).checkedGet();
        if (interfaces.isPresent()) {
            return interfaces.get().getInterfaceConfiguration().stream();
        }

        return Stream.empty();
    }
}
