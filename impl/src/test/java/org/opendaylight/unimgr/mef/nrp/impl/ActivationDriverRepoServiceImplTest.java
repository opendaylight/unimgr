/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.impl;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.unimgr.utils.ActivationDriverMocks.prepareDriver;

import java.util.Collections;
import java.util.Optional;

import org.junit.Test;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverBuilder;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverNotFoundException;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverRepoService;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170531.UniversalId;

/**
 * @author bartosz.michalik@amartus.com
 */
public class ActivationDriverRepoServiceImplTest {

    @Test(expected = ActivationDriverNotFoundException.class)
    public void testEmptyBuilderList() throws Exception {

        ActivationDriverRepoService driverRepo = new ActivationDriverRepoServiceImpl(Collections.emptyList());
        driverRepo.getDriver(new UniversalId("non-existing"));
    }


    @Test
    public void testMatchingByUuid() throws Exception {

        final UniversalId uuid = new UniversalId("aDriver");

        final ActivationDriver driver = mock(ActivationDriver.class);

        ActivationDriverBuilder builder = prepareDriver(() -> driver);
        when(builder.getNodeUuid()).thenReturn(uuid);

        ActivationDriverRepoService driverRepo = new ActivationDriverRepoServiceImpl(Collections.singletonList(
                builder
        ));

        final Optional<ActivationDriver> driver1 = driverRepo.getDriver(uuid);
        try {
            driverRepo.getDriver(new UniversalId("otherDriver"));
        } catch(ActivationDriverNotFoundException expected) {}

        assertTrue(driver1.isPresent());
    }
}
