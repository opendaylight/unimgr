/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.cli;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.Speed;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed100MBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10GBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10MBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed1GBuilder;

public final class Utils {

    private Utils() {

    }

    /**
     * Convert string to Speed.
     * @param speed string representation of speed
     * @return schema defined speed object
     */
    public static final Speed getSpeed(final String speed) {
        Speed speedObject = null;
        if (speed.equals("10M")) {
            speedObject = new Speed10MBuilder().setSpeed10M(true)
                                               .build();
        } else if (speed.equals("100M")) {
            speedObject = new Speed100MBuilder().setSpeed100M(true)
                                                .build();
        } else if (speed.equals("1G")) {
            speedObject = new Speed1GBuilder().setSpeed1G(true)
                                              .build();
        } else if (speed.equals("10G")) {
            speedObject = new Speed10GBuilder().setSpeed10G(true)
                                               .build();
        }
        return speedObject;
    }

}
