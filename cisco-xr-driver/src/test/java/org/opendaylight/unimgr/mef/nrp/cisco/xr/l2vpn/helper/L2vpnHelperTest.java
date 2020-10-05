/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.three.L2vpnXrVersionSixDotThreeHelper;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.Database;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.XconnectGroups;
//import org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.one.L2vpnXrVersionSixDotOneHelper;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
/*import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.Database;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.XconnectGroups;
*/
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.Database;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.XconnectGroups;

/*
 * @author krzysztof.bijakowski@amartus.com
 */
public class L2vpnHelperTest {

    @Test
    public void build() {
        //given
        XconnectGroups xconnectGroups =  Mockito.mock(XconnectGroups.class);

        //when
        L2vpn actual = L2vpnXrVersionSixDotThreeHelper.build(xconnectGroups);

        //then
        Database actualDatabase = actual.getDatabase();
        assertNotNull(actualDatabase);

        XconnectGroups actualXconnectGroups = actualDatabase.getXconnectGroups();
        assertEquals(xconnectGroups, actualXconnectGroups);
    }
}
