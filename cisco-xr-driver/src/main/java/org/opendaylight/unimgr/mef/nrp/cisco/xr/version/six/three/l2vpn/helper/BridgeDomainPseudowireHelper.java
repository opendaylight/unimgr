/*
 * Copyright (c) 2019 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.three.l2vpn.helper;

import java.util.LinkedList;
import java.util.List;

import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.PseudowireGenerator;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.PseudowireIdRange;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.BdPseudowires;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.BdPseudowiresBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.bd.pseudowires.BdPseudowire;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.bd.pseudowires.BdPseudowireBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;

/*
 * @author arif.hussain@xoriant.com
 */
public class BridgeDomainPseudowireHelper {

    private List<BdPseudowire> bdpseudowires;

    public BridgeDomainPseudowireHelper() {
        bdpseudowires = new LinkedList<>();
    }

    public BridgeDomainPseudowireHelper addBdPseudowire(Ipv4AddressNoZone neighbor) {
        PseudowireIdRange pwId = new PseudowireIdRange(PseudowireGenerator.getPseudowireId());

        BdPseudowire bdpseudowire =
                new BdPseudowireBuilder().setNeighbor(neighbor).setPseudowireId(pwId).build();
        bdpseudowires.add(bdpseudowire);

        return this;
    }

    public BdPseudowires build() {
        return new BdPseudowiresBuilder().setBdPseudowire(bdpseudowires).build();
    }
}
