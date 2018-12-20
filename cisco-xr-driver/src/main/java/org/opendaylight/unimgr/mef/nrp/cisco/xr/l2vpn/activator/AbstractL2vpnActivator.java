/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator;

import static org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort.toServicePort;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.InterfaceHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.L2vpnHelper;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivator;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.asr9k.policymgr.cfg.rev150518.PolicyManager;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.Database;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.XconnectGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.XconnectGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.XconnectGroupKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.P2pXconnects;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnect;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnectKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.Pseudowires;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.connectivity.service.end.point.attrs.NrpCarrierEthConnectivityEndPointResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Abstarct activator of VPLS-based L2 VPN on IOS-XR devices. It is responsible for handling activation and deactivation
 * process of VPN configuration and it provides generic transaction designated for this purpose.
 *
 * @author krzysztof.bijakowski@amartus.com
 */
public abstract class AbstractL2vpnActivator implements ResourceActivator {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractL2vpnActivator.class);
    private static final String NETCONF_TOPOLODY_NAME = "topology-netconf";
    private static final long mtu = 1500;

    protected DataBroker dataBroker;

    private MountPointService mountService;

    protected AbstractL2vpnActivator(DataBroker dataBroker, MountPointService mountService) {
        this.dataBroker = dataBroker;
        this.mountService = mountService;
    }

    @Override
    public void activate(List<EndPoint> endPoints, String serviceId) throws InterruptedException, ExecutionException {
        String innerName = getInnerName(serviceId);
        String outerName = getOuterName(serviceId);
        ServicePort port = null;
        ServicePort neighbor = null;
        for (EndPoint endPoint: endPoints) {
            if (port==null) {
                port = toServicePort(endPoint, NETCONF_TOPOLODY_NAME);
                NrpCarrierEthConnectivityEndPointResource attrs = endPoint.getAttrs() == null ? null : endPoint.getAttrs().getNrpCarrierEthConnectivityEndPointResource();
                if(attrs != null) {
                    port.setEgressBwpFlow(attrs.getEgressBwpFlow());
                    port.setIngressBwpFlow(attrs.getIngressBwpFlow());

                }
            } else {
                neighbor = toServicePort(endPoint, NETCONF_TOPOLODY_NAME);
            }
        }

        java.util.Optional<PolicyManager> qosConfig = activateQos(innerName, port);
        InterfaceConfigurations interfaceConfigurations = activateInterface(port, neighbor, mtu);
        Pseudowires pseudowires = activatePseudowire(neighbor);
        XconnectGroups xconnectGroups = activateXConnect(outerName, innerName, port, neighbor, pseudowires);
        L2vpn l2vpn = activateL2Vpn(xconnectGroups);

        doActivate(port.getNode().getValue(), interfaceConfigurations, l2vpn, qosConfig);
    }

    @Override
    public void deactivate(List<EndPoint> endPoints, String serviceId) throws InterruptedException, ExecutionException {
        String innerName = getInnerName(serviceId);
        String outerName = getOuterName(serviceId);
        ServicePort port = toServicePort(endPoints.stream().findFirst().get(), NETCONF_TOPOLODY_NAME);

        InstanceIdentifier<P2pXconnect> xconnectId = deactivateXConnect(outerName, innerName);
        InstanceIdentifier<InterfaceConfiguration> interfaceConfigurationId = deactivateInterface(port);

        doDeactivate(port.getNode().getValue(), xconnectId, interfaceConfigurationId);
    }

    // for now QoS is ignored
    protected void doActivate(String nodeName,
                              InterfaceConfigurations interfaceConfigurations,
                              L2vpn l2vpn,
                              java.util.Optional<PolicyManager> qosConfig) throws InterruptedException, ExecutionException {

        Optional<DataBroker> optional = getMountPointDataBroker(mountService, nodeName);
        if (!optional.isPresent()) {
            LOG.error("Could not retrieve MountPoint for {}", nodeName);
            return;
        }

        WriteTransaction transaction = optional.get().newWriteOnlyTransaction();
        transaction.merge(LogicalDatastoreType.CONFIGURATION, InterfaceHelper.getInterfaceConfigurationsId(), interfaceConfigurations);
        transaction.merge(LogicalDatastoreType.CONFIGURATION, L2vpnHelper.getL2vpnId(), l2vpn);
        transaction.commit().get();
    }

    protected void doDeactivate(String nodeName,
                                InstanceIdentifier<P2pXconnect> xconnectId,
                                InstanceIdentifier<InterfaceConfiguration> interfaceConfigurationId)
                                        throws InterruptedException, ExecutionException {

        Optional<DataBroker> optional = getMountPointDataBroker(mountService, nodeName);
        if (!optional.isPresent()) {
            LOG.error("Could not retrieve MountPoint for {}", nodeName);
            return;
        }

        WriteTransaction transaction = optional.get().newWriteOnlyTransaction();
        transaction.delete(LogicalDatastoreType.CONFIGURATION, xconnectId);
        transaction.delete(LogicalDatastoreType.CONFIGURATION, interfaceConfigurationId);
        transaction.commit().get();
    }

    protected abstract java.util.Optional<PolicyManager> activateQos(String name, ServicePort port);

    protected abstract InterfaceConfigurations activateInterface(ServicePort portA, ServicePort portZ, long mtu);

    protected abstract Pseudowires activatePseudowire(ServicePort neighbor);

    protected abstract XconnectGroups activateXConnect(String outerName, String innerName, ServicePort portA, ServicePort portZ, Pseudowires pseudowires);

    protected abstract L2vpn activateL2Vpn(XconnectGroups xconnectGroups);

    private InstanceIdentifier<P2pXconnect> deactivateXConnect(String outerName, String innerName) {
        return InstanceIdentifier.builder(L2vpn.class)
                .child(Database.class)
                .child(XconnectGroups.class)
                .child(XconnectGroup.class, new XconnectGroupKey(new CiscoIosXrString(outerName)))
                .child(P2pXconnects.class).child(P2pXconnect.class, new P2pXconnectKey(new CiscoIosXrString(innerName)))
                .build();
    }

    private InstanceIdentifier<InterfaceConfiguration> deactivateInterface(ServicePort port) {
        return InstanceIdentifier.builder(InterfaceConfigurations.class)
                .child(InterfaceConfiguration.class, new InterfaceConfigurationKey(new InterfaceActive("act"), InterfaceHelper.getInterfaceName(port)))
                .build();
    }

    protected abstract String getInnerName(String serviceId);
    protected abstract String getOuterName(String serviceId);

    /**
     * Find a node's NETCONF mount point and then retrieve its DataBroker.
     * e.
     * http://localhost:8080/restconf/config/network-topology:network-topology/
     *        topology/topology-netconf/node/{nodeName}/yang-ext:mount/
     */
    protected Optional<DataBroker> getMountPointDataBroker(MountPointService mountService, String nodeName) {
        NodeId nodeId = new NodeId(nodeName);

        InstanceIdentifier<Node> nodeInstanceId = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())))
                .child(Node.class, new NodeKey(nodeId))
                .build();

        final Optional<MountPoint> nodeOptional = mountService.getMountPoint(nodeInstanceId);

        if (!nodeOptional.isPresent()) {
            return Optional.empty();
        }

        MountPoint nodeMountPoint = nodeOptional.get();
        return Optional.of(nodeMountPoint.getService(DataBroker.class).get());
    }
}