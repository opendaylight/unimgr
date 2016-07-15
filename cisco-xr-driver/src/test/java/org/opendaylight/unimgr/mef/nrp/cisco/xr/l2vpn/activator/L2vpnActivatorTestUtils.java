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
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.unimgr.mef.nrp.common.MountPointHelper;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations._interface.configuration.mtus.Mtu;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.XconnectGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnect;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.attachment.circuits.AttachmentCircuit;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.Pseudowire;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.pseudowire.Neighbor;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.pseudowire.pseudowire.content.MplsStaticLabels;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPortBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

/**
 * Util class responsible for executing suitable assert operations on given objects.
 *
 * @author marek.ryznar@amartus.com
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(MountPointHelper.class)
public class L2vpnActivatorTestUtils {

    public static MountPointService getMockedMountPointService(Optional<DataBroker> optBroker){
        PowerMockito.mockStatic(MountPointHelper.class);
        PowerMockito.when(MountPointHelper.getDataBroker(Mockito.anyObject(),Mockito.anyString())).thenReturn(optBroker);
        return Mockito.mock(MountPointService.class);
    }

    public static void checkL2vpn(L2vpn l2vpn){
        assertNotNull(l2vpn);
        assertNotNull(l2vpn.getDatabase());
        assertNotNull(l2vpn.getDatabase().getXconnectGroups());
    }

    public static void checkXConnectGroup(XconnectGroup xconnectGroup, String outerName){
        assertNotNull(xconnectGroup);
        assertEquals(outerName,xconnectGroup.getName().getValue());
        assertNotNull(xconnectGroup.getP2pXconnects());
        assertNotNull(xconnectGroup.getP2pXconnects().getP2pXconnect());
    }

    public static void checkP2pXconnect(P2pXconnect p2pXconnect, String innerName){
        assertNotNull(p2pXconnect);
        assertEquals(innerName,p2pXconnect.getName().getValue());
        assertNotNull(p2pXconnect.getAttachmentCircuits());
        assertNotNull(p2pXconnect.getAttachmentCircuits().getAttachmentCircuit());
        assertNotNull(p2pXconnect.getPseudowires());
        assertNotNull(p2pXconnect.getPseudowires().getPseudowire());
    }

    public static void checkAttachmentCircuit(AttachmentCircuit attachmentCircuit, String port){
        assertTrue(attachmentCircuit.isEnable());
        assertEquals(port,attachmentCircuit.getName().getValue());
    }

    public static void checkPseudowire(Pseudowire pseudowire){
        assertNotNull(pseudowire);
        assertNotNull(pseudowire.getPseudowireId());    //getPseudowireId=PseudowireIdRange [_value=2000]   (hardcoded value)
        assertNotNull(pseudowire.getNeighbor());
        assertNotNull(pseudowire.getNeighbor().get(0));
    }

    public static void checkNeighbor(Neighbor neighbor){
        assertNotNull(neighbor);
        assertNotNull(neighbor.getXmlClass());  //getXmlClass=CiscoIosXrString [_value=static], (hardcoded value)
        assertNotNull(neighbor.getNeighbor());  //getNeighbor=Ipv4Address [_value=127.0.0.1],   (hardcoded value)
        assertNotNull(neighbor.getMplsStaticLabels());
    }

    public static void checkMplsStaticLabels(MplsStaticLabels mplsStaticLabels){
        assertNotNull(mplsStaticLabels);
        assertNotNull(mplsStaticLabels.getLocalStaticLabel());  //getLocalStaticLabel=PseudowireLabelRange [_value=2000]    (hardcoded value)
        assertNotNull(mplsStaticLabels.getRemoteStaticLabel()); //getRemoteStaticLabel=PseudowireLabelRange [_value=2000],  (hardcoded value)
    }

    public static void checkInterfaceConfigurations(InterfaceConfigurations interfaceConfigurations){
        assertNotNull(interfaceConfigurations);
        assertNotNull(interfaceConfigurations.getInterfaceConfiguration());
    }

    public static void checkInterfaceConfiguration(InterfaceConfiguration interfaceConfiguration, String portNo, boolean mtu){
        assertNotNull(interfaceConfiguration);
        assertNotNull(interfaceConfiguration.getActive()); //getActive=InterfaceActive [_value=act],                (hardcoded value)
        assertNotNull(interfaceConfiguration.getInterfaceModeNonPhysical()); //getInterfaceModeNonPhysical=Default  (hardcoded value)
        assertEquals(portNo,interfaceConfiguration.getInterfaceName().getValue());
        assertTrue(interfaceConfiguration.isShutdown());
        if(mtu){
            assertNotNull(interfaceConfiguration.getMtus());
            assertNotNull(interfaceConfiguration.getMtus().getMtu());
        }
    }

    public static void checkMtu(Mtu mtu, Long mtuValue){
        assertEquals(mtuValue,mtu.getMtu());
        assertNotNull(mtu.getOwner());  //getOwner=CiscoIosXrString [_value=GigabitEthernet],   (hardcoded value)
    }

    public static void checkDeactivation(Optional<DataBroker> optBroker){
        ReadOnlyTransaction transaction = optBroker.get().newReadOnlyTransaction();

        InstanceIdentifier<L2vpn> l2vpn = InstanceIdentifier.builder(L2vpn.class).build();
        InstanceIdentifier<InterfaceConfigurations> interfaceConfigurations = InstanceIdentifier.builder(InterfaceConfigurations.class).build();

        CheckedFuture<Optional<L2vpn>, ReadFailedException> driverL2vpn = transaction.read(LogicalDatastoreType.CONFIGURATION, l2vpn);
        CheckedFuture<Optional<InterfaceConfigurations>, ReadFailedException> driverInterfaceConfigurations = transaction.read(LogicalDatastoreType.CONFIGURATION, interfaceConfigurations);

        try {
            assertFalse(driverL2vpn.get().isPresent());
            assertFalse(driverInterfaceConfigurations.get().isPresent());
        } catch (InterruptedException | ExecutionException e) {
            fail(e.getMessage());
        }
    }

    public static FcPort port(String topo, String host, String port) {
        return new FcPortBuilder()
                .setTopology(new TopologyId(topo))
                .setNode(new NodeId(host))
                .setTp(new TpId(port))
                .build();
    }
}