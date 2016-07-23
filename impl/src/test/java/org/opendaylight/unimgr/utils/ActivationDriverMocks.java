/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.utils;

import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author bartosz.michalik@amartus.com
 */
public class ActivationDriverMocks {
    /**
     * Prepare mock {@link ActivationDriverBuilder}. The driver is produced via provided producer function. This covers
     * single port requests.
     * @param producer to build driver
     * @return driver builder mock
     */
    public static ActivationDriverBuilder prepareDriver(Function<FcPort, ActivationDriver> producer) {
        final ActivationDriverBuilder mock = mock(ActivationDriverBuilder.class);

        doAnswer(inv -> {
            FcPort port = (FcPort) inv.getArguments()[0];
            return Optional.ofNullable(producer.apply(port));
        }).when(mock).driverFor(any(FcPort.class), any(ActivationDriverBuilder.BuilderContext.class));

        doReturn(Optional.empty()).when(mock)
                .driverFor(any(FcPort.class), any(FcPort.class), any(ActivationDriverBuilder.BuilderContext.class));

        return mock;
    }

    /**
     * Prepare mock {@link ActivationDriverBuilder}. The driver is produced via provided producer function.  This covers
     * dual port requests (for internal cross-connect).
     * @param producer to build driver
     * @return driver builder mock
     */
    public static ActivationDriverBuilder prepareDriver(BiFunction<FcPort, FcPort, ActivationDriver> producer) {
        final ActivationDriverBuilder mock = mock(ActivationDriverBuilder.class);

        doAnswer(inv -> {
            FcPort port1 = (FcPort) inv.getArguments()[0];
            FcPort port2 = (FcPort) inv.getArguments()[1];
            return Optional.ofNullable(producer.apply(port1, port2));
        }).when(mock).driverFor(any(FcPort.class), any(FcPort.class), any(ActivationDriverBuilder.BuilderContext.class));

        doReturn(Optional.empty()).when(mock)
                .driverFor(any(FcPort.class), any(ActivationDriverBuilder.BuilderContext.class));
        return mock;
    }
}
