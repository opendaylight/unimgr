/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper;


import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.InterfaceHelper;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.AttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.AttachmentCircuitsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.attachment.circuits.AttachmentCircuit;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.attachment.circuits.AttachmentCircuitBuilder;
import org.opendaylight.yangtools.yang.common.Empty;

import java.util.LinkedList;
import java.util.List;

/**
 * Helper, supports configuration of AttachmentCircuits
 *
 * @author krzysztof.bijakowski@amartus.com
 */
public class AttachmentCircuitHelper {

    private List<AttachmentCircuit> attachmentCircuits;

    public AttachmentCircuitHelper() {
        attachmentCircuits = new LinkedList<>();
    }

    public AttachmentCircuitHelper addPort(ServicePort port, boolean isExclusive) {
        attachmentCircuits.add(
            new AttachmentCircuitBuilder()
                .setName((isExclusive) ? InterfaceHelper.getInterfaceName(port) : InterfaceHelper.getSubInterfaceName(port))
                .setEnable(Empty.getInstance())
                .build()
        );

        return this;
    }

    public AttachmentCircuits build() {
        return new AttachmentCircuitsBuilder()
            .setAttachmentCircuit(attachmentCircuits)
            .build();
    }
}
