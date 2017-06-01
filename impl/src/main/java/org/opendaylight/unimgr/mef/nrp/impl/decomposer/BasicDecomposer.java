/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.decomposer;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.unimgr.mef.nrp.api.Constraints;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.api.RequestDecomposer;
import org.opendaylight.unimgr.mef.nrp.api.Subrequrest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Basic graph based request decomposer
 * @author bartosz.michalik@amartus.com
 *
 */
public class BasicDecomposer implements RequestDecomposer {
    private static final Logger log = LoggerFactory.getLogger(BasicDecomposer.class);

    private final DataBroker broker;

    public BasicDecomposer(DataBroker broker) {
        this.broker = broker;
        log.trace("basic decomposer initialized");
    }

    /**
     * We currently support only one-to-one mapping between nep and sip
     * @param endpoints
     * @param constraint
     * @return
     */
    @Override
    public List<Subrequrest> decompose(List<EndPoint> endpoints, Constraints constraint) {
        return new DecompositionAction(endpoints, broker).decompose();
    }

}
