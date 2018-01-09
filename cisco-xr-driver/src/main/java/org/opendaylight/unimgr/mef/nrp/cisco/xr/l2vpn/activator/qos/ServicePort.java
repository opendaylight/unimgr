package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator.qos;

import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev170712.carrier.eth.connectivity.end.point.resource.IngressBwpFlow;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev170712.carrier.eth.connectivity.end.point.resource.EgressBwpFlow;

/**
 * @author bartosz.michalik@amartus.com
 */
public class ServicePort extends org.opendaylight.unimgr.mef.nrp.common.ServicePort {
    private IngressBwpFlow ingressBwpFlow;
    private EgressBwpFlow egressBwpFlow;
    public ServicePort(org.opendaylight.unimgr.mef.nrp.common.ServicePort servicePort) {
        super(servicePort.getTopology(), servicePort.getNode(), servicePort.getTp());
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
