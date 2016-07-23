/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.opendaylight.unimgr.utils.ActivationDriverMocks.prepareDriver;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverAmbiguousException;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverNotFoundException;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverRepoService;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPortBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

/**
 * @author bartosz.michalik@amartus.com
 */
public class ActivationDriverRepoServiceImplTest {

    @Test(expected = ActivationDriverNotFoundException.class)
    public void testEmptyBuilderList() throws Exception {

        ActivationDriverRepoService driverRepo = new ActivationDriverRepoServiceImpl(Collections.emptyList());
        final FcPort port = new FcPortBuilder()
                .setTopology(new TopologyId("a")).build();
        driverRepo.getDriver(port, null);
    }


    @Test(expected = ActivationDriverAmbiguousException.class)
    public void testConflictingBuilders() throws Exception {

        final ActivationDriver driver = mock(ActivationDriver.class);

        ActivationDriverRepoService driverRepo = new ActivationDriverRepoServiceImpl(Arrays.asList(
                prepareDriver(p -> driver), prepareDriver(p -> driver)
        ));

        final FcPort port = new FcPortBuilder()
                .setTopology(new TopologyId("a")).build();
        driverRepo.getDriver(port, null);
    }

    @Test
    public void testMatchingWithSinglePort() throws Exception {

        final ActivationDriver driver = mock(ActivationDriver.class);

        ActivationDriverRepoService driverRepo = new ActivationDriverRepoServiceImpl(Collections.singletonList(
                prepareDriver(p -> driver)
        ));

        final FcPort port = new FcPortBuilder()
                .setTopology(new TopologyId("a")).build();
        final ActivationDriver driverFromRepo = driverRepo.getDriver(port, null);
        assertEquals(driver, driverFromRepo);
    }

    @Test
    public void testMatchingWithDualPort() throws Exception {

        final ActivationDriver d1 = mock(ActivationDriver.class);
        final ActivationDriver d2 = mock(ActivationDriver.class);

        ActivationDriverRepoService driverRepo = new ActivationDriverRepoServiceImpl(Arrays.asList(
                prepareDriver(p -> d1), prepareDriver((a,b) -> d2)
        ));

        final FcPort portA = new FcPortBuilder()
                .setTopology(new TopologyId("a")).build();
        final FcPort portB = new FcPortBuilder()
                .setTopology(new TopologyId("b")).build();
        final ActivationDriver driverFromRepo = driverRepo.getDriver(portA, portB, null);
        assertEquals(d2, driverFromRepo);
    }
}
