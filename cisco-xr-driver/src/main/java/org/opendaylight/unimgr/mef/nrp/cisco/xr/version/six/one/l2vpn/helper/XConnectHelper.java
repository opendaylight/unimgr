/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.one.l2vpn.helper;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.XconnectGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.XconnectGroupsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.XconnectGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.XconnectGroupBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.XconnectGroupKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.P2pXconnects;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.P2pXconnectsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnect;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnectBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.AttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.Pseudowires;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString;


/*
 * Helper, supports configuration of cross-connect connection
 *
 * @author krzysztof.bijakowski@amartus.com
 */
public class XConnectHelper {

    private List<P2pXconnect> p2pXConnects;

    public static XconnectGroups createXConnectGroups(XconnectGroup xconnectGroup) {
        return createXConnectGroups(Collections.singletonList(xconnectGroup));
    }

    public static XconnectGroups createXConnectGroups(List<XconnectGroup> xconnectGroups) {
        return new XconnectGroupsBuilder()
            .setXconnectGroup(xconnectGroups)
            .build();
    }

    public XConnectHelper() {
        p2pXConnects = new LinkedList<>();
    }

    public XConnectHelper appendXConnect(String name, AttachmentCircuits attachmentCircuits, Pseudowires pseudowires) {
        P2pXconnect p2pXconnect = new P2pXconnectBuilder()
            .setName(new CiscoIosXrString(name))
            .setAttachmentCircuits(attachmentCircuits)
            .setPseudowires(pseudowires)
            .build();

        p2pXConnects.add(p2pXconnect);

        return this;
    }

    public XconnectGroup build(String name) {
        return new XconnectGroupBuilder()
            .withKey(new XconnectGroupKey(new CiscoIosXrString(name)))
            .setName(new CiscoIosXrString(name))
            .setP2pXconnects(buildP2pXconnects())
            .build();
    }

    private P2pXconnects buildP2pXconnects() {
        return new P2pXconnectsBuilder()
                .setP2pXconnect(p2pXConnects)
                .build();
    }
    public void deactivateXconnects() {
    	
    	
    }
}
