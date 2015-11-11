/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.impl;


import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.unimgr.api.IUnimgrConsoleProvider;
import org.opendaylight.unimgr.command.TransactionInvoker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Evc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Uni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class UnimgrProvider implements BindingAwareProvider, AutoCloseable, IUnimgrConsoleProvider {

    private static final Logger LOG = LoggerFactory.getLogger(UnimgrProvider.class);

    private UnimgrDataChangeListener listener;
    private TransactionInvoker invoker;

    private DataBroker dataBroker;
    private ServiceRegistration<IUnimgrConsoleProvider> unimgrConsoleRegistration;

    public UnimgrProvider() {
        LOG.info("Unimgr provider initialized");
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("UnimgrProvider Session Initiated");

        // Retrieve the data broker to create transactions
        dataBroker =  session.getSALService(DataBroker.class);
        invoker = new  TransactionInvoker();

        // Register the unimgr OSGi CLI
        BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        unimgrConsoleRegistration = context.registerService(IUnimgrConsoleProvider.class,
                                                            this,
                                                            null);

        // Register the uni data change listener
        listener = new UnimgrDataChangeListener(dataBroker, invoker);

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
        unimgrConsoleRegistration.unregister();
        listener.close();
    }

    protected void initDatastore(final LogicalDatastoreType type,
                                 TopologyId topoId) {
        InstanceIdentifier<Topology> path = InstanceIdentifier
                                                .create(NetworkTopology.class)
                                                .child(Topology.class,
                                                        new TopologyKey(topoId));
        initializeTopology(type);
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        CheckedFuture<Optional<Topology>, ReadFailedException> unimgrTp = transaction.read(type,
                                                                                           path);
        try {
            if (!unimgrTp.get().isPresent()) {
                TopologyBuilder tpb = new TopologyBuilder();
                tpb.setTopologyId(topoId);
                transaction.put(type, path, tpb.build());
                transaction.submit();
            } else {
                transaction.cancel();
            }
        } catch (Exception e) {
            LOG.error("Error initializing unimgr topology", e);
        }
    }

    private void initializeTopology(LogicalDatastoreType type) {
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        InstanceIdentifier<NetworkTopology> path = InstanceIdentifier.create(NetworkTopology.class);
        CheckedFuture<Optional<NetworkTopology>, ReadFailedException> topology = transaction.read(type,path);
        try {
            if (!topology.get().isPresent()) {
                NetworkTopologyBuilder ntb = new NetworkTopologyBuilder();
                transaction.put(type,path,ntb.build());
                transaction.submit();
            } else {
                transaction.cancel();
            }
        } catch (Exception e) {
            LOG.error("Error initializing unimgr topology {}",e);
        }
    }

    private OvsdbNodeRef createOvsdbNode(UniAugmentation uni) {
        Node ovsdbNode = UnimgrUtils.createOvsdbNode(dataBroker, uni);
        InstanceIdentifier<Node> ovsdbNodeIid = UnimgrMapper.getOvsdbNodeIid(ovsdbNode.getNodeId());
        OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(ovsdbNodeIid);
        return ovsdbNodeRef;
    }

    @Override
    public boolean addUni(UniAugmentation uniAug) {
        if (uniAug == null || uniAug.getIpAddress() == null || uniAug.getMacAddress() == null) {
            return false;
        }
        LOG.warn("print ip " + uniAug.getIpAddress().getValue());
        LOG.warn("print mac " + uniAug.getMacAddress().getValue());
        LOG.warn("print speed " + uniAug.getSpeed().toString());
        boolean result = false;
        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
      
        InstanceIdentifier<UniAugmentation> iidUni = InstanceIdentifier.create(UniAugmentation.class);
        UniAugmentation uni = new UniAugmentationBuilder(uniAug)
                                  .setOvsdbNodeRef(createOvsdbNode(uniAug))
                                  .build();
        try {
            Node nd = UnimgrUtils.readNode(dataBroker, LogicalDatastoreType.CONFIGURATION, uni.getOvsdbNodeRef().getValue()).get();
            
            LOG.warn("print nodeid " + nd.getNodeId().getValue());
            transaction.put(LogicalDatastoreType.CONFIGURATION, iidUni, uni, true);
            CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
            future.checkedGet();
            result = true;
        } catch (TransactionCommitFailedException | IllegalStateException e) {
            LOG.warn("Failed to Add Uni {}", e.getMessage());
        }
        return result;
    }

    @Override
    public boolean removeUni(IpAddress ipAddress) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<Uni> listUnis(LogicalDatastoreType dataStore) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uni getUni(IpAddress ipAddress) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public boolean removeEvc(String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean addEvc(Evc evc) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Evc getEvc(String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

}
