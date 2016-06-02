/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.api;

/**
 * This exception indicates that multiple activation drivers are candidates for activating a service. The service
 * will not be activated because the system failed to select a single driver candidate.
 */
public class ActivationDriverAmbiguousException extends RuntimeException {
    private static final long serialVersionUID = 3299218135930759678L;
}
