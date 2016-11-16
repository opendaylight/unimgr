/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.Ipvc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.Identifier45;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface ISubnetManager {
    public void assignIpUniNetworks(Identifier45 uniId, Identifier45 ipUniId, InstanceIdentifier<Ipvc> ipvcId);

    public void unAssignIpUniNetworks(Identifier45 uniId, Identifier45 ipUniId, InstanceIdentifier<Ipvc> ipvcId);
}
