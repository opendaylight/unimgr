/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.mef.nrp.impl;

import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.GFcPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author alex.feigin@hpe.com
 */
public class ActivationDriverRepoServiceImpl implements ActivationDriverRepoService {
    private static final Logger LOG = LoggerFactory.getLogger(ActivationDriverRepoServiceImpl.class);

    private Collection<ActivationDriverBuilder> builders = ConcurrentHashMap.newKeySet();


    /* (non-Javadoc)
     * @see org.mef.nrp.impl.ActivationDriverRepoService#bindBuilder(org.mef.nrp.impl.ActivationDriverBuilder)
     */
    @Override
    public void bindBuilder(ActivationDriverBuilder builder) {
        if (builder == null) {
            return;
        }
        LOG.info("ActivationDriverRepoService.bindBuilder got [{}] instance", builder.getClass().getSimpleName());
        builders.add(builder);
    }

    /* (non-Javadoc)
     * @see org.mef.nrp.impl.ActivationDriverRepoService#unbindBuilder(org.mef.nrp.impl.ActivationDriverBuilder)
     */
    @Override
    public void unbindBuilder(ActivationDriverBuilder builder) {
        if (builder==null)
        {
            return;
        }
        LOG.info("ActivationDriverRepoService.unbindBuilder got [{}] instance", builder.getClass().getSimpleName());
        builders.remove(builder);
    }

    protected ActivationDriver getDriver(Function<ActivationDriverBuilder, Optional<ActivationDriver>> driver) {
        final List<ActivationDriver> drivers = builders.stream().map(driver)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        if (drivers.size() > 1) {
            throw new ActivationDriverAmbiguousException();
        }
        if (drivers.size() == 0) {
            throw new ActivationDriverNotFoundException();
        }
        return drivers.get(0);
    }

    public ActivationDriver getDriver(GFcPort aPort, GFcPort zPort, ActivationDriverBuilder.BuilderContext context) {
        return getDriver(x -> x.driverFor(aPort, zPort, context));
    }

    public ActivationDriver getDriver(GFcPort port, ActivationDriverBuilder.BuilderContext context) {
        return getDriver(x -> x.driverFor(port, context));
    }
}
