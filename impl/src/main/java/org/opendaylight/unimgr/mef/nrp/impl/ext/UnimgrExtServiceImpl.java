/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.ext;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp_interface.rev170531.LayerProtocol1;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp_interface.rev170531.LayerProtocol1Builder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp_interface.rev170531.nrp.layer.protocol.attrs.g.NrpCgEthEnniSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp_interface.rev170531.nrp.layer.protocol.attrs.g.NrpCgEthInniSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp_interface.rev170531.nrp.layer.protocol.attrs.g.NrpCgEthUniSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170531.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170531.UniversalId;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170531.context.g.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170531.service._interface.point.g.LayerProtocol;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170531.service._interface.point.g.LayerProtocolBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170531.node.g.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170531.node.g.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170531.node.g.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170531.topology.g.Node;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170531.topology.g.NodeKey;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev700101.AddSipInput;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev700101.UnimgrExtService;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev700101.add.sip.input.SipType;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev700101.add.sip.input.sip.type.EnniSpec;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev700101.add.sip.input.sip.type.InniSpec;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev700101.add.sip.input.sip.type.UniSpec;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import com.google.common.base.Optional;

/**
 * @author bartosz.michalik@amartus.com
 */
public class UnimgrExtServiceImpl implements UnimgrExtService {

    private ExecutorService executor = new ThreadPoolExecutor(1, 4,
            10, TimeUnit.MINUTES,
            new LinkedBlockingQueue<>());

    private final DataBroker broker;

    public UnimgrExtServiceImpl(DataBroker broker) {
        this.broker = broker;
    }

    @Override
    public Future<RpcResult<Void>> addSip(AddSipInput input) {
        final UniversalId nepId = input.getNepId();
        final UniversalId nodeId = input.getNodeId();
        Objects.requireNonNull(nepId);
        Objects.requireNonNull(nodeId);
        final SipType sipType = input.getSipType();

        return executor.submit(() -> {
            ReadWriteTransaction tx = broker.newReadWriteTransaction();
            Optional<OwnedNodeEdgePoint> nep = tx.read(LogicalDatastoreType.OPERATIONAL, NrpDao.topo(TapiConstants.PRESTO_SYSTEM_TOPO)
                    .child(Node.class, new NodeKey(nodeId))
                    .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepId))
            ).checkedGet();
            if(!nep.isPresent()) return withError("NEP with id {0} for node {1} not found", nepId, nodeId);

            UniversalId sipId = new UniversalId("sip:" + nepId.getValue());

            List<UniversalId> sips = nep.get().getMappedServiceInterfacePoint();
            if(sips != null && !sips.isEmpty()) return withError("sip for NEP with id {0} for node {1} already defined", nepId, nodeId);

            NrpDao nrpDao = new NrpDao(tx);


            ServiceInterfacePointBuilder sipBuilder = new ServiceInterfacePointBuilder()
                    .setUuid(sipId)
                    .setLayerProtocol(Collections.singletonList(getLayerProtocol(sipType)));


            nrpDao.addSip(
                sipBuilder
                    .build());
            nrpDao.updateNep(nodeId, new OwnedNodeEdgePointBuilder(nep.get())
                    .setMappedServiceInterfacePoint(Collections.singletonList(sipId))
                    .build()

            );
            tx.submit().checkedGet();


            return success();
        });
    }

    private LayerProtocol getLayerProtocol(SipType sipType) {
        LayerProtocolBuilder lpBuilder = new LayerProtocolBuilder()
                .setLocalId("eth")
                //TODO add support for direction
                .setLayerProtocolName(LayerProtocolName.Eth);


        LayerProtocol1 lp = null;

        if(sipType instanceof InniSpec) {
            org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev700101.add.sip.input.sip.type.inni.spec.InniSpec spec = ((InniSpec) sipType).getInniSpec();
            if(spec != null) lp = new LayerProtocol1Builder()
                    .setNrpCgEthInniSpec(new NrpCgEthInniSpecBuilder(spec).build()).build();

        } else if (sipType instanceof EnniSpec) {
            org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev700101.add.sip.input.sip.type.enni.spec.EnniSpec spec = ((EnniSpec) sipType).getEnniSpec();
            if(spec != null) lp = new LayerProtocol1Builder()
                    .setNrpCgEthEnniSpec(new NrpCgEthEnniSpecBuilder(spec).build()).build();

        } else if(sipType instanceof UniSpec) {
            org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev700101.add.sip.input.sip.type.uni.spec.UniSpec spec = ((UniSpec) sipType).getUniSpec();
            if(spec != null) lp = new LayerProtocol1Builder()
                    .setNrpCgEthUniSpec(new NrpCgEthUniSpecBuilder(spec).build()).build();
        }

        lpBuilder.addAugmentation(LayerProtocol1.class, lp);
        return lpBuilder.build();
    }

    private static RpcResult<Void> success() {
        return RpcResultBuilder.<Void>success().build();
    }

    private static RpcResult<Void> withError(String error, Object ... params) {
        RpcResultBuilder<Void> failed = RpcResultBuilder.<Void>failed();
        if(error != null) {
            if(params.length > 0) {
               error = MessageFormat.format(error, params);
            }
            failed.withError(RpcError.ErrorType.APPLICATION, error);
        }

        return failed.build();
    }


}
