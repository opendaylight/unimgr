/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.common;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.opendaylight.unimgr.mef.nrp.api.EndPoint;

/**
 * Device facing SPI for activating or deactivating a fragment of an NRP
 * Connectivity Service on a single device.
 */
public interface ResourceActivator {

    /**
     * Activate connectivity betwee the provided endpoints.
     * @param endPoints list of endpoint to connect
     * @param serviceName generated service id
     * @throws ResourceActivatorException activation problem
     * @throws ExecutionException transaction execution failed
     * @throws InterruptedException transaction was interrupted
     */
    void activate(List<EndPoint> endPoints, String serviceName)
            throws  ResourceActivatorException, InterruptedException, ExecutionException;

    /**
     * Deactivate connectivity between the provided endpoints.
     * @param endPoints list of endpoint between which connection have to be deactivated
     * @param serviceName generated service id
     * @throws ResourceActivatorException activation problem
     * @throws ExecutionException transaction execution failed
     * @throws InterruptedException transaction was interrupted
     */
    void deactivate(List<EndPoint> endPoints, String serviceName)
            throws ResourceActivatorException, InterruptedException, ExecutionException;
}
