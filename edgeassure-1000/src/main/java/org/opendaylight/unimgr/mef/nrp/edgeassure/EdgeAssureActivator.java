/*
 * Copyright (c) 2016 Microsemi and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.edgeassure;

import java.util.LinkedList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.common.MountPointHelper;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivator;
import org.opendaylight.unimgr.mef.nrp.common.ResourceNotAvailableException;
import org.opendaylight.unimgr.utils.SipHandler;
import org.opendaylight.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.types.rev160229.Identifier45;
import org.opendaylight.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev160317.MefServices;
import org.opendaylight.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev160317.mef.services.Uni;
import org.opendaylight.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev160317.mef.services.uni.Evc;
import org.opendaylight.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev160317.mef.services.uni.EvcBuilder;
import org.opendaylight.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev160317.mef.services.uni.EvcKey;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170531.UniversalId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class EdgeAssureActivator implements ResourceActivator {

    private static final Logger log = LoggerFactory.getLogger(EdgeAssureActivator.class);
    private MountPointService mountService;
    DataBroker baseDataBroker;

    EdgeAssureActivator(DataBroker dataBroker, MountPointService mountService) {
        this.mountService = mountService;
        baseDataBroker = dataBroker;
    }

    @Override
    public void activate(List<EndPoint> endPoints, String serviceName) throws ResourceNotAvailableException, TransactionCommitFailedException {
        log.info("Activation called on EdgeAssureActivator");
        UniversalId sip = endPoints.get(0).getEndpoint().getServiceInterfacePoint();
        String nodeName = SipHandler.getDeviceName(sip);
        long evcId = 1;

        EvcBuilder evcBuilder = new EvcBuilder();
        evcBuilder.setEvcIndex(evcId).setName(new Identifier45("evc" + String.valueOf(evcId)));
        List<Evc> evcConfigs = new LinkedList<>();
        evcConfigs.add(evcBuilder.build());

        InstanceIdentifier<Evc> evcConfigId = InstanceIdentifier.builder(MefServices.class).child(Uni.class)
                .child(Evc.class, new EvcKey(evcId)).build();

        Optional<DataBroker> optional = MountPointHelper.getDataBroker(mountService, nodeName);
        if (optional.isPresent()) {
            DataBroker netconfDataBroker = optional.get();
            WriteTransaction w = netconfDataBroker.newWriteOnlyTransaction();
            w.merge(LogicalDatastoreType.CONFIGURATION, evcConfigId, evcBuilder.build());
        } else {
            log.error("");
        }
    }

    @Override
    public void deactivate(List<EndPoint> endPoints, String serviceName) throws TransactionCommitFailedException, ResourceNotAvailableException {
        log.info("Deactivation called on EdgeAssureActivator. Not yet implemented.");
    }
}