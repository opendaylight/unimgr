/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.api;

import java.text.MessageFormat;

public class FailureResult extends Exception {

    private static final long serialVersionUID = -995923939107287244L;

    public FailureResult(String message) {
        super(message);
    }

    public FailureResult(String message, Object ... args) {
        super(MessageFormat.format(message, args));
    }
}
