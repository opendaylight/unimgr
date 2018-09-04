/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import static org.junit.Assert.fail;

/**
 * @author marek.ryznar@amartus.com
 */
public class DataStoreTestUtils {
    private static final Logger LOG = LoggerFactory.getLogger(DataStoreTestUtils.class);

    public static <T extends DataObject> T read(InstanceIdentifier<?> instanceIdentifier, DataBroker dataBroker) {
        return read(instanceIdentifier,dataBroker,LogicalDatastoreType.OPERATIONAL);
    }

    public static <T extends DataObject> T readConfig(InstanceIdentifier<?> instanceIdentifier, DataBroker dataBroker) {
        return read(instanceIdentifier,dataBroker,LogicalDatastoreType.CONFIGURATION);
    }

    public static <T extends DataObject> void write(T object, InstanceIdentifier<T> instanceIdentifier, DataBroker dataBroker) {
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        transaction.put(LogicalDatastoreType.OPERATIONAL,instanceIdentifier,object,true);

        Futures.addCallback(transaction.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                LOG.debug("Object: {} created.",object.toString());
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.debug("Object: {} wasn't created due to a error: {}",object.toString(), t.getMessage());
                fail("Object  wasn't created due to a error: "+ t.getMessage());
            }
        });
    }

    public static void delete(InstanceIdentifier<?> instanceIdentifier, DataBroker dataBroker) {
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        transaction.delete(LogicalDatastoreType.OPERATIONAL, instanceIdentifier);

        Futures.addCallback(transaction.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                LOG.debug("Object: {} deleted.",instanceIdentifier.toString());
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.debug("Object: {} wasn't deleted due to a error: {}",instanceIdentifier.toString(), t.getMessage());
                fail("Object wasn't deleted due to a error: "+ t.getMessage());
            }
        });
    }

    private static <T extends DataObject> T read(InstanceIdentifier<?> instanceIdentifier, DataBroker dataBroker, LogicalDatastoreType type) {
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        try {
            Optional<T> opt = (Optional<T>) transaction.read(type,instanceIdentifier).checkedGet();
            if (opt.isPresent()) {
                return opt.get();
            } else {
                fail("Could not find object");
            }
        } catch (Exception e) {
            fail("Could not find object, "+e.getMessage());
        }
        return null;
    }
}
