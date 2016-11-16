/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.MefServices;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.MefService;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.MefServiceKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.Evc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.Uni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.UniKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.Unis;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.Ipvc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.Identifier45;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.RetailSvcIdType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MefServicesUtils {


    public static InstanceIdentifier<Unis> getEvcUnisInstanceIdentifier(String evcId) {
        return InstanceIdentifier.builder(MefServices.class)
                .child(MefService.class, new MefServiceKey(RetailSvcIdType.getDefaultInstance(evcId))).child(Evc.class)
                .child(Unis.class).build();
    }

    public static InstanceIdentifier<Uni> getEvcUniInstanceIdentifier(String uniId) {
        return InstanceIdentifier.builder(MefServices.class).child(MefService.class).child(Evc.class).child(Unis.class)
                .child(Uni.class, new UniKey(new Identifier45(uniId))).build();
    }
    
    public static InstanceIdentifier<Evc> getEvcsInstanceIdentifier() {
        return getMefServicesInstanceIdentifier().child(Evc.class);
    }

    public static InstanceIdentifier<MefService> getMefServicesInstanceIdentifier() {
        return InstanceIdentifier.create(MefServices.class).child(MefService.class);
    }
  
    public static InstanceIdentifier<Evc> getEvcInstanceInstanceIdentifier(String svcId) {
        return InstanceIdentifier.builder(MefServices.class)
                .child(MefService.class, new MefServiceKey(RetailSvcIdType.getDefaultInstance(svcId)))
                .child(Evc.class).build();
    }
    
    public static InstanceIdentifier<Ipvc> getIpvcsInstanceIdentifier() {
        return InstanceIdentifier.create(MefServices.class).child(MefService.class).child(Ipvc.class);
    }

}
