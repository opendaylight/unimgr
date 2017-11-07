/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.connectivityservice;

import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.api.FailureResult;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.unimgr.mef.nrp.impl.ActivationTransaction;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev170712.EndPoint5;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.common.rev170712.Uuid;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.connectivity.rev170712.UpdateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.connectivity.rev170712.UpdateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.connectivity.rev170712.UpdateConnectivityServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.connectivity.rev170712.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.connectivity.rev170712.update.connectivity.service.output.ServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.topology.rev170712.Node;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.topology.rev170712.topology.context.Topology;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

public class UpdateConnectivityAction implements Callable<RpcResult<UpdateConnectivityServiceOutput>> {

	private static final Logger LOG = LoggerFactory.getLogger(UpdateConnectivityServiceOutput.class);

	private TapiConnectivityServiceImpl service;
	private final UpdateConnectivityServiceInput input;
	private EndPoint endpoint;

	public UpdateConnectivityAction(TapiConnectivityServiceImpl tapiConnectivityService,
			UpdateConnectivityServiceInput input) {

		Objects.requireNonNull(tapiConnectivityService);
		Objects.requireNonNull(input);
		this.service = tapiConnectivityService;
		this.input = input;
	}

	@Override
	public RpcResult<UpdateConnectivityServiceOutput> call() throws Exception {

		LOG.debug("running UpdateConnectivityService task");

		NrpDao nrpDao = new NrpDao(service.getBroker().newReadOnlyTransaction());
		try {
			// TODO validate input
			// RequestValidator.ValidationResult validationResult =
			// validateInput();
			// if (!validationResult.isValid()) {
			// RpcResultBuilder<UpdateConnectivityServiceOutput> res =
			// RpcResultBuilder.failed();
			// validationResult.getProblems().forEach(p ->
			// res.withError(RpcError.ErrorType.APPLICATION, p));
			// return res.build();
			//
			// }

			endpoint = new EndPoint(input.getEndPoint(), input.getEndPoint().getAugmentation(EndPoint5.class));

			String serviceId = input.getServiceIdOrName();

			ActivationTransaction tx = prepareTransaction(nrpDao, serviceId);
			if (tx != null) {
				ActivationTransaction.Result txResult = tx.update();
				if (txResult.isSuccessful()) {
					LOG.info("ConnectivityService construct updated successfully, request = {} ", input);

					ConnectivityService service = nrpDao.getConnectivityService(serviceId);
					UpdateConnectivityServiceOutput result = new UpdateConnectivityServiceOutputBuilder()
							.setService(new ServiceBuilder(service).build()).build();
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

	private ActivationTransaction prepareTransaction(NrpDao nrpDao, String serviceId) throws FailureResult {
		ActivationTransaction tx = new ActivationTransaction();

		Optional<Uuid> nodeUuid = getNodeUuid(nrpDao);
		if (nodeUuid.isPresent()) {
			Optional<ActivationDriver> driver = service.getDriverRepo().getDriver(nodeUuid.get());
			if (!driver.isPresent()) {
				throw new IllegalStateException(MessageFormat.format("driver {} cannot be created", nodeUuid.get()));
			}
			driver.get().initialize(Arrays.asList(endpoint), serviceId, null);
			tx.addDriver(driver.get());
		}
		return tx;

	}

	private Optional<Uuid> getNodeUuid(NrpDao nrpDao) throws FailureResult {
		Optional<Uuid> result = Optional.empty();
		try {
			Topology prestoTopo = nrpDao.getTopology(TapiConstants.PRESTO_SYSTEM_TOPO);
			if (prestoTopo.getNode() == null) {
				throw new FailureResult("There are no nodes in {0} topology", TapiConstants.PRESTO_SYSTEM_TOPO);
			}
			for (Node node : prestoTopo.getNode()) {
				if (node.getOwnedNodeEdgePoint().stream().filter(nep -> nep.getMappedServiceInterfacePoint() != null).flatMap(nep -> nep.getMappedServiceInterfacePoint().stream())
						.anyMatch(sipUuid -> sipUuid.equals(endpoint.getEndpoint().getServiceInterfacePoint()))) {
					return Optional.of(node.getUuid());
				}
			}
		} catch (ReadFailedException e) {
			throw new FailureResult("Cannot read {0} topology", TapiConstants.PRESTO_SYSTEM_TOPO);
		}
		return result;
	}
}
