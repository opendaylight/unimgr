/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.command;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.AbstractCommand;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.unimgr.utils.OvsdbUtils;
import org.opendaylight.unimgr.utils.UniUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class UniUpdateCommand extends AbstractCommand<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(UniUpdateCommand.class);

    public UniUpdateCommand(final DataBroker dataBroker, final DataTreeModification<Node> updatedUniNode) {
        super(dataBroker, updatedUniNode);
    }

    @Override
    public void execute() {
        final UniAugmentation updatedUni =
                dataObject.getRootNode().getDataAfter().getAugmentation(UniAugmentation.class);
        final UniAugmentation formerUni =
                dataObject.getRootNode().getDataBefore().getAugmentation(UniAugmentation.class);
        if (formerUni != null) {
            final String formerUniIp = formerUni.getIpAddress().getIpv4Address().getValue();
            final String updatedUniIp = updatedUni.getIpAddress().getIpv4Address().getValue();
            final InstanceIdentifier<?> updatedUniIid = dataObject.getRootPath().getRootIdentifier();
            final String uniKey = updatedUniIid.firstKeyOf(Node.class).toString();
            Preconditions.checkArgument(formerUniIp.equals(updatedUniIp),
                    "Can't update UNI with a different IP address. Former IP was %s"
                            + "Updated IP is %s. Please create a UNI instead of updating this one %s",
                            formerUniIp, updatedUniIp, uniKey);
            Node ovsdbNode;
            if (updatedUni.getOvsdbNodeRef() != null) {
                LOG.info("OVSDB NODE ref retreive for updated UNI {}", updatedUni.getOvsdbNodeRef());
                final OvsdbNodeRef ovsdbNodeRef = updatedUni.getOvsdbNodeRef();
                final Optional<Node> optOvsdbNode =
                        MdsalUtils.readNode(dataBroker,LogicalDatastoreType.OPERATIONAL, ovsdbNodeRef.getValue());
                if (optOvsdbNode.isPresent()) {
                    ovsdbNode = optOvsdbNode.get();
                    LOG.info("Retrieved the OVSDB node {}", ovsdbNode.getNodeId());
                    // Update QoS entries to ovsdb if speed is configured to UNI node
                    if (updatedUni.getSpeed() != null) {
                        OvsdbUtils.createQoSForOvsdbNode(dataBroker, updatedUni);
                    }
                    UniUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL, updatedUniIid, updatedUni,
                                           ovsdbNode, dataBroker);
                }  else {
                    // This should never happen, because on creation,
                    // the UNI is assigned and OVSDB node
                    LOG.error("OVSDB node not found for UNI {}, but got OVSDB ref {}", uniKey,
                            updatedUni.getOvsdbNodeRef());
                    return;
                }
            } else {
                final Optional<Node> optOvsdbNode = OvsdbUtils.findOvsdbNode(dataBroker, updatedUni);
                if (optOvsdbNode.isPresent()) {
                    ovsdbNode = optOvsdbNode.get();
                    LOG.info("Retrieved the OVSDB node {}", ovsdbNode.getNodeId());
                    // Update QoS entries to ovsdb if speed is configured to UNI node
                    if (updatedUni.getSpeed() != null) {
                        OvsdbUtils.createQoSForOvsdbNode(dataBroker, updatedUni);
                    }
                    UniUtils.updateUniNode(LogicalDatastoreType.OPERATIONAL, updatedUniIid, updatedUni,
                                           ovsdbNode, dataBroker);
                } else {
                    // This should never happen, because on creation,
                    // the UNI is assigned and OVSDB node
                    LOG.error("OVSDB node not found for UNI {}", uniKey);
                    return;
                }
            }
            LOG.info("UNI {} updated", uniKey);
        }
    }
}
