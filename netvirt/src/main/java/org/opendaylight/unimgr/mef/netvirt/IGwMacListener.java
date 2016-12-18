/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;

public interface IGwMacListener {
    public void resolveGwMac (String vpnName, String portName, IpAddress srcIpAddress, IpAddress dstIpAddress, String subnet);
    public void unResolveGwMac(String vpnName, String portName, IpAddress srcIpAddress, IpAddress dstIpAddress, String subnet);
}
