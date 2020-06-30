/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.cisco.xr;

import static org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util.XrCapabilitiesService.NodeCapability.NETCONF;
import static org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util.XrCapabilitiesService.NodeCapability.NETCONF_CISCO_IOX_IFMGR;
import static org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util.XrCapabilitiesService.NodeCapability.NETCONF_CISCO_IOX_L2VPN;
import static org.opendaylight.unimgr.utils.CapabilitiesService.Capability.Mode.AND;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
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


/*
 * @author bartosz.michalik@amartus.com
 */
public class TopologyDataHandler implements DataTreeChangeListener<Node> {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyDataHandler.class);
    public static final InstanceIdentifier<Topology> NETCONF_TOPO_IID =
            InstanceIdentifier.create(NetworkTopology.class).child(Topology.class,
                    new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));

    private final int maxRetrials = 5;

    private final TopologyManager topologyManager;

    LoadingCache<NodeKey, KeyedInstanceIdentifier<Node, NodeKey>> mountIds =
            CacheBuilder.newBuilder().maximumSize(20)
                    .build(new CacheLoader<NodeKey, KeyedInstanceIdentifier<Node, NodeKey>>() {
                        public KeyedInstanceIdentifier<Node, NodeKey> load(final NodeKey key) {
                            return NETCONF_TOPO_IID.child(Node.class, key);
                        }
                    });


    private final DataBroker dataBroker;
    private final MountPointService mountService;


    private ListenerRegistration<TopologyDataHandler> registration;
    private XrCapabilitiesService capabilitiesService;


    public TopologyDataHandler(TopologyManager topologyManager, DataBroker dataBroker,
            MountPointService mountService) {
        this.topologyManager = topologyManager;
        Objects.requireNonNull(dataBroker);
        Objects.requireNonNull(mountService);
        this.dataBroker = dataBroker;
        this.mountService = mountService;
    }

    public void init() {
        LOG.debug("initializing topology handler for {}", XrDriverBuilder.XR_NODE);
        initializeWithRetrial(maxRetrials);
    }

    private void initializeWithRetrial(int retrialCouter) {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();

        NrpDao dao = new NrpDao(tx);
        dao.createNode(topologyManager.getSystemTopologyId(), XrDriverBuilder.XR_NODE,
                LayerProtocolName.ETH, null);

        Futures.addCallback(tx.commit(), new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(CommitInfo result) {
                LOG.info("Node {} created", XrDriverBuilder.XR_NODE);
                capabilitiesService = new XrCapabilitiesService(dataBroker);
                registerNetconfTreeListener();
            }

            @SuppressWarnings("checkstyle:emptyBlock")
            @Override
            public void onFailure(Throwable throwable) {
                if (retrialCouter != 0) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                    }
                    if (retrialCouter != maxRetrials) {
                        LOG.debug("Retrying initialization of {} for {} time",
                                XrDriverBuilder.XR_NODE, maxRetrials - retrialCouter + 1);
                    }
                    initializeWithRetrial(retrialCouter - 1);
                } else {
                    LOG.error("No node created due to the error", throwable);
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

        registration = dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, nodeId), this);
        LOG.info("netconf tree listener registered");
    }


    Function<DataObjectModification<Node>, Node> addedNode =
        mod -> (mod.getModificationType() == DataObjectModification.ModificationType.WRITE
            || mod.getModificationType() == DataObjectModification.ModificationType.SUBTREE_MODIFIED)
            ? mod.getDataAfter() : null;

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<Node>> changes) {

        List<Node> addedNodes = changes.stream().map(DataTreeModification::getRootNode)
                .map(addedNode::apply).filter(n -> {
                    if (n == null) {
                        return false;
                    }
                    return capabilitiesService.node(n).isSupporting(AND, NETCONF,
                            NETCONF_CISCO_IOX_IFMGR, NETCONF_CISCO_IOX_L2VPN);
                }).collect(Collectors.toList());
        try {
            onAddedNodes(addedNodes);
        } catch (ReadFailedException e) {
            // TODO improve error handling
            LOG.error("error while processing new Cisco nodes", e);
        }
    }

    private void onAddedNodes(@Nonnull Collection<Node> added) throws ReadFailedException {
        if (added.isEmpty()) {
            return;
        }
        LOG.debug("found {} added XR nodes", added.size());

        final ReadWriteTransaction topoTx = dataBroker.newReadWriteTransaction();
        NrpDao dao = new NrpDao(topoTx);
        toTp(added).forEach(nep -> {

            ServiceInterfacePoint sip = new ServiceInterfacePointBuilder()
                    .setUuid(new Uuid("sip:" + nep.getUuid().getValue()))
                    // .setState(St)
                    .setLayerProtocolName(Collections.singletonList(LayerProtocolName.ETH)).build();
            dao.addSip(sip);
            MappedServiceInterfacePoint sipRef =
                    TapiUtils.toSipRef(sip.getUuid(), MappedServiceInterfacePoint.class);
            nep = new OwnedNodeEdgePointBuilder(nep)
                    .setMappedServiceInterfacePoint(Collections.singletonList(sipRef)).build();
            LOG.trace("Adding nep {} to {} node", nep.getUuid(), XrDriverBuilder.XR_NODE);
            dao.updateNep(XrDriverBuilder.XR_NODE, nep);
        });

        Futures.addCallback(topoTx.commit(), new FutureCallback<CommitInfo>() {

            @Override
            public void onSuccess(CommitInfo result) {
                LOG.debug("TAPI node upadate successful");
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOG.warn("TAPI node upadate failed due to an error", throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    // simplyfied version of selecting
    private Pattern gbPort = Pattern.compile(".*(GigabitEthernet|TenGigE)[^-]+$");

    final Predicate<InterfaceConfiguration> isNep = ic -> {
        final String name = ic.key().getInterfaceName().getValue();
        return gbPort.matcher(name).matches();
    };

    @SuppressWarnings("checkstyle:illegalcatch")
    private List<OwnedNodeEdgePoint> toTp(Collection<Node> nodes) {
        OwnedNodeEdgePointBuilder tpBuilder = new OwnedNodeEdgePointBuilder();
        return nodes.stream().flatMap(cn -> {
            final NodeKey key = cn.key();
            try {
                KeyedInstanceIdentifier<Node, NodeKey> id = mountIds.get(key);
                Optional<MountPoint> mountPoint = mountService.getMountPoint(id);
                if (mountPoint.isPresent()) {
                    DataBroker deviceBroker = mountPoint.get().getService(DataBroker.class).get();
                    List<OwnedNodeEdgePoint> tps;
                    try (ReadTransaction tx = deviceBroker.newReadOnlyTransaction()) {
                        tps = ports(tx)
                          .filter(i -> {
                              boolean shutdown =
                                    i != null && i.getShutdown() != null;
                              return !shutdown;
                          }).filter(isNep::test).map(i -> {
                              InterfaceConfigurationKey ikey = i.key();
                              LOG.debug("found {} interface", ikey);

                              Uuid tpId = new Uuid(cn.getNodeId().getValue() + ":"
                                    + ikey.getInterfaceName().getValue());
                              return tpBuilder
                                    .setUuid(tpId)
                                    .withKey(new OwnedNodeEdgePointKey(tpId))
                                    .setLinkPortDirection(PortDirection.BIDIRECTIONAL)
                                    .setLinkPortRole(PortRole.SYMMETRIC)
                                    .setLayerProtocolName(LayerProtocolName.ETH).build();
                          }).collect(Collectors.toList());

                        return tps.stream();
                    } catch (Exception e) {
                        LOG.warn("erro", e);
                    }

                } else {
                    LOG.warn("no mount point for {}", key);
                }

            } catch (Exception e) {
                LOG.warn("error while processing for {} ", key, e);
            }
            return Stream.empty();
        }).collect(Collectors.toList());
    }

    private Stream<InterfaceConfiguration> ports(ReadTransaction tx) throws InterruptedException, ExecutionException {
        Optional<InterfaceConfigurations> interfaces = tx.read(
                                                            LogicalDatastoreType.OPERATIONAL,
                                                            InterfaceHelper.getInterfaceConfigurationsId()
                                                            ).get();
        if (interfaces.isPresent()) {
            return interfaces.get().getInterfaceConfiguration().stream();
        }

        return Stream.empty();
    }

}
