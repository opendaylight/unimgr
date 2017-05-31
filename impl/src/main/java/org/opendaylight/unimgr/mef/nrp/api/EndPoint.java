/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.api;

import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp_interface.rev170227.NrpCreateConnectivityServiceEndPointAttrs;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.UniversalId;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170227.ConnectivityServiceEndPoint;

/**
 * @see ConnectivityServiceEndPoint
 * @author bartosz.michalik@amartus.com
 */
public class EndPoint {
    private final ConnectivityServiceEndPoint endpoint;
    /**
     * Optional attributes
     * (likely to change to different implementation)
     */
    private final NrpCreateConnectivityServiceEndPointAttrs attrs;

    private UniversalId systemNepUuid;

    /**
     * Initialize endpoint
     * @param endpoint endpoint data
     * @param attrs associated NRP attributes
     */
    public EndPoint(ConnectivityServiceEndPoint endpoint, NrpCreateConnectivityServiceEndPointAttrs attrs) {
        this.endpoint = endpoint;
        this.attrs = attrs;
    }

    /**
     *
     * @return endpoints
     */
    public ConnectivityServiceEndPoint getEndpoint() {
        return endpoint;
    }

    /**
     *
     * @return attributes
     */
    public NrpCreateConnectivityServiceEndPointAttrs getAttrs() {
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
