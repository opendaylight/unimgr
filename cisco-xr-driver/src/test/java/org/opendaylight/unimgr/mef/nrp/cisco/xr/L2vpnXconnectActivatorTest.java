package org.opendaylight.unimgr.mef.nrp.cisco.xr;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator.L2vpnXconnectActivator;
import org.opendaylight.unimgr.mef.nrp.common.MountPointHelper;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPortBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MountPointHelper.class)
public class L2vpnXconnectActivatorTest extends AbstractDataBrokerTest {

    private L2vpnXconnectActivator l2vpnXconnectActivator;
    private MountPointService mountService;
    private DataBroker broker;
    private Optional<DataBroker> optBroker;

    @Before
    public void setUp(){
        broker = getDataBroker();

        optBroker = Optional.of(broker);

        PowerMockito.mockStatic(MountPointHelper.class);
        PowerMockito.when(MountPointHelper.getDataBroker(Mockito.anyObject(),Mockito.anyString())).thenReturn(optBroker);

        mountService = Mockito.mock(MountPointService.class);
        l2vpnXconnectActivator = new L2vpnXconnectActivator(broker,mountService);
    }

    @Test
    public void testActivate(){
        String nodeName = "NodeNameExample";
        String outerName = "OuterNameExample";
        String innerName = "InnernameExample";
        FcPort port = port("a", "localhost", "80");
        FcPort neighbor = port("z", "localhost", "8080");
        long mtu = 1500;

        //The following error is generated:
        //com.google.common.util.concurrent.UncheckedExecutionException: com.google.common.util.concurrent.UncheckedExecutionException: com.google.common.util.concurrent.UncheckedExecutionException: com.google.common.util.concurrent.UncheckedExecutionException: java.lang.IllegalStateException: Failed to instantiate prototype org.opendaylight.yangtools.binding.data.codec.gen.impl.DataObjectSerializerPrototype as org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations._interface.configuration.Mtus$StreamWriter
        //As far as I discovered, error is generated as described below:
        //at L2vpnActivator.doActivate()
        //at line with: transaction.merge(LogicalDatastoreType.CONFIGURATION, InterfaceHelper.getInterfaceConfigurationsId(), interfaceConfigurations);
        //at AbstractWriteTransaction.put(LogicalDatastoreType store, InstanceIdentifier<U> path, U data, boolean createParents)
        //by calling: Entry normalized = this.getCodec().toNormalizedNode(path, data);
        l2vpnXconnectActivator.activate(nodeName, outerName, innerName, port, neighbor, mtu);

        ReadOnlyTransaction transaction1 = optBroker.get().newReadOnlyTransaction();
        //Here I have to find out how to read stored data by activate() method
        //String output = transaction1.read(LogicalDatastoreType.CONFIGURATION, L2vpnHelper.getL2vpnId()).toString();

        //Here I would like to somehow verify the output, but first I would like to see how output looks
        //System.out.println(output);
    }

    private FcPort port(String topo, String host, String port) {
        return new FcPortBuilder()
                .setTopology(new TopologyId(topo))
                .setNode(new NodeId(host))
                .setTp(new TpId(port))
                .build();
    }
}
