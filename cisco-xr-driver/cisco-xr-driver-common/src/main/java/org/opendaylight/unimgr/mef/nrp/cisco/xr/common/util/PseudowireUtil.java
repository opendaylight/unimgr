/*
 * Copyright (c) 2020 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util;

import java.util.concurrent.atomic.AtomicLong;

public class PseudowireUtil {
    private static long pseudowireId;

    public static class IdGenerator {
        private static final AtomicLong IDGENERATOR = new AtomicLong(2000L);

        public static long generate() {

            // TODO implement real pseudowire-id generator
            return IDGENERATOR.getAndIncrement();
        }
    }

    public static long generatePseudowireId() {
        pseudowireId = IdGenerator.generate();
        return pseudowireId;
    }

}
