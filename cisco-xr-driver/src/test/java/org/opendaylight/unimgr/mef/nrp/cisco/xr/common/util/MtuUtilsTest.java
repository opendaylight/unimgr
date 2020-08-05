/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations._interface.configuration.Mtus;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations._interface.configuration.mtus.Mtu;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString;
import org.opendaylight.yangtools.yang.common.Uint32;


/*
 * @author krzysztof.bijakowski@amartus.com
 */

public class MtuUtilsTest {

    @Test
    public void testGenerateMtusSingle() {
        //given
        Long mtuValue = 1522L;
        CiscoIosXrString owner = new CiscoIosXrString("testAddCeps");

        //when
        Mtus actual = MtuUtils2.generateMtus(mtuValue, owner);

        //then
        assertNotNull(actual);

        List<Mtu> actualMtuList = actual.getMtu();
        assertEquals(actualMtuList.size(), 1);

        Mtu actualMtu = actualMtuList.get(0);
        assertEquals(Uint32.valueOf(mtuValue), actualMtu.getMtu());
        assertEquals(owner, actualMtu.getOwner());
    }

    @Test
    public void testGenerateMtusMultiple() {
        //given
        List<Long> mtuValues = new LinkedList<>();
        mtuValues.add(1522L);
        mtuValues.add(3000L);

        CiscoIosXrString owner = new CiscoIosXrString("testAddCeps");

        //when
        Mtus actual = MtuUtils2.generateMtus(mtuValues, owner);

        //then
        assertNotNull(actual);

        List<Mtu> actualMtuList = actual.getMtu();
        assertEquals(actualMtuList.size(), 2);

        Mtu actualMtu = actualMtuList.get(0);
        assertEquals(Uint32.valueOf(mtuValues.get(0)), actualMtu.getMtu());
        assertEquals(owner, actualMtu.getOwner());

        actualMtu = actualMtuList.get(1);
        assertEquals(Uint32.valueOf(mtuValues.get(1)), actualMtu.getMtu());
        assertEquals(owner, actualMtu.getOwner());
    }
}
