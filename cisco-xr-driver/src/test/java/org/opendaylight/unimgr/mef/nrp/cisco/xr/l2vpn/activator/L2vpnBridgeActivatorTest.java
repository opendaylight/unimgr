/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.common.MountPointHelper;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivatorException;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.XconnectGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnect;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.attachment.circuits.AttachmentCircuit;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

/**
 * @author marek.ryznar@amartus.com
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(MountPointHelper.class)
public class L2vpnBridgeActivatorTest extends AbstractDataBrokerTest{
    private static final Logger log = LoggerFactory.getLogger(L2vpnBridgeActivatorTest.class);

    private L2vpnBridgeActivator l2vpnBridgeActivator;
    private MountPointService mountService;
    private Optional<DataBroker> optBroker;
    private String nodeName;
    private String outerName;
    private String innerName;
    private String portNo1;
    private String portNo2;
    private FcPort port;
    private FcPort neighbor;
    private Long mtu ;

    @Before
    public void setUp(){
        //given
        DataBroker broker = getDataBroker();
        optBroker = Optional.of(broker);
        mountService = L2vpnActivatorTestUtils.getMockedMountPointService(optBroker);
        l2vpnBridgeActivator = new L2vpnBridgeActivator(broker,mountService);

        nodeName = "NodeNameExample";
        outerName = "OuterNameExample";
        innerName = "InnerNameExample";
        portNo1 = "80";
        portNo2 = "8080";
        port = L2vpnActivatorTestUtils.port("a", "localhost", portNo1);
        neighbor = L2vpnActivatorTestUtils.port("z", "localhost", portNo2);
        mtu = 1500L;
    }

    @Ignore
    @Test
    public void testActivateAndDeactivate(){
        //when
        activate();

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
        L2vpnActivatorTestUtils.checkDeactivated(optBroker,portNo1);
    }

    private void deactivate(){
        try {
            l2vpnBridgeActivator.deactivate(null,null);
        } catch (TransactionCommitFailedException e) {
            fail("Error during deactivation : " + e.getMessage());
        } catch (ResourceActivatorException e) {
            e.printStackTrace();
        }
    }

    private void activate(){
        log.debug("activate L2VPN");
        try {
            l2vpnBridgeActivator.activate(null,null);
        } catch (TransactionCommitFailedException e) {
            fail("Error during activation : " + e.getMessage());
        } catch (ResourceActivatorException e) {
            e.printStackTrace();
        }
    }

    private void checkL2vpnTree(CheckedFuture<Optional<L2vpn>, ReadFailedException> driverL2vpn) throws InterruptedException, ExecutionException {
        if (driverL2vpn.get().isPresent()){
            L2vpn l2vpn = driverL2vpn.get().get();
            L2vpnActivatorTestUtils.checkL2vpn(l2vpn);

            XconnectGroup xconnectGroup = l2vpn.getDatabase().getXconnectGroups().getXconnectGroup().get(0);
            L2vpnActivatorTestUtils.checkXConnectGroup(xconnectGroup,outerName);

            P2pXconnect p2pXconnect = xconnectGroup.getP2pXconnects().getP2pXconnect().get(0);
            L2vpnActivatorTestUtils.checkP2pXconnect(p2pXconnect,innerName);

            List<AttachmentCircuit> attachmentCircuits = p2pXconnect.getAttachmentCircuits().getAttachmentCircuit();
            assertNotNull(attachmentCircuits);
            assertEquals(2, attachmentCircuits.size());

            attachmentCircuits.sort(
                    (AttachmentCircuit ac1, AttachmentCircuit ac2)
                            -> ac1.getName().getValue().compareTo(ac2.getName().getValue()));

            L2vpnActivatorTestUtils.checkAttachmentCircuit(attachmentCircuits.get(0), portNo1);
            L2vpnActivatorTestUtils.checkAttachmentCircuit(attachmentCircuits.get(1), portNo2);
        } else {
            fail("L2vpn was not found.");
        }
    }

    private void checkInterfaceConfigurationTree(CheckedFuture<Optional<InterfaceConfigurations>, ReadFailedException> driverInterfaceConfigurations) throws InterruptedException, ExecutionException{
        if (driverInterfaceConfigurations.get().isPresent()){
            InterfaceConfigurations interfaceConfigurations = driverInterfaceConfigurations.get().get();
            L2vpnActivatorTestUtils.checkInterfaceConfigurations(interfaceConfigurations);

            List<InterfaceConfiguration> interfaceConfigurationList = interfaceConfigurations.getInterfaceConfiguration();
            interfaceConfigurationList.sort(
                    (InterfaceConfiguration ic1, InterfaceConfiguration ic2)
                            -> ic1.getInterfaceName().getValue().compareTo(ic2.getInterfaceName().getValue()));

            L2vpnActivatorTestUtils.checkInterfaceConfiguration(interfaceConfigurationList.get(0),portNo1,false);
            L2vpnActivatorTestUtils.checkInterfaceConfiguration(interfaceConfigurationList.get(1),portNo2,false);
        } else {
            fail("InterfaceConfigurations was not found.");
        }
    }
}