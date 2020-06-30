/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverBuilder;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverNotFoundException;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverRepoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default application repo that is populated with the application driver builders registered as OSGi services.
 *
 * @author alex.feigin@hpe.com
 * @author bartosz.michalik@amartus.com [modifications]
 */
public class ActivationDriverRepoServiceImpl implements ActivationDriverRepoService {
    private static final Logger LOG = LoggerFactory.getLogger(ActivationDriverRepoServiceImpl.class);

    private final Collection<ActivationDriverBuilder> builders;

    public ActivationDriverRepoServiceImpl() {
        this.builders = Collections.emptyList();
    }

    public ActivationDriverRepoServiceImpl(List<ActivationDriverBuilder> builders) {
        LOG.debug("Activation drivers initialized");
        this.builders = builders;
    }

    public void bind(ActivationDriverBuilder builder) {
        LOG.debug("builder {} bound", builder);
    }

    public void unbind(ActivationDriverBuilder builder) {
        LOG.debug("builder {} unbound", builder);
    }

    @Override
    public Optional<ActivationDriver> getDriver(String activationDriverId) {
        ActivationDriverBuilder builder = builders.stream()
                .filter(db -> db.getActivationDriverId().equals(activationDriverId))
                .findFirst().orElseThrow(() ->
                new ActivationDriverNotFoundException(MessageFormat
                        .format("No driver with id {0} registered", activationDriverId)));
        return builder.driverFor(new ActivationDriverBuilder.BuilderContext());

    }
}
