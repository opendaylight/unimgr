/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.six.three.l2vpn.activator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.google.common.util.concurrent.FluentFuture;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.six.three.l2vpn.activator.L2vpnLocalConnectActivator;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.xconnect.groups.XconnectGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnect;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev170626.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.attachment.circuits.AttachmentCircuit;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ServiceType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
 * @author marek.ryznar@amartus.com
 */
public class L2vpnLocalConnectionActivatorTest extends AbstractConcurrentDataBrokerTest {
    private static final Logger LOG = LoggerFactory.getLogger(L2vpnLocalConnectionActivatorTest.class);

    private L2vpnLocalConnectActivator l2VpnLocalConnectActivator;
    private MountPointService mountService;
    private String outerName;
    private String innerName;
    private String portNo1;
    private String portNo2;
    private String deviceName = "localhost";
    private List<EndPoint> endPoints;
    private String serviceId = "serviceId";

    @Before
    public void setUp() {
        //given
        MountPoint mp = Mockito.mock(MountPoint.class);
        Mockito.when(mp.getService(DataBroker.class)).thenReturn(Optional.of(getDataBroker()));
        mountService = Mockito.mock(MountPointService.class);
        Mockito.when(mountService.getMountPoint(Mockito.any())).thenReturn(Optional.of(mp));

        l2VpnLocalConnectActivator = new L2vpnLocalConnectActivator(getDataBroker(),mountService);

        outerName = "serviceId";
        innerName = "serviceId";
        portNo1 = "8080";
        portNo2 = "8081";
        endPoints = L2vpnTestUtils.mockEndpoints(deviceName,deviceName,portNo1,portNo2);
    }

    @Test
    public void testActivateAndDeactivate() {
        //when
        activate();

        //then
        ReadTransaction transaction = getDataBroker().newReadOnlyTransaction();

        InstanceIdentifier<L2vpn> l2vpn = InstanceIdentifier.builder(L2vpn.class).build();
        InstanceIdentifier<InterfaceConfigurations> interfaceConfigurations =
                        InstanceIdentifier.builder(InterfaceConfigurations.class).build();

        FluentFuture<Optional<L2vpn>> driverL2vpn = transaction.read(LogicalDatastoreType.CONFIGURATION, l2vpn);
        FluentFuture<Optional<InterfaceConfigurations>> driverInterfaceConfigurations =
                    transaction.read(LogicalDatastoreType.CONFIGURATION, interfaceConfigurations);

        try {
            checkL2vpnTree(driverL2vpn);
            checkInterfaceConfigurationTree(driverInterfaceConfigurations);
        } catch (InterruptedException | ExecutionException e) {
            fail(e.getMessage());
        }

        //when
        deactivate();

        //then
        L2vpnTestUtils.checkDeactivated(getDataBroker(), portNo2);
    }

    private void deactivate() {
        try {
            l2VpnLocalConnectActivator.deactivate(endPoints,serviceId, true, ServiceType.POINTTOPOINTCONNECTIVITY);
        } catch (InterruptedException | ExecutionException e) {
            fail("Error during deactivation : " + e.getMessage());
        }
    }

    private void activate() {
        LOG.debug("activate L2VPN");
        try {
            l2VpnLocalConnectActivator.activate(endPoints,serviceId, true, ServiceType.POINTTOPOINTCONNECTIVITY);
        } catch (InterruptedException | ExecutionException e) {
            fail("Error during activation : " + e.getMessage());
        }
    }

    private void checkL2vpnTree(FluentFuture<Optional<L2vpn>> driverL2vpn)
                                                    throws InterruptedException, ExecutionException {
        if (driverL2vpn.get().isPresent()) {
            L2vpn l2vpn = driverL2vpn.get().get();
            L2vpnTestUtils.checkL2vpn(l2vpn);

            XconnectGroup xconnectGroup = l2vpn.getDatabase().getXconnectGroups().getXconnectGroup().get(0);
            L2vpnTestUtils.checkXConnectGroup(xconnectGroup,outerName);

            P2pXconnect p2pXconnect = xconnectGroup.getP2pXconnects().getP2pXconnect().get(0);
            L2vpnTestUtils.checkP2pXconnect(p2pXconnect,innerName);

            List<AttachmentCircuit> attachmentCircuits = p2pXconnect.getAttachmentCircuits().getAttachmentCircuit();
            assertNotNull(attachmentCircuits);
            assertEquals(2, attachmentCircuits.size());

            List<AttachmentCircuit> sorted = attachmentCircuits.stream()
                .sorted(
                    (AttachmentCircuit ac1, AttachmentCircuit ac2)
                        -> ac1.getName().getValue().compareTo(ac2.getName().getValue()))
                .collect(Collectors.toList());

            L2vpnTestUtils.checkAttachmentCircuit(sorted.get(0), portNo1);
            L2vpnTestUtils.checkAttachmentCircuit(sorted.get(1), portNo2);
        } else {
            fail("L2vpn was not found.");
        }
    }

    private void checkInterfaceConfigurationTree(
                                        FluentFuture<Optional<InterfaceConfigurations>> driverInterfaceConfigurations)
                                               throws InterruptedException, ExecutionException {
        if (driverInterfaceConfigurations.get().isPresent()) {
            InterfaceConfigurations interfaceConfigurations = driverInterfaceConfigurations.get().get();
            L2vpnTestUtils.checkInterfaceConfigurations(interfaceConfigurations);

            List<InterfaceConfiguration> interfaceConfigurationList =
                                                                interfaceConfigurations.getInterfaceConfiguration();
            List<InterfaceConfiguration> sorted = interfaceConfigurationList.stream()
                .sorted(
                    (InterfaceConfiguration ic1, InterfaceConfiguration ic2)
                        -> ic1.getInterfaceName().getValue().compareTo(ic2.getInterfaceName().getValue()))
                .collect(Collectors.toList());

            L2vpnTestUtils.checkInterfaceConfiguration(sorted.get(0),portNo1,false);
            L2vpnTestUtils.checkInterfaceConfiguration(sorted.get(1),portNo2,false);
        } else {
            fail("InterfaceConfigurations was not found.");
        }
    }
}
