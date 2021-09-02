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

import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.XconnectGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.xconnect.groups.XconnectGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.xconnect.groups.xconnect.group.P2pXconnects;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnect;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.AttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.Pseudowires;


/*
 * @author krzysztof.bijakowski@amartus.com
 */
public class XConnectHelperTest {

    @Test
    public void testCreateXConnectGroupsSingle() {
        //given
        XconnectGroup xconnectGroup = Mockito.mock(XconnectGroup.class);

        //when
        XconnectGroups xconnectGroups = XConnectHelper.createXConnectGroups(xconnectGroup);

        //then
        assertNotNull(xconnectGroups);

        List<XconnectGroup> xconnectGroupList = xconnectGroups.getXconnectGroup();
        assertNotNull(xconnectGroupList);
        assertEquals(1, xconnectGroupList.size());

        assertEquals(xconnectGroup, xconnectGroupList.get(0));

    }

    @Test
    public void testCreateXConnectGroupsMultiple() {
        //given
        List<XconnectGroup> xconnectGroupList = new LinkedList<>();
        xconnectGroupList.add(Mockito.mock(XconnectGroup.class));
        xconnectGroupList.add(Mockito.mock(XconnectGroup.class));

        //when
        XconnectGroups xconnectGroups = XConnectHelper.createXConnectGroups(xconnectGroupList);

        //then
        assertNotNull(xconnectGroups);
        assertEquals(xconnectGroupList, xconnectGroups.getXconnectGroup());
    }

    @Test
    public void testBuild() {
        //given
        String xconnectName = "ExampleXConnectName";
        String xconnectGroupName = "ExampleXConnectGroupName";
        AttachmentCircuits attachmentCircuits = Mockito.mock(AttachmentCircuits.class);
        Pseudowires pseudowires = Mockito.mock(Pseudowires.class);

        //when
        XconnectGroup xconnectGroup = new XConnectHelper().appendXConnect(
                                                                        xconnectName,
                                                                        attachmentCircuits,
                                                                        pseudowires).build(xconnectGroupName);

        //then
        assertNotNull(xconnectGroup);
        assertEquals(xconnectGroupName, xconnectGroup.getName().getValue());
        assertEquals(xconnectGroupName, xconnectGroup.key().getName().getValue());

        P2pXconnects p2pXconnects = xconnectGroup.getP2pXconnects();
        assertEquals(p2pXconnects, xconnectGroup.getP2pXconnects());
        assertNotNull(p2pXconnects);

        List<P2pXconnect> p2pXconnectList =  p2pXconnects.getP2pXconnect();
        assertNotNull(p2pXconnectList);
        assertEquals(1, p2pXconnectList.size());

        P2pXconnect p2pXconnect = p2pXconnectList.get(0);
        assertNotNull(p2pXconnect);
        assertEquals(xconnectName, p2pXconnect.getName().getValue());
        assertEquals(pseudowires, p2pXconnect.getPseudowires());
        assertEquals(attachmentCircuits, p2pXconnect.getAttachmentCircuits());
    }
}