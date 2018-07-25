package org.opendaylight.unimgr.mef.nrp.ovs.util;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.unimgr.utils.CapabilitiesService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import java.util.function.BiFunction;

/**
 * @author bartosz.michalik@amartus.com
 */
public class OvsCapabilitiesService extends CapabilitiesService {
    public OvsCapabilitiesService(DataBroker dataBroker) {
        super(dataBroker);
    }

    public enum NodeCapability implements Capability<Node> {
        OVSDB((dbBroker,node) -> node.augmentation(OvsdbBridgeAugmentation.class) != null);

        private BiFunction<DataBroker, Node, Boolean> condition;

        NodeCapability(BiFunction<DataBroker, Node, Boolean> condition) {
            this.condition = condition;
        }

        @Override
        public BiFunction<DataBroker, Node, Boolean> getCondition() {
            return condition;
        }
    }
}
