/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.utils;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MdsalUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MdsalUtils.class);

    private MdsalUtils() {
        throw new AssertionError("Instantiating utility class.");
    }

    /**
     * Read a specific datastore type and return a DataObject as a casted
     * class type Object.
     * @param dataBroker The dataBroker instance to create transactions
     * @param store The store type to query
     * @param path The generic path to query
     * @return The DataObject as a casted Object
     */
    public static <D extends org.opendaylight.yangtools.yang.binding.DataObject> D read(
            DataBroker dataBroker,
            final LogicalDatastoreType store,
            final InstanceIdentifier<D> path)  {
        D result = null;
        final ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        Optional<D> optionalDataObject;
        final CheckedFuture<Optional<D>, ReadFailedException> future = transaction.read(store, path);
        try {
            optionalDataObject = future.checkedGet();
            if (optionalDataObject.isPresent()) {
                result = optionalDataObject.get();
            } else {
                LOG.debug("{}: Failed to read {}",
                        Thread.currentThread().getStackTrace()[1], path);
            }
        } catch (final ReadFailedException e) {
            LOG.warn("Failed to read {} ", path, e);
        }
        transaction.close();
        return result;
    }

    /**
     * Read a specific datastore type and return a optional of DataObject.
     * @param dataBroker The dataBroker instance to create transactions
     * @param store The store type to query
     * @param path The generic path to query
     * @return Read object optional
     */
    public static <D extends org.opendaylight.yangtools.yang.binding.DataObject> Optional<D> readOptional(
            DataBroker dataBroker,
            final LogicalDatastoreType store,
            final InstanceIdentifier<D> path) {

        final ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        Optional<D> optionalDataObject = Optional.absent();
        final CheckedFuture<Optional<D>, ReadFailedException> future = transaction.read(store, path);
        try {
            optionalDataObject = future.checkedGet();
        } catch (final ReadFailedException e) {
            LOG.warn("Failed to read {} ", path, e);
        }

        transaction.close();
        return optionalDataObject;
    }
}
