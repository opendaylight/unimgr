/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.unimgr.api.IUnimgrConsoleProvider;
import org.opendaylight.unimgr.command.TransactionInvoker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev150622.Evcs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev150622.EvcsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev150622.Unis;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev150622.UnisBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev150622.evcs.Evc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev150622.unis.Uni;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class UnimgrProvider implements BindingAwareProvider, AutoCloseable, IUnimgrConsoleProvider {

    private static final Logger LOG = LoggerFactory.getLogger(UnimgrProvider.class);

    private UniDataChangeListener unimgrDataChangeListener;
    private EvcDataChangeListener evcDataChangeListener;
    private UnimgrDataChangeListener listener;
    private TransactionInvoker invoker;

    private DataBroker dataBroker;
    private ServiceRegistration<IUnimgrConsoleProvider> unimgrConsoleRegistration;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("VcpeProvider Session Initiated");

        dataBroker =  session.getSALService(DataBroker.class);
        invoker = new  TransactionInvoker();
        // Initialize operational and default config data in MD-SAL data store
        BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        unimgrConsoleRegistration = context.registerService(IUnimgrConsoleProvider.class, this, null);

        unimgrDataChangeListener = new UniDataChangeListener(dataBroker);
        evcDataChangeListener = new EvcDataChangeListener(dataBroker);
        listener = new UnimgrDataChangeListener(dataBroker, invoker);

        // Init UNI Config & Operational stores
        Unis unis = new UnisBuilder().build();
        initDatastore(LogicalDatastoreType.CONFIGURATION, UnimgrMapper.getUnisIid(), unis);
        initDatastore(LogicalDatastoreType.OPERATIONAL, UnimgrMapper.getUnisIid(), unis);
        // Init EVC Config & Operational stores
        Evcs evcs = new EvcsBuilder().build();
        initDatastore(LogicalDatastoreType.CONFIGURATION, UnimgrMapper.getEvcsIid(), evcs);
        initDatastore(LogicalDatastoreType.OPERATIONAL, UnimgrMapper.getEvcsIid(), evcs);
    }

    @Override
    public void close() throws Exception {
        LOG.info("VcpeProvider Closed");
        unimgrConsoleRegistration.unregister();
        unimgrDataChangeListener.close();
        evcDataChangeListener.close();
        listener.close();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void initDatastore(final LogicalDatastoreType store, InstanceIdentifier iid, final DataObject object) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(store, iid, object);

        // Perform the tx.submit asynchronously
        Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.info("initStore {} with object {} succeeded", store, object);
            }
            @Override
            public void onFailure(final Throwable throwable)  {
                LOG.error("initStore {} with object {} failed", store, object);
            }
        });
    }

    @Override
    public boolean addUni(Uni uni) {
        Unis unis;
        List<Uni> listOfUnis = listUnis(true);

        try {
            listOfUnis.add(uni);
            unis = new UnisBuilder().setUni(listOfUnis).build();

            // Place default config data in data store tree
            WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
            tx.put(LogicalDatastoreType.CONFIGURATION, UnimgrMapper.getUnisIid(), unis);

            // Perform the tx.submit synchronously
            tx.submit();
        } catch (Exception e) {
            LOG.error("addUni: failed: {}", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean removeUni(String id) {
        try {
            // Removes default config data in data store tree
            WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
            tx.delete(LogicalDatastoreType.CONFIGURATION, UnimgrMapper.getUniIid(id));
            System.out.println(UnimgrMapper.getUniIid(id));
            // Perform the tx.submit synchronously
            tx.submit();
        } catch (Exception e) {
            LOG.info("RemoveUni: failed: {}", e);
            return false;
        }
        return true;
    }

    @Override
    public List<Uni> listUnis(boolean isConfigurationDatastore) {
        List<Uni> listOfUnis = null;

        try {
            ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction();
            listOfUnis = tx.read((isConfigurationDatastore) ? LogicalDatastoreType.CONFIGURATION
                    : LogicalDatastoreType.OPERATIONAL, UnimgrMapper.getUnisIid()).checkedGet().get().getUni();
        } catch (Exception e) {
            LOG.error("ListIntents: failed: {}", e);
        }

        if (listOfUnis == null) {
            listOfUnis = new ArrayList<Uni>();
        }
        LOG.info("ListUnisConfiguration: list of unis retrieved sucessfully");
        return listOfUnis;
    }

    @Override
    public Uni getUni(String id) {
        Uni uni = null;

        try {
            InstanceIdentifier<Uni> iid = UnimgrMapper.getUniIid(id);
            System.out.println(UnimgrMapper.getUniIid(id));

            ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction();
            uni = tx.read(LogicalDatastoreType.CONFIGURATION, iid).checkedGet().get();

            if (uni == null) {
                uni = tx.read(LogicalDatastoreType.OPERATIONAL, iid).checkedGet().get();
            }
        } catch (Exception e) {
            LOG.error("getUni: failed: {}", e);
            return null;
        }
        LOG.info("getUni: Uni retrieved sucessfully");
        return uni;
    }

    @Override
    public boolean removeEvc(String uuid) {
        try {
            WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
            tx.delete(LogicalDatastoreType.CONFIGURATION, UnimgrMapper.getEvcIid(uuid));
            System.out.println(UnimgrMapper.getEvcIid(uuid));
            tx.submit();
        } catch (Exception e) {
            LOG.info("Remove Evc: failed: {}", e);
            return false;
        }
        return true;
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
