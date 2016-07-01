/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.api;

import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;

/**
 * This interface is used to request an ActivationDriver for a given MEF service fragment.
 */
public interface ActivationDriverRepoService {

    /**
     * Get driver for a port.
     * @param port to
     * @param context blackboard for recording state during driver selection
     * @return activation driver
     * @throws ActivationDriverAmbiguousException when multiple drivers declare they can configure port
     * @throws ActivationDriverNotFoundException when no driver found for port
     */
    ActivationDriver getDriver(FcPort port, ActivationDriverBuilder.BuilderContext context);

    /**
     * Get driver for two ports on a single device.
     * @param portA from port
     * @param portZ to port
     * @param context blackboard for recording state during driver selection
     * @return activation driver
     * @throws ActivationDriverAmbiguousException when multiple drivers declare they can configure ports
     * @throws ActivationDriverNotFoundException when no driver found for ports
     */
    ActivationDriver getDriver(FcPort portA, FcPort portZ, ActivationDriverBuilder.BuilderContext context);
}