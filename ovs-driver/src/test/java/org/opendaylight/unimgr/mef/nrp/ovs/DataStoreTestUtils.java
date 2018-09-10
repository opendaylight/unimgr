/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs;

import static org.junit.Assert.fail;

import java.util.Optional;

import javax.annotation.Nullable;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

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

        Futures.addCallback(transaction.commit(), new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(@Nullable CommitInfo result) {
                LOG.debug("Object: {} created.",object.toString());
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.debug("Object: {} wasn't created due to a error: {}",object.toString(), t.getMessage());
                fail("Object  wasn't created due to a error: "+ t.getMessage());
            }
        }, MoreExecutors.directExecutor());
    }

    public static void delete(InstanceIdentifier<?> instanceIdentifier, DataBroker dataBroker) {
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        transaction.delete(LogicalDatastoreType.OPERATIONAL, instanceIdentifier);

        Futures.addCallback(transaction.commit(), new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(@Nullable CommitInfo result) {
                LOG.debug("Object: {} deleted.",instanceIdentifier.toString());
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.debug("Object: {} wasn't deleted due to a error: {}",instanceIdentifier.toString(), t.getMessage());
                fail("Object wasn't deleted due to a error: "+ t.getMessage());
            }
        }, MoreExecutors.directExecutor());
    }

    private static <T extends DataObject> T read(InstanceIdentifier<?> instanceIdentifier, DataBroker dataBroker, LogicalDatastoreType type) {
        ReadTransaction transaction = dataBroker.newReadOnlyTransaction();
        try {
            Optional<T> opt = (Optional<T>) transaction.read(type,instanceIdentifier).get();
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
