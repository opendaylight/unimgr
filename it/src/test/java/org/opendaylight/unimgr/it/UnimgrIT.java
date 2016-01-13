package org.opendaylight.unimgr.it;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.math.BigInteger;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.uni.Speed;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class UnimgrIT extends AbstractMdsalTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(UnimgrIT.class);

    @Inject
    private BundleContext bundleContext;

    private String macAddress = "01:23:45:67:89:ab";
    private String macLayer = "IEEE 802.3-2005";
    private String mode = "Full Duplex";
    private String mtuSize = "0";
    private String physicalMedium = "UNI TypeFull Duplex 2 Physical Interface";
    private Speed speed = null;
    private String type = "";
    private String ipAddress = "10.0.0.1";

    @Test
    public void createAndDeleteUNITest() {
        assertNotNull(bundleContext);
        assertNotNull(getSession());

        DataBroker dataBroker =  getSession().getSALService(DataBroker.class);
        assertNotNull(dataBroker);

        initDatastore(dataBroker, LogicalDatastoreType.CONFIGURATION, new TopologyId(new Uri("unimgr:uni")));
        initDatastore(dataBroker, LogicalDatastoreType.OPERATIONAL, new TopologyId(new Uri("unimgr:uni")));

        UniAugmentation uni = new UniAugmentationBuilder()
                .setMacAddress(new MacAddress(macAddress))
                .setMacLayer(macLayer)
                .setMode(mode)
                .setMtuSize(BigInteger.valueOf(Long.valueOf(mtuSize)))
                .setPhysicalMedium(physicalMedium)
                .setSpeed(speed)
                .setType(type)
                .setIpAddress(new IpAddress(ipAddress.toCharArray()))
                .build();

        boolean hasCreated = createUniNode(dataBroker, uni);
        assertTrue(hasCreated);

        boolean hasDeleted = deleteNode(dataBroker, uni);
        assertTrue(hasDeleted);
    }

    private void initDatastore(final DataBroker dataBroker, final LogicalDatastoreType type, TopologyId topoId) {
        InstanceIdentifier<Topology> path = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class,
                new TopologyKey(topoId));
        initializeTopology(dataBroker, type);
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        CheckedFuture<Optional<Topology>, ReadFailedException> unimgrTp = transaction.read(type, path);
        try {
            if (!unimgrTp.get().isPresent()) {
                TopologyBuilder tpb = new TopologyBuilder();
                tpb.setTopologyId(topoId);
                transaction.put(type, path, tpb.build());
                transaction.submit();
            } else {
                transaction.cancel();
            }
        } catch (Exception e) {
            LOG.error("Error initializing unimgr topology", e);
        }
    }

    private void initializeTopology(final DataBroker dataBroker, LogicalDatastoreType type) {
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        InstanceIdentifier<NetworkTopology> path = InstanceIdentifier.create(NetworkTopology.class);
        CheckedFuture<Optional<NetworkTopology>, ReadFailedException> topology = transaction.read(type,path);
        try {
            if (!topology.get().isPresent()) {
                NetworkTopologyBuilder ntb = new NetworkTopologyBuilder();
                transaction.put(type,path,ntb.build());
                transaction.submit();
            } else {
                transaction.cancel();
            }
        } catch (Exception e) {
            LOG.error("Error initializing unimgr topology {}",e);
        }
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
                .artifactId("unimgr-features")
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

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        System.out.println("TestProbeBuilder gets called");
        probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*");
        return probe;
    }

    private static boolean createUniNode(DataBroker dataBroker, UniAugmentation uni) {
        NodeId uniNodeId = new NodeId(createUniNodeId(uni.getIpAddress()));
        boolean result = false;
        try {
            InstanceIdentifier<Node> nodePath = InstanceIdentifier
                    .create(NetworkTopology.class)
                    .child(Topology.class,
                            new TopologyKey(new TopologyId(new Uri("unimgr:uni"))))
                    .child(Node.class,
                            new NodeKey(uniNodeId));

            NodeKey uniNodeKey = new NodeKey(uniNodeId);
            Node nodeData = new NodeBuilder()
                                .setNodeId(uniNodeId)
                                .setKey(uniNodeKey)
                                .addAugmentation(UniAugmentation.class, uni)
                                .build();
            WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            transaction.put(LogicalDatastoreType.CONFIGURATION, nodePath, nodeData);
            CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
            future.checkedGet();
            result = true;
            LOG.info("Created and submitted a new Uni node {}", nodeData.getNodeId());
        } catch (Exception e) {
            LOG.error("Exception while creating Uni Node" + "Uni Node Id: {}", uniNodeId);
        }
        return result;
    }

    private static NodeId createUniNodeId(IpAddress ipAddress) {
        return new NodeId("uni://" + ipAddress.getIpv4Address().getValue().toString());
    }

    private static boolean deleteNode(DataBroker dataBroker, UniAugmentation uni) {
        NodeId uniNodeId = new NodeId(createUniNodeId(uni.getIpAddress()));
        InstanceIdentifier<Node> genericNode = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class,
                        new TopologyKey(new TopologyId(new Uri("unimgr:uni"))))
                .child(Node.class,
                        new NodeKey(uniNodeId));

        LOG.info("Received a request to delete node {}", genericNode);
        boolean result = false;
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.delete(LogicalDatastoreType.CONFIGURATION, genericNode);
        try {
            transaction.submit().checkedGet();
            result = true;
        } catch (TransactionCommitFailedException e) {
            LOG.error("Unable to remove node with Iid {} from store {}.", genericNode, LogicalDatastoreType.CONFIGURATION);
        }
        return result;
    }
}
