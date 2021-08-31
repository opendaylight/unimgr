/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper;

/*
 * Cisco IOS XR 6.4.1, rev170626
 * Cisco IOS XR 6.2.1, rev151109
 */
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.L2vpnBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.Database;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.DatabaseBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.BridgeDomainGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.XconnectGroups;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/*
 * Helper, supports configuration of L2VPN
 *
 * @author krzysztof.bijakowski@amartus.com
 */
public final class L2vpnHelper {

    private L2vpnHelper() {
    }

    public static InstanceIdentifier<L2vpn> getL2vpnId() {
        return InstanceIdentifier.builder(L2vpn.class).build();
    }

    public static L2vpn build(XconnectGroups xconnectGroups) {
        Database database = new DatabaseBuilder()
            .setXconnectGroups(xconnectGroups)
            .build();

        return new L2vpnBuilder()
            .setDatabase(database)
            .build();
    }

    public static L2vpn build(BridgeDomainGroups bridgeDomainGroups) {
        Database database = new DatabaseBuilder()
            .setBridgeDomainGroups(bridgeDomainGroups)
            .build();

        return new L2vpnBuilder()
            .setDatabase(database)
            .build();
    }
}
