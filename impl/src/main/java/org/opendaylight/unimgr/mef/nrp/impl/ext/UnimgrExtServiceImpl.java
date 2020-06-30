/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.ext;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.unimgr.mef.nrp.common.TapiUtils;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.ServiceInterfacePoint1;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.ServiceInterfacePoint1Builder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.sip.attrs.NrpCarrierEthEnniNResourceBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.sip.attrs.NrpCarrierEthInniNResourceBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.sip.attrs.NrpCarrierEthUniNResourceBuilder;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.AddSipInput;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.AddSipOutput;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.UnimgrExtService;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.add.sip.input.SipType;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.add.sip.input.sip.type.EnniSpec;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.add.sip.input.sip.type.InniSpec;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.add.sip.input.sip.type.UniSpec;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.tapi.context.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.edge.point.MappedServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.NodeKey;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Implementation of unimgr specific rpc calls.
 * @author bartosz.michalik@amartus.com
 */
public class UnimgrExtServiceImpl implements UnimgrExtService {

    private ListeningExecutorService executor = MoreExecutors.listeningDecorator(
        new ThreadPoolExecutor(1, 4,
            10, TimeUnit.MINUTES,
            new LinkedBlockingQueue<>()));

    private final DataBroker broker;

    public UnimgrExtServiceImpl(DataBroker broker) {
        this.broker = broker;
    }

    @Override
    public ListenableFuture<RpcResult<AddSipOutput>> addSip(AddSipInput input) {
        final Uuid nepId = input.getNepId();
        final Uuid nodeId = input.getNodeId();
        Objects.requireNonNull(nepId);
        Objects.requireNonNull(nodeId);
        final SipType sipType = input.getSipType();

        return executor.submit(() -> {
            ReadWriteTransaction tx = broker.newReadWriteTransaction();
            Optional<OwnedNodeEdgePoint> nep = tx
                    .read(LogicalDatastoreType.OPERATIONAL, NrpDao.topo(TapiConstants.PRESTO_SYSTEM_TOPO)
                    .child(Node.class, new NodeKey(nodeId))
                    .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepId))
            ).get();
            if (!nep.isPresent()) {
                return withError("NEP with id {0} for node {1} not found", nepId, nodeId);
            }

            Uuid sipId = new Uuid("sip:" + nepId.getValue());

            List<MappedServiceInterfacePoint> sips = nep.get().getMappedServiceInterfacePoint();
            if (sips != null && !sips.isEmpty()) {
                return withError("sip for NEP with id {0} for node {1} already defined", nepId, nodeId);
            }

            NrpDao nrpDao = new NrpDao(tx);

            ServiceInterfacePoint1 sip1 = getServiceInterfacePoint(sipType);
            ServiceInterfacePointBuilder sipBuilder = new ServiceInterfacePointBuilder()
                    .setUuid(sipId)
                    .addAugmentation(ServiceInterfacePoint1.class, sip1)
                    .setLayerProtocolName(Collections.singletonList(LayerProtocolName.ETH));

            nrpDao.addSip(
                sipBuilder
                    .build());
            nrpDao.updateNep(nodeId, new OwnedNodeEdgePointBuilder(nep.get())
                    .setMappedServiceInterfacePoint(
                            Collections.singletonList(TapiUtils.toSipRef(sipId, MappedServiceInterfacePoint.class)))
                    .build()

            );
            tx.commit().get();

            return RpcResultBuilder.<AddSipOutput>success().build();
        });
    }

    private ServiceInterfacePoint1 getServiceInterfacePoint(SipType sipType) {

        ServiceInterfacePoint1 sip = null;

        if (sipType instanceof InniSpec) {
            org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531
                    .add.sip.input.sip.type.inni.spec.InniSpec spec = ((InniSpec) sipType).getInniSpec();
            if (spec != null) {
                sip = new ServiceInterfacePoint1Builder()
                    .setNrpCarrierEthInniNResource(new NrpCarrierEthInniNResourceBuilder(spec).build()).build();
            }

        } else if (sipType instanceof EnniSpec) {
            org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531
                    .add.sip.input.sip.type.enni.spec.EnniSpec spec = ((EnniSpec) sipType).getEnniSpec();
            if (spec != null) {
                sip = new ServiceInterfacePoint1Builder()
                    .setNrpCarrierEthEnniNResource(new NrpCarrierEthEnniNResourceBuilder(spec).build()).build();
            }

        } else if (sipType instanceof UniSpec) {
            org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531
                    .add.sip.input.sip.type.uni.spec.UniSpec spec = ((UniSpec) sipType).getUniSpec();
            if (spec != null) {
                sip = new ServiceInterfacePoint1Builder()
                    .setNrpCarrierEthUniNResource(new NrpCarrierEthUniNResourceBuilder(spec).build()).build();
            }
        }

        return sip;
    }

    /*private static RpcResult<Void> success() {
        return RpcResultBuilder.<Void>success().build();
    }*/

    private static RpcResult<AddSipOutput> withError(String error, Object ... params) {
        RpcResultBuilder<AddSipOutput> failed = RpcResultBuilder.failed();
        if (error != null) {
            if (params.length > 0) {
                error = MessageFormat.format(error, params);
            }
            failed.withError(RpcError.ErrorType.APPLICATION, error);
        }

        return failed.build();
    }


}
