/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl;

import java.text.MessageFormat;
import java.util.Random;

/**
 * Dummy resource pool.
 * @author bartosz.michalik@amartus.com
 */
public class ConnectivityServiceIdResourcePool {

    private static Random r = new Random();

    private static String pattern = "{0}:{1}";

    public String getServiceId() {
        return MessageFormat.format(pattern,Long.toString(System.currentTimeMillis(), 16), Integer.toString(r.nextInt(), 16));
    }

}
