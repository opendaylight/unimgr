/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.api;

/**
 * his exception indicates that no activation drivers was found for activating a service. The service
 * will not be activated because the system failed to identify any driver candidate.
 */
public class ActivationDriverNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 8093501625481543532L;

    public ActivationDriverNotFoundException() {
    }

    public ActivationDriverNotFoundException(String message) {
        super(message);
    }
}
