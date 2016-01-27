/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.command;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.unimgr.impl.UnimgrUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Optional;

public class UniUpdateCommand extends AbstractUpdateCommand {

    private static final Logger LOG = LoggerFactory.getLogger(UniUpdateCommand.class);

    public UniUpdateCommand(DataBroker dataBroker,
            Map<InstanceIdentifier<?>, DataObject> changes) {
        super.dataBroker = dataBroker;
        super.changes = changes;
    }

    @Override
    public void execute() {
        for (final Entry<InstanceIdentifier<?>, DataObject> updated : changes.entrySet()) {
            if (updated.getValue() != null && updated.getValue() instanceof UniAugmentation) {
                final UniAugmentation uni = (UniAugmentation) updated.getValue();
                final InstanceIdentifier<?> uniIID = UnimgrMapper.getUniIid(dataBroker,
                        uni.getIpAddress(), LogicalDatastoreType.OPERATIONAL);
                final UniAugmentation uniAug = UnimgrUtils.getUni(dataBroker, LogicalDatastoreType.OPERATIONAL, uni.getIpAddress());
                Preconditions.checkArgument(uniAug != null, "No UNI %s on OPERATION Topoligy",
                        uni.getIpAddress().getIpv4Address().getValue());
                Preconditions.checkArgument(uniAug.getIpAddress().getIpv4Address().getValue().equals(
                        uni.getIpAddress().getIpv4Address().getValue()), "New IpAddress %s should be the same than %s",
                        uniAug.getIpAddress().getIpv4Address().getValue(),
                        uni.getIpAddress().getIpv4Address().getValue());
                Node ovsdbNode;
                if (uni.getOvsdbNodeRef() != null) {
                    final OvsdbNodeRef ovsdbNodeRef = uni.getOvsdbNodeRef();
                    final Optional<Node> ovsdbNodeIID = UnimgrUtils.readNode(dataBroker,
                            LogicalDatastoreType.OPERATIONAL, ovsdbNodeRef.getValue());
                    if(ovsdbNodeIID.isPresent()){
                        ovsdbNode= ovsdbNodeIID.get();
                    } else {
                        final Optional<Node> optionalOvsdbNode = UnimgrUtils.findOvsdbNode(dataBroker, uni);
                        if (optionalOvsdbNode.isPresent()) {
                            ovsdbNode = optionalOvsdbNode.get();
                        } else {
                            ovsdbNode = UnimgrUtils.createOvsdbNode(dataBroker, uni);
                        }
                    }
                    LOG.trace("UNI updated {}.", uni.getIpAddress().getIpv4Address());
                } else {
                    final Optional<Node> optionalOvsdbNode = UnimgrUtils.findOvsdbNode(dataBroker, uni);
                    if (optionalOvsdbNode.isPresent()) {
                        ovsdbNode = optionalOvsdbNode.get();
                    } else {
                        ovsdbNode = UnimgrUtils.createOvsdbNode(dataBroker, uni);
                    }
                }
                UnimgrUtils.deleteNode(dataBroker, uniIID, LogicalDatastoreType.OPERATIONAL);
                UnimgrUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL, uniIID,
                        uni, ovsdbNode, dataBroker);
            }
//            if (updated.getValue() != null && updated.getValue() instanceof OvsdbNodeAugmentation) {
//                final OvsdbNodeAugmentation ovsdbNodeAugmentation = (OvsdbNodeAugmentation) updated
//                        .getValue();
//                final InstanceIdentifier<Node> ovsdbIID = updated.getKey().firstIdentifierOf(Node.class);
//
//                if (ovsdbNodeAugmentation != null) {
//                    final IpAddress ipAddress = ovsdbNodeAugmentation.getConnectionInfo().getRemoteIp();
//                    LOG.trace("Received an OVSDB node to update {}",  ipAddress.getIpv4Address().getValue());
//                    final List<ManagedNodeEntry> managedNodeEntries = ovsdbNodeAugmentation.getManagedNodeEntry();
//
//                    if (managedNodeEntries != null) {
//                        for (final ManagedNodeEntry managedNodeEntry : managedNodeEntries) {
//                            LOG.trace("Received an update from an OVSDB node {}.", managedNodeEntry.getKey());
//
//                            //final InstanceIdentifier<Node> ovsdbIID = UnimgrMapper.getOvsdbNodeIid(ipAddress);//.firstIdentifierOf(Node.class);
//                            final InstanceIdentifier<Node> uniIID = UnimgrMapper.getUniIid(
//                                    dataBroker, ipAddress, LogicalDatastoreType.OPERATIONAL);
//                            final Optional<Node> ovsdbOptionalNode = UnimgrUtils.readNode(
//                                    dataBroker, LogicalDatastoreType.OPERATIONAL, ovsdbIID);
//
//                            if(ovsdbOptionalNode.isPresent()){
//                                final Node ovsdbNode = ovsdbOptionalNode.get();
//                                final UniAugmentation uni = UnimgrUtils.getUni(dataBroker, LogicalDatastoreType.CONFIGURATION, ipAddress);
//                                UnimgrUtils.deleteNode(dataBroker, ovsdbIID, LogicalDatastoreType.OPERATIONAL);
//                                UnimgrUtils.createOvsdbNode(dataBroker, ovsdbNode.getNodeId(), uni);
//                            }
//                        }
//                    }
//                }
//            }
        }
    }
}
