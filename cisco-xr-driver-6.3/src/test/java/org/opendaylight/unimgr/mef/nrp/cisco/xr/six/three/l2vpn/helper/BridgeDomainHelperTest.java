/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.six.three.l2vpn.helper;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.six.three.l2vpn.helper.BridgeDomainHelper;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.BridgeDomainGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.bridge.domain.groups.BridgeDomainGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.BdAttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.BdPseudowires;
import org.powermock.api.mockito.PowerMockito;

/*
 * @author Om.SAwasthi@Xoriant.Com
 *
 */
public class BridgeDomainHelperTest {
    private BridgeDomainHelper bridgeDomainHelper;

    @Before
    public void setUp() throws Exception {
        bridgeDomainHelper = new BridgeDomainHelper();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void createBridgeDomainGroupsTest() {
        BridgeDomainGroup bridgeDomainGroup = PowerMockito.mock(BridgeDomainGroup.class);
        BridgeDomainGroups bridgeDoaminGroups =
                BridgeDomainHelper.createBridgeDomainGroups(bridgeDomainGroup);
        Assert.assertNotNull(bridgeDoaminGroups);

    }

    @SuppressWarnings("deprecation")
    @Test
    public void appendBridgeDomainTest() {
        String name = "ciscoD2";
        BdAttachmentCircuits bdattachmentCircuits = PowerMockito.mock(BdAttachmentCircuits.class);
        BdPseudowires bdpseudowires = PowerMockito.mock(BdPseudowires.class);
        BridgeDomainHelper bridgeDomainHelper1 =
                bridgeDomainHelper.appendBridgeDomain(name, bdattachmentCircuits, bdpseudowires);
        Assert.assertNotNull(bridgeDomainHelper1);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void buildTest() {
        BridgeDomainGroup bridgeDomainGroups = bridgeDomainHelper.build("ciscoD2");
        Assert.assertNotNull(bridgeDomainGroups);
    }

}
