/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.api;

import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev170712.NrpConnectivityServiceEndPointAttrs;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.common.rev170712.Uuid;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.connectivity.rev170712.ConnectivityServiceEndPoint;

/**
 * @see ConnectivityServiceEndPoint
 * @author bartosz.michalik@amartus.com
 */
public class EndPoint {
    private final ConnectivityServiceEndPoint endpoint;

    /**
     * Optional attributes.
     * (likely to change to different implementation)
     */
    private final NrpConnectivityServiceEndPointAttrs attrs;

    private Uuid systemNepUuid;

    /**
     * Initialize endpoint.
     * @param endpoint endpoint data
     * @param attrs associated NRP attributes
     */
    public EndPoint(ConnectivityServiceEndPoint endpoint, NrpConnectivityServiceEndPointAttrs attrs) {
        this.endpoint = endpoint;
        this.attrs = attrs;
    }

    public ConnectivityServiceEndPoint getEndpoint() {
        return endpoint;
    }

    public NrpConnectivityServiceEndPointAttrs getAttrs() {
        return attrs;
    }

    public Uuid getSystemNepUuid() {
        return systemNepUuid;
    }

    public EndPoint setSystemNepUuid(Uuid systemNepUuid) {
        this.systemNepUuid = systemNepUuid;
        return this;
    }
}
