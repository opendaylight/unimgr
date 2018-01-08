/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.api;

import java.util.List;

/**
 * Request decomposer is responsible for decomposition of ConnectivityService
 * requests into one or many driver requests.
 * @see org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.connectivity.context.ConnectivityService
 * @author bartosz.michalik@amartus.com
 */
public interface RequestDecomposer {
    /**
     * Decompose the provided endpoint connectivity request into a list of sub-requests.
     * @param endpoints list of connectiviy request endpoints
     * @param constraint on decoposition
     * @return list of subrequests - one per driver
     */
    List<Subrequrest> decompose(List<EndPoint> endpoints, Constraints constraint) throws FailureResult;
}
