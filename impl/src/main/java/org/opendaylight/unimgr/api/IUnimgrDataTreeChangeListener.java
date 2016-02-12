/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.api;

import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * abstract interface for unimgr data tree changes.
 * @author mohamed el-serngawy
 * @param <D> extended data object
 */
public interface IUnimgrDataTreeChangeListener<D extends DataObject> extends DataTreeChangeListener<D>, AutoCloseable {

    /**
     * method should implements the added data object command
     * @param newDataObject
     */
    void add(DataTreeModification<D> newDataObject);

    /**
     * method should implements the removed data object command
     * @param removedDataObject
     */
    void remove(DataTreeModification<D> removedDataObject);

    /**
     * method should implements the updated data object command
     * @param modifiedDataObject
     */
    void update(DataTreeModification<D> modifiedDataObject);
}
