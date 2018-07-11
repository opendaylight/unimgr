/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.activator;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivator;
import org.opendaylight.unimgr.mef.nrp.common.ResourceNotAvailableException;
import org.opendaylight.unimgr.mef.nrp.ovs.transaction.TableTransaction;
import org.opendaylight.unimgr.mef.nrp.ovs.transaction.TopologyTransaction;
import org.opendaylight.unimgr.mef.nrp.ovs.util.OpenFlowUtils;
import org.opendaylight.unimgr.mef.nrp.ovs.util.OvsdbUtils;
import org.opendaylight.unimgr.mef.nrp.ovs.util.VlanUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author marek.ryznar@amartus.com
 */
public class OvsActivator implements ResourceActivator {

    private final DataBroker dataBroker;
    private static final Logger LOG = LoggerFactory.getLogger(OvsActivator.class);

    //TODO introduce poll synced with ovsdb config
    private static final AtomicInteger queueNumberGenerator = new AtomicInteger(200);

    public OvsActivator(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    /**
     * Set state for the driver for a (de)activation transaction.
     * @param endPoints list of endpoint to interconnect
     */
    @Override
    public void activate(List<EndPoint> endPoints, String serviceName) throws ResourceNotAvailableException, TransactionCommitFailedException {
        OvsActivatorHelper.validateExternalVLANs(endPoints);

        VlanUtils vlanUtils = new VlanUtils(dataBroker, endPoints.iterator().next().getNepRef().getNodeId().getValue());

        for (EndPoint endPoint:endPoints) {
            activateEndpoint(endPoint, serviceName, vlanUtils);
        }

    }

    private void activateEndpoint(EndPoint endPoint, String serviceName, VlanUtils vlanUtils) throws ResourceNotAvailableException, TransactionCommitFailedException {
        // Transaction - Get Open vSwitch node and its flow table
        String portName = OvsActivatorHelper.getPortName(endPoint.getEndpoint().getServiceInterfacePoint().getServiceInterfacePointId().getValue());
        TopologyTransaction topologyTransaction = new TopologyTransaction(dataBroker);
        Node node = topologyTransaction.readNode(portName);
        Table table = OpenFlowUtils.getTable(node);

        // Prepare list of flows to be added/removed
        List<Flow> flowsToWrite = new ArrayList<>();
        List<Flow> flowsToDelete = new ArrayList<>();
        List<Link> interswitchLinks = topologyTransaction.readInterswitchLinks(node);
        if (!OpenFlowUtils.isTablePreconfigured(table)) {
            LOG.debug("Table is not preconfigured. Adding base flows.");
            flowsToWrite.addAll(OpenFlowUtils.getBaseFlows(interswitchLinks));
            flowsToDelete.addAll(OpenFlowUtils.getExistingFlowsWithoutLLDP(table));
        }

        OvsActivatorHelper ovsActivatorHelper = new OvsActivatorHelper(topologyTransaction, endPoint);
        String openFlowPortName = ovsActivatorHelper.getOpenFlowPortName();
        long queueNumber = queueNumberGenerator.getAndIncrement();
        flowsToWrite.addAll(OpenFlowUtils.getVlanFlows(openFlowPortName, vlanUtils.getVlanID(serviceName), ovsActivatorHelper.getCeVlanId(),interswitchLinks, serviceName, queueNumber));

        // Transaction - Add flows related to service to table and remove unnecessary flows
        TableTransaction tableTransaction = new TableTransaction(dataBroker, node, table);
        tableTransaction.deleteFlows(flowsToDelete, true);
        tableTransaction.writeFlows(flowsToWrite);

		List<String> outputPortNames = interswitchLinks.stream()
				.map(link -> ovsActivatorHelper.getTpNameFromOpenFlowPortName(link.getLinkId().getValue()))
				.collect(Collectors.toList());

		if(ovsActivatorHelper.isIBwpConfigured()) {
            //Create egress qos
            OvsdbUtils.createEgressQos(dataBroker, portName, outputPortNames, ovsActivatorHelper.getQosMinRate(),
                    ovsActivatorHelper.getQosMaxRate(), serviceName, queueNumber);
        }



    }

	@Override
    public void deactivate(List<EndPoint> endPoints, String serviceName) throws TransactionCommitFailedException, ResourceNotAvailableException {

        for (EndPoint endPoint:endPoints) {
        	deactivateEndpoint(endPoint, serviceName);
        }
        new VlanUtils(dataBroker, endPoints.iterator().next().getNepRef().getNodeId().getValue()).releaseServiceVlan(serviceName);

    }

    private void deactivateEndpoint(EndPoint endPoint, String serviceName) throws ResourceNotAvailableException, TransactionCommitFailedException {

        // Transaction - Get Open vSwitch node and its flow table
        TopologyTransaction topologyTransaction = new TopologyTransaction(dataBroker);
        OvsActivatorHelper ovsActivatorHelper = new OvsActivatorHelper(topologyTransaction, endPoint);

        Node openFlowNode = topologyTransaction.readNodeOF(ovsActivatorHelper.getOpenFlowPortName());
        Table table = OpenFlowUtils.getTable(openFlowNode);
        // Get list of flows to be removed
        List<Flow> flowsToDelete = OpenFlowUtils.getServiceFlows(table, serviceName);

        // Transaction - Remove flows related to service from table
        TableTransaction tableTransaction = new TableTransaction(dataBroker, openFlowNode, table);
        tableTransaction.deleteFlows(flowsToDelete, false);

        String portName = OvsActivatorHelper.getPortName(endPoint.getEndpoint().getServiceInterfacePoint().getServiceInterfacePointId().getValue());
        Node node = topologyTransaction.readNode(portName);

        //list with endpoint + all interswitch ports
    	List<String> tpsWithQos = topologyTransaction.readInterswitchLinks(node).stream()
				.map(link -> ovsActivatorHelper.getTpNameFromOpenFlowPortName(link.getLinkId().getValue()))
				.collect(Collectors.toList());
    	tpsWithQos.add(portName);

        OvsdbUtils.removeQosEntryFromTerminationPoints(dataBroker, serviceName, tpsWithQos);
    }

	public void update(List<EndPoint> endPoints, String serviceName) throws ResourceNotAvailableException, TransactionCommitFailedException {
        OvsActivatorHelper.validateExternalVLANs(endPoints);
        for (EndPoint endPoint:endPoints) {
            updateEndpoint(endPoint, serviceName);
        }
	}

	private void updateEndpoint(EndPoint endPoint, String serviceName) throws ResourceNotAvailableException, TransactionCommitFailedException{

		TopologyTransaction topologyTransaction = new TopologyTransaction(dataBroker);
	    OvsActivatorHelper ovsActivatorHelper = new OvsActivatorHelper(topologyTransaction, endPoint);

		String portName = OvsActivatorHelper.getPortName(endPoint.getEndpoint().getServiceInterfacePoint().getServiceInterfacePointId().getValue());
		Node node = topologyTransaction.readNode(portName);

		//list with endpoint + all interswitch ports
		List<String> interswitchPorts = topologyTransaction.readInterswitchLinks(node).stream()
				.map(link -> ovsActivatorHelper.getTpNameFromOpenFlowPortName(link.getLinkId().getValue()))
				.collect(Collectors.toList());

		List<String> tpsWithQos = new LinkedList<>(interswitchPorts);
		tpsWithQos.add(portName);

		//remove old egress qos
		OvsdbUtils.removeQosEntryFromTerminationPoints(dataBroker, serviceName, tpsWithQos);


		long queueNumber = queueNumberGenerator.getAndIncrement();
		if(ovsActivatorHelper.isIBwpConfigured()) {
		        //Create egress qos
			OvsdbUtils.createEgressQos(dataBroker, portName, interswitchPorts, ovsActivatorHelper.getQosMinRate(),
				ovsActivatorHelper.getQosMaxRate(), serviceName, queueNumber);
		}

		//modify flow with new queue number
		 Table table = OpenFlowUtils.getTable(node);
        TableTransaction tableTransaction = new TableTransaction(dataBroker, node, table);
		tableTransaction.writeFlow(OpenFlowUtils.createVlanIngressFlow(ovsActivatorHelper.getOpenFlowPortName(), new VlanUtils(dataBroker, endPoint.getNepRef().getNodeId().getValue()).getVlanID(serviceName) , ovsActivatorHelper.getCeVlanId(),
				serviceName, topologyTransaction.readInterswitchLinks(node), queueNumber));
	}

}
