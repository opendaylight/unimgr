/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.connectivityservice;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Predicate;

import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.api.FailureResult;
import org.opendaylight.unimgr.mef.nrp.api.RequestValidator;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.unimgr.mef.nrp.impl.ActivationTransaction;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint7;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.NodeAdiAugmentation;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.update.connectivity.service.output.ServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.OwnedNodeEdgePointRef;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.link.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.context.Topology;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateConnectivityAction implements Callable<RpcResult<UpdateConnectivityServiceOutput>> {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateConnectivityServiceOutput.class);

    private TapiConnectivityServiceImpl service;
    private final UpdateConnectivityServiceInput input;
    private EndPoint endpoint;
    private NrpDao nrpDao;

    UpdateConnectivityAction(TapiConnectivityServiceImpl tapiConnectivityService,
                             UpdateConnectivityServiceInput input) {

        Objects.requireNonNull(tapiConnectivityService);
        Objects.requireNonNull(input);
        this.service = tapiConnectivityService;
        this.input = input;
    }

    @Override
    @SuppressWarnings("checkstyle:illegalcatch")
    public RpcResult<UpdateConnectivityServiceOutput> call() {

        LOG.debug("running UpdateConnectivityService task");

        nrpDao = new NrpDao(service.getBroker().newReadWriteTransaction());
        try {
            // TODO validate input
            RequestValidator.ValidationResult validationResult = service.getValidator().checkValid(input);

            if (validationResult.invalid()) {
                RpcResultBuilder<UpdateConnectivityServiceOutput> res = RpcResultBuilder.failed();
                validationResult.getProblems().forEach(
                    p -> res.withError(RpcError.ErrorType.APPLICATION, p));
                return res.build();
            }

            endpoint = new EndPoint(input.getEndPoint(), input.getEndPoint().augmentation(EndPoint7.class));

            String serviceId = input.getServiceIdOrName();

            ActivationTransaction tx = prepareTransaction(serviceId);
            if (tx != null) {
                ActivationTransaction.Result txResult = tx.update();
                if (txResult.isSuccessful()) {
                    LOG.info("ConnectivityService construct updated successfully, request = {} ", input);

                    //XXX we might be also supporting CS constraints update
                    ConnectivityService cs = nrpDao.updateCsEndPoint(serviceId, input.getEndPoint());

                    UpdateConnectivityServiceOutput result = new UpdateConnectivityServiceOutputBuilder()
                            .setService(new ServiceBuilder(cs).build()).build();
                    return RpcResultBuilder.success(result).build();
                } else {
                    LOG.warn("UpdateConnectivityService failed, reason = {}, request = {}", txResult.getMessage(),
                            input);
                }
            }
            throw new IllegalStateException("no transaction created for update connectivity request");

        } catch (Exception e) {
            LOG.warn("Exception in update connectivity service", e);
            return RpcResultBuilder.<UpdateConnectivityServiceOutput>failed()
                    .withError(ErrorType.APPLICATION, e.getMessage()).build();
        }

    }

    private ActivationTransaction prepareTransaction(String serviceId) throws FailureResult {
        ActivationTransaction tx = new ActivationTransaction();

        Optional<? extends OwnedNodeEdgePointRef> nepRef = getNep();
        nepRef.ifPresent(ownedNodeEdgePointRef -> {
            try {
                Node node = nrpDao.getNode(ownedNodeEdgePointRef.getNodeId());
                NodeAdiAugmentation aug = node.augmentation(NodeAdiAugmentation.class);
                if (aug != null) {
                    Optional<ActivationDriver> driver = service.getDriverRepo().getDriver(aug.getActivationDriverId());
                    if (!driver.isPresent()) {
                        throw new IllegalStateException(MessageFormat
                                .format("driver {} cannot be constructed", aug.getActivationDriverId()));
                    }

                    endpoint.setNepRef(ownedNodeEdgePointRef);

                    driver.get().initialize(Collections.singletonList(endpoint), serviceId, null, true, null);
                    tx.addDriver(driver.get());
                } else {
                    LOG.warn("No driver information for node {}", node.getUuid());
                }
            } catch (ReadFailedException e) {
                LOG.warn("Error while reading node", e);
            }
        });
        return tx;

    }

    private Optional<? extends OwnedNodeEdgePointRef> getNep() throws FailureResult {

        try {
            Topology prestoTopo = nrpDao.getTopology(TapiConstants.PRESTO_SYSTEM_TOPO);
            if (prestoTopo.getNode() == null) {
                throw new FailureResult("There are no nodes in {0} topology", TapiConstants.PRESTO_SYSTEM_TOPO);
            }

            final Predicate<OwnedNodeEdgePoint> hasSip = nep -> nep.getMappedServiceInterfacePoint() != null
                    && nep.getMappedServiceInterfacePoint().stream()
                    .anyMatch(sip ->
                         sip.getServiceInterfacePointId().equals(
                                 endpoint.getEndpoint().getServiceInterfacePoint().getServiceInterfacePointId()
                         )
                    );

            final NodeEdgePointBuilder nepBuilder = new NodeEdgePointBuilder()
                    .setTopologyId(new Uuid(TapiConstants.PRESTO_SYSTEM_TOPO));
            final Function<Node, Optional<? extends OwnedNodeEdgePointRef>> getSip =
                (Node node) -> node.getOwnedNodeEdgePoint()
                    .stream().filter(nep -> nep.getMappedServiceInterfacePoint() != null)
                    .filter(hasSip)
                    .map(nep -> {
                        nepBuilder.setNodeId(node.getUuid());
                        nepBuilder.setOwnedNodeEdgePointId(nep.getUuid());
                        return nepBuilder.build();
                    })
                    .findFirst();


            return prestoTopo.getNode().stream()
                    .map(getSip::apply)
                    .filter(Optional::isPresent)
                    .findFirst().orElse(Optional.empty());

        } catch (ReadFailedException e) {
            throw new FailureResult("Cannot read {0} topology", TapiConstants.PRESTO_SYSTEM_TOPO);
        }
    }
}
