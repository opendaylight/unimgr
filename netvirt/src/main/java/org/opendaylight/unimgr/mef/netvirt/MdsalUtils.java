/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class MdsalUtils {

    private static final Logger logger = LoggerFactory.getLogger(MdsalUtils.class);

    public static <T extends DataObject> void syncWrite(DataBroker broker, LogicalDatastoreType datastoreType,
            InstanceIdentifier<T> path, T data) {
        CheckedFuture<Void, TransactionCommitFailedException> futures = write(broker, datastoreType, path, data);
        try {
            futures.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error writing to datastore (path, data) : ({}, {})", path, data);
            throw new RuntimeException(e.getMessage());
        }
    }

    public static <T extends DataObject> void syncUpdate(DataBroker broker, LogicalDatastoreType datastoreType,
            InstanceIdentifier<T> path, T data) {
        CheckedFuture<Void, TransactionCommitFailedException> futures = update(broker, datastoreType, path, data);
        try {
            futures.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error writing to datastore (path, data) : ({}, {})", path, data);
            throw new RuntimeException(e.getMessage());
        }
    }

    public static <T extends DataObject> void syncDelete(DataBroker broker, LogicalDatastoreType datastoreType,
            InstanceIdentifier<T> obj) {
        CheckedFuture<Void, TransactionCommitFailedException> futures = delete(broker, datastoreType, obj);
        try {
            futures.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error deleting from datastore (path) : ({})", obj);
            throw new RuntimeException(e.getMessage());
        }
    }

    public static <T extends DataObject> CheckedFuture<Void, TransactionCommitFailedException> write(DataBroker broker,
            LogicalDatastoreType datastoreType, InstanceIdentifier<T> path, T data) {
        WriteTransaction tx = broker.newWriteOnlyTransaction();
        tx.put(datastoreType, path, data, true);
        CheckedFuture<Void, TransactionCommitFailedException> futures = tx.submit();
        return futures;
    }

    public static <T extends DataObject> CheckedFuture<Void, TransactionCommitFailedException> update(DataBroker broker,
            LogicalDatastoreType datastoreType, InstanceIdentifier<T> path, T data) {
        WriteTransaction tx = broker.newWriteOnlyTransaction();
        tx.merge(datastoreType, path, data, true);
        CheckedFuture<Void, TransactionCommitFailedException> futures = tx.submit();
        return futures;
    }

    public static <T extends DataObject> CheckedFuture<Void, TransactionCommitFailedException> delete(DataBroker broker,
            LogicalDatastoreType datastoreType, InstanceIdentifier<T> obj) {
        WriteTransaction tx = broker.newWriteOnlyTransaction();
        tx.delete(datastoreType, obj);
        CheckedFuture<Void, TransactionCommitFailedException> futures = tx.submit();
        return futures;
    }

    public static <T extends DataObject> Optional<T> read(DataBroker broker, LogicalDatastoreType datastoreType,
            InstanceIdentifier<T> path) {
        ReadOnlyTransaction tx = broker.newReadOnlyTransaction();
        Optional<T> result = Optional.absent();
        try {
            CheckedFuture<Optional<T>, ReadFailedException> checkedFuture = tx.read(datastoreType, path);
            result = checkedFuture.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public static WriteTransaction createTransaction(DataBroker dataBroker) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        return tx;
    }

    public static void commitTransaction(WriteTransaction tx) {
        try {
            CheckedFuture<Void, TransactionCommitFailedException> futures = tx.submit();
            futures.get();
        } catch (Exception e) {
            logger.error("failed to commit transaction due to exception ", e);
        }
    }

}
