/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.api;

import java.util.Optional;

/**
 * This interface is used to request an ActivationDriver for a given MEF service fragment.
 */
public interface ActivationDriverRepoService {

    /**
     * Get driver by universal id.
     * @param activationDriverId driver id
     * @return activation driver
     * @throws ActivationDriverAmbiguousException when multiple drivers declare they can configure port
     * @throws ActivationDriverNotFoundException when no driver found for port
     */
    Optional<ActivationDriver> getDriver(String activationDriverId);
}