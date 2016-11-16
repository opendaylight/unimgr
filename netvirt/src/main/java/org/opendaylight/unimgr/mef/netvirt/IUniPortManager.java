/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import java.util.List;

import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.Identifier45;

public interface IUniPortManager {

	void updateOperUni(String uniId);
	
	void removeUniPorts(String uniId);
	
	void addCeVlan(String uniId, Long vlanId);
	
	void removeCeVlan(String uniId, Long vlanId);

        List<String> getUniVlanInterfaces(String uniId);
        
        String getUniVlanInterface(String uniId, Long vlanId);

}
