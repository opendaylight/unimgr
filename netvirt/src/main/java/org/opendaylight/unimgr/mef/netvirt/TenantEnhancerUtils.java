/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.netvirt;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.MefService;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.MefServiceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TenantEnhancerUtils {
    private static final Logger log = LoggerFactory.getLogger(TenantEnhancerUtils.class);

    public static boolean isServiceTenanted(MefService service) {
        return service.getTenantId().equals("");
    }

    public static boolean isUniTenanted(
            org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni uni) {
        return uni.getTenantId().equals("");
    }

    public static void updateService(DataBroker dataBroker, String tenant, MefService service) {
        log.info("service is {}", service);

        MefServiceBuilder builder = new MefServiceBuilder();
        builder.setKey(service.getKey());
        builder.setTenantId(tenant);
        MdsalUtils.syncUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION,
                MefUtils.getMefServiceInstanceIdentifier(service.getSvcId()), service);
    }
}