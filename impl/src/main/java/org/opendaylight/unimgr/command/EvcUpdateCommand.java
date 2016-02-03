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
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.unimgr.impl.UnimgrUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class EvcUpdateCommand extends AbstractUpdateCommand {

    private static final Logger LOG = LoggerFactory.getLogger(EvcUpdateCommand.class);

    public EvcUpdateCommand(final DataBroker dataBroker,
            final Map<InstanceIdentifier<?>, DataObject> changes) {
        super.dataBroker = dataBroker;
        super.changes = changes;
    }

    @Override
    public void execute() {
        for (final Entry<InstanceIdentifier<?>, DataObject> updated : changes.entrySet()) {
            if ((updated.getValue() != null) && (updated.getValue() instanceof EvcAugmentation)) {
                final EvcAugmentation evc = (EvcAugmentation) updated.getValue();
                final InstanceIdentifier<?> evcKey = updated.getKey();

                // FIXME: For now, we assume that there is 1 uni per
                // source/destination
                if ((evc.getUniDest() == null) || evc.getUniDest().isEmpty()) {
                    LOG.error("Destination UNI cannot be null.");
                    break;
                }
                if ((evc.getUniSource() == null) || evc.getUniSource().isEmpty()) {
                    LOG.error("Source UNI cannot be null.");
                    break;
                }

                final Ipv4Address laterUni1Ip = evc.getUniSource().iterator().next().getIpAddress().getIpv4Address();
                final Ipv4Address laterUni2Ip = evc.getUniDest().iterator().next().getIpAddress().getIpv4Address();
                LOG.trace("New EVC created, source IP: {} destination IP {}.", laterUni1Ip, laterUni2Ip);


                final ReadTransaction readTransac = dataBroker.newReadOnlyTransaction();
                final CheckedFuture<?, ReadFailedException> retFormerEvc = readTransac.read(LogicalDatastoreType.OPERATIONAL, evcKey);
                EvcAugmentation formerEvc;
                try {
                    formerEvc = (EvcAugmentation) ((Optional<EvcAugmentation>) retFormerEvc.checkedGet()).get();
                    final Ipv4Address formerUni1ip = formerEvc.getUniDest().iterator().next().getIpAddress().getIpv4Address();
                    final Ipv4Address formerUni2ip = formerEvc.getUniDest().iterator().next().getIpAddress().getIpv4Address();

                    if (formerUni1ip.equals(laterUni1Ip)) {
                        // do nothing
                    } else if (formerUni1ip.equals(laterUni2Ip)) {
                        // do nothing
                    } else {
                        LOG.info("{} is not part of the EVC, removing configuration", formerUni1ip);
                        final InstanceIdentifier<?> formerUniIID = UnimgrMapper.getUniIid(dataBroker, new IpAddress(formerUni1ip), LogicalDatastoreType.OPERATIONAL);
                        final Optional<Node> formerUni = UnimgrUtils.readNode(dataBroker, LogicalDatastoreType.OPERATIONAL, formerUniIID);
                        UnimgrUtils.deleteEvcData(dataBroker, formerUni);
                    }
                    if (formerUni2ip.equals(laterUni1Ip)) {
                        // do nothing
                    } else if (formerUni2ip.equals(laterUni2Ip)) {
                        // do nothing
                    } else {
                        LOG.info("{} is not part of the EVC, removing configuration", formerUni1ip);
                        final InstanceIdentifier<?> formerUniIID = UnimgrMapper.getUniIid(dataBroker, new IpAddress(formerUni1ip), LogicalDatastoreType.OPERATIONAL);
                        final Optional<Node> formerUni = UnimgrUtils.readNode(dataBroker, LogicalDatastoreType.OPERATIONAL, formerUniIID);
                        UnimgrUtils.deleteEvcData(dataBroker, formerUni);
                        }
                } catch (ReadFailedException e) {
                    LOG.error("Failed to retrieve former EVC {}", evcKey, e);
                }

                InstanceIdentifier<Node> sourceUniIid;
                InstanceIdentifier<Node> destinationUniIid;

                final InstanceIdentifier<?> iidSource = evc.getUniSource().iterator().next().getUni();
                if (iidSource != null) {
                    sourceUniIid = iidSource.firstIdentifierOf(Node.class);
                } else {
                    sourceUniIid = UnimgrMapper.getUniIid(dataBroker,
                            evc.getUniSource().iterator().next().getIpAddress(),
                            LogicalDatastoreType.OPERATIONAL);
                }
                final InstanceIdentifier<?> iidDest = evc.getUniDest().iterator().next().getUni();
                if (iidDest != null) {
                    destinationUniIid = iidDest.firstIdentifierOf(Node.class);
                } else {
                    destinationUniIid = UnimgrMapper.getUniIid(dataBroker,
                            evc.getUniDest().iterator().next().getIpAddress(),
                            LogicalDatastoreType.OPERATIONAL);
                }

                // Retrieve the source and destination UNIs
                final Optional<Node> optionalUniSource = UnimgrUtils.readNode(dataBroker,
                        LogicalDatastoreType.OPERATIONAL,
                        sourceUniIid);
                final Optional<Node> optionalUniDestination = UnimgrUtils.readNode(dataBroker,
                        LogicalDatastoreType.OPERATIONAL,
                        destinationUniIid);

                Node uniSource = null;
                Node uniDestination = null;

                if (optionalUniSource.isPresent() && optionalUniDestination.isPresent()) {
                    uniSource = optionalUniSource.get();
                    uniDestination = optionalUniDestination.get();

                    // Retrieve the source and/or destination OVSDB node
                    final UniAugmentation sourceUniAugmentation =
                            uniSource.getAugmentation(UniAugmentation.class);
                    final UniAugmentation destinationUniAugmentation =
                            uniDestination.getAugmentation(UniAugmentation.class);
                    final Optional<Node> optionalSourceOvsdbNode =
                            UnimgrUtils.readNode(dataBroker,
                                    LogicalDatastoreType.OPERATIONAL,
                                    sourceUniAugmentation
                                    .getOvsdbNodeRef()
                                    .getValue());
                    final Optional<Node> optionalDestinationOvsdbNode =
                            UnimgrUtils.readNode(dataBroker,
                                    LogicalDatastoreType.OPERATIONAL,
                                    destinationUniAugmentation
                                    .getOvsdbNodeRef()
                                    .getValue());
                    if (optionalSourceOvsdbNode.isPresent() && optionalDestinationOvsdbNode.isPresent()) {
                        // Retrieve the source and/or destination bridge
                        final InstanceIdentifier<Node> sourceBridgeIid =
                                UnimgrMapper.getOvsdbBridgeNodeIid(optionalSourceOvsdbNode.get());
                        final Optional<Node> optionalSourceBr = UnimgrUtils.readNode(dataBroker,
                                LogicalDatastoreType.OPERATIONAL,
                                sourceBridgeIid);
                        final InstanceIdentifier<Node> destinationBridgeIid =
                                UnimgrMapper.getOvsdbBridgeNodeIid(optionalDestinationOvsdbNode.get());
                        final Optional<Node> optionalDestinationBr = UnimgrUtils.readNode(dataBroker,
                                LogicalDatastoreType.OPERATIONAL,
                                destinationBridgeIid);
                        //update ovsdb qos-entry and queues with max-rate to match evc ingress BW
                        UnimgrUtils.updateMaxRate(dataBroker, sourceUniAugmentation, destinationUniAugmentation, evc);
                        Node sourceBr = null;
                        Node destinationBr = null;
                        if (optionalSourceBr.isPresent() && optionalDestinationBr.isPresent()) {
                            sourceBr = optionalSourceBr.get();
                            destinationBr = optionalDestinationBr.get();

                            // Creating termination points (OVSDB CONFIG
                            // datastore)
                            UnimgrUtils.createTerminationPointNode(dataBroker,
                                    uniSource.getAugmentation(UniAugmentation.class),
                                    sourceBr,
                                    UnimgrConstants.DEFAULT_BRIDGE_NAME,
                                    UnimgrConstants.DEFAULT_TUNNEL_IFACE);

                            // Create GRE tunnel (OVSDB CONFIG datastore)
                            UnimgrUtils.createGreTunnel(dataBroker,
                                    uniSource.getAugmentation(UniAugmentation.class),
                                    uniDestination.getAugmentation(UniAugmentation.class),
                                    sourceBr,
                                    UnimgrConstants.DEFAULT_BRIDGE_NAME,
                                    UnimgrConstants.DEFAULT_GRE_TUNNEL_NAME);

                            // Create termination points (CONFIG datastore)
                            UnimgrUtils.createTerminationPointNode(dataBroker,
                                    uniSource.getAugmentation(UniAugmentation.class),
                                    destinationBr,
                                    UnimgrConstants.DEFAULT_BRIDGE_NAME,
                                    UnimgrConstants.DEFAULT_TUNNEL_IFACE);

                            // Create GRE tunnel (OVSDB CONFIG datastore)
                            UnimgrUtils.createGreTunnel(dataBroker,
                                    uniDestination.getAugmentation(UniAugmentation.class),
                                    uniSource.getAugmentation(UniAugmentation.class), destinationBr,
                                    UnimgrConstants.DEFAULT_BRIDGE_NAME,
                                    UnimgrConstants.DEFAULT_GRE_TUNNEL_NAME);

                            // Update EVC
                            UnimgrUtils.updateEvcNode(LogicalDatastoreType.CONFIGURATION,
                                    evcKey,
                                    evc,
                                    sourceUniIid,
                                    destinationUniIid,
                                    dataBroker);
                            UnimgrUtils.updateEvcNode(LogicalDatastoreType.OPERATIONAL,
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
