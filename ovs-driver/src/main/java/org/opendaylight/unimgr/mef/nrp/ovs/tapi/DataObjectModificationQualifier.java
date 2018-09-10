/*
 * Copyright (c) 2018 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.ovs.tapi;

import static org.opendaylight.unimgr.mef.nrp.ovs.util.OvsCapabilitiesService.NodeCapability.OVSDB;
import static org.opendaylight.unimgr.utils.CapabilitiesService.Capability.Mode.AND;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.unimgr.utils.CapabilitiesService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class created to classify object according to its modification types.
 *
 * @author marek.ryznar@amartus.com
 */
public class DataObjectModificationQualifier {
    private static final Logger LOG = LoggerFactory.getLogger(DataObjectModificationQualifier.class);
    private CapabilitiesService capabilitiesService;

    public DataObjectModificationQualifier(DataBroker dataBroker) {
        capabilitiesService = new CapabilitiesService(dataBroker);
    }

    private Function<Node,Boolean> isOvs = node -> capabilitiesService.node(node).isSupporting(AND, OVSDB);

    protected void checkNodes(List<DataObjectModification<? extends DataObject>> nodes,
                              Map<TerminationPoint,String> toAddMap,
                              Map<TerminationPoint,String> toUpdateMap, Map<TerminationPoint,String> toDeleteMap) {
        Node n;
        for (DataObjectModification<?> node: nodes) {
            switch (node.getModificationType()) {
                //new ovs node
                case WRITE :
                    n = (Node) node.getDataAfter();
                    if (!isOvs.apply(n) || n.getTerminationPoint() == null) {
                        break;
                    }
                    String bn1 = n.augmentation(OvsdbBridgeAugmentation.class).getBridgeName().getValue();
                    n.getTerminationPoint().forEach(tp -> toAddMap.put(tp,bn1));
                break;
                case SUBTREE_MODIFIED:
                    checkTerminationPoints(node, toAddMap, toUpdateMap, toDeleteMap);
                break;
                //whole ovs-node eg. s1 deleted
                case DELETE:
                    n = (Node) node.getDataBefore();
                    if (!isOvs.apply(n) || n.getTerminationPoint() == null) {
                        break;
                    }
                    String bn2 = n.augmentation(OvsdbBridgeAugmentation.class).getBridgeName().getValue();
                    n.getTerminationPoint().forEach(tp -> toDeleteMap.put(tp,bn2));
                break;
                default:
                    LOG.debug("Not supported modification type: {}",node.getModificationType());
                break;
            }
        }
    }

    private void checkTerminationPoints(DataObjectModification<?> node,
                                        Map<TerminationPoint,String> toAddMap,
                                        Map<TerminationPoint,String> toUpdateMap,
                                        Map<TerminationPoint,String> toDeleteMap) {
        Node n = (Node) node.getDataAfter();
        if (!isOvs.apply(n)) {
            return ;
        }
        String bridgeName = n.augmentation(OvsdbBridgeAugmentation.class).getBridgeName().getValue();
        Collection<? extends DataObjectModification<? extends DataObject>> modifiedChildren = node.getModifiedChildren();

        TerminationPoint terminationPoint;
        for (DataObjectModification<?> tp: modifiedChildren) {
            if (!tp.getDataType().equals(TerminationPoint.class)) {
                continue;
            }
            switch (tp.getModificationType()) {
                //new port added eg. s1-eth7
                case WRITE:
                    terminationPoint = (TerminationPoint) tp.getDataAfter();
                    toAddMap.put(terminationPoint,bridgeName);
                break;
                case SUBTREE_MODIFIED:
                    terminationPoint = (TerminationPoint) tp.getDataAfter();
                    if (!tp.getDataBefore().equals(tp.getDataAfter())) {
                        toUpdateMap.put(terminationPoint,bridgeName);
                    }
                break;
                case DELETE:
                    terminationPoint = (TerminationPoint) tp.getDataBefore();
                    toDeleteMap.put(terminationPoint,bridgeName);
                break;
                default:
                    LOG.debug("Not supported modification type: SUBTREE_MODIFIED.{}",tp.getModificationType());
                break;
            }
        }
    }
}
