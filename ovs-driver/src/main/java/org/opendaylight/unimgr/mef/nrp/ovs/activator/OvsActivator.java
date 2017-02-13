/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.activator;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivator;
import org.opendaylight.unimgr.mef.nrp.common.ResourceNotAvailableException;
import org.opendaylight.unimgr.mef.nrp.ovs.transaction.TableTransaction;
import org.opendaylight.unimgr.mef.nrp.ovs.transaction.TopologyTransaction;
import org.opendaylight.unimgr.mef.nrp.ovs.util.OpenFlowUtils;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author marek.ryznar@amartus.com
 */
public class OvsActivator implements ResourceActivator {

    private final DataBroker dataBroker;
    private static final Logger LOG = LoggerFactory.getLogger(OvsActivator.class);

    public OvsActivator(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public void activate(String nodeName, String outerName, String innerName, FcPort flowPoint, FcPort neighbor, long mtu) throws TransactionCommitFailedException, ResourceNotAvailableException {
        // Transaction - Get Open vSwitch node and its flow table
        String portName = flowPoint.getTp().getValue();
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
            flowsToDelete.addAll(OpenFlowUtils.getExistingFlows(table));
        }
        OvsActivatorHelper ovsActivatorHelper = new OvsActivatorHelper(topologyTransaction, flowPoint);
        String openFlowPortName = ovsActivatorHelper.getOpenFlowPortName();
        int externalVlanId = ovsActivatorHelper.getServiceVlanId();
        int internalVlanId = ovsActivatorHelper.getInternalVlanId();
        flowsToWrite.addAll(OpenFlowUtils.getVlanFlows(openFlowPortName, externalVlanId, internalVlanId, interswitchLinks, outerName));

        // Transaction - Add flows related to service to table and remove unnecessary flows
        TableTransaction tableTransaction = new TableTransaction(dataBroker, node, table);
        tableTransaction.deleteFlows(flowsToDelete, true);
        tableTransaction.writeFlows(flowsToWrite);
    }

    @Override
    public void deactivate(String nodeName, String outerName, String innerName, FcPort flowPoint, FcPort neighbor, long mtu) throws TransactionCommitFailedException, ResourceNotAvailableException {
        // Transaction - Get Open vSwitch node and its flow table
        String portName = flowPoint.getTp().getValue();
        TopologyTransaction topologyTransaction = new TopologyTransaction(dataBroker);
        Node node = topologyTransaction.readNode(portName);
        Table table = OpenFlowUtils.getTable(node);

        // Get list of flows to be removed
        List<Flow> flowsToDelete = OpenFlowUtils.getServiceFlows(table, outerName);

        // Transaction - Remove flows related to service from table
        TableTransaction tableTransaction = new TableTransaction(dataBroker, node, table);
        tableTransaction.deleteFlows(flowsToDelete, false);
    }
}
