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
 * Request decomposer is responsible for decomposition of {@link org.opendaylight.yang.gen.v1.urn.mef.yang.tapiconnectivity.rev170531.connectivity.context.g.ConnectivityService}
 * requests into one or many driver requests.
 * @author bartosz.michalik@amartus.com
 */
public interface RequestDecomposer {
    /**
     *
     * @param endpoints list of connectiviy request endpoints
     * @param constraint on decoposition
     * @return list of subrequests - one per driver
     */
    List<Subrequrest> decompose(List<EndPoint> endpoints, Constraints constraint) throws FailureResult;
}
