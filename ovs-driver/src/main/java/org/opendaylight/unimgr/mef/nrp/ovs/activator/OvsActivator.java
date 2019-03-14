/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.activator;

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
import org.opendaylight.unimgr.mef.nrp.ovs.util.EtreeUtils;
import org.opendaylight.unimgr.mef.nrp.ovs.util.OpenFlowUtils;
import org.opendaylight.unimgr.mef.nrp.ovs.util.OvsdbUtils;
import org.opendaylight.unimgr.mef.nrp.ovs.util.VlanUtils;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ServiceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.unimgr.mef.nrp.api.FailureResult;

/**
 * Ovs driver activator.
 * @author marek.ryznar@amartus.com
 */
public class OvsActivator implements ResourceActivator {

    private static final Logger LOG = LoggerFactory.getLogger(OvsActivator.class);
    //TODO introduce poll synced with ovsdb config
    private static final AtomicInteger QUEUE_NUMBER_GENERATOR = new AtomicInteger(200);

    private final DataBroker dataBroker;

    public OvsActivator(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    /**
     * Set state for the driver for a (de)activation transaction.
     * @param endPoints list of endpoint to interconnect
     */
    @Override
    public void activate(List<EndPoint> endPoints, String serviceName, boolean isExclusive, String serviceType)
            throws ResourceNotAvailableException, TransactionCommitFailedException {
        OvsActivatorHelper.validateExternalVLANs(endPoints);

        VlanUtils vlanUtils = new VlanUtils(dataBroker, endPoints.iterator().next().getNepRef().getNodeId().getValue());
        EtreeUtils eTreeUtils = new EtreeUtils();
        long rootCount = endPoints.stream().filter(node -> (node.getEndpoint().getRole()!=null  && node.getEndpoint().getRole().equals(PortRole.ROOT))).count();

        for (EndPoint endPoint:endPoints) {
            activateEndpoint(endPoint, serviceName, vlanUtils, isExclusive, serviceType, rootCount, eTreeUtils);
        }

    }

    private void activateEndpoint(EndPoint endPoint, String serviceName, VlanUtils vlanUtils, boolean isExclusive, String serviceType, long rootCount, EtreeUtils eTreeUtils)
            throws ResourceNotAvailableException, TransactionCommitFailedException {
        // Transaction - Get Open vSwitch node and its flow table
        String portName = OvsActivatorHelper.getPortName(endPoint.getEndpoint().getServiceInterfacePoint()
                .getServiceInterfacePointId().getValue());
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
        long queueNumber = QUEUE_NUMBER_GENERATOR.getAndIncrement();
        LOG.info("VLAN ID = {} ", ovsActivatorHelper.getCeVlanId().isPresent());

        if(isExclusive && ! ovsActivatorHelper.getCeVlanId().isPresent()){
            LOG.info( " NEW LOGIC here openFlowPortName {}" , openFlowPortName);
            // Port based E-tree service 
            if(serviceType != null && serviceType.equals(ServiceType.ROOTEDMULTIPOINTCONNECTIVITY.getName())) {
                flowsToWrite.addAll(OpenFlowUtils.getEpTree(endPoint.getEndpoint().getRole().getName(), openFlowPortName, vlanUtils.getVlanID(serviceName), ovsActivatorHelper.getCeVlanId(),interswitchLinks, serviceName, queueNumber, rootCount, eTreeUtils, isExclusive));
            } else {
                flowsToWrite.addAll(OpenFlowUtils.getFlows(openFlowPortName, interswitchLinks, serviceName, queueNumber));
            }
        }
        else{
            LOG.info( "EXISTING LOGIC");
            if(serviceType != null && serviceType.equals(ServiceType.ROOTEDMULTIPOINTCONNECTIVITY.getName())) {
                flowsToWrite.addAll(OpenFlowUtils.getEvpTree(endPoint.getEndpoint().getRole().getName(), openFlowPortName, vlanUtils.getVlanID(serviceName), ovsActivatorHelper.getCeVlanId(),interswitchLinks, serviceName, queueNumber, rootCount, eTreeUtils));
            }else {
                flowsToWrite.addAll(OpenFlowUtils.getVlanFlows(openFlowPortName, vlanUtils.getVlanID(serviceName), ovsActivatorHelper.getCeVlanId(),interswitchLinks, serviceName, queueNumber));
            }
            
        }

        // Transaction - Add flows related to service to table and remove unnecessary flows
        TableTransaction tableTransaction = new TableTransaction(dataBroker, node, table);
        tableTransaction.deleteFlows(flowsToDelete, true);
        tableTransaction.writeFlows(flowsToWrite);

        List<String> outputPortNames = interswitchLinks.stream()
                .map(link -> ovsActivatorHelper.getTpNameFromOpenFlowPortName(link.getLinkId().getValue()))
                .collect(Collectors.toList());

        if (ovsActivatorHelper.isIBwpConfigured()) {
            //Create egress qos
            OvsdbUtils.createEgressQos(dataBroker, portName, outputPortNames, ovsActivatorHelper.getQosMinRate(),
                    ovsActivatorHelper.getQosMaxRate(), serviceName, queueNumber);
        }

    }

    @Override
    public void deactivate(List<EndPoint> endPoints, String serviceName, String serviceType)
            throws TransactionCommitFailedException, ResourceNotAvailableException {
        boolean isExclusive = false;

        for (EndPoint endPoint:endPoints) {
            deactivateEndpoint(endPoint, serviceName);
        }
        new VlanUtils(dataBroker, endPoints.iterator().next().getNepRef().getNodeId().getValue()).releaseServiceVlan(serviceName);
        try {
            isExclusive = new EtreeUtils().getServiceType(dataBroker, serviceName);
            if (serviceType != null && serviceType.equals(ServiceType.ROOTEDMULTIPOINTCONNECTIVITY.getName()) && ! isExclusive) {
                new EtreeUtils().releaseTreeServiceVlan(serviceName);
            }
        } catch (FailureResult e) {
            LOG.error("Unable to find out service type result with serviceid { " + serviceName + " }", e);
        }
        
    }

    private void deactivateEndpoint(EndPoint endPoint, String serviceName)
            throws ResourceNotAvailableException, TransactionCommitFailedException {

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

        String portName = OvsActivatorHelper.getPortName(endPoint
                .getEndpoint().getServiceInterfacePoint().getServiceInterfacePointId().getValue());
        Node node = topologyTransaction.readNode(portName);

        //list with endpoint + all interswitch ports
        List<String> tpsWithQos = topologyTransaction.readInterswitchLinks(node).stream()
                .map(link -> ovsActivatorHelper.getTpNameFromOpenFlowPortName(link.getLinkId().getValue()))
                .collect(Collectors.toList());
        tpsWithQos.add(portName);

        OvsdbUtils.removeQosEntryFromTerminationPoints(dataBroker, serviceName, tpsWithQos);
    }

    public void update(List<EndPoint> endPoints, String serviceName)
            throws ResourceNotAvailableException, TransactionCommitFailedException {
        OvsActivatorHelper.validateExternalVLANs(endPoints);
        for (EndPoint endPoint:endPoints) {
            updateEndpoint(endPoint, serviceName);
        }
    }

    private void updateEndpoint(EndPoint endPoint, String serviceName)
            throws ResourceNotAvailableException, TransactionCommitFailedException {

        TopologyTransaction topologyTransaction = new TopologyTransaction(dataBroker);
        OvsActivatorHelper ovsActivatorHelper = new OvsActivatorHelper(topologyTransaction, endPoint);

        String portName = OvsActivatorHelper.getPortName(endPoint
                .getEndpoint().getServiceInterfacePoint().getServiceInterfacePointId().getValue());
        Node node = topologyTransaction.readNode(portName);

        //list with endpoint + all interswitch ports
        List<String> interswitchPorts = topologyTransaction.readInterswitchLinks(node).stream()
                .map(link -> ovsActivatorHelper.getTpNameFromOpenFlowPortName(link.getLinkId().getValue()))
                .collect(Collectors.toList());

        List<String> tpsWithQos = new LinkedList<>(interswitchPorts);
        tpsWithQos.add(portName);

        //remove old egress qos
        OvsdbUtils.removeQosEntryFromTerminationPoints(dataBroker, serviceName, tpsWithQos);


        long queueNumber = QUEUE_NUMBER_GENERATOR.getAndIncrement();
        if (ovsActivatorHelper.isIBwpConfigured()) {
                //Create egress qos
            OvsdbUtils.createEgressQos(dataBroker, portName, interswitchPorts, ovsActivatorHelper.getQosMinRate(),
                ovsActivatorHelper.getQosMaxRate(), serviceName, queueNumber);
        }

        //modify flow with new queue number
        Table table = OpenFlowUtils.getTable(node);
        TableTransaction tableTransaction = new TableTransaction(dataBroker, node, table);
        tableTransaction.writeFlow(OpenFlowUtils.createVlanIngressFlow(ovsActivatorHelper.getOpenFlowPortName(),
                new VlanUtils(dataBroker, endPoint.getNepRef().getNodeId().getValue())
                        .getVlanID(serviceName) , ovsActivatorHelper.getCeVlanId(),
                serviceName, topologyTransaction.readInterswitchLinks(node), queueNumber));
    }

}
