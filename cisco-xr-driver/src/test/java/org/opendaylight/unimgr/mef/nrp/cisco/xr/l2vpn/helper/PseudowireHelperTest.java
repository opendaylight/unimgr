/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.Pseudowires;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.Pseudowire;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.pseudowire.Neighbor;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.pseudowire.pseudowire.content.MplsStaticLabels;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author krzysztof.bijakowski@amartus.com
 */
public class PseudowireHelperTest {

    @Test
    public void testConfigureNeighbor() {
        //given
        Long pseudowireId = 2000L;
        Ipv4AddressNoZone neighborAddress = new Ipv4AddressNoZone("1.2.3.4");

        //when
        List<Neighbor> actual = new PseudowireHelper(pseudowireId).configureNeighbor(neighborAddress).getNeighbors();

        //then
        assertNotNull(actual);
        assertEquals(1, actual.size());

        Neighbor actualNeighbor = actual.get(0);
        assertNotNull(actualNeighbor);
        assertEquals(neighborAddress, actualNeighbor.getNeighbor());
        assertEquals(new CiscoIosXrString("static"), actualNeighbor.getXmlClass());

        MplsStaticLabels actualMplsStaticLabels = actualNeighbor.getMplsStaticLabels();
        assertNotNull(actualMplsStaticLabels);
        assertEquals(pseudowireId, actualMplsStaticLabels.getLocalStaticLabel().getValue());
        assertEquals(pseudowireId, actualMplsStaticLabels.getRemoteStaticLabel().getValue());
    }

    @Test
    public void testConfigurePseudowire() {
        //given
        Long pseudowireId = 2000L;
        Ipv4AddressNoZone neighborAddress = new Ipv4AddressNoZone("1.2.3.4");

        //when
        Pseudowires actual = new PseudowireHelper(pseudowireId).configureNeighbor(neighborAddress).configurePseudowire().getPseudowires();

        //then
        assertNotNull(actual);

        List<Pseudowire> actualPseudowireList = actual.getPseudowire();
        assertNotNull(actualPseudowireList);
        assertEquals(1, actualPseudowireList.size());

        Pseudowire actualPseudowire = actualPseudowireList.get(0);
        assertNotNull(actualPseudowire.getNeighbor());
        assertEquals(1, actualPseudowire.getNeighbor().size());
        assertEquals(pseudowireId, actualPseudowire.getPseudowireId().getValue());
    }
}
