/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.api;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yangtools.yang.binding.DataObject;

public abstract class AbstractCommand<D extends DataObject> {

    protected DataBroker dataBroker;
    protected DataTreeModification<D> dataObject;

    /**
     * Abstract command basic constructor.
     * @param dataBroker the data broker
     * @param dataObject the object change to process
     */
    public AbstractCommand(final DataBroker dataBroker, final DataTreeModification<D> dataObject) {
        this.dataBroker = dataBroker;
        this.dataObject = dataObject;
    }

    /**
     * Abstract execute method.
     */
    public abstract void execute();

}
