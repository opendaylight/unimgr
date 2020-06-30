/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.api;

import java.util.Collection;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.yangtools.yang.binding.DataObject;


/**
 * abstract class for unimgr data tree changes.
 * @author mohamed el-serngawy
 * @param <D> extended data object
 */
public abstract class UnimgrDataTreeChangeListener<D extends DataObject>
        implements DataTreeChangeListener<D>, AutoCloseable {

    protected DataBroker dataBroker;

    public UnimgrDataTreeChangeListener(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    /**
     * Basic Implementation of DataTreeChange Listener to execute add, update or remove command
     * based on the data object modification type.
     */
    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<D>> collection) {
        for (final DataTreeModification<D> change : collection) {
            final DataObjectModification<D> root = change.getRootNode();
            switch (root.getModificationType()) {
                case SUBTREE_MODIFIED:
                    update(change);
                    break;
                case WRITE:
                    // Treat an overwrite as an update
                    boolean update = change.getRootNode().getDataBefore() != null;
                    if (update) {
                        update(change);
                    } else {
                        add(change);
                    }
                    break;
                case DELETE:
                    remove(change);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Method should implements the added data object command.
     * @param newDataObject newly added object
     */
    public abstract void add(DataTreeModification<D> newDataObject);

    /**
     * Method should implements the removed data object command.
     * @param removedDataObject existing object being removed
     */
    public abstract void remove(DataTreeModification<D> removedDataObject);

    /**
     * Method should implements the updated data object command.
     * @param modifiedDataObject existing object being modified
     */
    public abstract void update(DataTreeModification<D> modifiedDataObject);
}
