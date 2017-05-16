/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.netvirt;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.liblldp.NetUtils;
import org.opendaylight.controller.liblldp.PacketException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.globals.IfmConstants;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.NWUtil;
import org.opendaylight.genius.mdsalutil.actions.ActionOutput;
import org.opendaylight.genius.mdsalutil.actions.ActionPuntToController;
import org.opendaylight.genius.mdsalutil.instructions.InstructionApplyActions;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.matches.MatchEthernetSource;
import org.opendaylight.genius.mdsalutil.packet.Ethernet;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.action.rev150203.action.grouping.action.choice.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * @author davidg
 *
 */
public class SamplePacketHandler extends UnimgrDataTreeChangeListener<FlowCapableNodeConnector> implements PacketProcessingListener {


    private final IMdsalApiManager mdsalUtil;
    private final PacketProcessingService pktService;
    private static String dpnID;

    public SamplePacketHandler(final DataBroker dataBroker, IMdsalApiManager mdsalUtil, PacketProcessingService pktService) {
        super(dataBroker);
        this.mdsalUtil = mdsalUtil;
        this.pktService = pktService;
        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<FlowCapableNodeConnector> dataTreeIid = new DataTreeIdentifier<>(
                    LogicalDatastoreType.OPERATIONAL, getInstanceIdentifier());
            dataBroker.registerDataTreeChangeListener(dataTreeIid, this);

        } catch (final Exception e) {
            throw new IllegalStateException("Node connector listener registration failed.", e);
        }
    }

    @SuppressWarnings("deprecation")
    private InstanceIdentifier<FlowCapableNodeConnector> getInstanceIdentifier() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class).child(NodeConnector.class)
                .augmentation(FlowCapableNodeConnector.class);
    }

    /* (non-Javadoc)
     * @see org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener#onPacketReceived(org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived)
     */
    @Override
    public void onPacketReceived(PacketReceived notification) {
        byte[] data = notification.getPayload();
        Ethernet res = new Ethernet();

        try {
            res.deserialize(data, 0, data.length * NetUtils.NumBitsInAByte);
            byte[] srcMac = res.getSourceMACAddress();
            final String macAddress = NWUtil.toStringMacAddress(srcMac);

            List<MatchInfo> mkMatches = new ArrayList<>();
            mkMatches.add(new MatchEthernetSource(new MacAddress(macAddress)));
            List<InstructionInfo> instructions = new ArrayList<>();
            List<ActionInfo> actionsInfos = new ArrayList<>();
            instructions.add(new InstructionApplyActions(actionsInfos));
            FlowEntity flow = MDSALUtil.buildFlowEntity(new BigInteger(dpnID), (short)0,
                    macAddress,
                    2000, macAddress,0,0,BigInteger.ZERO,mkMatches, instructions);
            mdsalUtil.installFlow(flow);

            List<Action> actions = new ArrayList<>();
            actions.add(new ActionOutput(new Uri("1")).buildAction());
            TransmitPacketInput output = MDSALUtil.getPacketOut(actions, data, new BigInteger(dpnID));
            this.pktService.transmitPacket(output);
        } catch (PacketException e) {
            e.printStackTrace();
        }


    }


    /* (non-Javadoc)
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub

    }


    /* (non-Javadoc)
     * @see org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener#add(org.opendaylight.controller.md.sal.binding.api.DataTreeModification)
     */
    @Override
    public void add(DataTreeModification<FlowCapableNodeConnector> newDataObject) {
        FlowCapableNodeConnector data = newDataObject.getRootNode().getDataAfter();

        dpnID = getDpnIdFromNodeConnector(newDataObject);

        List<MatchInfo> mkMatches = new ArrayList<>();
        List<InstructionInfo> instructions = new ArrayList<>();
        List<ActionInfo> actionsInfos = new ArrayList<>();
        actionsInfos.add(new ActionPuntToController());
        instructions.add(new InstructionApplyActions(actionsInfos));
        FlowEntity flow = MDSALUtil.buildFlowEntity(new BigInteger(dpnID), (short)0,
                "default" + dpnID,
                1000, "default" + dpnID,0,0,BigInteger.ZERO,mkMatches, instructions);
        mdsalUtil.installFlow(flow);

        // TODO Auto-generated method stub

    }

    @SuppressWarnings("deprecation")
    private String getDpnIdFromNodeConnector(DataTreeModification<FlowCapableNodeConnector> newDataObject) {
        InstanceIdentifier<FlowCapableNodeConnector> key = newDataObject.getRootPath().getRootIdentifier();
        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();

        String dpnFromNodeConnectorId = getDpnFromNodeConnectorId(nodeConnectorId);
        return dpnFromNodeConnectorId;
    }

    @SuppressWarnings("deprecation")
    private static String getDpnFromNodeConnectorId(NodeConnectorId portId) {
        /*
         * NodeConnectorId is of form 'openflow:dpnid:portnum'
         */
        String[] split = portId.getValue().split(IfmConstants.OF_URI_SEPARATOR);
        return split[1];
    }



    /* (non-Javadoc)
     * @see org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener#remove(org.opendaylight.controller.md.sal.binding.api.DataTreeModification)
     */
    @Override
    public void remove(DataTreeModification<FlowCapableNodeConnector> removedDataObject) {
        // TODO Auto-generated method stub

    }


    /* (non-Javadoc)
     * @see org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener#update(org.opendaylight.controller.md.sal.binding.api.DataTreeModification)
     */
    @Override
    public void update(DataTreeModification<FlowCapableNodeConnector> modifiedDataObject) {
        // TODO Auto-generated method stub

    }

}
