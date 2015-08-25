/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.vcpe.api;

import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface IVcpeDataChangeListener extends DataChangeListener,
        AutoCloseable {

    public void create(Map<InstanceIdentifier<?>, DataObject> changes);

    public void update(Map<InstanceIdentifier<?>, DataObject> changes);

    public void delete(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes);
}
