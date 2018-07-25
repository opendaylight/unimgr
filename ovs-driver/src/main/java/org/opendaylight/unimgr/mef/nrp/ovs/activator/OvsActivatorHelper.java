/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.activator;

import static java.util.stream.Collectors.toSet;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.common.ResourceNotAvailableException;
import org.opendaylight.unimgr.mef.nrp.ovs.transaction.TopologyTransaction;
import org.opendaylight.unimgr.mef.nrp.ovs.util.NullAwareDatastoreGetter;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev180321.carrier.eth.connectivity.end.point.resource.IngressBwpFlow;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.connectivity.service.end.point.attrs.NrpCarrierEthConnectivityEndPointResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for OvsDriver activation.
 *
 * @author jakub.niezgoda@amartus.com
 */
class OvsActivatorHelper {
    private List<NullAwareDatastoreGetter<Node>> nodes;
    private EndPoint endPoint;
    private String tpName;
    private BiMap<String, String> portMap;

    private static final String INGRESS_BWP_FLOW_NOT_SET_ERROR_MESSAGE
            = "Ingress bwp flow is not set for End Point '%s'.";
    private static final String ATTRS_NOT_SET_ERROR_MESSAGE = "End Point '%s' does not have '%s' set.";
    private static final String VLANS_DIFFERENT_ERROR_MESSAFE
            = "External VLANs defined on end points has to be the same or not defined. Current values %s";


    private static final Logger LOG = LoggerFactory.getLogger(OvsActivatorHelper.class);

    OvsActivatorHelper(TopologyTransaction topologyTransaction, EndPoint endPoint) {
        this.nodes = topologyTransaction.readNodes();
        this.endPoint = endPoint;
        tpName = getPortName(endPoint.getEndpoint().getServiceInterfacePoint().getServiceInterfacePointId().getValue());
        this.portMap = createPortMap();
    }

    private OvsActivatorHelper(EndPoint endPoint) {
        this.endPoint = endPoint;
    }

    /**
     * Returns VLAN Id of the endPoint.
     *
     * @return int with VLAN Id
     */
    Optional<Integer> getCeVlanId() throws ResourceNotAvailableException {

        if ((endPoint.getAttrs() != null)
                && (endPoint.getAttrs().getNrpCarrierEthConnectivityEndPointResource() != null)) {
            NrpCarrierEthConnectivityEndPointResource attr
                    = endPoint.getAttrs().getNrpCarrierEthConnectivityEndPointResource();
            if ((attr.getCeVlanIdListAndUntag() != null)
                    && !(attr.getCeVlanIdListAndUntag().getVlanId().isEmpty())) {
                //for now we support only one CE VLAN
                return Optional.of(attr.getCeVlanIdListAndUntag().getVlanId().get(0).getVlanId().getValue().intValue());
            } else {
                return Optional.empty(); //port-base service
            }
        } else {
            String className = NrpCarrierEthConnectivityEndPointResource.class.toString();
            LOG.warn(String.format(ATTRS_NOT_SET_ERROR_MESSAGE, tpName, className));
            throw new ResourceNotAvailableException(String.format(ATTRS_NOT_SET_ERROR_MESSAGE, tpName, className));
        }
    }

    /**
     * Returns port name in openflow plugin naming convention (e.g. openflow:1:4)
     *
     * @return String with openflow port name
     */
    String getOpenFlowPortName() {
        return portMap.get(tpName);
    }

    /**
     * Returns port name for specifiec port name in openflow convention.
     * @param openFlowPortName port in openflow plugin naming convention
     * @return String with port name
     */
    String getTpNameFromOpenFlowPortName(String openFlowPortName) {
        return portMap.inverse().get(openFlowPortName);
    }

    private BiMap<String, String> createPortMap() {
        BiMap<String, String> result = HashBiMap.create();
        for (NullAwareDatastoreGetter<Node> node : nodes) {
            if (node.get().isPresent()) {
                for (NodeConnector nodeConnector : node.get().get().getNodeConnector()) {
                    String ofName = nodeConnector.getId().getValue();
                    FlowCapableNodeConnector flowCapableNodeConnector
                            = nodeConnector.augmentation(FlowCapableNodeConnector.class);
                    String name = flowCapableNodeConnector.getName();
                    result.put(name, ofName);
                }
            }
        }
        return result;
    }

    protected static String getPortName(String sip) {
        String[] tab = sip.split(":");
        return tab[tab.length - 1];
    }

    public long getQosMinRate() throws ResourceNotAvailableException {
        IngressBwpFlow ingressBwpFlow = getIngressBwpFlow();
        if (ingressBwpFlow != null) {
            //TODO add validation
            return ingressBwpFlow.getCir().getValue();
        }

        LOG.warn(String.format(INGRESS_BWP_FLOW_NOT_SET_ERROR_MESSAGE, tpName));
        throw new ResourceNotAvailableException(String.format(INGRESS_BWP_FLOW_NOT_SET_ERROR_MESSAGE, tpName));
    }

    public long getQosMaxRate() throws ResourceNotAvailableException {

        IngressBwpFlow ingressBwpFlow = getIngressBwpFlow();
        if (ingressBwpFlow != null) {
            //TODO add validation
            return ingressBwpFlow.getCir().getValue() + ingressBwpFlow.getEir().getValue();
        }

        LOG.warn(String.format(INGRESS_BWP_FLOW_NOT_SET_ERROR_MESSAGE, tpName));
        throw new ResourceNotAvailableException(String.format(INGRESS_BWP_FLOW_NOT_SET_ERROR_MESSAGE, tpName));
    }

    private IngressBwpFlow getIngressBwpFlow() {
        if ((endPoint.getAttrs() == null)
                || (endPoint.getAttrs().getNrpCarrierEthConnectivityEndPointResource() == null)) {
            return null;
        }
        return endPoint.getAttrs().getNrpCarrierEthConnectivityEndPointResource().getIngressBwpFlow();
    }

    protected boolean isIBwpConfigured() {
        return getIngressBwpFlow() != null;
    }

    public static void validateExternalVLANs(List<EndPoint> endPoints) throws ResourceNotAvailableException {
        Set<Optional> vlans = endPoints.stream().map(ep -> {
            try {
                return new OvsActivatorHelper(ep).getCeVlanId();
            } catch (ResourceNotAvailableException e) {
                return Optional.empty();
            }
        }).filter(i -> i.isPresent()).collect(toSet());
        if (vlans.size() > 1) {
            throw new ResourceNotAvailableException(String.format(VLANS_DIFFERENT_ERROR_MESSAFE, vlans.toString()));
        }
    }
}
