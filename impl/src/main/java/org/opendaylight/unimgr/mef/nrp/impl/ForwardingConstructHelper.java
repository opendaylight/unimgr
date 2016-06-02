/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl;

import java.util.Objects;

import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.GForwardingConstruct;

public class ForwardingConstructHelper {

    /**
     * Test to see if both ends of a ForwardingConstruct are on the same node.
     * @param forwardingConstruct the ForwardingConstruct to test
     * @return true if both ends are on same node
     */
    public static boolean isTheSameNode(GForwardingConstruct forwardingConstruct) {
        String hostA = host(ltp(forwardingConstruct, 0));
        String hostZ = host(ltp(forwardingConstruct, 1));

        return Objects.equals(hostA, hostZ);
    }

    public static String ltp(GForwardingConstruct fc, int port) {
        return fc.getFcPort().get(port).getLtpRefList().get(0).getValue();
    }

    public static String host(String ltp) {
        return ltp.split(":")[0];
    }
}
