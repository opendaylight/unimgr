/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl;

import org.opendaylight.unimgr.mef.nrp.api.RequestValidator;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.CreateConnectivityServiceInput;

/**
 * @author bartosz.michalik@amartus.com
 */
public class DefaultValidator implements RequestValidator {
    @Override
    public ValidationResult checkValid(CreateConnectivityServiceInput input) {
        return new ValidationResult();
    }
}
