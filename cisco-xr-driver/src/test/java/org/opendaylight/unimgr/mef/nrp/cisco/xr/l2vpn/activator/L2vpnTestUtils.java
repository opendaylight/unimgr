/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.common.MountPointHelper;
import org.opendaylight.unimgr.mef.nrp.common.ServicePort;
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
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev171221.NrpConnectivityServiceEndPointAttrs;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.ConnectivityServiceEndPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

/**
 * Util class responsible for executing suitable assert operations on given objects.
 *
 * @author marek.ryznar@amartus.com
 */
public class L2vpnTestUtils {

    public static MountPointService getMockedMountPointService(Optional<DataBroker> optBroker) {
        PowerMockito.mockStatic(MountPointHelper.class);
        PowerMockito.when(MountPointHelper.getDataBroker(Mockito.anyObject(),Mockito.anyString())).thenReturn(optBroker);
        return Mockito.mock(MountPointService.class);
    }

    public static void checkL2vpn(L2vpn l2vpn) {
        assertNotNull(l2vpn);
        assertNotNull(l2vpn.getDatabase());
        assertNotNull(l2vpn.getDatabase().getXconnectGroups());
    }

    public static void checkXConnectGroup(XconnectGroup xconnectGroup, String outerName) {
        assertNotNull(xconnectGroup);
        assertEquals(outerName,xconnectGroup.getName().getValue());
        assertNotNull(xconnectGroup.getP2pXconnects());
        assertNotNull(xconnectGroup.getP2pXconnects().getP2pXconnect());
    }

    public static void checkP2pXconnect(P2pXconnect p2pXconnect, String innerName) {
        assertNotNull(p2pXconnect);
        assertEquals(innerName,p2pXconnect.getName().getValue());
        assertNotNull(p2pXconnect.getAttachmentCircuits());
        assertNotNull(p2pXconnect.getAttachmentCircuits().getAttachmentCircuit());
        assertNotNull(p2pXconnect.getPseudowires());
        assertNotNull(p2pXconnect.getPseudowires().getPseudowire());
    }

    public static void checkAttachmentCircuit(AttachmentCircuit attachmentCircuit, String port) {
        assertTrue(attachmentCircuit.isEnable());
        assertEquals(port,attachmentCircuit.getName().getValue());
    }

    public static void checkPseudowire(Pseudowire pseudowire) {
        assertNotNull(pseudowire);
        assertNotNull(pseudowire.getPseudowireId());
        assertNotNull(pseudowire.getNeighbor());
        assertNotNull(pseudowire.getNeighbor().get(0));
    }

    public static void checkNeighbor(Neighbor neighbor) {
        assertNotNull(neighbor);
        assertNotNull(neighbor.getXmlClass());
        assertNotNull(neighbor.getNeighbor());
        assertNotNull(neighbor.getMplsStaticLabels());
    }

    public static void checkMplsStaticLabels(MplsStaticLabels mplsStaticLabels) {
        assertNotNull(mplsStaticLabels);
        assertNotNull(mplsStaticLabels.getLocalStaticLabel());
        assertNotNull(mplsStaticLabels.getRemoteStaticLabel());
    }

    public static void checkInterfaceConfigurations(InterfaceConfigurations interfaceConfigurations) {
        assertNotNull(interfaceConfigurations);
        assertNotNull(interfaceConfigurations.getInterfaceConfiguration());
    }

    public static void checkInterfaceConfiguration(InterfaceConfiguration interfaceConfiguration, String portNo, boolean mtu) {
        assertNotNull(interfaceConfiguration);
        assertNotNull(interfaceConfiguration.getActive());
        assertNotNull(interfaceConfiguration.getInterfaceModeNonPhysical());
        assertEquals(portNo,interfaceConfiguration.getInterfaceName().getValue());
        assertTrue(interfaceConfiguration.isShutdown());
        if (mtu) {
            assertNotNull(interfaceConfiguration.getMtus());
            assertNotNull(interfaceConfiguration.getMtus().getMtu());
        }
    }

    public static void checkMtu(Mtu mtu, Long mtuValue) {
        assertEquals(mtuValue,mtu.getMtu());
        assertNotNull(mtu.getOwner());
    }

    public static ServicePort port(String topo, String host, String port) {
        return new ServicePort(new TopologyId(topo), new NodeId(host), new TpId(port));
    }

    public static void checkDeactivated(Optional<DataBroker> optBroker, String deactivatedPort)  {
        ReadOnlyTransaction transaction = optBroker.get().newReadOnlyTransaction();

        InstanceIdentifier<L2vpn> l2vpnIid = InstanceIdentifier.builder(L2vpn.class).build();
        InstanceIdentifier<InterfaceConfigurations> interfaceConfigurationsIid = InstanceIdentifier.builder(InterfaceConfigurations.class).build();

        CheckedFuture<Optional<L2vpn>, ReadFailedException> driverL2vpn = transaction.read(LogicalDatastoreType.CONFIGURATION, l2vpnIid);
        CheckedFuture<Optional<InterfaceConfigurations>, ReadFailedException> driverInterfaceConfigurations = transaction.read(LogicalDatastoreType.CONFIGURATION, interfaceConfigurationsIid);

        try {
            checkL2vpnDeactivation(driverL2vpn);
            checkInterfaceConfigurationDeactivation(driverInterfaceConfigurations,deactivatedPort);
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    private static void checkL2vpnDeactivation(CheckedFuture<Optional<L2vpn>, ReadFailedException>driverL2vpn) throws ExecutionException, InterruptedException {
        if (driverL2vpn.get().isPresent()) {
            L2vpn l2vpn = driverL2vpn.get().get();
            L2vpnTestUtils.checkL2vpn(l2vpn);

            XconnectGroup xconnectGroup = l2vpn.getDatabase().getXconnectGroups().getXconnectGroup().get(0);
            assertTrue(xconnectGroup.getP2pXconnects().getP2pXconnect().isEmpty());
        } else {
            fail("L2vpn was not found.");
        }
    }

    private static void checkInterfaceConfigurationDeactivation(CheckedFuture<Optional<InterfaceConfigurations>, ReadFailedException> driverInterfaceConfigurations, String deactivatedPort) throws InterruptedException, ExecutionException{
        if (driverInterfaceConfigurations.get().isPresent()) {
            InterfaceConfigurations interfaceConfigurations = driverInterfaceConfigurations.get().get();
            L2vpnTestUtils.checkInterfaceConfigurations(interfaceConfigurations);

            List<InterfaceConfiguration> interfaceConfigurationList = interfaceConfigurations.getInterfaceConfiguration();
            assertFalse(interfaceConfigurationList.stream().anyMatch(x -> x.getInterfaceName().getValue().equals(deactivatedPort)));
        } else {
            fail("InterfaceConfigurations was not found.");
        }
    }

    public static List<EndPoint> mockEndpoints(String device1Name, String device2Name, String portNo1, String portNo2) {
        List<EndPoint> endPoints = new ArrayList<>();
        endPoints.add(mockEndPoint("sip:"+device1Name+":"+portNo1));
        endPoints.add(mockEndPoint("sip:"+device2Name+":"+portNo2));
        return endPoints;
    }

    private static EndPoint mockEndPoint(String portName) {
        ConnectivityServiceEndPoint connectivityServiceEndPoint = mock(ConnectivityServiceEndPoint.class);
        NrpConnectivityServiceEndPointAttrs attrs = mock(NrpConnectivityServiceEndPointAttrs.class);
        //UNI port mock
        when(connectivityServiceEndPoint.getServiceInterfacePoint())
                .thenReturn(new Uuid(portName));

        return new EndPoint(connectivityServiceEndPoint,attrs);
    }
}