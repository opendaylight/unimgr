/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.activator;

import org.opendaylight.unimgr.mef.nrp.common.ResourceNotAvailableException;
import org.opendaylight.unimgr.mef.nrp.ovs.exception.VlanNotSetException;
import org.opendaylight.unimgr.mef.nrp.ovs.transaction.TopologyTransaction;
import org.opendaylight.unimgr.mef.nrp.ovs.util.VlanUtils;
import org.opendaylight.unimgr.utils.NullAwareDatastoreGetter;
import org.opendaylight.yang.gen.v1.urn.mef.unimgr.ext.rev160725.FcPort1;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for OvsDriver activation.
 *
 * @author jakub.niezgoda@amartus.com
 */
class OvsActivatorHelper {
    private List<NullAwareDatastoreGetter<Node>> nodes;
    private FcPort flowPoint;
    private Map<String, String> portMap;

    private static final String CTAG_VLAN_ID_NOT_SET_ERROR_MESSAGE = "C-Tag VLAN Id not set for termination point '%s'.";
    private static final String FC_PORT_NOT_AUGMENTED_ERROR_MESSAGE = "Forwarding Construct port '%s' does not have '%s' augmentation.";

    private static final Logger LOG = LoggerFactory.getLogger(OvsActivatorHelper.class);

    OvsActivatorHelper(TopologyTransaction topologyTransaction, FcPort flowPoint) {
        this.nodes = topologyTransaction.readNodes();
        this.flowPoint = flowPoint;
        this.portMap = createPortMap(nodes);
    }

    /**
     * Returns VLAN Id of the service
     *
     * @return Integer with VLAN Id
     */
    int getServiceVlanId() throws ResourceNotAvailableException {
        FcPort1 fcPort1 = flowPoint.getAugmentation(FcPort1.class);
        String tpName = flowPoint.getTp().getValue();

        if (fcPort1 != null) {
            if (fcPort1.getCTagVlanId() != null) {
                return fcPort1.getCTagVlanId().getValue().intValue();
            } else {
                LOG.warn(String.format(CTAG_VLAN_ID_NOT_SET_ERROR_MESSAGE, tpName));
                throw new VlanNotSetException(String.format(CTAG_VLAN_ID_NOT_SET_ERROR_MESSAGE, tpName));
            }
        } else {
            String fcPort1ClassName = FcPort1.class.toString();
            LOG.warn(String.format(FC_PORT_NOT_AUGMENTED_ERROR_MESSAGE, tpName, fcPort1ClassName));
            throw new ResourceNotAvailableException(String.format(FC_PORT_NOT_AUGMENTED_ERROR_MESSAGE, tpName, fcPort1ClassName));
        }
    }

    /**
     * Returns VLAN Id to be used internally in OvSwitch network
     *
     * @return Integer with VLAN Id
     */
    int getInternalVlanId() throws ResourceNotAvailableException {
        VlanUtils vlanUtils = new VlanUtils(nodes);
        int serviceVlanId = getServiceVlanId();

        if (vlanUtils.isVlanInUse(serviceVlanId)) {
            LOG.debug("VLAN ID = '" + serviceVlanId + "' already in use.");
            return vlanUtils.generateVlanID();
        } else {
            LOG.debug("VLAN ID = '" + serviceVlanId + "' not in use.");
            return serviceVlanId;
        }
    }

    /**
     * Returns port name in openflow plugin convention (e.g. openflow:1:4)
     *
     * @return String with port name
     */
    String getOpenFlowPortName() {
        return portMap.get(flowPoint.getTp().getValue());
    }

    private Map<String, String> createPortMap(List<NullAwareDatastoreGetter<Node>> nodes) {
        Map<String, String> portMap = new HashMap<>();
        for (NullAwareDatastoreGetter<Node> node : nodes) {
            if (node.get().isPresent()){
                for (NodeConnector nodeConnector : node.get().get().getNodeConnector()) {
                    String ofName = nodeConnector.getId().getValue();
                    FlowCapableNodeConnector flowCapableNodeConnector = nodeConnector.getAugmentation(FlowCapableNodeConnector.class);
                    String name = flowCapableNodeConnector.getName();
                    portMap.put(name, ofName);
                }
            }
        }
        return portMap;
    }
}
