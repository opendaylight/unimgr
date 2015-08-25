package org.opendaylight.vcpe.command;

import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.vcpe.impl.VcpeConstants;
import org.opendaylight.vcpe.impl.VcpeMapper;
import org.opendaylight.vcpe.impl.VcpeUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vcpe.rev150622.evcs.Evc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vcpe.rev150622.unis.Uni;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class EvcCreateCommand extends AbstractCreateCommand {

    private static final Logger LOG = LoggerFactory.getLogger(EvcCreateCommand.class);

    public EvcCreateCommand(DataBroker dataBroker,
            Map<InstanceIdentifier<?>, DataObject> changes) {
        super.dataBroker = dataBroker;
        super.changes = changes;
    }

    @Override
    public void execute() {
        for (Entry<InstanceIdentifier<?>, DataObject> created : changes
                .entrySet()) {
            if (created.getValue() != null && created.getValue() instanceof Evc) {
                Evc evc = (Evc) created.getValue();
                LOG.info("New EVC created with id {}.", evc.getId());
                if (evc.getUniDest() == null || evc.getUniDest().isEmpty()) {
                    LOG.error("Destination UNI cannot be null.");
                    break;
                }
                if (evc.getUniSource() == null || evc.getUniSource().isEmpty()) {
                    LOG.error("Source UNI cannot be null.");
                    break;
                }
                // Get the destination UNI
                NodeId destUniNodeID = evc.getUniDest().get(0).getUni();
                InstanceIdentifier<Uni> destinationNodeIid = VcpeMapper.getUniIid(destUniNodeID);
                Optional<Uni> optionalDestination = VcpeUtils.readUniNode(dataBroker, destinationNodeIid);
                Uni destinationUni = optionalDestination.get();
                NodeId ovsdbDestinationNodeId = VcpeMapper.createNodeId(destinationUni.getIpAddress());
                // Get the source UNI
                NodeId sourceUniNodeID = evc.getUniSource().get(0).getUni();
                InstanceIdentifier<Uni> sourceNodeIid = VcpeMapper.getUniIid(sourceUniNodeID);
                Optional<Uni> optionalSource = VcpeUtils.readUniNode(dataBroker, sourceNodeIid);
                Uni sourceUni = optionalSource.get();
                NodeId ovsdbSourceNodeId = VcpeMapper.createNodeId(sourceUni.getIpAddress());

                // Set source
                Node sourceBr1 = VcpeUtils.readNode(
                        dataBroker,
                        VcpeMapper.getOvsdbBridgeNodeIID(ovsdbSourceNodeId,
                                VcpeConstants.DEFAULT_BRIDGE_NAME)).get();
                VcpeUtils.createTerminationPointNode(dataBroker,
                        destinationUni, sourceBr1,
                        VcpeConstants.DEFAULT_BRIDGE_NAME,
                        VcpeConstants.DEFAULT_INTERNAL_IFACE, null);
                Node sourceBr2 = VcpeUtils.readNode(
                        dataBroker,
                        VcpeMapper.getOvsdbBridgeNodeIID(ovsdbSourceNodeId,
                                VcpeConstants.DEFAULT_BRIDGE_NAME)).get();
                VcpeUtils.createGreTunnel(dataBroker, sourceUni,
                        destinationUni, sourceBr2,
                        VcpeConstants.DEFAULT_BRIDGE_NAME, "gre0");

                // Set destination
                Node destinationBr1 = VcpeUtils.readNode(
                        dataBroker,
                        VcpeMapper.getOvsdbBridgeNodeIID(ovsdbDestinationNodeId,
                                VcpeConstants.DEFAULT_BRIDGE_NAME)).get();
                VcpeUtils.createTerminationPointNode(dataBroker,
                        destinationUni, destinationBr1,
                        VcpeConstants.DEFAULT_BRIDGE_NAME,
                        VcpeConstants.DEFAULT_INTERNAL_IFACE, null);
                Node destinationBr2 = VcpeUtils.readNode(
                        dataBroker,
                        VcpeMapper.getOvsdbBridgeNodeIID(ovsdbDestinationNodeId,
                                VcpeConstants.DEFAULT_BRIDGE_NAME)).get();
                VcpeUtils.createGreTunnel(dataBroker, destinationUni,
                        sourceUni, destinationBr2,
                        VcpeConstants.DEFAULT_BRIDGE_NAME, "gre0");
            }
        }
    }

}
