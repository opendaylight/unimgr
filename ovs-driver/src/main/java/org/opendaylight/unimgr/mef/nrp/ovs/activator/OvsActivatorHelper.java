/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.activator;

import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.common.ResourceNotAvailableException;
import org.opendaylight.unimgr.mef.nrp.ovs.exception.VlanNotSetException;
import org.opendaylight.unimgr.mef.nrp.ovs.transaction.TopologyTransaction;
import org.opendaylight.unimgr.mef.nrp.ovs.util.VlanUtils;
import org.opendaylight.unimgr.utils.NullAwareDatastoreGetter;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp_interface.rev170227.nrp.create.connectivity.service.end.point.attrs.NrpCgEthFrameFlowCpaAspec;
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
    private EndPoint endPoint;
    private String tpName;
    private Map<String, String> portMap;

    private static final String CTAG_VLAN_ID_NOT_SET_ERROR_MESSAGE = "C-Tag VLAN Id not set for End Point '%s'.";
    private static final String ATTRS_NOT_SET_ERROR_MESSAGE = "End Point '%s' does not have '%s' set.";


    private static final Logger LOG = LoggerFactory.getLogger(OvsActivatorHelper.class);

    OvsActivatorHelper(TopologyTransaction topologyTransaction, EndPoint endPoint) {
        this.nodes = topologyTransaction.readNodes();
        this.endPoint = endPoint;
        tpName = getPortName(endPoint.getEndpoint().getServiceInterfacePoint().getValue());
        this.portMap = createPortMap(nodes);
    }

    /**
     * Returns VLAN Id of the service
     *
     * @return Integer with VLAN Id
     */
    int getCeVlanId() throws ResourceNotAvailableException {

        if( (endPoint.getAttrs() != null) && (endPoint.getAttrs().getNrpCgEthFrameFlowCpaAspec()!=null) ){
            NrpCgEthFrameFlowCpaAspec attr = endPoint.getAttrs().getNrpCgEthFrameFlowCpaAspec();
            if( (attr.getCeVlanIdList()!=null) && !(attr.getCeVlanIdList().getVlanIdList().isEmpty()) ){
                //for now we support only one CE VLAN
                return attr.getCeVlanIdList().getVlanIdList().get(0).getVlanId().getValue().intValue();
            } else {
                LOG.warn(String.format(CTAG_VLAN_ID_NOT_SET_ERROR_MESSAGE, tpName));
                throw new VlanNotSetException(String.format(CTAG_VLAN_ID_NOT_SET_ERROR_MESSAGE, tpName));
            }
        } else {
            String className = NrpCgEthFrameFlowCpaAspec.class.toString();
            LOG.warn(String.format(ATTRS_NOT_SET_ERROR_MESSAGE, tpName, className));
            throw new ResourceNotAvailableException(String.format(ATTRS_NOT_SET_ERROR_MESSAGE, tpName, className));
        }
    }

    /**
     * Returns VLAN Id to be used internally in OvSwitch network
     *
     * @return Integer with VLAN Id
     */
    int getInternalVlanId() throws ResourceNotAvailableException {
        VlanUtils vlanUtils = new VlanUtils(nodes);
        int serviceVlanId = getCeVlanId();

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
        return portMap.get(tpName);
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

    protected static String getPortName(String sip){
        String[] tab = sip.split(":");
        return tab[tab.length-1];
    }
}
