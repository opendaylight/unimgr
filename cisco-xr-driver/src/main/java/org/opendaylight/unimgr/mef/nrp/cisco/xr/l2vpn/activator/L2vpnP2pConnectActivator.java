/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.BandwidthProfileHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.InterfaceHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util.LoopbackUtils;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util.MtuUtils;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.AttachmentCircuitHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.L2vpnHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.PseudowireHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.XConnectHelper;
import org.opendaylight.unimgr.mef.nrp.common.FixedServiceNaming;
import org.opendaylight.unimgr.mef.nrp.common.ServicePort;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.asr9k.policymgr.cfg.rev150518.PolicyManager;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations._interface.configuration.Mtus;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.XconnectGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.XconnectGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.AttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.Pseudowires;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString;

import java.util.Optional;

import static org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.BandwidthProfileComposition.BwpApplicability.UNI;
import static org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.BandwidthProfileComposition.BwpDirection.EGRESS;
import static org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.BandwidthProfileComposition.BwpDirection.INGRESS;


/**
 * Activator of VPLS-based L2 VPN using cross connect connection on IOS-XR devices
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
        return new BandwidthProfileHelper(dataBroker, port)
                .addPolicyMap(name, INGRESS, UNI)
                .addPolicyMap(name, EGRESS, UNI)
                .build();
    }

    @Override
    public InterfaceConfigurations activateInterface(ServicePort port, ServicePort neighbor, long mtu) {
        String interfraceName = port.getInterfaceName();
        Mtus mtus = new MtuUtils().generateMtus(mtu, new CiscoIosXrString(interfraceName));

        return new InterfaceHelper()
            .addInterface(port, Optional.of(mtus), true)
            .build();
    }

    @Override
    public Pseudowires activatePseudowire(ServicePort neighbor) {
        return new PseudowireHelper()
             .addPseudowire(LoopbackUtils.getIpv4Address(neighbor, dataBroker))
             .build();
    }

    @Override
    public XconnectGroups activateXConnect(String outerName, String innerName, ServicePort port, ServicePort neighbor, Pseudowires pseudowires) {
        AttachmentCircuits attachmentCircuits = new AttachmentCircuitHelper()
             .addPort(port)
             .build();

        XconnectGroup xconnectGroup = new XConnectHelper()
             .appendXConnect(innerName, attachmentCircuits, pseudowires)
             .build(outerName);

        return XConnectHelper.createXConnectGroups(xconnectGroup);
    }

    @Override
    public L2vpn activateL2Vpn(XconnectGroups xconnectGroups) {
        return L2vpnHelper.build(xconnectGroups);
    }

    @Override
    protected String getInnerName(String serviceId) {
        return namingProvider.getInnerName(replaceForbidenCharacters(serviceId));
    }

    @Override
    protected String getOuterName(String serviceId) {
        return namingProvider.getOuterName(replaceForbidenCharacters(serviceId));
    }

    /**
     * ASR 9000 can't accept colon in xconnect group name, so it have to be replaced with underscore.
     * If any other restriction will be found, this is a good place to change serviceId name.
     *
     * @param serviceId old service id
     * @return new service id
     */
    private String replaceForbidenCharacters(String serviceId){
        return serviceId.replace(":","_");
    }
}
