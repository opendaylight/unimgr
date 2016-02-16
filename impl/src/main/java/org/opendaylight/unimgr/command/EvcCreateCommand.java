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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.unimgr.utils.EvcUtils;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.unimgr.utils.OvsdbUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class EvcCreateCommand extends AbstractCreateCommand {

    private static final Logger LOG = LoggerFactory.getLogger(EvcCreateCommand.class);

    public EvcCreateCommand(DataBroker dataBroker,
                            Map<InstanceIdentifier<?>, DataObject> changes) {
        super.dataBroker = dataBroker;
        super.changes = changes;
    }

    @Override
    public void execute() {
        for (Entry<InstanceIdentifier<?>, DataObject> created : changes.entrySet()) {
            if ((created.getValue() != null) && (created.getValue() instanceof EvcAugmentation)) {
                EvcAugmentation evc = (EvcAugmentation) created.getValue();
                InstanceIdentifier<?> evcKey = created.getKey();
                // For now, we assume that there is 1 uni per source/destination
                if ((evc.getUniDest() == null) || evc.getUniDest().isEmpty()) {
                    LOG.error("Destination UNI cannot be null.");
                    break;
                }
                if ((evc.getUniSource() == null) || evc.getUniSource().isEmpty()) {
                    LOG.error("Source UNI cannot be null.");
                    break;
                }
                LOG.info("New EVC created, source IP: {} destination IP {}.",
                        evc.getUniSource().iterator().next().getIpAddress().getIpv4Address(),
                        evc.getUniDest().iterator().next().getIpAddress().getIpv4Address());
                InstanceIdentifier<Node> sourceUniIid;
                InstanceIdentifier<Node> destinationUniIid;
                //FIXME we are assuming that there is only 1 UNI source and destination
                // per evc
                InstanceIdentifier<?> iidSource = evc.getUniSource().iterator().next().getUni();
                if (iidSource != null) {
                    sourceUniIid = iidSource.firstIdentifierOf(Node.class);
                } else {
                    sourceUniIid = UnimgrMapper.getUniIid(dataBroker,
                                                          evc.getUniSource().iterator().next().getIpAddress(),
                                                          LogicalDatastoreType.OPERATIONAL);
                }
                InstanceIdentifier<?> iidDest = evc.getUniDest().iterator().next().getUni();
                if (iidDest != null) {
                    destinationUniIid = iidDest.firstIdentifierOf(Node.class);
                } else {
                    destinationUniIid = UnimgrMapper.getUniIid(dataBroker,
                                                               evc.getUniDest().iterator().next().getIpAddress(),
                                                               LogicalDatastoreType.OPERATIONAL);
                }
                Optional<Node> optionalUniSource = MdsalUtils.readNode(dataBroker,
                                                                        LogicalDatastoreType.OPERATIONAL,
                                                                        sourceUniIid);
                Optional<Node> optionalUniDestination = MdsalUtils.readNode(dataBroker,
                                                                             LogicalDatastoreType.OPERATIONAL,
                                                                             destinationUniIid);
                Node uniSource;
                Node uniDestination;
                // Retrieve the source and destination Unis
                if (optionalUniSource.isPresent() && optionalUniDestination.isPresent()) {
                    uniSource = optionalUniSource.get();
                    uniDestination = optionalUniDestination.get();
                    // Set source and destination
                    UniAugmentation sourceUniAugmentation =
                                        uniSource.getAugmentation(UniAugmentation.class);
                    UniAugmentation destinationUniAugmentation =
                                        uniDestination.getAugmentation(UniAugmentation.class);
                    Optional<Node> optionalSourceOvsdbNode =
                            MdsalUtils.readNode(dataBroker,
                                                            LogicalDatastoreType.OPERATIONAL,
                                                            sourceUniAugmentation
                                                                .getOvsdbNodeRef()
                                                                .getValue());
                    Optional<Node> optionalDestinationOvsdbNode =
                            MdsalUtils.readNode(dataBroker,
                                                            LogicalDatastoreType.OPERATIONAL,
                                                            destinationUniAugmentation
                                                                .getOvsdbNodeRef()
                                                                .getValue());
                    if (optionalSourceOvsdbNode.isPresent() && optionalDestinationOvsdbNode.isPresent()) {
                        InstanceIdentifier<Node> sourceBridgeIid =
                                UnimgrMapper.getOvsdbBridgeNodeIid(optionalSourceOvsdbNode.get());
                        Optional<Node> optionalSourceBr = MdsalUtils.readNode(dataBroker,
                                                                               LogicalDatastoreType.OPERATIONAL,
                                                                               sourceBridgeIid);
                        InstanceIdentifier<Node> destinationBridgeIid =
                                UnimgrMapper.getOvsdbBridgeNodeIid(optionalDestinationOvsdbNode.get());
                        Optional<Node> optionalDestinationBr = MdsalUtils.readNode(dataBroker,
                                                                                    LogicalDatastoreType.OPERATIONAL,
                                                                                    destinationBridgeIid);
                        //update ovsdb qos-entry and queues with max-rate to match evc ingress BW
                        OvsdbUtils.updateMaxRate(dataBroker, sourceUniAugmentation, destinationUniAugmentation, evc);
                        Node sourceBr;
                        Node destinationBr;
                        if (optionalSourceBr.isPresent() && optionalDestinationBr.isPresent()) {
                            sourceBr = optionalSourceBr.get();
                            destinationBr = optionalDestinationBr.get();
                            OvsdbUtils.createTerminationPointNode(dataBroker,
                                                                   uniSource.getAugmentation(UniAugmentation.class),
                                                                   sourceBr,
                                                                   UnimgrConstants.DEFAULT_BRIDGE_NAME,
                                                                   UnimgrConstants.DEFAULT_TUNNEL_IFACE);
                            OvsdbUtils.createGreTunnel(dataBroker,
                                                        uniSource.getAugmentation(UniAugmentation.class),
                                                        uniDestination.getAugmentation(UniAugmentation.class),
                                                        sourceBr,
                                                        UnimgrConstants.DEFAULT_BRIDGE_NAME,
                                                        UnimgrConstants.DEFAULT_GRE_TUNNEL_NAME);
                            OvsdbUtils.createTerminationPointNode(dataBroker,
                                                                   uniDestination.getAugmentation(UniAugmentation.class),
                                                                   destinationBr,
                                                                   UnimgrConstants.DEFAULT_BRIDGE_NAME,
                                                                   UnimgrConstants.DEFAULT_TUNNEL_IFACE);
                            OvsdbUtils.createGreTunnel(dataBroker,
                                                        uniDestination.getAugmentation(UniAugmentation.class),
                                                        uniSource.getAugmentation(UniAugmentation.class), destinationBr,
                                                        UnimgrConstants.DEFAULT_BRIDGE_NAME,
                                                        UnimgrConstants.DEFAULT_GRE_TUNNEL_NAME);
                            EvcUtils.updateEvcNode(LogicalDatastoreType.CONFIGURATION,
                                                      evcKey,
                                                      evc,
                                                      sourceUniIid,
                                                      destinationUniIid,
                                                      dataBroker);
                            EvcUtils.updateEvcNode(LogicalDatastoreType.OPERATIONAL,
                                                      evcKey,
                                                      evc,
                                                      sourceUniIid,
                                                      destinationUniIid,
                                                      dataBroker);
                        } else {
                            LOG.info("Unable to retrieve the source and/or destination bridge.");
                        }
                    } else {
                        LOG.info("Uname to retrieve the source and/or destination ovsdbNode.");
                    }
                } else {
                    LOG.info("Unable to retrieve the source and/or destination Uni.");
                }
            }
        }
    }

}
