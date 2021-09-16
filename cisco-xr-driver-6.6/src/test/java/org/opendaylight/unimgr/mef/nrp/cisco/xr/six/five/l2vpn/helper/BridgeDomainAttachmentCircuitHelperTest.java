/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.l2vpn.helper;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.l2vpn.helper.BridgeDomainAttachmentCircuitHelper;
import org.opendaylight.unimgr.mef.nrp.common.TapiUtils;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.bridge.domain.table.bridge.domains.bridge.domain.BdAttachmentCircuits;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.BdAttachmentCircuits;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ConnectivityServiceEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.end.point.ServiceInterfacePoint;

/*
 * @author Om.SAwasthi@Xoriant.Com
 *
 */
public class BridgeDomainAttachmentCircuitHelperTest {

    private BridgeDomainAttachmentCircuitHelper bdAttachmentCircuitHelper;
    private EndPoint ep1;
    private ServicePort port;
    private static final String UUID1 = "sip:ciscoD1:GigabitEthernet0/0/0/1";
    private static final String NETCONF_TOPOLODY_NAME = "topology-netconf";

    @Before
    public void setUp() throws Exception {
        bdAttachmentCircuitHelper = new BridgeDomainAttachmentCircuitHelper();

    }

    @Test
    public void testAddPort() {
        ConnectivityServiceEndPoint cep =
                new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
                        .connectivity.service.EndPointBuilder()
                        .setServiceInterfacePoint(TapiUtils.toSipRef(new Uuid(UUID1), ServiceInterfacePoint.class))
                        .setDirection(PortDirection.BIDIRECTIONAL).build();
        ep1 = new EndPoint(cep, null);
        port = ServicePort.toServicePort(ep1, NETCONF_TOPOLODY_NAME);
        port.setVlanId(301L);
        BridgeDomainAttachmentCircuitHelper bdAttachmentCircuitHelper1 =
                bdAttachmentCircuitHelper.addPort(port, false);
        Assert.assertNotNull(bdAttachmentCircuitHelper1);

    }

    @Test
    public void testBuild() {
        BdAttachmentCircuits bdAttachmentCircuits = bdAttachmentCircuitHelper.build();
        Assert.assertNotNull(bdAttachmentCircuits);
    }

}
