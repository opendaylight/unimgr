/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.api;

import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.NrpConnectivityServiceEndPointAttrs;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ConnectivityServiceEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.OwnedNodeEdgePointRef;

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

    private OwnedNodeEdgePointRef ref;
    private String localId;

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


    public EndPoint setNepRef(OwnedNodeEdgePointRef ref) {
        this.ref = ref;
        return this;
    }

    public OwnedNodeEdgePointRef getNepRef() {
        return ref;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EndPoint endPoint = (EndPoint) o;
        return Objects.equals(endpoint, endPoint.endpoint) &&
                Objects.equals(attrs, endPoint.attrs) &&
                Objects.equals(ref, endPoint.ref);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint, attrs, ref);
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public String getLocalId() {
        return localId;
    }
}
