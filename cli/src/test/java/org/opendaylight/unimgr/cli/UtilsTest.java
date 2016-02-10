/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.cli;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.Speed;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed100M;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed100MBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10G;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10GBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10M;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10MBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed1G;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed1GBuilder;

public class UtilsTest {

    @Test
    public void testGetSpeed() {
        // Test 10M
        final Speed10M speed10M = new Speed10MBuilder().setSpeed10M(true).build();
        Speed getSpeed = Utils.getSpeed("10M");
        assertEquals(speed10M, getSpeed);

        // Test 100M
        final Speed100M speedObject100M = new Speed100MBuilder().setSpeed100M(true).build();
        getSpeed = Utils.getSpeed("100M");
        assertEquals(speedObject100M, getSpeed);

        // Test 1G
        final Speed1G speedObject1G = new Speed1GBuilder().setSpeed1G(true).build();
        getSpeed = Utils.getSpeed("1G");
        assertEquals(speedObject1G, getSpeed);

        // Test 10G
        final Speed10G speedObject10G = new Speed10GBuilder().setSpeed10G(true).build();
        getSpeed = Utils.getSpeed("10G");
        assertEquals(speedObject10G, getSpeed);

        // Test other
        getSpeed = Utils.getSpeed("1");
        assertEquals(null, getSpeed);
    }

}
