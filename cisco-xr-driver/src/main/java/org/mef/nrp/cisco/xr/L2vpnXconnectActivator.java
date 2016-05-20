package org.mef.nrp.cisco.xr;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.mef.nrp.impl.ResourceActivator;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurationsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations._interface.configuration.MtusBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations._interface.configuration.mtus.Mtu;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations._interface.configuration.mtus.MtuBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.InterfaceConfiguration3;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.InterfaceConfiguration3Builder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpnBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.PseudowireIdRange;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.PseudowireLabelRange;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109._interface.configurations._interface.configuration.L2Transport;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109._interface.configurations._interface.configuration.L2TransportBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.Database;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.DatabaseBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.XconnectGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.XconnectGroupsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.XconnectGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.XconnectGroupBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.XconnectGroupKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.P2pXconnects;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.P2pXconnectsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnect;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnectBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnectKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.AttachmentCircuitsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.PseudowiresBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.attachment.circuits.AttachmentCircuit;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.attachment.circuits.AttachmentCircuitBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.Pseudowire;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.PseudowireBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.pseudowire.Neighbor;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.pseudowire.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.p2p.xconnect.pseudowires.pseudowire.pseudowire.content.MplsStaticLabelsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.GFcPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

public class L2vpnXconnectActivator implements ResourceActivator {

    private static final Logger log = LoggerFactory.getLogger(L2vpnXconnectActivator.class);
    private static final Map<String, String> loopbackMap = ImmutableMap.of("asr-101", "192.168.0.1", "asr-102",
            "192.168.0.2", "asr-103", "192.168.0.3");
    private static final AtomicLong pwIdGenerator = new AtomicLong(2000L);
    private MountPointService mountService;

    public L2vpnXconnectActivator(DataBroker dataBroker, MountPointService mountService) {
        this.mountService = mountService;
    }

    @Override
    public void activate(String nodeName, String outerName, String innerName, GFcPort port, GFcPort neighbor, long mtu) {

        String portLtpId = port.getLtpRefList().get(0).getValue();
        String neighborLtpId = neighbor.getLtpRefList().get(0).getValue();

        String neighborHostname = neighborLtpId.split(":")[0];
        InterfaceName interfaceName = new InterfaceName(portLtpId.split(":")[1]);

        // XXX: need to flesh out real method to find neighbor's loopback
        String neighborLoopback = loopbackMap.get(neighborHostname);
        if (neighborLoopback == null) {
            log.warn("No loopback address found for {}", neighborHostname);
            neighborLoopback = "127.0.0.1";
        }

        Ipv4AddressNoZone neighborAddress = new Ipv4AddressNoZone(neighborLoopback);
        InterfaceActive intActive = new InterfaceActive("act");

        // XXX: need to implement real pseudowire-id generator
        long pwIdVal = pwIdGenerator.getAndIncrement();

        MtuBuilder mtuBuilder = new MtuBuilder();
        mtuBuilder.setMtu(mtu);
        mtuBuilder.setOwner(new CiscoIosXrString("GigabitEthernet"));

        List<Mtu> mtus = new LinkedList<>();
        mtus.add(mtuBuilder.build());
        MtusBuilder mtusBuilder = new MtusBuilder();
        mtusBuilder.setMtu(mtus);

        InterfaceConfigurationBuilder intConfigBuilder = new InterfaceConfigurationBuilder();
        intConfigBuilder.setInterfaceName(interfaceName).setActive(intActive).setMtus(mtusBuilder.build())
                .setShutdown(Boolean.FALSE);

        L2Transport l2transport = new L2TransportBuilder().setEnabled(true).build();
        InterfaceConfiguration3 augmentation = new InterfaceConfiguration3Builder().setL2Transport(l2transport).build();
        intConfigBuilder.addAugmentation(InterfaceConfiguration3.class, augmentation);

        List<InterfaceConfiguration> intConfigs = new LinkedList<>();
        intConfigs.add(intConfigBuilder.build());
        InterfaceConfigurationsBuilder intConfigsBuilder = new InterfaceConfigurationsBuilder();
        intConfigsBuilder.setInterfaceConfiguration(intConfigs);

        InstanceIdentifier<InterfaceConfigurations> intConfigsId = InstanceIdentifier
                .builder(InterfaceConfigurations.class).build();

        AttachmentCircuitBuilder attachmentCircuitBuilder = new AttachmentCircuitBuilder();
        attachmentCircuitBuilder.setName(interfaceName).setEnable(Boolean.TRUE);

        List<AttachmentCircuit> attachmentCircuits = new LinkedList<>();
        attachmentCircuits.add(attachmentCircuitBuilder.build());
        AttachmentCircuitsBuilder attachmentCircuitsBuilder = new AttachmentCircuitsBuilder();
        attachmentCircuitsBuilder.setAttachmentCircuit(attachmentCircuits);

        PseudowireLabelRange label = new PseudowireLabelRange(pwIdVal);
        MplsStaticLabelsBuilder labelBuilder = new MplsStaticLabelsBuilder();
        labelBuilder.setLocalStaticLabel(label).setRemoteStaticLabel(label);

        NeighborBuilder neighborBuilder = new NeighborBuilder();
        neighborBuilder.setNeighbor(neighborAddress).setMplsStaticLabels(labelBuilder.build())
                .setXmlClass(new CiscoIosXrString("static"));

        List<Neighbor> neighbors = new LinkedList<>();
        neighbors.add(neighborBuilder.build());

        // XXX
        PseudowireIdRange pwId = new PseudowireIdRange(pwIdVal);
        PseudowireBuilder pseudowireBuilder = new PseudowireBuilder();
        pseudowireBuilder.setNeighbor(neighbors).setPseudowireId(pwId);

        List<Pseudowire> pseudowires = new LinkedList<>();
        pseudowires.add(pseudowireBuilder.build());
        PseudowiresBuilder pseudowiresBuilder = new PseudowiresBuilder();
        pseudowiresBuilder.setPseudowire(pseudowires);

        P2pXconnectBuilder p2pXconnectBuilder = new P2pXconnectBuilder();
        p2pXconnectBuilder.setName(new CiscoIosXrString(innerName))
                .setAttachmentCircuits(attachmentCircuitsBuilder.build()).setPseudowires(pseudowiresBuilder.build());

        List<P2pXconnect> p2pXconnects = new LinkedList<>();
        p2pXconnects.add(p2pXconnectBuilder.build());
        P2pXconnectsBuilder p2pXconnectsBuilder = new P2pXconnectsBuilder();
        p2pXconnectsBuilder.setP2pXconnect(p2pXconnects);

        XconnectGroupBuilder xconnectGroupBuilder = new XconnectGroupBuilder();
        xconnectGroupBuilder.setKey(new XconnectGroupKey(new CiscoIosXrString(outerName)));
        xconnectGroupBuilder.setName(new CiscoIosXrString(outerName)).setP2pXconnects(p2pXconnectsBuilder.build());

        List<XconnectGroup> xconnectGroups = new LinkedList<>();
        xconnectGroups.add(xconnectGroupBuilder.build());
        XconnectGroupsBuilder xconnectGroupsBuilder = new XconnectGroupsBuilder();
        xconnectGroupsBuilder.setXconnectGroup(xconnectGroups);

        DatabaseBuilder dbBuilder = new DatabaseBuilder();
        dbBuilder.setXconnectGroups(xconnectGroupsBuilder.build());

        L2vpnBuilder l2vpnBuilder = new L2vpnBuilder();
        l2vpnBuilder.setDatabase(dbBuilder.build());

        InstanceIdentifier<L2vpn> l2vpnId = InstanceIdentifier.builder(L2vpn.class).build();

        DataBroker nodeDataBroker = getNodeDataBroker(nodeName);
        if (nodeDataBroker == null) {
            log.error("Could not retrieve MountPoint for {}", nodeName);
            return;
        }
        WriteTransaction w = nodeDataBroker.newWriteOnlyTransaction();
        w.merge(LogicalDatastoreType.CONFIGURATION, intConfigsId, intConfigsBuilder.build());
        w.merge(LogicalDatastoreType.CONFIGURATION, l2vpnId, l2vpnBuilder.build());

        try {
            w.submit().checkedGet();
            log.info("Service activated: {} {} {}", nodeName, outerName, innerName);
        } catch (TransactionCommitFailedException e) {
            log.error("Transaction failed", e);
        }
    }

    @Override
    public void deactivate(String nodeName, String outerName, String innerName, GFcPort port, GFcPort neighbor,
            long mtu) {
        String portLtpId = port.getLtpRefList().get(0).getValue();
        String neighborLtpId = neighbor.getLtpRefList().get(0).getValue();

        String neighborHostname = neighborLtpId.split(":")[0];
        InterfaceName interfaceName = new InterfaceName(portLtpId.split(":")[1]);

        // XXX: need to flesh out real method to find neighbor's loopback
        String neighborLoopback = loopbackMap.get(neighborHostname);
        if (neighborLoopback == null) {
            log.warn("No loopback address found for {}", neighborHostname);
            neighborLoopback = "127.0.0.1";
        }

        Ipv4AddressNoZone neighborAddress = new Ipv4AddressNoZone(neighborLoopback);
        InterfaceActive intActive = new InterfaceActive("act");

        InstanceIdentifier<P2pXconnect> p2pId = InstanceIdentifier.builder(L2vpn.class).child(Database.class)
                .child(XconnectGroups.class)
                .child(XconnectGroup.class, new XconnectGroupKey(new CiscoIosXrString(outerName)))
                .child(P2pXconnects.class).child(P2pXconnect.class, new P2pXconnectKey(new CiscoIosXrString(innerName)))
                .build();

        InstanceIdentifier<InterfaceConfiguration> intConfigId = InstanceIdentifier
                .builder(InterfaceConfigurations.class)
                .child(InterfaceConfiguration.class, new InterfaceConfigurationKey(intActive, interfaceName)).build();

        DataBroker nodeDataBroker = getNodeDataBroker(nodeName);
        if (nodeDataBroker == null) {
            log.error("Could not retrieve MountPoint for {}", nodeName);
            return;
        }
        WriteTransaction w = nodeDataBroker.newWriteOnlyTransaction();
        w.delete(LogicalDatastoreType.CONFIGURATION, p2pId);
        w.delete(LogicalDatastoreType.CONFIGURATION, intConfigId);

        try {
            w.submit().checkedGet();
            log.info("Service deactivated: {} {} {}", nodeName, outerName, innerName);
        } catch (TransactionCommitFailedException e) {
            log.error("Transaction failed", e);
        }
    }

    private DataBroker getNodeDataBroker(String nodeName) {
        NodeId nodeId = new NodeId(nodeName);

        InstanceIdentifier<Node> nodeInstanceId = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())))
                .child(Node.class, new NodeKey(nodeId)).build();

        final Optional<MountPoint> nodeOptional = mountService.getMountPoint(nodeInstanceId);

        if (!nodeOptional.isPresent()) {
            return null;
        }

        MountPoint nodeMountPoint = nodeOptional.get();
        return nodeMountPoint.getService(DataBroker.class).get();
    }

}
