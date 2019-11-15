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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.common.TapiUtils;
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
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.NrpConnectivityServiceEndPointAttrs;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ConnectivityServiceEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.end.point.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.util.concurrent.FluentFuture;

/**
 * Util class responsible for executing suitable assert operations on given objects.
 *
 * @author marek.ryznar@amartus.com
 */
public class L2vpnTestUtils {

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
        assertNull(interfaceConfiguration.isShutdown());
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

    public static void checkDeactivated(DataBroker broker, String deactivatedPort)  {
        ReadTransaction transaction = broker.newReadOnlyTransaction();

        InstanceIdentifier<L2vpn> l2vpnIid = InstanceIdentifier.builder(L2vpn.class).build();
        InstanceIdentifier<InterfaceConfigurations> interfaceConfigurationsIid = InstanceIdentifier.builder(InterfaceConfigurations.class).build();

        FluentFuture<Optional<L2vpn>> driverL2vpn = transaction.read(LogicalDatastoreType.CONFIGURATION, l2vpnIid);
        FluentFuture<Optional<InterfaceConfigurations>> driverInterfaceConfigurations = transaction.read(LogicalDatastoreType.CONFIGURATION, interfaceConfigurationsIid);

        try {
            checkL2vpnDeactivation(driverL2vpn);
            checkInterfaceConfigurationDeactivation(driverInterfaceConfigurations,deactivatedPort);
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    private static void checkL2vpnDeactivation(FluentFuture<Optional<L2vpn>> driverL2vpn) throws ExecutionException, InterruptedException {
        if (driverL2vpn.get().isPresent()) {
            L2vpn l2vpn = driverL2vpn.get().get();
            L2vpnTestUtils.checkL2vpn(l2vpn);

            XconnectGroup xconnectGroup = l2vpn.getDatabase().getXconnectGroups().getXconnectGroup().get(0);
            assertTrue(xconnectGroup.getP2pXconnects() == null
                    || xconnectGroup.getP2pXconnects().getP2pXconnect().isEmpty());
        } else {
            fail("L2vpn was not found.");
        }
    }

    private static void checkInterfaceConfigurationDeactivation(FluentFuture<Optional<InterfaceConfigurations>> driverInterfaceConfigurations, String deactivatedPort) throws InterruptedException, ExecutionException{
        if (driverInterfaceConfigurations.get().isPresent()) {
            InterfaceConfigurations interfaceConfigurations = driverInterfaceConfigurations.get().get();
            L2vpnTestUtils.checkInterfaceConfigurations(interfaceConfigurations);

            List<InterfaceConfiguration> interfaceConfigurationList = interfaceConfigurations.getInterfaceConfiguration();
            assertTrue(interfaceConfigurationList.stream().anyMatch(x -> x.getInterfaceName().getValue().equals(deactivatedPort)));
        } else {
            // Semantics changed so interface-configurations container disappears when empty?
//            fail("InterfaceConfigurations was not found.");
        }
    }

    public static List<EndPoint> mockEndpoints(String device1Name, String device2Name, String portNo1, String portNo2) {
        List<EndPoint> endPoints = new ArrayList<>();
        endPoints.add(mockEndPoint("sip:" + device1Name + ":" + portNo1));
        endPoints.add(mockEndPoint("sip:" + device2Name + ":" + portNo2));
        return endPoints;
    }

    private static EndPoint mockEndPoint(String portName) {
        ConnectivityServiceEndPoint connectivityServiceEndPoint = mock(ConnectivityServiceEndPoint.class);
        NrpConnectivityServiceEndPointAttrs attrs = mock(NrpConnectivityServiceEndPointAttrs.class);
        //UNI port mock
        ServiceInterfacePoint sipRef = TapiUtils.toSipRef(new Uuid(portName), ServiceInterfacePoint.class);
        when(connectivityServiceEndPoint.getServiceInterfacePoint())
                .thenReturn(sipRef);

        return new EndPoint(connectivityServiceEndPoint,attrs);
    }
}
