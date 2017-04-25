/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.template.tapi;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.unimgr.mef.nrp.template.TemplateConstants;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm_connectivity.rev170227.NaturalNumber;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp_interface.rev170227.LayerProtocol1;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp_interface.rev170227.LayerProtocol1Builder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp_interface.rev170227.nrp.layer.protocol.attrs.NrpCgEthUniSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.*;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.context.attrs.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.context.attrs.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.service._interface.point.LayerProtocol;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.service._interface.point.LayerProtocolBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170227.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170227.node.OwnedNodeEdgePointBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author bartosz.michalik@amartus.com
 */
public class TopologyDataHandler {
    private static final Logger log = LoggerFactory.getLogger(TopologyDataHandler.class);
    private DataBroker dataBroker;

    public TopologyDataHandler(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void init() {
        Objects.requireNonNull(dataBroker);
        log.info("Starting topology handler");
        // this is a static and simplistic topology push to the TAPI system topology

        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();

        try {

            // we have prepared an dao abstraction to make it easier to use some of the common interactions with
            // MD-SAL but you can use tx.put tx.merge etc. by yourself if you prefere to
            NrpDao nrpDao = new NrpDao(tx);
            //we are creating a list of NodeEdgePoints for the node no sips are added to the system
            List<OwnedNodeEdgePoint> someEndpoints = createSomeEndpoints(1, 2, 5, 7);
            nrpDao.createSystemNode(TemplateConstants.DRIVER_ID, someEndpoints);
            //add sip for one of these endpoints

            //create sid and add it to model
            ServiceInterfacePoint someSid = createSomeSid("some-sid-id");
            nrpDao.addSip(someSid);

            //update an existing nep wit mapping to sip
            OwnedNodeEdgePoint updatedNep = new OwnedNodeEdgePointBuilder(someEndpoints.get(1))
                    .setMappedServiceInterfacePoint(Collections.singletonList(someSid.getUuid()))
                    .build();

            nrpDao.updateNep(TemplateConstants.DRIVER_ID, updatedNep);


            tx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            log.error("Adding node to system topology has failed", e);
        }

    }

    private ServiceInterfacePoint createSomeSid(String idx) {
        return new ServiceInterfacePointBuilder()
                .setUuid(new UniversalId("sip" + ":" + TemplateConstants.DRIVER_ID + ":" + idx))
                .setLayerProtocol(layerProtocolWithUniAttributes())
                .build();
    }

    private List<LayerProtocol> layerProtocolWithUniAttributes() {

        LayerProtocol layerProtocol = new LayerProtocolBuilder()
                .setLocalId("eth")
                .setLayerProtocolName(LayerProtocolName.Eth)
                .addAugmentation(LayerProtocol1.class, new LayerProtocol1Builder()
                        .setNrpCgEthUniSpec(
                                new NrpCgEthUniSpecBuilder()
                                        .setMaxFrameSize(new NaturalNumber(new BigInteger("1703")))
                                        .build()
                        ).build()
                ).build();

        return Collections.singletonList(layerProtocol);
    }

    private List<OwnedNodeEdgePoint> createSomeEndpoints(int... indexes) {

        return Arrays.stream(indexes).mapToObj(idx -> new OwnedNodeEdgePointBuilder()
                .setUuid(new UniversalId(TemplateConstants.DRIVER_ID + ":nep" + idx))
                .setLayerProtocol(Collections.singletonList(
                        new org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170227.node.edge.point.LayerProtocolBuilder()
                            .setLocalId("eth")
                            .setLayerProtocolName(LayerProtocolName.Eth)
                            .build()))
                .setLinkPortDirection(PortDirection.Bidirectional)
                .setLinkPortRole(PortRole.Symmetric)
                .setTerminationDirection(TerminationDirection.Bidirectional)
                .build()).collect(Collectors.toList());
    }

    public void close() {
        log.info("Closing topology handler");
    }

}
