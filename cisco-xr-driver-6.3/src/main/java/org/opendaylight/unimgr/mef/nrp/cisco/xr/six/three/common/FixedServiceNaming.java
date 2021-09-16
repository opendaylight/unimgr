/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.cisco.xr.six.three.common;

import org.opendaylight.unimgr.mef.nrp.common.ServiceNaming;

/**
 * This is a placeholder class for implementing service naming when activating and deactivating MEF services.
 * TODO: Implement a more robust naming provider.
 */
public class FixedServiceNaming implements ServiceNaming {

    @Override
    public String getOuterName(String id) {
        return "EUR16-" + id;
    }

    @Override
    public String getInnerName(String id) {
        return "EUR16-" + id;
    }

    @Override
    public String replaceForbidenCharacters(String id) {
        return id.replace(":", "_");
    }

}