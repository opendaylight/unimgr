/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator;

import static org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort.toServicePort;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivator;
import org.opendaylight.unimgr.utils.NetconfConstants;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev161215.PolicyManager;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.Database;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.XconnectGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.XconnectGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.XconnectGroupKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.P2pXconnects;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnect;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnectKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.Pseudowires;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.connectivity.service.end.point.attrs.NrpCarrierEthConnectivityEndPointResource;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ServiceType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
 * Abstarct activator of VPLS-based L2 VPN on IOS-XR devices. It is responsible for handling
 * activation and deactivation process of VPN configuration and it provides generic transaction
 * designated for this purpose.
 *
 * @author krzysztof.bijakowski@amartus.com
 */

public abstract class AbstractL2vpnActivator implements ResourceActivator {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractL2vpnActivator.class);
    private static final long MTU = 1500;
    private static List<String> DVLS = new ArrayList<String>();
    private static List<Uuid> INLS = new ArrayList<Uuid>();
    protected DataBroker dataBroker;
    protected MountPointService mountService;

    protected AbstractL2vpnActivator(DataBroker dataBroker, MountPointService mountService) {
        LOG.info(" L2vpn XConnect activator initiated...");

        this.dataBroker = dataBroker;
        this.mountService = mountService;
        INLS.clear();
        DVLS.clear();
    }

    @Override
    public void activate(List<EndPoint> endPoints, String serviceId, boolean isExclusive, ServiceType serviceType)
                            throws InterruptedException, ExecutionException {
        String innerOuterName = getInnerName(serviceId);

        ServicePort port = null;
        ServicePort neighbor = null;

        for (EndPoint endPoint : endPoints) {
            if (port == null) {
                port = toServicePort(endPoint, NetconfConstants.NETCONF_TOPOLODY_NAME);
                NrpCarrierEthConnectivityEndPointResource attrs = endPoint.getAttrs() == null ? null
                        : endPoint.getAttrs().getNrpCarrierEthConnectivityEndPointResource();
                if (attrs != null) {
                    port.setEgressBwpFlow(attrs.getEgressBwpFlow());
                    port.setIngressBwpFlow(attrs.getIngressBwpFlow());

                }
            } else {
                neighbor = toServicePort(endPoint, NetconfConstants.NETCONF_TOPOLODY_NAME);
            }
        }

        java.util.Optional<PolicyManager> qosConfig = activateQos(innerOuterName, port);
        InterfaceConfigurations interfaceConfigurations = activateInterface(port, neighbor, MTU, isExclusive);
        Pseudowires pseudowires = activatePseudowire(neighbor);
        XconnectGroups xconnectGroups =
                 activateXConnect(innerOuterName, innerOuterName, port, neighbor, pseudowires, isExclusive);
        L2vpn l2vpn = activateL2Vpn(xconnectGroups);

        // create sub interface for tag based service
        if (!isExclusive) {
            InterfaceConfigurations subInterfaceConfigurations = createSubInterface(port, neighbor, MTU);
            createSubInterface(port.getNode().getValue(), subInterfaceConfigurations, mountService);
        }

        doActivate(port.getNode().getValue(), interfaceConfigurations, l2vpn, mountService, qosConfig);
    }

    @Override
    public void deactivate(
                        List<EndPoint> endPoints,
                        String serviceId,
                        boolean isExclusive,
                        ServiceType serviceType) throws InterruptedException, ExecutionException {
        String innerOuterName = getInnerName(serviceId);
        ServicePort port = toServicePort(endPoints.stream().findFirst().get(), NetconfConstants.NETCONF_TOPOLODY_NAME);

        InstanceIdentifier<P2pXconnect> xconnectId = deactivateXConnect(innerOuterName, innerOuterName);
        LOG.debug("Is service has vlan ? validate isExclusive : ", isExclusive);
        InstanceIdentifier<InterfaceConfiguration> interfaceConfigurationId = deactivateInterface(port, isExclusive);
        doDeactivate(
                    port,
                    xconnectId,
                    interfaceConfigurationId,
                    isExclusive,
                    endPoints.stream().findFirst().get(),
                    mountService,
                    DVLS,
                    INLS);
    }

    protected abstract java.util.Optional<PolicyManager> activateQos(String name, ServicePort port);

    protected abstract String getInnerName(String serviceId);

    protected abstract String getOuterName(String serviceId);

    protected abstract InterfaceConfigurations activateInterface(
                                                                ServicePort portA,
                                                                ServicePort portZ,
                                                                long mtu,
                                                                boolean isExclusive);

    protected abstract void createSubInterface(
                                            String value,
                                            InterfaceConfigurations subInterfaceConfigurations,
                                            MountPointService mountService2)
                                            throws InterruptedException, ExecutionException;

    protected abstract InterfaceConfigurations createSubInterface(ServicePort portA, ServicePort portZ, long mtu);

    protected abstract Pseudowires activatePseudowire(ServicePort neighbor);

    protected abstract XconnectGroups activateXConnect(
                                                    String outerName,
                                                    String innerName,
                                                    ServicePort portA,
                                                    ServicePort portZ,
                                                    Pseudowires pseudowires,
                                                    boolean isExclusive);

    protected abstract L2vpn activateL2Vpn(XconnectGroups xconnectGroups);

    protected abstract void doActivate(
                                    String node,
                                    InterfaceConfigurations interfaceConfigurations,
                                    L2vpn l2vpn,
                                    MountPointService mountService2,
                                    java.util.Optional<PolicyManager> qosConfig)
                                    throws InterruptedException, ExecutionException;

    protected abstract InstanceIdentifier<InterfaceConfiguration> deactivateInterface(
                                                                                    ServicePort port,
                                                                                    boolean isExclusive);

    private InstanceIdentifier<P2pXconnect> deactivateXConnect(String outerName, String innerName) {
        return InstanceIdentifier.builder(L2vpn.class).child(Database.class)
                .child(XconnectGroups.class)
                .child(XconnectGroup.class, new XconnectGroupKey(new CiscoIosXrString(outerName)))
                .child(P2pXconnects.class)
                .child(P2pXconnect.class, new P2pXconnectKey(new CiscoIosXrString(innerName)))
                .build();
    }

    protected abstract void doDeactivate(
                                        ServicePort port,
                                        InstanceIdentifier<P2pXconnect> xconnectId,
                                        InstanceIdentifier<InterfaceConfiguration> interfaceConfigurationId,
                                        boolean isExclusive,
                                        EndPoint endPoint,
                                        MountPointService mountService2,
                                        List<String> dvls,
                                        List<Uuid> inls) throws InterruptedException, ExecutionException;

}
