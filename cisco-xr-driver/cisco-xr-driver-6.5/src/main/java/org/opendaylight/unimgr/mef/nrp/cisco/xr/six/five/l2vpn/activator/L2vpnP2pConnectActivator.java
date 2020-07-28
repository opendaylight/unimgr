/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.l2vpn.activator;

import static org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.BandwidthProfileComposition.BwpApplicability.UNI;
import static org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.BandwidthProfileComposition.BwpDirection.EGRESS;
import static org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.BandwidthProfileComposition.BwpDirection.INGRESS;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.FixedServiceNaming;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.BandwidthProfileHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util.LoopbackUtils;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.l2vpn.helper.AttachmentCircuitHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.l2vpn.helper.L2vpnHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.l2vpn.helper.PseudowireHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.l2vpn.helper.XConnectHelper;
/*import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration;*/
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev161215.PolicyManager;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.XconnectGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.xconnect.groups.XconnectGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnect;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.AttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.Pseudowires;
/*import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.XconnectGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.XconnectGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnect;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.AttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.Pseudowires;*/
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/*
 * Activator of VPLS-based L2 VPN using cross connect connection on IOS-XR devices.
 *
 * @author krzysztof.bijakowski@amartus.com
 */
public class L2vpnP2pConnectActivator extends AbstractL2vpnActivator {

    private final FixedServiceNaming namingProvider;

    public L2vpnP2pConnectActivator(DataBroker dataBroker, MountPointService mountService) {
        super(dataBroker, mountService);
        namingProvider = new FixedServiceNaming();
    }

    @Override
    protected Optional<PolicyManager> activateQos(String name, ServicePort port) {
        return null;
        /*new BandwidthProfileHelper(port)
                .addPolicyMap(name, INGRESS, UNI)
                .addPolicyMap(name, EGRESS, UNI)
                .build();*/
    }

    @Override
    protected String getInnerName(String serviceId) {
        return namingProvider.replaceForbidenCharacters(serviceId);
    }

    @Override
    protected String getOuterName(String serviceId) {
        return namingProvider.replaceForbidenCharacters(serviceId);
    }

    @Override
    protected InterfaceConfigurations activateInterface(ServicePort port, ServicePort neighbor,
            long mtu, boolean isExclusive) {

        return new InterfaceActivator().activate(port, neighbor, mtu, isExclusive);
    }

    @Override
    protected void createSubInterface(String nodeName,
            InterfaceConfigurations subInterfaceConfigurations, MountPointService mountService2)
            throws InterruptedException, ExecutionException {

        new TransactionActivator().activateSubInterface(nodeName, subInterfaceConfigurations, mountService2);
    }

    @Override
    protected InterfaceConfigurations createSubInterface(ServicePort port, ServicePort neighbor,
            long mtu) {

        return new InterfaceActivator().buildSubInterface(port, neighbor, mtu);
    }

    @Override
    protected Pseudowires activatePseudowire(ServicePort neighbor) {

        return new PseudowireHelper()
                .addPseudowire(LoopbackUtils.getIpv4Address(neighbor, dataBroker)).build();
    }

    @Override
    protected XconnectGroups activateXConnect(String outerName, String innerName, ServicePort port,
            ServicePort portZ, Pseudowires pseudowires, boolean isExclusive) {

        AttachmentCircuits attachmentCircuits = new AttachmentCircuitHelper()
                .addPort(port, isExclusive)
                .build();

        XconnectGroup xconnectGroup = new XConnectHelper()
                .appendXConnect(innerName, attachmentCircuits, pseudowires)
                .build(outerName);

        return XConnectHelper.createXConnectGroups(xconnectGroup);
    }

    @Override
    protected L2vpn activateL2Vpn(XconnectGroups xconnectGroups) {

        return L2vpnHelper.build(xconnectGroups);
    }

    @Override
    protected void doActivate(String nodeName, InterfaceConfigurations interfaceConfigurations,
            L2vpn l2vpn, MountPointService mountService2, Optional<PolicyManager> qosConfig)
            throws InterruptedException, ExecutionException {

        new TransactionActivator().activate(nodeName, interfaceConfigurations, l2vpn, mountService, qosConfig);
    }

    @Override
    protected InstanceIdentifier<InterfaceConfiguration> deactivateInterface(ServicePort port,
            boolean isExclusive) {

        return new InterfaceActivator().deactivate(port, isExclusive);
    }

    @Override
    protected void doDeactivate(ServicePort port, InstanceIdentifier<P2pXconnect> xconnectId,
            InstanceIdentifier<InterfaceConfiguration> interfaceConfigurationId,
            boolean isExclusive, EndPoint endPoint, MountPointService mountService2,
            List<String> dvls, List<Uuid> inls) throws InterruptedException, ExecutionException {

        new TransactionActivator().deactivate(
                                            port,
                                            xconnectId,
                                            interfaceConfigurationId,
                                            isExclusive,
                                            endPoint,
                                            mountService,
                                            dvls,
                                            inls);
    }

}
