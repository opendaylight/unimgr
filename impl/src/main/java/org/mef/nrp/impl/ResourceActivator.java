/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.mef.nrp.impl;

import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.GFcPort;

public interface ResourceActivator {

    public void activate(String nodeName, String outerName, String innerName, GFcPort flowPoint, GFcPort neighbor, long mtu);

    public void deactivate(String nodeName, String outerName, String innerName, GFcPort flowPoint, GFcPort neighbor, long mtu);
}
