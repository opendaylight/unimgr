/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverBuilder;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverRepoService;
import org.opendaylight.unimgr.mef.nrp.impl.ActivationDriverRepoServiceImpl;


public final class ActivationDriverMocks {

    private ActivationDriverMocks() {
    }

    /**
     * Prepare mock {@link ActivationDriverBuilder}. The driver is produced via provided producer function. This covers
     * single port requests.
     * @param supplier to build driver
     * @return driver builder mock
     */
    public static ActivationDriverBuilder prepareDriver(Supplier<ActivationDriver> supplier) {
        final ActivationDriverBuilder mock = mock(ActivationDriverBuilder.class);
        doAnswer(inv -> Optional.ofNullable(
                supplier.get())).when(mock).driverFor(any(ActivationDriverBuilder.BuilderContext.class));
        return mock;
    }


    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        HashMap<String , ActivationDriver> drivers = new HashMap<>();

        private Builder() {}

        public Builder add(String id, ActivationDriver driver) {
            drivers.put(id, driver);
            return this;
        }

        public ActivationDriverRepoService build() {
            List<ActivationDriverBuilder> builders = drivers.entrySet().stream().map(e -> {
                ActivationDriverBuilder mock = mock(ActivationDriverBuilder.class);
                when(mock.getActivationDriverId()).thenReturn(e.getKey());
                when(mock.driverFor(any(ActivationDriverBuilder.BuilderContext.class)))
                    .thenReturn(Optional.of(e.getValue()));

                return mock;
            }).collect(Collectors.toList());
            return new ActivationDriverRepoServiceImpl(builders);
        }
    }
}
