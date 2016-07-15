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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.unimgr.mef.nrp.common.MountPointHelper;
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

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.fail;

/**
 * @author marek.ryznar@amartus.com
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(MountPointHelper.class)
public class L2vpnBridgeActivatorTest extends AbstractDataBrokerTest{

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
        mtu = Long.valueOf(1500);
    }

    @Test
    public void testActivate(){
        //when
        l2vpnBridgeActivator.activate(nodeName, outerName, innerName, port, neighbor, mtu);

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
    }

    @Test
    public void testDeactivate(){
        //when
        l2vpnBridgeActivator.deactivate(nodeName,outerName,innerName,port,neighbor,mtu);

        //then
        L2vpnActivatorTestUtils.checkDeactivation(optBroker);
    }

    private void checkL2vpnTree(CheckedFuture<Optional<L2vpn>, ReadFailedException> driverL2vpn) throws InterruptedException, ExecutionException {
       if (driverL2vpn.get().isPresent()){
           L2vpn l2vpn = driverL2vpn.get().get();
           L2vpnActivatorTestUtils.checkL2vpn(l2vpn);

           XconnectGroup xconnectGroup = l2vpn.getDatabase().getXconnectGroups().getXconnectGroup().get(0);
           L2vpnActivatorTestUtils.checkXConnectGroup(xconnectGroup,outerName);

           P2pXconnect p2pXconnect = xconnectGroup.getP2pXconnects().getP2pXconnect().get(0);
           L2vpnActivatorTestUtils.checkP2pXconnect(p2pXconnect,innerName);

           AttachmentCircuit attachmentCircuit = p2pXconnect.getAttachmentCircuits().getAttachmentCircuit().get(0);
           L2vpnActivatorTestUtils.checkAttachmentCircuit(attachmentCircuit,portNo2);
           attachmentCircuit = p2pXconnect.getAttachmentCircuits().getAttachmentCircuit().get(1);
           L2vpnActivatorTestUtils.checkAttachmentCircuit(attachmentCircuit,portNo1);
       } else {
           fail("L2vpn was not found.");
       }
    }

    private void checkInterfaceConfigurationTree(CheckedFuture<Optional<InterfaceConfigurations>, ReadFailedException> driverInterfaceConfigurations) throws InterruptedException, ExecutionException{
        if (driverInterfaceConfigurations.get().isPresent()){
            InterfaceConfigurations interfaceConfigurations = driverInterfaceConfigurations.get().get();
            L2vpnActivatorTestUtils.checkInterfaceConfigurations(interfaceConfigurations);

            InterfaceConfiguration interfaceConfiguration = interfaceConfigurations.getInterfaceConfiguration().get(0);
            L2vpnActivatorTestUtils.checkInterfaceConfiguration(interfaceConfiguration,portNo2,false);
            interfaceConfiguration = interfaceConfigurations.getInterfaceConfiguration().get(1);
            L2vpnActivatorTestUtils.checkInterfaceConfiguration(interfaceConfiguration,portNo1,false);
        } else {
            fail("InterfaceConfigurations was not found.");
        }
    }
}
