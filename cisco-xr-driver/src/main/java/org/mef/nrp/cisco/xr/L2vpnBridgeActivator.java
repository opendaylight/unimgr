package org.mef.nrp.cisco.xr;

import java.util.LinkedList;
import java.util.List;

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
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.InterfaceConfiguration3;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.InterfaceConfiguration3Builder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpnBuilder;
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
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.GFcPort;
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

public class L2vpnBridgeActivator implements ResourceActivator {

    private static final Logger log = LoggerFactory.getLogger(L2vpnBridgeActivator.class);
    private MountPointService mountService;

    public L2vpnBridgeActivator(DataBroker dataBroker, MountPointService mountService) {
        this.mountService = mountService;
    }

    @Override
    public void activate(String nodeName, String outerName, String innerName, GFcPort port, GFcPort neighbor, long mtu) {

        String aEndLtpId = port.getLtpRefList().get(0).getValue();
        String zEndLtpId = neighbor.getLtpRefList().get(0).getValue();

        InterfaceName aEndIfName = new InterfaceName(aEndLtpId.split(":")[1]);
        InterfaceName zEndIfName = new InterfaceName(zEndLtpId.split(":")[1]);
        InterfaceName[] both = new InterfaceName[] { aEndIfName, zEndIfName };

        List<InterfaceConfiguration> intConfigs = new LinkedList<>();
        for (InterfaceName ifName : both) {
            InterfaceConfigurationBuilder intConfigBuilder = new InterfaceConfigurationBuilder();
            intConfigBuilder.setInterfaceName(ifName).setActive(new InterfaceActive("act")).setShutdown(Boolean.FALSE);

            L2Transport l2transport = new L2TransportBuilder().setEnabled(true).build();
            InterfaceConfiguration3 augmentation = new InterfaceConfiguration3Builder().setL2Transport(l2transport)
                    .build();
            intConfigBuilder.addAugmentation(InterfaceConfiguration3.class, augmentation);

            intConfigs.add(intConfigBuilder.build());
        }
        InterfaceConfigurationsBuilder intConfigsBuilder = new InterfaceConfigurationsBuilder();
        intConfigsBuilder.setInterfaceConfiguration(intConfigs);

        InstanceIdentifier<InterfaceConfigurations> intConfigsId = InstanceIdentifier
                .builder(InterfaceConfigurations.class).build();

        AttachmentCircuitBuilder attachmentCircuitBuilderA = new AttachmentCircuitBuilder();
        attachmentCircuitBuilderA.setName(aEndIfName).setEnable(Boolean.TRUE);
        AttachmentCircuitBuilder attachmentCircuitBuilderZ = new AttachmentCircuitBuilder();
        attachmentCircuitBuilderZ.setName(zEndIfName).setEnable(Boolean.TRUE);

        List<AttachmentCircuit> attachmentCircuits = new LinkedList<>();
        attachmentCircuits.add(attachmentCircuitBuilderA.build());
        attachmentCircuits.add(attachmentCircuitBuilderZ.build());

        AttachmentCircuitsBuilder attachmentCircuitsBuilder = new AttachmentCircuitsBuilder();
        attachmentCircuitsBuilder.setAttachmentCircuit(attachmentCircuits);

        List<Pseudowire> pseudowires = new LinkedList<>();
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
        xconnectGroupBuilder.setKey(new XconnectGroupKey(new CiscoIosXrString("local")));
        xconnectGroupBuilder.setName(new CiscoIosXrString("local")).setP2pXconnects(p2pXconnectsBuilder.build());

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

        WriteTransaction w = null;
        try {
            w = nodeDataBroker.newWriteOnlyTransaction();
            w.merge(LogicalDatastoreType.CONFIGURATION, intConfigsId, intConfigsBuilder.build());
            w.merge(LogicalDatastoreType.CONFIGURATION, l2vpnId, l2vpnBuilder.build());
            try {
                w.submit().checkedGet();
                log.info("Service activated: {} {} {}", nodeName, outerName, innerName);
            } catch (TransactionCommitFailedException e) {
                log.error("Transaction failed", e);
            }
        } catch (Throwable t) {
            if (w != null)
                w.cancel();
            log.error("Failed to create write transaction", t);
        }
    }

    @Override
    public void deactivate(String nodeName, String outerName, String innerName, GFcPort port, GFcPort neighbor,
            long mtu) {
        String portLtpId = port.getLtpRefList().get(0).getValue();
        InterfaceName interfaceName = new InterfaceName(portLtpId.split(":")[1]);

        InterfaceActive intActive = new InterfaceActive("act");

        InstanceIdentifier<P2pXconnect> p2pId = InstanceIdentifier.builder(L2vpn.class).child(Database.class)
                .child(XconnectGroups.class)
                .child(XconnectGroup.class, new XconnectGroupKey(new CiscoIosXrString("local")))
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
