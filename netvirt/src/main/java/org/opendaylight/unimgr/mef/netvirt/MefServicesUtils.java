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
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.MefServices;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.MefService;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.MefServiceKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.Evc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.EvcBuilder;
//import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni;
//import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.PhysicalLayers;
//import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.Links;
//import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.Link;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.Uni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.UniKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.Unis;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.Ipvc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.EvcIdType;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.EvcType;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.Identifier45;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.RetailSvcIdType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class MefServicesUtils {

    private static final Logger logger = LoggerFactory.getLogger(MefServicesUtils.class);

    public static InstanceIdentifier<Unis> getEvcUnisInstanceIdentifier(String evcId) {
        return InstanceIdentifier.builder(MefServices.class)
                .child(MefService.class, new MefServiceKey(RetailSvcIdType.getDefaultInstance(evcId))).child(Evc.class)
                .child(Unis.class).build();
    }

    public static InstanceIdentifier<Uni> getEvcUniInstanceIdentifier(String uniId) {
        return InstanceIdentifier.builder(MefServices.class).child(MefService.class).child(Evc.class).child(Unis.class)
                .child(Uni.class, new UniKey(new Identifier45(uniId))).build();
    }

    public static InstanceIdentifier<Evc> getEvcInstanceIdentifier() {
        return getMefServiceInstanceIdentifier().child(Evc.class);
    }

    public static InstanceIdentifier<MefService> getMefServiceInstanceIdentifier() {
        return InstanceIdentifier.create(MefServices.class).child(MefService.class);
    }

    public static InstanceIdentifier<MefService> getMefServiceInstanceIdentifier(RetailSvcIdType retailSvcIdType) {
        return InstanceIdentifier.create(MefServices.class).child(MefService.class, new MefServiceKey(retailSvcIdType));
    }

    public static InstanceIdentifier<MefServices> getMefServicesInstanceIdentifier() {
        return InstanceIdentifier.create(MefServices.class);
    }

    private static InstanceIdentifier<Evc> getEvcInstanceInstanceIdentifier(String instanceName) {
        return InstanceIdentifier.builder(MefServices.class)
                .child(MefService.class, new MefServiceKey(RetailSvcIdType.getDefaultInstance(instanceName)))
                .child(Evc.class).build();
    }

    public static void createEvcInstance(DataBroker dataBroker, String instanceName) {
        Evc einst = createEvcInstance(instanceName);

        MdsalUtils.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION,
                getEvcInstanceInstanceIdentifier(instanceName), einst);
    }

    private static InstanceIdentifier<Uni> getEvcUniInstanceIdentifier(String serviceName, String uniId) {
        return InstanceIdentifier.builder(MefServices.class)
                .child(MefService.class, new MefServiceKey(RetailSvcIdType.getDefaultInstance(serviceName)))
                .child(Evc.class).child(Unis.class).child(Uni.class, //
                        new UniKey(Identifier45.getDefaultInstance(uniId)))
                .build();
    }

    private static Evc createEvcInstance(String instanceName) {
        EvcBuilder evcBuilder = new EvcBuilder();
        evcBuilder.setAdminStateEnabled(false);
        evcBuilder.setEvcId(EvcIdType.getDefaultInstance(instanceName));
        evcBuilder.setEvcType(EvcType.MultipointToMultipoint);
        return evcBuilder.build();
    }

    public static Boolean isEvcExists(DataBroker dataBroker, String instanceName) {
        Optional<Evc> evc = getEvc(dataBroker, instanceName);
        return evc.isPresent();
    }

    public static Boolean isEvcAdminStateEnabled(DataBroker dataBroker, String instanceName) {
        Optional<Evc> evc = getEvc(dataBroker, instanceName);
        return evc.isPresent() && evc.get().isAdminStateEnabled();

    }

    public static Optional<Evc> getEvc(DataBroker dataBroker, String instanceName) {
        Optional<Evc> evc = MdsalUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                getEvcInstanceInstanceIdentifier(instanceName));
        return evc;
    }

    public static Boolean isEvcUniExists(DataBroker dataBroker, String instanceName, String uniId) {
        logger.info("searching for uni id {} in service {}", uniId, instanceName);
        Optional<Uni> uni = MdsalUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                getEvcUniInstanceIdentifier(instanceName, uniId));
        if (uni.isPresent()) {
            logger.info("found uni");
        } else {
            logger.info("no uni");
        }
        return uni.isPresent();
    }

    public static InstanceIdentifier<Ipvc> getIpvcInstanceIdentifier() {
        return InstanceIdentifier.create(MefServices.class).child(MefService.class).child(Ipvc.class);
    }

}
