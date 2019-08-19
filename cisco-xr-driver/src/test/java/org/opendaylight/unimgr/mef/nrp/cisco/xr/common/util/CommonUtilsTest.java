/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.common.TapiUtils;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ConnectivityServiceEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.end.point.ServiceInterfacePoint;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;

/**
 * @author Om.SAwasthi@Xoriant.Com
 *
 */
public class CommonUtilsTest {
    public static final String METHODNAME1 = "isSameDevice";
    public static final String METHODNAME2 = "isSameInterface";
    public static final String UUID = "sip:ciscoD1:GigabitEthernet0/0/0/1";
    private EndPoint ep;
    private CommonUtils util;

    @Before
    public void setUp() throws Exception {

        ConnectivityServiceEndPoint cep =
                new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.EndPointBuilder()
                        .setServiceInterfacePoint(
                                TapiUtils.toSipRef(new Uuid(UUID), ServiceInterfacePoint.class))
                        .setDirection(PortDirection.BIDIRECTIONAL).build();
        ep = new EndPoint(cep, null);
        util = new CommonUtils();
    }

    @Test
    public void isSameDeviceTest() {
        MemberModifier.suppress(MemberMatcher.method(CommonUtils.class, METHODNAME1));

        List<String> ls = new ArrayList<String>();

        assertFalse(util.isSameDevice(ep, ls));

        ls.add("ciscoD1");

        assertTrue(util.isSameDevice(ep, ls));

    }

    @Test
    public void isSameInterfaceTest() {
        MemberModifier.suppress(MemberMatcher.method(CommonUtils.class, METHODNAME2));

        List<Uuid> ls = new ArrayList<Uuid>();

        assertFalse(util.isSameInterface(ep, ls));

        ls.add(new Uuid(UUID));

        assertTrue(util.isSameInterface(ep, ls));

    }
}

