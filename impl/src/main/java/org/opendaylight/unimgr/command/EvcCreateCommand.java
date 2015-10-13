/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.command;

import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.unimgr.impl.UnimgrUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Uni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvcCreateCommand extends AbstractCreateCommand {

    private static final Logger LOG = LoggerFactory.getLogger(EvcCreateCommand.class);

    public EvcCreateCommand(DataBroker dataBroker,
            Map<InstanceIdentifier<?>, DataObject> changes) {
        super.dataBroker = dataBroker;
        super.changes = changes;
    }

    @Override
    public void execute() {
        for (Entry<InstanceIdentifier<?>, DataObject> created : changes
                .entrySet()) {
            if (created.getValue() != null && created.getValue() instanceof EvcAugmentation) {
                EvcAugmentation evc = (EvcAugmentation) created.getValue();
                LOG.info("New EVC created with id {}.", evc.getEvcId());
                if (evc.getUniDest() == null || evc.getUniDest().isEmpty()) {
                    LOG.error("Destination UNI cannot be null.");
                    break;
                }
                if (evc.getUniSource() == null || evc.getUniSource().isEmpty()) {
                    LOG.error("Source UNI cannot be null.");
                    break;
                }
                // Get the destination UNI
                InstanceIdentifier<Node> destinationNodeIid = (InstanceIdentifier<Node>) evc.getUniDest().iterator()
                        .next().getUni();
                Uni destinationUni = UnimgrUtils.readNode(dataBroker, destinationNodeIid).get()
                        .getAugmentation(UniAugmentation.class);
                NodeId ovsdbDestinationNodeId = UnimgrMapper.createNodeId(destinationUni.getIpAddress());
                // Get the source UNI
                InstanceIdentifier<Node> sourceIid = (InstanceIdentifier<Node>) evc.getUniSource().iterator().next()
                        .getUni();
                Uni sourceUni = UnimgrUtils.readNode(dataBroker, sourceIid).get()
                        .getAugmentation(UniAugmentation.class);
                NodeId ovsdbSourceNodeId = UnimgrMapper.createNodeId(sourceUni.getIpAddress());

                // Set source
                Node sourceBr1 = UnimgrUtils.readNode(
                        dataBroker,
                        UnimgrMapper.getOvsdbBridgeNodeIID(ovsdbSourceNodeId,
                                UnimgrConstants.DEFAULT_BRIDGE_NAME)).get();
                UnimgrUtils.createTerminationPointNode(dataBroker,
                        destinationUni, sourceBr1,
                        UnimgrConstants.DEFAULT_BRIDGE_NAME,
                        UnimgrConstants.DEFAULT_INTERNAL_IFACE, null);
                Node sourceBr2 = UnimgrUtils.readNode(
                        dataBroker,
                        UnimgrMapper.getOvsdbBridgeNodeIID(ovsdbSourceNodeId,
                                UnimgrConstants.DEFAULT_BRIDGE_NAME)).get();
                UnimgrUtils.createGreTunnel(dataBroker, sourceUni,
                        destinationUni, sourceBr2,
                        UnimgrConstants.DEFAULT_BRIDGE_NAME, "gre0");

                // Set destination
                Node destinationBr1 = UnimgrUtils.readNode(
                        dataBroker,
                        UnimgrMapper.getOvsdbBridgeNodeIID(ovsdbDestinationNodeId,
                                UnimgrConstants.DEFAULT_BRIDGE_NAME)).get();
                UnimgrUtils.createTerminationPointNode(dataBroker,
                        destinationUni, destinationBr1,
                        UnimgrConstants.DEFAULT_BRIDGE_NAME,
                        UnimgrConstants.DEFAULT_INTERNAL_IFACE, null);
                Node destinationBr2 = UnimgrUtils.readNode(
                        dataBroker,
                        UnimgrMapper.getOvsdbBridgeNodeIID(ovsdbDestinationNodeId,
                                UnimgrConstants.DEFAULT_BRIDGE_NAME)).get();
                UnimgrUtils.createGreTunnel(dataBroker, destinationUni,
                        sourceUni, destinationBr2,
                        UnimgrConstants.DEFAULT_BRIDGE_NAME, "gre0");
            }
        }
    }

}
