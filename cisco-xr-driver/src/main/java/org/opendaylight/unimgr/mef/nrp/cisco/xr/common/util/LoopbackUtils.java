/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util;

import com.google.common.collect.ImmutableMap;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Tools designated to support operations on loopback interfaces data
 *
 * @author krzysztof.bijakowski@amartus.com
 */
public class LoopbackUtils {
    private static final Logger LOG = LoggerFactory.getLogger(LoopbackUtils.class);

    private static final String DEFAULT_LOOPBACK = "127.0.0.1";

    private static final Map<String, String> loopbackMap = ImmutableMap.of(
            "asr-101", "192.168.0.1",
            "asr-102", "192.168.0.2",
            "asr-103", "192.168.0.3"
    );

    // TODO: implement real method to find neighbor's loopback
    public static Ipv4AddressNoZone getIpv4Address(FcPort port) {
        String hostname = port.getNode().getValue();

        String loopback = loopbackMap.get(hostname);
        if (loopback == null) {
            LOG.warn("No loopback address found for {}", hostname);
            loopback = DEFAULT_LOOPBACK;
        }

        return new Ipv4AddressNoZone(loopback);
    }
}
