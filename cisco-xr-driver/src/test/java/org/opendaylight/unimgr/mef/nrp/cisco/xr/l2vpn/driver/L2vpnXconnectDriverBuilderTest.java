/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.driver;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.unimgr.utils.NodeTestUtils.*;

public class L2vpnXconnectDriverBuilderTest {

    private ActivationDriverBuilder.BuilderContext context;

    @Before
    public void setUp() {
        context = new ActivationDriverBuilder.BuilderContext();
    }

    @Test
    public void testDriverForSinglePortNoNode() {
        //given
        FcPort port = mockFcPort();

        //when
        Optional<ActivationDriver> result =  new L2vpnXconnectDriverBuilder(mockDataBroker(com.google.common.base.Optional.absent()), null).driverFor(port, context);

        //then
        assertFalse(result.isPresent());
    }

    @Test
    public void testDriverForSinglePortNetconfNode() {
        //given
        FcPort port = mockFcPort();

        //when
        Optional<ActivationDriver> result =  new L2vpnXconnectDriverBuilder(mockDataBroker(mockNetconfNode(false)), null).driverFor(port, context);

        //then
        assertFalse(result.isPresent());
    }

    @Test
    public void testDriverForSinglePortNetconfNodeCapabilities() {
        //given
        FcPort port = mockFcPort();

        //when
        L2vpnXconnectDriverBuilder driverBuilder = new L2vpnXconnectDriverBuilder(mockDataBroker(mockNetconfNode(true)), null);
        Optional<ActivationDriver> result = driverBuilder.driverFor(port, context);

        //then
        assertTrue(result.isPresent());
        assertEquals(driverBuilder.getDriver().getClass(), result.get().getClass());
    }

    @Test
    public void testDriverForTwoPortsNoNode() {
        //given
        FcPort portA = mockFcPort(1);
        FcPort portZ = mockFcPort(2);

        //when
        Optional<ActivationDriver> result =  new L2vpnXconnectDriverBuilder(mockDataBroker(com.google.common.base.Optional.absent()), null).driverFor(portA, portZ, context);

        //then
        assertFalse(result.isPresent());
    }

    @Test
    public void testDriverForTwoPortsNetconfNode() {
        //given
        FcPort portA = mockFcPort(1);
        FcPort portZ = mockFcPort(2);

        //when
        Optional<ActivationDriver> result =  new L2vpnXconnectDriverBuilder(mockDataBroker(mockNetconfNode(false)), null).driverFor(portA, portZ, context);

        //then
        assertFalse(result.isPresent());
    }

    @Test
    public void testDriverForTwoPortsNetconfNodeCapabilities() {
        //given
        FcPort portA = mockFcPort(1);
        FcPort portZ = mockFcPort(2);

        //when
        Optional<ActivationDriver> result =   new L2vpnXconnectDriverBuilder(mockDataBroker(mockNetconfNode(true)), null).driverFor(portA, portZ, context);

        //then
        assertFalse(result.isPresent());
    }
}
