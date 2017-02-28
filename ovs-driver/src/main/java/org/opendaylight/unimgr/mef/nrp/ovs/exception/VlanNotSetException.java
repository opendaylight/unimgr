/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.exception;

import org.opendaylight.unimgr.mef.nrp.common.ResourceNotAvailableException;

/**
 * Exception thrown when C-Tag VLAN ID is not set for termination point
 *
 * @author jakub.niezgoda@amartus.com
 */
public class VlanNotSetException extends ResourceNotAvailableException {
    public VlanNotSetException(String message){
        super(message);
    }
}