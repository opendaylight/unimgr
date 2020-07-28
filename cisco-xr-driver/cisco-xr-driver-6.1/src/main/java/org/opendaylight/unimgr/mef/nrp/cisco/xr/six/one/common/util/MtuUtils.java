/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.six.one.common.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations._interface.configuration.Mtus;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations._interface.configuration.MtusBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations._interface.configuration.mtus.Mtu;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations._interface.configuration.mtus.MtuBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString;
import org.opendaylight.yangtools.yang.common.Uint32;


/*
 * Tools designated to support MTU-related configuration objects processing
 *
 * @author krzysztof.bijakowski@amartus.com
 */
public final class MtuUtils {

    private MtuUtils() {
    }

    public static Mtus generateMtus(List<Long> mtuValues, CiscoIosXrString owner) {
        List<Mtu> mtus = new LinkedList<>();

        for (Long mtuValue : mtuValues) {
            mtus.add(generateMtu(mtuValue, owner));
        }

        return new MtusBuilder()
            .setMtu(mtus)
            .build();
    }

    public static Mtus generateMtus(long mtuValue, CiscoIosXrString owner) {
        List<Long> mtuValues = new ArrayList<>();
        mtuValues.add(mtuValue);

        return generateMtus(mtuValues, owner);
    }

    private static Mtu generateMtu(long mtuValue, CiscoIosXrString owner) {
        return new MtuBuilder()
            .setMtu(Uint32.valueOf(mtuValue))
            .setOwner(owner)
            .build();
    }
}
