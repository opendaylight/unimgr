/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.command;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.AbstractCommand;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.unimgr.utils.OvsdbUtils;
import org.opendaylight.unimgr.utils.UniUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class OvsNodeAddCommand extends AbstractCommand<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(OvsNodeAddCommand.class);

    public OvsNodeAddCommand(final DataBroker dataBroker, final DataTreeModification<Node> newOvsNode) {
        super(dataBroker, newOvsNode);
    }

    @Override
    public void execute() {
        final Node ovsNode = dataObject.getRootNode().getDataAfter();
        final OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsNode.getAugmentation(OvsdbNodeAugmentation.class);
        final InstanceIdentifier<Node> ovsdbIid = dataObject.getRootPath().getRootIdentifier();
        if (ovsdbNodeAugmentation != null) {
            LOG.info("Received an OVSDB node create {}",
                    ovsdbNodeAugmentation.getConnectionInfo()
                                         .getRemoteIp()
                                         .getIpv4Address()
                                         .getValue());
            final List<Node> uniNodes = UniUtils.getUniNodes(dataBroker);
            if (uniNodes != null && !uniNodes.isEmpty()) {
                for (final Node uniNode: uniNodes) {
                    final UniAugmentation uniAugmentation = uniNode.getAugmentation(UniAugmentation.class);
                    if (uniAugmentation.getOvsdbNodeRef() != null
                            && uniAugmentation.getOvsdbNodeRef().getValue() != null) {
                        final InstanceIdentifier<Node> ovsdbNodeRefIid = uniAugmentation
                                                                    .getOvsdbNodeRef()
                                                                    .getValue()
                                                                    .firstIdentifierOf(Node.class);
                        if (ovsdbNodeRefIid.equals(ovsdbIid)) {
                            final Optional<Node> optionalOvsdbNode = MdsalUtils.readNode(dataBroker,
                                                                                    LogicalDatastoreType.OPERATIONAL,
                                                                                    ovsdbIid);
                            if (optionalOvsdbNode.isPresent()) {
                                final InstanceIdentifier<Node> uniIid =
                                                            UnimgrMapper.getUniIid(dataBroker,
                                                                                   uniAugmentation.getIpAddress(),
                                                                                   LogicalDatastoreType.CONFIGURATION);
                                // Update QoS entries to ovsdb if speed is configured to UNI node
                                if (uniAugmentation.getSpeed() != null) {
                                    OvsdbUtils.createQoSForOvsdbNode(dataBroker, uniAugmentation);
                                }
                                OvsdbUtils.createBridgeNode(dataBroker,
                                                             ovsdbIid,
                                                             uniAugmentation,
                                                             UnimgrConstants.DEFAULT_BRIDGE_NAME);
                                UniUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL,
                                                          uniIid,
                                                          uniAugmentation,
                                                          ovsdbIid,
                                                          dataBroker);
                            }
                        }
                    } else if (ovsdbNodeAugmentation
                                  .getConnectionInfo()
                                  .getRemoteIp()
                                  .equals(uniAugmentation.getIpAddress())) {
                        final InstanceIdentifier<Node> uniIid = UnimgrMapper.getUniIid(dataBroker,
                                                                                 uniAugmentation.getIpAddress(),
                                                                                 LogicalDatastoreType.CONFIGURATION);
                        OvsdbUtils.createBridgeNode(dataBroker,
                                                     ovsdbIid,
                                                     uniAugmentation,
                                                     UnimgrConstants.DEFAULT_BRIDGE_NAME);
                        UniUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL,
                                                  uniIid,
                                                  uniAugmentation,
                                                  ovsdbIid,
                                                  dataBroker);
                    }
                }
            } else {
                LOG.info("Received a new OVSDB node connection from {}"
                        + ovsdbNodeAugmentation.getConnectionInfo()
                                .getRemoteIp().getIpv4Address());
            }
        }
    }

}
