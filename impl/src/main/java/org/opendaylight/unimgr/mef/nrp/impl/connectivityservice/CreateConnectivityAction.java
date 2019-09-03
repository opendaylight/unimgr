/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.connectivityservice;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.api.FailureResult;
import org.opendaylight.unimgr.mef.nrp.api.RequestValidator;
import org.opendaylight.unimgr.mef.nrp.api.Subrequrest;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.unimgr.mef.nrp.common.TapiUtils;
import org.opendaylight.unimgr.mef.nrp.impl.ActivationTransaction;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint1;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint2;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.GlobalClass;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ConnectivityServiceEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ServiceType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.cep.list.ConnectionEndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connection.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connection.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connection.end.point.ParentNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connection.end.point.ParentNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.create.connectivity.service.input.ConnConstraint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.create.connectivity.service.input.ConnConstraintBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.create.connectivity.service.output.ServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.NodeRef;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.OwnedNodeEdgePointRef;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create connectivity implementation.
 * @author bartosz.michalik@amartus.com
 */
class CreateConnectivityAction implements Callable<RpcResult<CreateConnectivityServiceOutput>> {
    private static final Logger LOG = LoggerFactory.getLogger(CreateConnectivityAction.class);

    private TapiConnectivityServiceImpl service;
    private final CreateConnectivityServiceInput input;
    private List<Subrequrest> decomposedRequest;
    private List<EndPoint> endpoints;

    CreateConnectivityAction(TapiConnectivityServiceImpl tapiConnectivityService,
                             CreateConnectivityServiceInput input) {
        Objects.requireNonNull(tapiConnectivityService);
        Objects.requireNonNull(input);
        this.service = tapiConnectivityService;
        this.input = input;
    }

    @Override
    @SuppressWarnings("checkstyle:illegalcatch")
    public RpcResult<CreateConnectivityServiceOutput> call() {
        LOG.debug("running CreateConnectivityService task");

        try {
            RequestValidator.ValidationResult validationResult = validateInput();
            if (validationResult.invalid()) {
                LOG.debug("validation for create connectivity service failed = {}", input);
                RpcResultBuilder<CreateConnectivityServiceOutput> res = RpcResultBuilder.failed();
                validationResult.getProblems().forEach(p -> res.withError(RpcError.ErrorType.APPLICATION, p));
                return res.build();

            }

            endpoints = input.getEndPoint() == null ? Collections.emptyList() :
                input.getEndPoint().stream().map(ep -> {
                    EndPoint2 nrpAttributes = ep.augmentation(EndPoint2.class);
                    EndPoint endPoint = new EndPoint(ep, nrpAttributes);
                    endPoint.setLocalId(ep.getLocalId());
                    return endPoint;
                }).collect(Collectors.toList());

            String uniqueStamp = service.getServiceIdPool().getServiceId();
            LOG.debug("connectivity service passed validation, request = {}", input);

            ActivationTransaction tx = prepareTransaction(toCsId(uniqueStamp), input.getConnConstraint().isIsExclusive(), input.getConnConstraint().getServiceType());
            if (tx != null) {
                ActivationTransaction.Result txResult = tx.activate();
                if (txResult.isSuccessful()) {
                    LOG.info("ConnectivityService construct activated successfully, request = {} ", input);

                    // XXX [bm] when createConnectivityModel methods throws an exception we have desync
                    // (devices are configured but no data stored in MD-SAL. How should we address that?
                    ConnectivityService cs = createConnectivityModel(uniqueStamp);
                    CreateConnectivityServiceOutput result = new CreateConnectivityServiceOutputBuilder()
                            .setService(new ServiceBuilder(cs).build()).build();
                    return RpcResultBuilder.success(result).build();
                } else {
                    LOG.warn("CreateConnectivityService failed, reason = {}, request = {}",
                            txResult.getMessage(), input);
                }
            }
            throw new IllegalStateException("no transaction created for create connectivity request");

        } catch (Exception e) {
            LOG.warn("Exception in create connectivity service", e);
            return RpcResultBuilder
                    .<CreateConnectivityServiceOutput>failed()
                    .withError(ErrorType.APPLICATION, e.getMessage())
                    .build();
        }
    }

    private ActivationTransaction prepareTransaction(String serviceId, boolean isExclusive, ServiceType serviceType) throws FailureResult {
        LOG.debug("decompose request");
        decomposedRequest = service.getDecomposer().decompose(endpoints, null);

        if (decomposedRequest == null || decomposedRequest.isEmpty()) {
            throw new FailureResult("Cannot define activation scheme for "
                    + endpoints.stream().map(e -> e.getEndpoint()
                            .getServiceInterfacePoint().getServiceInterfacePointId().getValue()
                            )
                    .collect(Collectors.joining(",", "[", "]")));
        }

        ActivationTransaction tx = new ActivationTransaction();

        decomposedRequest.stream().map(s -> {
            Optional<ActivationDriver> driver = service.getDriverRepo().getDriver(s.getActivationDriverId());
            if (!driver.isPresent()) {
                throw new IllegalStateException(MessageFormat
                        .format("driver {} cannot be created", s.getNodeUuid()));
            }
            driver.get().initialize(s.getEndpoints(), serviceId, null, isExclusive, serviceType);
            LOG.debug("driver {} added to activation transaction", driver.get());
            return driver.get();
        }).forEach(tx::addDriver);

        return tx;
    }

    private RequestValidator.ValidationResult validateInput() {
        return service.getValidator().checkValid(input);
    }

    private String toCsId(String uniqueStamp) {
        return "cs:" + uniqueStamp;
    }

    private ConnectivityService createConnectivityModel(String uniqueStamp)
            throws TimeoutException, InterruptedException, ExecutionException {
        assert decomposedRequest != null : "this method can be only run after request was successfuly decomposed";
        //sort of unique ;)

        LOG.debug("Preparing connectivity related model for {}", uniqueStamp);

        final ReadWriteTransaction tx = service.getBroker().newReadWriteTransaction();
        NrpDao nrpDao = new NrpDao(tx);

        List<Connection> systemConnections = decomposedRequest.stream().map(s -> new ConnectionBuilder()
                .setUuid(new Uuid("conn:" + s.getNodeUuid().getValue() + ":" + uniqueStamp))
//                        .setState()
                .setDirection(ForwardingDirection.BIDIRECTIONAL)
                .setLayerProtocolName(LayerProtocolName.ETH)

                .setConnectionEndPoint(
                        createConnectionPoints(nrpDao, TapiUtils
                                .toNodeRef(s.getNodeUuid()), s.getEndpoints(), uniqueStamp))
                .build())
            .collect(Collectors.toList());

        Connection globalConnection = new ConnectionBuilder()
                .setUuid(new Uuid("conn:" + TapiConstants.PRESTO_ABSTRACT_NODE + ":" + uniqueStamp))
//                        .setState()
                .setDirection(ForwardingDirection.BIDIRECTIONAL)
                .setLayerProtocolName(LayerProtocolName.ETH)
//                .setContainerNode(new Uuid(TapiConstants.PRESTO_ABSTRACT_NODE))
                .setConnectionEndPoint(
                        createConnectionPoints(nrpDao, TapiUtils
                                .toNodeRef(new Uuid(TapiConstants.PRESTO_ABSTRACT_NODE)), endpoints, uniqueStamp))
                .setRoute(Collections.singletonList(new RouteBuilder()
                        .setLocalId("route")
                        .setConnectionEndPoint(systemConnections.stream()
                                .map(GlobalClass::getUuid)
                                .collect(Collectors.toList()))
                        .build())
                ).build();

        ConnConstraint connConstraint = input.getConnConstraint() == null
                ? new ConnConstraintBuilder().build() : new ConnConstraintBuilder(input.getConnConstraint()).build();

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
                .connectivity.context.ConnectivityService cs =
                new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
                        .connectivity.context.ConnectivityServiceBuilder(connConstraint)
                .setUuid(new Uuid(toCsId(uniqueStamp)))
                .setConnection(Collections.singletonList(globalConnection.getUuid()))
                .setEndPoint(toConnectivityServiceEps(endpoints))
                .build();

        systemConnections.forEach(c -> tx.put(LogicalDatastoreType.OPERATIONAL, TapiConnectivityServiceImpl
                .CONNECTIVITY_CTX.child(Connection.class, new ConnectionKey(c.getUuid())), c));
        tx.put(LogicalDatastoreType.OPERATIONAL,
                TapiConnectivityServiceImpl.CONNECTIVITY_CTX.child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi
                                .connectivity.rev180307.connectivity.context.ConnectivityService.class,
                        new ConnectivityServiceKey(cs.getUuid())), cs);

        tx.put(LogicalDatastoreType.OPERATIONAL, TapiConnectivityServiceImpl.CONNECTIVITY_CTX.child(Connection.class,
                new ConnectionKey(globalConnection.getUuid())), globalConnection);

        LOG.debug("Storing connectivity related model for {} to operational data store", uniqueStamp);


        try {
            tx.commit().get(500, TimeUnit.MILLISECONDS);
            LOG.info("Success with serializing Connections and Connectivity Service for {}", uniqueStamp);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.error("Error with committing Connections and Connectivity Service for {} within {} ms",
                    uniqueStamp, 500);
            throw e;
        }

        return new ConnectivityServiceBuilder(cs).build();
    }

    private List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
            .connectivity.service.EndPoint> toConnectivityServiceEps(List<EndPoint> eps) {
        return eps.stream().map(ep -> {
            String id = ep.getLocalId();
            if (id == null) {
                id = "sep:" + Integer.toString(ep.getNepRef()
                        .getOwnedNodeEdgePointId().getValue().hashCode(), 16);
            }
            return new EndPointBuilder()
                .setLocalId(id)
                .setServiceInterfacePoint(ep.getEndpoint().getServiceInterfacePoint())
                .setDirection(PortDirection.BIDIRECTIONAL)
                .setLayerProtocolName(LayerProtocolName.ETH)
                .setRole(ep.getEndpoint().getRole())
                .addAugmentation(EndPoint1.class, new EndPoint1Builder(ep.getAttrs()).build())
                .build();

            }
        ).collect(Collectors.toList());
    }

    private ConnectionEndPointBuilder populateData(ConnectionEndPointBuilder builder,
                                                   ConnectivityServiceEndPoint csep) {
        Objects.requireNonNull(builder);
        Objects.requireNonNull(csep);

        builder
               .setParentNodeEdgePoint(Collections.emptyList())
               .setClientNodeEdgePoint(Collections.emptyList())
                .setOperationalState(csep.getOperationalState())
                .setLayerProtocolName(csep.getLayerProtocolName())
                .setLifecycleState(csep.getLifecycleState())
                .setConnectionPortRole(csep.getRole())
                .setConnectionPortDirection(csep.getDirection());
        return builder;
    }

    private List<ConnectionEndPoint> createConnectionPoints(NrpDao nrpDao, NodeRef ref,
                                                            List<EndPoint> eps, String uniqueStamp) {

        Optional<ConnectivityServiceEndPoint> defaultCsEp = eps.stream()
                .filter(ep -> ep.getEndpoint() != null).map(EndPoint::getEndpoint).findFirst();

        ConnectionEndPointBuilder defB = new ConnectionEndPointBuilder();

        if (defaultCsEp.isPresent()) {
            populateData(defB, defaultCsEp.get());
        } else {
            defB
                .setOperationalState(OperationalState.ENABLED)
                .setLayerProtocolName(LayerProtocolName.ETH)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setConnectionPortRole(PortRole.SYMMETRIC)
                .setConnectionPortDirection(PortDirection.BIDIRECTIONAL);
        }

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.cep.list
                .ConnectionEndPoint defaultVal = defB.build();

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connection
                .ConnectionEndPointBuilder cepRefBuilder
                = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
                .connection.ConnectionEndPointBuilder();
        cepRefBuilder
                .setTopologyId(ref.getTopologyId())
                .setNodeId(ref.getNodeId());


        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
                .connection.ConnectionEndPoint> ceps = new LinkedList<>();

        ParentNodeEdgePointBuilder pBuilder = new ParentNodeEdgePointBuilder(ref);

        for (EndPoint ep : eps) {
            ConnectionEndPointBuilder builder = new ConnectionEndPointBuilder(defaultVal);
            ConnectivityServiceEndPoint csp = ep.getEndpoint();
            OwnedNodeEdgePointRef nepRef = ep.getNepRef();
            ParentNodeEdgePoint parentRef = pBuilder.setOwnedNodeEdgePointId(nepRef.getOwnedNodeEdgePointId()).build();
            builder.setParentNodeEdgePoint(Arrays.asList(parentRef));
            cepRefBuilder.setOwnedNodeEdgePointId(nepRef.getOwnedNodeEdgePointId());
            cepRefBuilder.setConnectionEndPointId(new Uuid("cep:"
                    + nepRef.getOwnedNodeEdgePointId().getValue() + ":" + uniqueStamp));
            if (csp != null) {
                populateData(builder, csp);
            }
            builder.setUuid(cepRefBuilder.getConnectionEndPointId());
            builder.withKey(null);
            ConnectionEndPoint cepRef = nrpDao.addConnectionEndPoint(cepRefBuilder.build(), builder.build());
            ceps.add(cepRef);
        }

        return ceps;
    }
}
