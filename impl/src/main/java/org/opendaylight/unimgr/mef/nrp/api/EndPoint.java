/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.api;

import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev171221.NrpConnectivityServiceEndPointAttrs;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.ConnectivityServiceEndPoint;

import java.util.Objects;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EndPoint endPoint = (EndPoint) o;
        return Objects.equals(endpoint, endPoint.endpoint) &&
                Objects.equals(attrs, endPoint.attrs) &&
                Objects.equals(systemNepUuid, endPoint.systemNepUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint, attrs, systemNepUuid);
    }
}
