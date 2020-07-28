/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.l2vpn.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.common.helper.InterfaceHelper;
/*mport org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.AttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.attachment.circuits.AttachmentCircuit;*/
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


/*
 * @author krzysztof.bijakowski@amartus.com
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(InterfaceHelper.class)
public class AttachmentCircuitHelperTest {

    //@Test
   /* public void testBuild() {
        //given
        InterfaceName interfaceName = new InterfaceName("GigabitEthernet0/0/1");

        ServicePort port = Mockito.mock(ServicePort.class);
        PowerMockito.mockStatic(InterfaceHelper.class);
        PowerMockito.when(InterfaceHelper.getInterfaceName(port)).thenReturn(interfaceName);

        //when
        AttachmentCircuits actual = new AttachmentCircuitHelper().addPort(port, true).build();

        //then
        List<AttachmentCircuit> actualAttachmentCircuitList = actual.getAttachmentCircuit();
        assertNotNull(actualAttachmentCircuitList);
        assertEquals(1, actualAttachmentCircuitList.size());

        AttachmentCircuit actualAttachmentCircuit = actualAttachmentCircuitList.get(0);
        assertNotNull(actualAttachmentCircuit);
        assertEquals(interfaceName, actualAttachmentCircuit.getName());
        assertNotNull(actualAttachmentCircuit.getEnable());
    }*/
}
