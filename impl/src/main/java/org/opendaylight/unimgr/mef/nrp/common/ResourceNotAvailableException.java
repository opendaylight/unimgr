/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.common;

/**
 * Created by marek.ryznar@amartus.com.
 */
public class ResourceNotAvailableException extends ResourceActivatorException {
    private static final long serialVersionUID = -5293322322613916698L;

    public ResourceNotAvailableException() {
        super();
    }

    public ResourceNotAvailableException(String message) {
        super(message);
    }
}
