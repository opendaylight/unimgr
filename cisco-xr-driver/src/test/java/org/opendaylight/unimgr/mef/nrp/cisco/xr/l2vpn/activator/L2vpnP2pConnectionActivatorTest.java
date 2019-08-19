/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator;

import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.MountPointHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.PseudowireHelper;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations._interface.configuration.mtus.Mtu;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.XconnectGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnect;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.attachment.circuits.AttachmentCircuit;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.Pseudowire;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.pseudowire.Neighbor;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ServiceType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


/**
 * @author marek.ryznar@amartus.com
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(MountPointHelper.class)
public class L2vpnP2pConnectionActivatorTest extends AbstractDataBrokerTest {

    private L2vpnP2pConnectActivator l2VpnP2PConnectActivator;
    private MountPointService mountService;
    private Optional<DataBroker> optBroker;
    private Long mtu;
    private String deviceName = "localhost";
    private String portNo1 = "8080";
    private String portNo2 = "8081";
    private String serviceId = "serviceId";
    private List<EndPoint> endPoints;
    private List<EndPoint> endPoints1;
    @Before
    public void setUp() {
        //given
        DataBroker broker = getDataBroker();
        optBroker = Optional.of(broker);

        mountService = L2vpnTestUtils.getMockedMountPointService(optBroker);
        l2VpnP2PConnectActivator = new L2vpnP2pConnectActivator(broker,mountService);

        mtu = Long.valueOf(1500);
        endPoints = L2vpnTestUtils.mockEndpoints(deviceName,deviceName,portNo1,portNo2);
        endPoints1 = L2vpnTestUtils.mockEndpoints(deviceName,deviceName,portNo2,portNo1);

    }

    @Test
    public void testActivateAndDeactivate() {
        //when
        try {
            PseudowireHelper.generatePseudowireId();
            l2VpnP2PConnectActivator.activate(endPoints,serviceId, true, ServiceType.POINTTOPOINTCONNECTIVITY);
            l2VpnP2PConnectActivator.activate(endPoints1,serviceId, true, ServiceType.POINTTOPOINTCONNECTIVITY);

        } catch (TransactionCommitFailedException e) {
            fail("Error during activation : " + e.getMessage());
        }

        //then
        ReadOnlyTransaction transaction = optBroker.get().newReadOnlyTransaction();

        InstanceIdentifier<L2vpn> l2vpn = InstanceIdentifier.builder(L2vpn.class).build();
        InstanceIdentifier<InterfaceConfigurations> interfaceConfigurations = InstanceIdentifier.builder(InterfaceConfigurations.class).build();

        CheckedFuture<Optional<L2vpn>, ReadFailedException> driverL2vpn = transaction.read(LogicalDatastoreType.CONFIGURATION, l2vpn);
        CheckedFuture<Optional<InterfaceConfigurations>, ReadFailedException> driverInterfaceConfigurations = transaction.read(LogicalDatastoreType.CONFIGURATION, interfaceConfigurations);

        try {
            checkL2vpnTree(driverL2vpn);
            checkInterfaceConfigurationTree(driverInterfaceConfigurations);
        } catch (InterruptedException | ExecutionException e) {
            fail(e.getMessage());
        }

        //when
        deactivate();

        //then
        L2vpnTestUtils.checkDeactivated(optBroker,portNo2);
    }

    private void deactivate() {
        //when
        try {
            l2VpnP2PConnectActivator.deactivate(endPoints,serviceId, true, ServiceType.POINTTOPOINTCONNECTIVITY);
        } catch (TransactionCommitFailedException e) {
            fail("Error during deactivation : " + e.getMessage());
        }
    }

    private void checkL2vpnTree(CheckedFuture<Optional<L2vpn>, ReadFailedException> driverL2vpn) throws InterruptedException, ExecutionException {
        if (driverL2vpn.get().isPresent()) {
            L2vpn l2vpn = driverL2vpn.get().get();
            L2vpnTestUtils.checkL2vpn(l2vpn);

            XconnectGroup xconnectGroup = l2vpn.getDatabase().getXconnectGroups().getXconnectGroup().get(0);
            L2vpnTestUtils.checkXConnectGroup(xconnectGroup, serviceId);

            P2pXconnect p2pXconnect = xconnectGroup.getP2pXconnects().getP2pXconnect().get(0);
            L2vpnTestUtils.checkP2pXconnect(p2pXconnect, serviceId);

            AttachmentCircuit attachmentCircuit = p2pXconnect.getAttachmentCircuits().getAttachmentCircuit().get(0);
            L2vpnTestUtils.checkAttachmentCircuit(attachmentCircuit,portNo1);

            Pseudowire pseudowire = p2pXconnect.getPseudowires().getPseudowire().get(0);
            L2vpnTestUtils.checkPseudowire(pseudowire);

            Neighbor neighbor = pseudowire.getNeighbor().get(0);
            L2vpnTestUtils.checkNeighbor(neighbor);

           // MplsStaticLabels mplsStaticLabels = neighbor.getMplsStaticLabels();
           // L2vpnTestUtils.checkMplsStaticLabels(mplsStaticLabels);
        } else {
            fail("L2vpn was not found.");
        }
    }

    private void checkInterfaceConfigurationTree(CheckedFuture<Optional<InterfaceConfigurations>, ReadFailedException> driverInterfaceConfigurations) throws InterruptedException, ExecutionException {
        if (driverInterfaceConfigurations.get().isPresent()) {
            InterfaceConfigurations interfaceConfigurations = driverInterfaceConfigurations.get().get();
            L2vpnTestUtils.checkInterfaceConfigurations(interfaceConfigurations);

            List<InterfaceConfiguration> interfaceConfigurationList = interfaceConfigurations.getInterfaceConfiguration();
            interfaceConfigurationList.sort(
                    (InterfaceConfiguration ic1, InterfaceConfiguration ic2)
                            -> ic1.getInterfaceName().getValue().compareTo(ic2.getInterfaceName().getValue()));

            L2vpnTestUtils.checkInterfaceConfiguration(interfaceConfigurationList.get(0),portNo1,false);

            Mtu mtu1 = interfaceConfigurationList.get(0).getMtus().getMtu().get(0);
            L2vpnTestUtils.checkMtu(mtu1,mtu);
        } else {
            fail("InterfaceConfigurations was not found.");
        }
    }

}
