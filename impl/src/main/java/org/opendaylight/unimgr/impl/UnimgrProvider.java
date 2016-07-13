/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.impl;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.unimgr.api.IUnimgrConsoleProvider;
import org.opendaylight.unimgr.utils.EvcUtils;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.unimgr.utils.OvsdbUtils;
import org.opendaylight.unimgr.utils.UniUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Evc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSource;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class UnimgrProvider implements BindingAwareProvider, AutoCloseable, IUnimgrConsoleProvider {

    private static final Logger LOG = LoggerFactory.getLogger(UnimgrProvider.class);
    private DataBroker dataBroker;
    private EvcDataTreeChangeListener evcListener;
    private OvsNodeDataTreeChangeListener ovsListener;
    private UniDataTreeChangeListener uniListener;

    public UnimgrProvider(DataBroker dataBroker) {
        LOG.info("Unimgr provider initialized");
        this.dataBroker = dataBroker;
    }

    @Override
    public boolean addEvc(final EvcAugmentation evc) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean addUni(final UniAugmentation uniAug) {
        if ((uniAug == null) || (uniAug.getIpAddress() == null) || (uniAug.getMacAddress() == null)) {
            return false;
        }
        return UniUtils.createUniNode(dataBroker, uniAug);
    }

    @Override
    public Evc getEvc(final String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UniAugmentation getUni(final IpAddress ipAddress) {
        return UniUtils.getUni(dataBroker, LogicalDatastoreType.OPERATIONAL, ipAddress);
    }

    protected void initDatastore(final LogicalDatastoreType type,
                                 final TopologyId topoId) {
        final InstanceIdentifier<Topology> path = InstanceIdentifier
                                                .create(NetworkTopology.class)
                                                .child(Topology.class,
                                                        new TopologyKey(topoId));
        initializeTopology(type);
        final ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        final CheckedFuture<Optional<Topology>, ReadFailedException> unimgrTp = transaction.read(type,
                                                                                           path);
        try {
            if (!unimgrTp.get().isPresent()) {
                final TopologyBuilder tpb = new TopologyBuilder();
                tpb.setTopologyId(topoId);
                transaction.put(type, path, tpb.build());
                transaction.submit().get();
            } else {
                transaction.cancel();
            }
        } catch (final Exception e) {
            LOG.error("Error initializing unimgr topology", e);
        }
    }

    private void initializeTopology(final LogicalDatastoreType type) {
        final ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        final InstanceIdentifier<NetworkTopology> path = InstanceIdentifier.create(NetworkTopology.class);
        final CheckedFuture<Optional<NetworkTopology>, ReadFailedException> topology = transaction.read(type,path);
        try {
            if (!topology.get().isPresent()) {
                final NetworkTopologyBuilder ntb = new NetworkTopologyBuilder();
                transaction.put(type,path,ntb.build());
                transaction.submit().get();
            } else {
                transaction.cancel();
            }
        } catch (final Exception e) {
            LOG.error("Error initializing unimgr topology {}", e);
        }
    }

    @Override
    public List<UniAugmentation> listUnis(final LogicalDatastoreType dataStoreType) {
        return UniUtils.getUnis(dataBroker, dataStoreType);
    }

    @Override
    public void onSessionInitiated(ProviderContext providerContext) {
        //not called as provider is not registered in ODL context
    }

    /**
     * Initialization method for UnimgrProvider, used by blueprint.
     */
    public void init() {
        LOG.info("UnimgrProvider Session Initiated");

        // Register the data trees change listener
        uniListener = new UniDataTreeChangeListener(dataBroker);
        evcListener = new EvcDataTreeChangeListener(dataBroker);
        ovsListener = new OvsNodeDataTreeChangeListener(dataBroker);

        // Initialize operational and default config data in MD-SAL data store
        initDatastore(LogicalDatastoreType.CONFIGURATION,
                UnimgrConstants.UNI_TOPOLOGY_ID);
        initDatastore(LogicalDatastoreType.OPERATIONAL,
                UnimgrConstants.UNI_TOPOLOGY_ID);
        initDatastore(LogicalDatastoreType.CONFIGURATION,
                UnimgrConstants.EVC_TOPOLOGY_ID);
        initDatastore(LogicalDatastoreType.OPERATIONAL,
                UnimgrConstants.EVC_TOPOLOGY_ID);
    }


    @Override
    public void close() throws Exception {
        LOG.info("UnimgrProvider Closed");
        uniListener.close();
        evcListener.close();
        ovsListener.close();
    }

    @Override
    public boolean removeEvc(final String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeUni(final IpAddress ipAddress) {
        final InstanceIdentifier<Node> iidUni =
                UnimgrMapper.getUniIid(dataBroker, ipAddress, LogicalDatastoreType.CONFIGURATION);
        if (iidUni == null) {
            return false;
        }
        return MdsalUtils.deleteNode(dataBroker, iidUni, LogicalDatastoreType.CONFIGURATION);
    }

    @Override
    public boolean updateEvc(final InstanceIdentifier<Link> evcKey, final EvcAugmentation evc,
                             final UniSource uniSource, final UniDest uniDest) {
        final InstanceIdentifier<?> sourceUniIid = uniSource.getUni();
        final InstanceIdentifier<?> destinationUniIid = uniDest.getUni();
        return EvcUtils.updateEvcNode(LogicalDatastoreType.CONFIGURATION, evcKey, evc, sourceUniIid,
                destinationUniIid, dataBroker);
    }

    @Override
    public boolean updateUni(final UniAugmentation uni) {
        // Remove the old UNI with IpAdress and create a new one with updated informations
        if (uni != null) {
            LOG.trace("UNI updated {}.", uni.getIpAddress().getIpv4Address());
            final InstanceIdentifier<?> uniIID = UnimgrMapper.getUniIid(dataBroker,
                    uni.getIpAddress(), LogicalDatastoreType.OPERATIONAL);
            Node ovsdbNode;
            if (uni.getOvsdbNodeRef() != null) {
                final OvsdbNodeRef ovsdbNodeRef = uni.getOvsdbNodeRef();
                ovsdbNode = MdsalUtils.readNode(dataBroker,
                        LogicalDatastoreType.OPERATIONAL, ovsdbNodeRef.getValue()).get();

                UniUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL, uniIID, uni, ovsdbNode, dataBroker);
                LOG.trace("UNI updated {}.", uni.getIpAddress().getIpv4Address());
            } else {
                final Optional<Node> optionalOvsdbNode = OvsdbUtils.findOvsdbNode(dataBroker, uni);
                ovsdbNode = optionalOvsdbNode.get();
            }
            MdsalUtils.deleteNode(dataBroker, uniIID, LogicalDatastoreType.OPERATIONAL);
            return (UniUtils.updateUniNode(LogicalDatastoreType.CONFIGURATION, uniIID,
                    uni, ovsdbNode, dataBroker) && UniUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL, uniIID,
                    uni, ovsdbNode, dataBroker));
        }
        return false;
    }
}
