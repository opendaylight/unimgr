/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.utils;

import com.google.common.base.Optional;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import static org.junit.Assert.*;
import static org.opendaylight.unimgr.utils.NodeTestUtils.*;
import static org.opendaylight.unimgr.utils.CapabilitiesService.Capability.Mode.AND;
import static org.opendaylight.unimgr.utils.CapabilitiesService.NodeContext.NodeCapability.*;

public class CapabilitiesServiceTest extends AbstractConcurrentDataBrokerTest {
    @Test
    public void testNode() {
        //given
        Optional<Node> mockedNodeOptional = mockNode();
        DataBroker mockedDataBrocker = mockDataBroker(Optional.absent());
        CapabilitiesService capabilitiesService = new CapabilitiesService(mockedDataBrocker);

        //when
        CapabilitiesService.NodeContext context = capabilitiesService.node(mockedNodeOptional.get());

        //then
        assertNotNull(context);
        assertTrue(context.getNode().isPresent());
        assertEquals(mockedNodeOptional.get(), context.getNode().get());
    }

    @Test
    public void testNodeIsSupportingSingleCapabilityPositive() {
        //given
        Optional<Node> mockedNodeOptional = mockNetconfNode(false);
        DataBroker mockedDataBrocker = mockDataBroker(Optional.absent());

        //when
        boolean result = new CapabilitiesService(mockedDataBrocker)
                .node(mockedNodeOptional.get())
                .isSupporting(NETCONF);

        //then
        assertTrue(result);
    }

    @Test
    public void testNodeIsSupportingSingleCapabilityNegative() {
        //given
        Optional<Node> mockedNodeOptional = mockNode();
        DataBroker mockedDataBrocker = mockDataBroker(Optional.absent());

        //when
        boolean result = new CapabilitiesService(mockedDataBrocker)
                .node(mockedNodeOptional.get())
                .isSupporting(NETCONF);

        //then
        assertFalse(result);
    }

    @Test
    public void testNodeIsSupportingMultipleCapabilitiesPositive() {
        //given
        Optional<Node> mockedNodeOptional = mockNetconfNode(true);
        DataBroker mockedDataBrocker = mockDataBroker(Optional.absent());

        //when
        boolean result = new CapabilitiesService(mockedDataBrocker)
                .node(mockedNodeOptional.get())
                .isSupporting(AND, NETCONF, NETCONF_CISCO_IOX_L2VPN, NETCONF_CISCO_IOX_IFMGR);

        //then
        assertTrue(result);
    }

    @Test
    public void testNodeIsSupportingMultipleCapabilitiesNegative() {
        //given
        Optional<Node> mockedNodeOptional = mockNode();
        DataBroker mockedDataBrocker = mockDataBroker(Optional.absent());

        //when
        boolean result = new CapabilitiesService(mockedDataBrocker)
                .node(mockedNodeOptional.get())
                .isSupporting(AND, NETCONF, NETCONF_CISCO_IOX_L2VPN, NETCONF_CISCO_IOX_IFMGR);

        //then
        assertFalse(result);
    }
}
