/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.api;

import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp_interface.rev170531.NrpConnectivityServiceEndPointAttrsG;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170531.UniversalId;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170531.ConnectivityServiceEndPointG;

/**
 * @see ConnectivityServiceEndPointG
 * @author bartosz.michalik@amartus.com
 */
public class EndPoint {
    private final ConnectivityServiceEndPointG endpoint;
    /**
     * Optional attributes
     * (likely to change to different implementation)
     */
    private final NrpConnectivityServiceEndPointAttrsG attrs;

    private UniversalId systemNepUuid;

    /**
     * Initialize endpoint
     * @param endpoint endpoint data
     * @param attrs associated NRP attributes
     */
    public EndPoint(ConnectivityServiceEndPointG endpoint, NrpConnectivityServiceEndPointAttrsG attrs) {
        this.endpoint = endpoint;
        this.attrs = attrs;
    }

    /**
     *
     * @return endpoints
     */
    public ConnectivityServiceEndPointG getEndpoint() {
        return endpoint;
    }

    /**
     *
     * @return attributes
     */
    public NrpConnectivityServiceEndPointAttrsG getAttrs() {
        return attrs;
    }

    public UniversalId getSystemNepUuid() {
        return systemNepUuid;
    }

    public EndPoint setSystemNepUuid(UniversalId systemNepUuid) {
        this.systemNepUuid = systemNepUuid;
        return this;
    }
}
