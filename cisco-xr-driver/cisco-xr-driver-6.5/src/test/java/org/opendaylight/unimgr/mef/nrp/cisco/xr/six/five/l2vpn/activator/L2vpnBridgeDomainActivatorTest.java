/*
 * Copyright (c) 2019 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.l2vpn.activator;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.l2vpn.activator.L2vpnBridgeDomainActivator;
import org.opendaylight.unimgr.mef.nrp.common.TapiUtils;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.bridge.domain.table.BridgeDomains;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.bridge.domain.BdAttachmentCircuits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.bridge.domain.BdPseudowires;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.bridge.domain.table.bridge.domains.bridge.domain.bd.attachment.circuits.BdAttachmentCircuit;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.BridgeDomainGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.bridge.domain.groups.BridgeDomainGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ConnectivityServiceEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.end.point.ServiceInterfacePoint;
import org.powermock.api.mockito.PowerMockito;


/*
 * @author Om.SAwasthi@Xoriant.Com
 *
 */
public class L2vpnBridgeDomainActivatorTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private MountPointService mountService;
    private static final String UUID1 = "sip:ciscoD1:GigabitEthernet0/0/0/1";
    private static final String UUID2 = "sip:ciscoD2:GigabitEthernet0/0/0/1";
    private static final String NETCONF_TOPOLODY_NAME = "topology-netconf";
    private EndPoint ep1;
    private EndPoint ep2;
    private ServicePort port;
    private ServicePort neighbor;
    private L2vpnBridgeDomainActivator l2vpnBridgeDomainActivator;

    @Before
    public void setUp() throws Exception {
        ConnectivityServiceEndPoint cep =
                new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
                .connectivity.service.EndPointBuilder()
                        .setServiceInterfacePoint(
                                TapiUtils.toSipRef(new Uuid(UUID1), ServiceInterfacePoint.class))
                        .setDirection(PortDirection.BIDIRECTIONAL).build();
        ep1 = new EndPoint(cep, null);
        ConnectivityServiceEndPoint cep1 =
                new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
                .connectivity.service.EndPointBuilder()
                        .setServiceInterfacePoint(
                                TapiUtils.toSipRef(new Uuid(UUID2), ServiceInterfacePoint.class))
                        .setDirection(PortDirection.BIDIRECTIONAL).build();
        ep2 = new EndPoint(cep1, null);
        port = ServicePort.toServicePort(ep1, NETCONF_TOPOLODY_NAME);
        neighbor = ServicePort.toServicePort(ep2, NETCONF_TOPOLODY_NAME);
        l2vpnBridgeDomainActivator = new L2vpnBridgeDomainActivator(dataBroker, mountService);
    }

    @Test
    public void activateBridgeDomainTest() {

        String outerName = "cs:16b9e18aa84:-364cc8e5";
        String innerName = "cs:16b9e18aa84:-364cc8e5";
        BdPseudowires bdPseudowires = PowerMockito.mock(BdPseudowires.class);
        boolean isExclusive = true;

        BridgeDomainGroups dominGroups = l2vpnBridgeDomainActivator.activateBridgeDomain(outerName,
                innerName, port, neighbor, bdPseudowires, isExclusive);
        List<BridgeDomainGroup> domainGroupList = dominGroups.getBridgeDomainGroup();
        BridgeDomainGroup bridgeDomainGroup = domainGroupList.get(0);
        BridgeDomains bridgeDomains = bridgeDomainGroup.getBridgeDomains();
        List<BridgeDomain> bdlist = bridgeDomains.getBridgeDomain();

        BridgeDomain bd = (BridgeDomain) bdlist.get(0);
        BdAttachmentCircuits bdAttachmentCircuits = bd.getBdAttachmentCircuits();
        List<BdAttachmentCircuit> bdAttachmentCircuitList =
                bdAttachmentCircuits.getBdAttachmentCircuit();
        BdAttachmentCircuit bdAttachmentCircuit = bdAttachmentCircuitList.get(0);
        assertEquals("GigabitEthernet0/0/0/1", bdAttachmentCircuit.getName().getValue());
    }

    @Test
    public void activateInterfaceTest() {

        InterfaceConfigurations interfaceConfigruaton =
                l2vpnBridgeDomainActivator.activateInterface(port, neighbor, 2000, false);
        interfaceConfigruaton.implementedInterface();
        List<InterfaceConfiguration> list = interfaceConfigruaton.getInterfaceConfiguration();
        InterfaceConfiguration interfaceConfiguration = list.get(0);
        assertEquals("act", interfaceConfiguration.getActive().getValue());
    }

    @Test
    public void createSubInterfaceTest() {
        port.setVlanId(301L);
        InterfaceConfigurations interfaceConfiguration =
                l2vpnBridgeDomainActivator.createSubInterface(port, neighbor, 2000);
        Assert.assertNotNull(interfaceConfiguration);
    }

}
