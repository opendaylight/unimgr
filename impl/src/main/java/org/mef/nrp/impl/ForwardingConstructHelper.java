/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.mef.nrp.impl;

import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.GForwardingConstruct;

public class ForwardingConstructHelper {

    public static boolean isTheSameNode(GForwardingConstruct forwardingConstruct) {
    	String aHost = host(ltp(forwardingConstruct, 0));
    	String zHost = host(ltp(forwardingConstruct, 1));

        return aHost != null && zHost != null && aHost.equals(zHost);
    }

    public static String ltp(GForwardingConstruct fc, int port) {
    	return fc.getFcPort().get(port).getLtpRefList().get(0).getValue();
    }

    public static String host(String ltp) {
    	return ltp.split(":")[0];
    }
}
