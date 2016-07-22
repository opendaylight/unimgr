/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.InterfaceHelper;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.AttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.attachment.circuits.AttachmentCircuit;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author krzysztof.bijakowski@amartus.com
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(InterfaceHelper.class)
public class AttachmentCircuitHelperTest {

    @Test
    public void testConfigure() {
        //given
        InterfaceName interfaceName = new InterfaceName("GigabitEthernet0/0/1");

        FcPort port = Mockito.mock(FcPort.class);
        PowerMockito.mockStatic(InterfaceHelper.class);
        PowerMockito.when(InterfaceHelper.getInterfaceName(port)).thenReturn(interfaceName);

        //when
        AttachmentCircuitHelper helper = new AttachmentCircuitHelper();
        AttachmentCircuits actual = helper.configure(port).getAttachmentCircuits();

        //then
        List<AttachmentCircuit> actualAttachmentCircuitList = actual.getAttachmentCircuit();
        assertNotNull(actualAttachmentCircuitList);
        assertEquals(1, actualAttachmentCircuitList.size());

        AttachmentCircuit actualAttachmentCircuit = actualAttachmentCircuitList.get(0);
        assertNotNull(actualAttachmentCircuit);
        assertEquals(interfaceName, actualAttachmentCircuit.getName());
        assertTrue(actualAttachmentCircuit.isEnable());
    }
}
