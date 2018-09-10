/*
 * Copyright © 2016 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.it;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDestKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSourceKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FluentFuture;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class UnimgrIT extends AbstractMdsalTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(UnimgrIT.class);
    private DataBroker dataBroker;

    private static final String MAC_ADDRESS_1 = "68:5b:35:bc:0f:7d";
    private static final String MAC_ADDRESS_2 = "68:5b:35:bc:0f:7e";
    private static final String MAC_LAYER = "IEEE 802.3-2005";
    private static final String MODE = "Full Duplex";
    private static final String MTU_SIZE = "0";
    private static final String PHY_MEDIUM = "UNI TypeFull Duplex 2 Physical Interface";
    private static final String TYPE = "";
    private static final String IP_1 = "10.0.0.1";
    private static final String IP_2 = "10.0.0.2";
    private static final String EVC_ID_1 = "1";

    @Override
    public void setup() throws Exception {
        super.setup();
        Thread.sleep(3000);
        dataBroker =  getSession().getSALService(DataBroker.class);
        Assert.assertNotNull("db should not be null", dataBroker);
    }

    @Override
    public String getModuleName() {
        return "unimgr";
    }

    @Override
    public String getInstanceName() {
        return "unimgr-default";
    }

    @Override
    public MavenUrlReference getFeatureRepo() {
        return maven()
                .groupId("org.opendaylight.unimgr")
                .artifactId("features4-unimgr")
                .classifier("features")
                .type("xml")
                .versionAsInProject();
    }

    @Override
    public String getFeatureName() {
        return "odl-unimgr";
    }

    @Override
    public Option getLoggingOption() {
        Option option = editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                logConfiguration(UnimgrIT.class),
                LogLevel.INFO.name());
        option = composite(option, super.getLoggingOption());
        return option;
    }

    @Test
    public void testUnimgrFeatureLoad() {
        Assert.assertTrue(true);
    }

    @Test
    public void testUnimgr() {
        InstanceIdentifier<Topology> uniTopoPath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class,
                        new TopologyKey(new TopologyId(new Uri("unimgr:uni"))));
        InstanceIdentifier<Topology> evcTopoPath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class,
                        new TopologyKey(new TopologyId(new Uri("unimgr:evc"))));
        InstanceIdentifier<Topology> ovdbTopoPath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(new Uri("ovsdb:1"))));

        // Read from md-sal and check if it is initialized with Uni and Evc augmentations
        Topology topology = read(LogicalDatastoreType.CONFIGURATION, uniTopoPath);
        Assert.assertNotNull("UNI Topology could not be found in " + LogicalDatastoreType.CONFIGURATION,
                topology);
        topology = read(LogicalDatastoreType.OPERATIONAL, uniTopoPath);
        Assert.assertNotNull("UNI Topology could not be found in " + LogicalDatastoreType.OPERATIONAL,
                topology);
        topology = read(LogicalDatastoreType.CONFIGURATION, evcTopoPath);
        Assert.assertNotNull("EVC Topology could not be found in " + LogicalDatastoreType.CONFIGURATION,
                topology);
        topology = read(LogicalDatastoreType.OPERATIONAL, evcTopoPath);
        Assert.assertNotNull("EVC Topology could not be found in " + LogicalDatastoreType.OPERATIONAL,
                topology);
        topology = read(LogicalDatastoreType.CONFIGURATION, ovdbTopoPath);
        Assert.assertNotNull("OVSDB Topology could not be found in " + LogicalDatastoreType.CONFIGURATION,
                topology);
        topology = read(LogicalDatastoreType.CONFIGURATION, ovdbTopoPath);
        Assert.assertNotNull("OVSDB Topology could not be found in " + LogicalDatastoreType.OPERATIONAL,
                topology);
    }

    public void createAndDeleteUNITest() {
        LOG.info("Test for create and delete UNI");

        InstanceIdentifier<Node> nodePath = createUniNode(MAC_ADDRESS_1, IP_1);
        Assert.assertNotNull(nodePath);
        Assert.assertTrue(validateUni(true, nodePath));

        InstanceIdentifier<Node> deletedNodePath = deleteNode(MAC_ADDRESS_1, IP_1);
        Assert.assertNotNull(deletedNodePath);
        Assert.assertTrue(validateUni(false, deletedNodePath));
    }

    public void testCreateAndDeleteEvc() {
        LOG.info("Test for create Evc");
        // Create an evc between the two Uni nodes
        InstanceIdentifier<Link> evcIid = createEvcLink(IP_1, MAC_ADDRESS_1, IP_2, MAC_ADDRESS_2, EVC_ID_1);
        Assert.assertNotNull(evcIid);

        // Validate Evc create operation
        boolean status = validateEvc(true, EVC_ID_1);
        Assert.assertTrue(status);

        //Delete the Evc
        evcIid = deleteEvc(EVC_ID_1);
        Assert.assertNotNull(evcIid);

        // Validate Evc delete operation
        status = validateEvc(false, EVC_ID_1);
        Assert.assertTrue(status);
    }

    private boolean validateEvc(boolean forCreate, String evcId) {
        InstanceIdentifier<Link> iid = getEvcLinkIid(evcId);
        Link evc = read(LogicalDatastoreType.CONFIGURATION, iid);
        if (forCreate && evc != null) {
            return true;
        } else if (!forCreate && evc == null) {
            return true;
        }
        return false;
    }

    private InstanceIdentifier<Node> createUniNode(String macAddress, String ipAddress) {
        UniAugmentation uni = new UniAugmentationBuilder()
                .setMacAddress(new MacAddress(macAddress))
                .setMacLayer(MAC_LAYER)
                .setMode(MODE)
                .setMtuSize(BigInteger.valueOf(Long.valueOf(MTU_SIZE)))
                .setPhysicalMedium(PHY_MEDIUM)
                .setType(TYPE)
                .setIpAddress(new IpAddress(ipAddress.toCharArray()))
                .build();

        NodeId uniNodeId = new NodeId(new NodeId("uni://" + uni.getIpAddress().getIpv4Address().getValue().toString()));
        InstanceIdentifier<Node> uniNodeIid = null;
        uniNodeIid = getUniIid("uni://" + uni.getIpAddress().getIpv4Address().getValue().toString());
        NodeKey uniNodeKey = new NodeKey(uniNodeId);
        Node nodeData = new NodeBuilder()
                                    .setNodeId(uniNodeId)
                                    .setKey(uniNodeKey)
                                    .addAugmentation(UniAugmentation.class, uni)
                                    .build();
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        try {
            writeUNI(uniNodeIid, nodeData, transaction);
            LOG.info("Created and submitted a new Uni node {}", nodeData.getNodeId());
            return uniNodeIid;
        } catch (Exception e) {
            transaction.cancel();
            LOG.error("Could not create Uni Node - Id: {}, {}", uniNodeId, e);
        }
        return null;
    }

    private void writeUNI(InstanceIdentifier<Node> uniNodeIid, Node nodeData, WriteTransaction transaction) throws TransactionCommitFailedException {
        transaction.put(LogicalDatastoreType.CONFIGURATION, uniNodeIid, nodeData);
        FluentFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        future.checkedGet();
    }

    private InstanceIdentifier<Link> deleteEvc(String evcId) {
        LinkId evcLinkId = new LinkId(new LinkId("evc://" + evcId));
        InstanceIdentifier<Link> evcLinkIid = null;
        evcLinkIid = getEvcLinkIid(evcId);
        if (evcLinkIid != null) {
            WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            transaction.delete(LogicalDatastoreType.CONFIGURATION, evcLinkIid);
            FluentFuture<Void, TransactionCommitFailedException> future = transaction.submit();
            try {
                future.checkedGet();
            } catch (TransactionCommitFailedException e) {
                LOG.error("Error while deleting Evc {}", evcLinkIid);
            }
            LOG.info("Deleted an Evc link {}", evcLinkId);
        }
        return evcLinkIid;
    }

    private InstanceIdentifier<Link> createEvcLink(String srcUniIp, String srcMac,
            String dstUniIp, String dstMac, String evcId) {
        // Create two Uni nodes before creating an Evc
        InstanceIdentifier<Node> uniIid = createUniNode(srcMac, srcUniIp);
        Assert.assertNotNull(uniIid);

        uniIid = createUniNode(dstMac, dstUniIp);
        Assert.assertNotNull(uniIid);

        // Create Evc link between the two Uni Nodes
        List<UniSource> src = new ArrayList<>();
        InstanceIdentifier<?> srcUniIid = getUniIid("uni://" + srcUniIp);
        UniSource uniSrc = new UniSourceBuilder()
                .setIpAddress(new IpAddress(srcUniIp.toCharArray()))
                .setOrder((short) 1)
                .setKey(new UniSourceKey((short) 1))
                .setUni(srcUniIid)
                .build();
        src.add(uniSrc);

        List<UniDest> dst = new ArrayList<>();
        InstanceIdentifier<?> dstUniIid = getUniIid("uni://" + dstUniIp);;
        UniDest uniDst = new UniDestBuilder()
                .setIpAddress(new IpAddress(dstUniIp.toCharArray()))
                .setOrder((short) 2)
                .setKey(new UniDestKey((short) 2))
                .setUni(dstUniIid)
                .build();
        dst.add(uniDst);

        EvcAugmentation evc = new EvcAugmentationBuilder()
                .setUniDest(dst)
                .setUniSource(src)
                .build();

        LinkId evcLinkId = new LinkId(new LinkId("evc://" + evcId));
        InstanceIdentifier<Link> evcLinkIid = null;
        try {
            evcLinkIid = getEvcLinkIid(evcId);
            LinkKey evcLinkKey = new LinkKey(evcLinkId);
            Source mandatorySrcNode = new SourceBuilder().setSourceNode(new NodeId("uni://" + srcUniIp)).build();
            Destination mandatoryDstNode = new DestinationBuilder().setDestNode(new NodeId("uni://" + dstUniIp)).build();
            Link linkData = new LinkBuilder()
                        .setKey(evcLinkKey)
                        .setSource(mandatorySrcNode)
                        .setDestination(mandatoryDstNode)
                        .setLinkId(evcLinkId)
                        .addAugmentation(EvcAugmentation.class, evc)
                        .build();
            WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            transaction.put(LogicalDatastoreType.CONFIGURATION, evcLinkIid, linkData);
            FluentFuture<Void, TransactionCommitFailedException> future = transaction.submit();
            future.checkedGet();
            LOG.info("Created and submitted a new Evc link {}", evcLinkId);
        } catch (Exception e) {
            LOG.error("Exception while creating Evc " + "Evc link Id: {}, {}", evcLinkId, e);
            return null;
        }
        return evcLinkIid;
    }

    private InstanceIdentifier<Node> getUniIid(String nodeId) {
        NodeId uniNodeId = new NodeId(new NodeId(nodeId));
        InstanceIdentifier<Node> uniNodeIid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(new Uri("unimgr:uni"))))
                .child(Node.class, new NodeKey(uniNodeId));
        return uniNodeIid;
    }

    private InstanceIdentifier<Link> getEvcLinkIid(String linkId) {
        LinkId evcLinkId = new LinkId(new LinkId("evc://" + linkId));
        InstanceIdentifier<Link> linkPath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class,new TopologyKey(new TopologyId(new Uri("unimgr:evc"))))
                .child(Link.class, new LinkKey(evcLinkId));
        return linkPath;
    }

    private <D extends org.opendaylight.yangtools.yang.binding.DataObject> D read(
            final LogicalDatastoreType store, final InstanceIdentifier<D> path)  {
        D result = null;
        final ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        Optional<D> optionalDataObject;
        FluentFuture<Optional<D>, ReadFailedException> future = transaction.read(store, path);
        try {
            optionalDataObject = future.checkedGet();
            if (optionalDataObject.isPresent()) {
                result = optionalDataObject.get();
            } else {
                LOG.error("{}: Failed to read {}",
                        Thread.currentThread().getStackTrace()[1], path);
            }
        } catch (ReadFailedException e) {
            LOG.error("Failed to read {} ", path, e);
        }
        transaction.close();
        return result;
    }

    private boolean validateUni(boolean forCreate, InstanceIdentifier<Node> iid) {
        Node uni = read(LogicalDatastoreType.CONFIGURATION, iid);
        if (forCreate && uni != null) {
            return true;
        } else if (!forCreate && uni == null) {
            return true;
        }
        return false;
    }

    private InstanceIdentifier<Node> deleteNode(String macAddress, String ipAddress) {
        UniAugmentation uni = new UniAugmentationBuilder().setMacAddress(new MacAddress(macAddress))
                .setMacLayer(MAC_LAYER).setMode(MODE).setMtuSize(BigInteger.valueOf(Long.valueOf(MTU_SIZE)))
                .setPhysicalMedium(PHY_MEDIUM).setType(TYPE).setIpAddress(new IpAddress(ipAddress.toCharArray()))
                .build();

        NodeId uniNodeId = new NodeId(new NodeId("uni://" + uni.getIpAddress().getIpv4Address().getValue().toString()));
        InstanceIdentifier<Node> genericNode = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class,
                        new TopologyKey(new TopologyId(new Uri("unimgr:uni"))))
                .child(Node.class,
                        new NodeKey(uniNodeId));

        LOG.info("Received a request to delete node {}", genericNode);
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.delete(LogicalDatastoreType.CONFIGURATION, genericNode);
        try {
            transaction.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("Unable to remove node with Iid {} from store {}.", genericNode, LogicalDatastoreType.CONFIGURATION);
            return null;
        }
        return genericNode;
    }
}
