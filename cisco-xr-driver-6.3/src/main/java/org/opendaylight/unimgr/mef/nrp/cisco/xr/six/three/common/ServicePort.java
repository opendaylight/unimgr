/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.six.three.common;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.six.three.common.util.SipHandler;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev180321.carrier.eth.connectivity.end.point.resource.EgressBwpFlow;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev180321.carrier.eth.connectivity.end.point.resource.IngressBwpFlow;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.connectivity.service.end.point.attrs.NrpCarrierEthConnectivityEndPointResource;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;



/**
 * Class representing port (replacement for FcPort).
 *
 * @author marek.ryznar@amartus.com
 */
public class ServicePort {
    private static final String PATTERN = ".+?(?=((((\\d+)/)+)\\d+))";
    private static final Pattern INTERFACE_NAME_PATTERN = Pattern.compile(PATTERN);
    private static final String DEFAULT_INTERFACE_NAME = "GigabitEthernet";

    private IngressBwpFlow ingressBwpFlow;
    private EgressBwpFlow egressBwpFlow;

    //netconf topology
    private final TopologyId topoId;
    //represents device ie dev-68 in netconf topology
    private final  NodeId nodeId;
    //defines port
    private final TpId tpId;
    //defines cTag VLAN ID
    private Long vlanId = null;

    public ServicePort(TopologyId topoId, NodeId nodeId, TpId tpId) {
        this.topoId = topoId;
        this.nodeId = nodeId;
        this.tpId = tpId;
    }

    public TopologyId getTopology() {
        return topoId;
    }


    public NodeId getNode() {
        return nodeId;
    }

    public TpId getTp() {
        return tpId;
    }


    public Long getVlanId() {
        return vlanId;
    }

    public void setVlanId(Long vlanId) {
        this.vlanId = vlanId;
    }

    public static ServicePort toServicePort(EndPoint endPoint, String topologyName) {
        return toServicePort(endPoint, new TopologyId(topologyName));
    }

    public static ServicePort toServicePort(EndPoint endPoint, TopologyId topologyId) {
        Uuid sip = endPoint.getEndpoint().getServiceInterfacePoint().getServiceInterfacePointId();
        NodeId nodeId = new NodeId(SipHandler.getDeviceName(sip));
        TpId tpId = new TpId(SipHandler.getPortName(sip));
        ServicePort servicePort = new ServicePort(topologyId,nodeId,tpId);
        if (hasVlan(endPoint)) {
            servicePort.setVlanId((long) getVlan(endPoint));
        }
        return servicePort;
    }

    public static boolean hasVlan(EndPoint endPoint) {
        if ((endPoint.getAttrs() != null)
                && (endPoint.getAttrs().getNrpCarrierEthConnectivityEndPointResource() != null)) {
            NrpCarrierEthConnectivityEndPointResource attr =
                    endPoint.getAttrs().getNrpCarrierEthConnectivityEndPointResource();
            if ((attr.getCeVlanIdListAndUntag() != null)
                    && !(attr.getCeVlanIdListAndUntag().getVlanId().isEmpty())) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private static int getVlan(EndPoint endPoint) {
        return endPoint.getAttrs().getNrpCarrierEthConnectivityEndPointResource()
                .getCeVlanIdListAndUntag().getVlanId().get(0).getVlanId().getValue().intValue();
    }

    public static boolean isSameDevice(EndPoint endPoint, List<String> ls) {
        Uuid sip = endPoint.getEndpoint().getServiceInterfacePoint().getServiceInterfacePointId();
        NodeId nodeId = new NodeId(SipHandler.getDeviceName(sip));

        if (ls.size() == 0) {
            ls.add(nodeId.getValue());
        } else if (ls.size() > 0) {
            List<String> listWithoutDuplicates =
                    ls.stream().distinct().collect(Collectors.toList());

            java.util.Optional<String> preset = listWithoutDuplicates.stream()
                    .filter(x -> x.equals(nodeId.getValue())).findFirst();

            if (preset.isPresent()) {
                return true;
            }
            ls.add(nodeId.getValue());
        }

        return false;
    }

    public String getInterfaceName() {
       // TpId tpId = this.getTp();
        Matcher matcher = INTERFACE_NAME_PATTERN.matcher(this.getTp().getValue());
        return matcher.find() ? matcher.group() : DEFAULT_INTERFACE_NAME;
    }

    public IngressBwpFlow getIngressBwpFlow() {
        return ingressBwpFlow;
    }

    public void setIngressBwpFlow(IngressBwpFlow ingressBwpFlow) {
        this.ingressBwpFlow = ingressBwpFlow;
    }

    public EgressBwpFlow getEgressBwpFlow() {
        return egressBwpFlow;
    }

    public void setEgressBwpFlow(EgressBwpFlow egressBwpFlow) {
        this.egressBwpFlow = egressBwpFlow;
    }
}
