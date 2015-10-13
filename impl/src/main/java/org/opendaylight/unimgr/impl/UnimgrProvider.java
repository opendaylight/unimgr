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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.unimgr.api.IUnimgrConsoleProvider;
import org.opendaylight.unimgr.command.TransactionInvoker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Evc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Uni;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
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

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("UnimgrProvider Session Initiated");

        dataBroker =  session.getSALService(DataBroker.class);
        invoker = new  TransactionInvoker();

        BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        unimgrConsoleRegistration = context.registerService(IUnimgrConsoleProvider.class, this, null);

        listener = new UnimgrDataChangeListener(dataBroker, invoker);

        // Initialize operational and default config data in MD-SAL data store
        initDatastore(LogicalDatastoreType.CONFIGURATION, UnimgrConstants.UNI_TOPOLOGY_ID);
        initDatastore(LogicalDatastoreType.OPERATIONAL, UnimgrConstants.UNI_TOPOLOGY_ID);
        initDatastore(LogicalDatastoreType.CONFIGURATION, UnimgrConstants.EVC_TOPOLOGY_ID);
        initDatastore(LogicalDatastoreType.OPERATIONAL, UnimgrConstants.EVC_TOPOLOGY_ID);
    }

    @Override
    public void close() throws Exception {
        LOG.info("UnimgrProvider Closed");
        unimgrConsoleRegistration.unregister();
        listener.close();
    }

    protected void initDatastore(final LogicalDatastoreType type, TopologyId topoId) {
        InstanceIdentifier<Topology> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(topoId));
        initializeTopology(type);
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        CheckedFuture<Optional<Topology>, ReadFailedException> unimgrTp = transaction.read(type, path);
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

    @Override
    public boolean addUni(Uni uni) {
        //TODO Uncomment
        if (uni.getIpAddress() == null || uni.getMacAddress() == null) {
            return false;
        }
//        UniAugmentation uniAugmentation = new UniAugmentationBuilder()
//                                                .setIpAddress(uni.getIpAddress())
//                                                .setMacAddress(uni.getMacAddress())
//                                                .build();
//        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
//        InstanceIdentifier<Node> path = UnimgrMapper.getUniAugmentationIidByMac(uni.getMacAddress());
        return true;
    }

    @Override
    public boolean removeUni(String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<Uni> listUnis(boolean isConfigurationDatastore) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uni getUni(String uuid) {
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
