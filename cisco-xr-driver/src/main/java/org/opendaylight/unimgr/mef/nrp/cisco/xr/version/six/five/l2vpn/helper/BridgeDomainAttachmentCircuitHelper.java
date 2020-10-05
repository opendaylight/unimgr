/*
 * Copyright (c) 2019 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.five.l2vpn.helper;

import java.util.LinkedList;
import java.util.List;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.InterfaceHelper;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.bridge.domain.BdAttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.bridge.domain.BdAttachmentCircuitsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.bridge.domain.bd.attachment.circuits.BdAttachmentCircuit;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.bridge.domain.BdAttachmentCircuits;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.bridge.domain.BdAttachmentCircuitsBuilder;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.bridge.domain.bd.attachment.circuits.BdAttachmentCircuit;
//import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.bridge.domain.bd.attachment.circuits.BdAttachmentCircuitBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr._6._5.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.bridge.domain.bd.attachment.circuits.BdAttachmentCircuitBuilder;

/*
 * @author arif.hussain@xoriant.com
 */
public class BridgeDomainAttachmentCircuitHelper {
    private List<BdAttachmentCircuit> bdAttachmentCircuits;


    public BridgeDomainAttachmentCircuitHelper() {
        bdAttachmentCircuits = new LinkedList<>();
    }

    public BridgeDomainAttachmentCircuitHelper addPort(ServicePort port, boolean isExclusive) {
        bdAttachmentCircuits.add(
            new BdAttachmentCircuitBuilder()
                .setName((isExclusive)
                    ? InterfaceHelper.getInterfaceName(port) : InterfaceHelper.getSubInterfaceName(port))
                .build());

        return this;
    }


    public BdAttachmentCircuits build() {
        return new BdAttachmentCircuitsBuilder().setBdAttachmentCircuit(bdAttachmentCircuits)
                .build();
    }
}
