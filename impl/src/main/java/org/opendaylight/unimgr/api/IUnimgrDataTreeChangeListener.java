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

public interface IUnimgrDataTreeChangeListener<D extends DataObject> extends DataTreeChangeListener<D>, AutoCloseable {

    void add(DataTreeModification<D> newDataObject);

    void update(DataTreeModification<D> modifiedDataObject);

    void remove(DataTreeModification<D> removedDataObject);

}
