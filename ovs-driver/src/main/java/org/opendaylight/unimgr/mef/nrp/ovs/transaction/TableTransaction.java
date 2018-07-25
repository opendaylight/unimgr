/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.transaction;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Performs output (write/delete) transactions related to openflow flows
 * during OvsDriver activation/deactivation.
 *
 * @author jakub.niezgoda@amartus.com
 */
public class TableTransaction {

    private DataBroker dataBroker;
    private InstanceIdentifier<Table> tableInstanceId;

    private static final Logger LOG = LoggerFactory.getLogger(TableTransaction.class);

    /**
     * Creates and initialize TableTransaction object.
     *
     * @param dataBroker access to data tree store
     * @param node       openflow network node
     * @param table      flow table of node
     */
    public TableTransaction(DataBroker dataBroker, Node node, Table table) {
        this.dataBroker = dataBroker;
        this.tableInstanceId = getTableIid(node.key(), table.key());
    }

    /**
     * Writes flows to the flow table.
     *
     * @param flows list of flows to be added
     * @throws TransactionCommitFailedException if writing flows transaction fails
     */
    public void writeFlows(List<Flow> flows) throws TransactionCommitFailedException {
        for (Flow flow : flows) {
            writeFlow(flow);
        }
    }

    /**
     * Writes flow to the flow table.
     *
     * @param flow flow to be added
     * @throws TransactionCommitFailedException if writing flow transaction fails
     */
    public void writeFlow(Flow flow) throws TransactionCommitFailedException {
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        LOG.debug("Writing flow '" + flow.getId().getValue()
                + "' to " + LogicalDatastoreType.CONFIGURATION + " data store.");
        transaction.put(LogicalDatastoreType.CONFIGURATION, getFlowIid(flow), flow, true);
        transaction.submit().checkedGet();
    }

    /**
     * Deletes flows from the flow table.
     *
     * @param flows list of flows to be deleted
     * @param writeToConfigurationDataStoreFirst if set flows are written to CONFIGURATION data store before deletion
     * @throws TransactionCommitFailedException if writing/deleting flows transaction fails
     */
    public void deleteFlows(List<Flow> flows, boolean writeToConfigurationDataStoreFirst)
            throws TransactionCommitFailedException {
        if (writeToConfigurationDataStoreFirst) {
            writeFlows(flows);
        }
        for (Flow flow : flows) {
            deleteFlow(flow);
        }
    }

    /**
     * Deletes flow from the flow table.
     *
     * @param flow flow to be deleted
     * @throws TransactionCommitFailedException if deleting flow transaction fails
     */
    public void deleteFlow(Flow flow) throws TransactionCommitFailedException {
        WriteTransaction deleteTransaction = dataBroker.newWriteOnlyTransaction();
        LOG.debug("Deleting flow '" + flow.getId().getValue()
                + "' from " + LogicalDatastoreType.CONFIGURATION + " data store.");
        deleteTransaction.delete(LogicalDatastoreType.CONFIGURATION, getFlowIid(flow));
        deleteTransaction.submit().checkedGet();
    }

    private InstanceIdentifier<Table> getTableIid(NodeKey nodeKey, TableKey tableKey) {
        return InstanceIdentifier.builder(Nodes.class)
                                 .child(Node.class, nodeKey)
                                 .augmentation(FlowCapableNode.class)
                                 .child(Table.class, tableKey)
                                 .build();
    }

    private InstanceIdentifier<Flow> getFlowIid(Flow flow) {
        return tableInstanceId.child(Flow.class, flow.key());
    }
}
