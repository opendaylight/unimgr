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
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

/**
 * @author marek.ryznar@amartus.com
 */
public class DataStoreTestUtils {
    private static final Logger LOG = LoggerFactory.getLogger(DataStoreTestUtils.class);


    private static int MAX_RETRIALS = 5;
    public static <T extends DataObject> T read(InstanceIdentifier<T> instanceIdentifier, DataBroker dataBroker) {
        return read(instanceIdentifier,dataBroker,LogicalDatastoreType.OPERATIONAL, MAX_RETRIALS);
    }

    public static <T extends DataObject> T readConfig(InstanceIdentifier<T> instanceIdentifier, DataBroker dataBroker) {
        return read(instanceIdentifier,dataBroker,LogicalDatastoreType.CONFIGURATION, MAX_RETRIALS);
    }

    public static <T extends DataObject> void write(T object, InstanceIdentifier<T> instanceIdentifier, DataBroker dataBroker) {
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        transaction.put(LogicalDatastoreType.OPERATIONAL,instanceIdentifier,object,true);

        try {
            transaction.submit().checkedGet();
            LOG.debug("Object: {} created.",object.toString());
        } catch (TransactionCommitFailedException e) {
            LOG.debug("Object: {} wasn't created due to a error: {}",object.toString(), e.getMessage());
            fail("Object  wasn't created due to a error: "+ e.getMessage());
        }
    }

    public static void delete(InstanceIdentifier instanceIdentifier, DataBroker dataBroker) {
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        transaction.delete(LogicalDatastoreType.OPERATIONAL, instanceIdentifier);

        try {
            transaction.submit().checkedGet();
            LOG.debug("Object: {} deleted.",instanceIdentifier.toString());
        } catch (TransactionCommitFailedException e) {
            LOG.debug("Object: {} wasn't deleted due to a error: {}",instanceIdentifier.toString(), e.getMessage());
            fail("Object wasn't deleted due to a error: "+ e.getMessage());
        }
    }

    private static <T extends DataObject> T read(InstanceIdentifier<T> instanceIdentifier, DataBroker dataBroker, LogicalDatastoreType type, int retirals) {
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        try {
            Optional<T> opt = transaction.read(type,instanceIdentifier).checkedGet();
            if (opt.isPresent()) {
                return opt.get();
            } else {
                if(retirals > 0) {
                    TimeUnit.MILLISECONDS.sleep(10);
                    read(instanceIdentifier,dataBroker, type, retirals - 1);
                }
                fail("Could not find object");
            }
        } catch (Exception e) {
            fail("Could not find object, "+e.getMessage());
        }
        return null;
    }
}
