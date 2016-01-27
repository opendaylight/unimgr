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
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.unimgr.impl.UnimgrUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        for (final Entry<InstanceIdentifier<?>, DataObject> created : changes.entrySet()) {
            if (created.getValue() != null && created.getValue() instanceof OvsdbNodeAugmentation) {
                final OvsdbNodeAugmentation ovsdbNodeAugmentation = (OvsdbNodeAugmentation) created
                        .getValue();
                final InstanceIdentifier<Node> ovsdbIid = created.getKey()
                        .firstIdentifierOf(Node.class);
                if (ovsdbNodeAugmentation != null) {
                    LOG.trace("Received an OVSDB node create {}",
                            ovsdbNodeAugmentation.getConnectionInfo()
                                    .getRemoteIp().getIpv4Address().getValue());
                    final List<ManagedNodeEntry> managedNodeEntries = ovsdbNodeAugmentation
                            .getManagedNodeEntry();
                    if (managedNodeEntries != null) {
                        for (final ManagedNodeEntry managedNodeEntry : managedNodeEntries) {
                            LOG.trace("Received an update from an OVSDB node {}.",
                                    managedNodeEntry.getKey());
                            // We received a node update from the southbound plugin
                            // so we have to check if it belongs to the UNI
                            final InstanceIdentifier<Node> bridgeIid = managedNodeEntry.
                                    getBridgeRef().getValue().firstIdentifierOf(Node.class);
                            final Optional<Node> optNode = UnimgrUtils.readNode(dataBroker,
                                    LogicalDatastoreType.OPERATIONAL, bridgeIid);
                            if(optNode.isPresent()){
                                final Node bridgeNode = optNode.get();
                                final InstanceIdentifier<TerminationPoint> iidGreTermPoint =
                                        UnimgrMapper.getTerminationPointIid(bridgeNode,
                                                UnimgrConstants.DEFAULT_GRE_TUNNEL_NAME);
                                if (iidGreTermPoint.equals(ovsdbIid)){
                                    updateUni(ovsdbIid, iidGreTermPoint);
                                }
                                final InstanceIdentifier<TerminationPoint> iidEthTermPoint =
                                        UnimgrMapper.getTerminationPointIid(bridgeNode,
                                                UnimgrConstants.DEFAULT_TUNNEL_IFACE);

                                if (iidEthTermPoint.equals(ovsdbIid)){
                                    updateUni(ovsdbIid, iidEthTermPoint);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateUni(InstanceIdentifier<Node> ovsdbIid, InstanceIdentifier<TerminationPoint> iid){
        final Optional<Node> optionalOvsdbNode = UnimgrUtils.readNode(dataBroker,
                LogicalDatastoreType.OPERATIONAL, ovsdbIid);
        if(optionalOvsdbNode.isPresent()){
            final Node node = optionalOvsdbNode.get();
            final UniAugmentation uni = node.getAugmentation(UniAugmentation.class);
//            UniAugmentation uniTerminationPoint =
//                    UnimgrUtils.getUni(dataBroker, LogicalDatastoreType.CONFIGURATION, uni.getIpAddress());
            // TODO
//            final LogicalDatastoreType dataStore = LogicalDatastoreType.CONFIGURATION;
//            final InstanceIdentifier<?> uniKey;
//            final UniAugmentation uni;
//            final InstanceIdentifier<?> ovsdbNodeIid;
//            UnimgrUtils.updateUniNode(dataStore, uniKey, uni, ovsdbNodeIid, dataBroker);
        }

    }

}
