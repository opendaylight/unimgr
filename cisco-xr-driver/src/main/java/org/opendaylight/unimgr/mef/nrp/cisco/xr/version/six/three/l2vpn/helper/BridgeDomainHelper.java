/*
 * Copyright (c) 2019 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.three.l2vpn.helper;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.BridgeDomainGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.BridgeDomainGroupsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.BridgeDomainGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.BridgeDomainGroupBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.BridgeDomainGroupKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.BridgeDomains;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.BridgeDomainsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.BdAttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.BdPseudowires;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString;

/*
 * @author arif.hussain@xoriant.com
 */
public class BridgeDomainHelper {


    private List<BridgeDomain> bridgeDomains;


    public BridgeDomainHelper() {
        bridgeDomains = new LinkedList<>();
    }

    public static BridgeDomainGroups createBridgeDomainGroups(BridgeDomainGroup bridgeDomainGroup) {
        return createBridgeDomainGroups(Collections.singletonList(bridgeDomainGroup));
    }

    private static BridgeDomainGroups createBridgeDomainGroups(
            List<BridgeDomainGroup> bridgeDomainGroups) {
        return new BridgeDomainGroupsBuilder().setBridgeDomainGroup(bridgeDomainGroups).build();
    }

    public BridgeDomainHelper appendBridgeDomain(
                                                String name,
                                                BdAttachmentCircuits bdattachmentCircuits,
                                                BdPseudowires bdpseudowires) {
        BridgeDomain bridgeDomain = new BridgeDomainBuilder()
            .setName(new CiscoIosXrString(name))
            .setBdAttachmentCircuits(bdattachmentCircuits)
            .setBdPseudowires(bdpseudowires)
            .build();

        bridgeDomains.add(bridgeDomain);

        return this;
    }

    public BridgeDomainGroup build(String name) {
        return new BridgeDomainGroupBuilder()
            .withKey(new BridgeDomainGroupKey(new CiscoIosXrString(name)))
            .setName(new CiscoIosXrString(name))
            .setBridgeDomains(buildBridgeDomains())
            .build();
    }

    private BridgeDomains buildBridgeDomains() {
        return new BridgeDomainsBuilder()
                .setBridgeDomain(bridgeDomains)
                .build();
    }
}
