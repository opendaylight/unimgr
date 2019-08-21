/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.PseudowireIdRange;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.Pseudowires;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.PseudowiresBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.Pseudowire;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.PseudowireBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.pseudowire.Neighbor;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.pseudowire.NeighborBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;

/**
 * Helper, supports configuration of VPLS neighbors and pseudowires
 *
 * @author krzysztof.bijakowski@amartus.com
 */
public class PseudowireHelper {

    private static long pseudowireId;

    private List<Pseudowire> pseudowires;

    public static class IdGenerator {
        private static final AtomicLong idGenerator = new AtomicLong(2000L);

        public static long generate() {

            // TODO implement real pseudowire-id generator
            return idGenerator.getAndIncrement();
        }
    }

    public PseudowireHelper() {
        pseudowires = new LinkedList<>();
    }

    public static long generatePseudowireId() {
        pseudowireId = IdGenerator.generate();
        return pseudowireId;
    }

    public PseudowireHelper addPseudowire(Ipv4AddressNoZone neighbor) {
        PseudowireIdRange pwId = new PseudowireIdRange(pseudowireId);


        Pseudowire pseudowire = new PseudowireBuilder().setNeighbor(createNeighbor(neighbor))
                .setPseudowireId(pwId).build();

        pseudowires.add(pseudowire);

        return this;
    }

    public Pseudowires build() {
        return new PseudowiresBuilder().setPseudowire(pseudowires).build();
    }

    private List<Neighbor> createNeighbor(Ipv4AddressNoZone address) {

        Neighbor neighbor = new NeighborBuilder().setNeighbor(address)
                // .setMplsStaticLabels(mplsStaticLabels)
                // .setXmlClass(new CiscoIosXrString("static"))
                .build();

        return Collections.singletonList(neighbor);
    }

    public static long getPseudowireId() {
        return pseudowireId;
    }
}
