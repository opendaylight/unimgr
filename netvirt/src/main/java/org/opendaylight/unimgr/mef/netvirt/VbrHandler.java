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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.actions.ActionNxResubmit;
import org.opendaylight.genius.mdsalutil.actions.ActionPopVlan;
import org.opendaylight.genius.mdsalutil.actions.ActionPuntToController;
import org.opendaylight.genius.mdsalutil.actions.ActionPushVlan;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldVlanVid;
import org.opendaylight.genius.mdsalutil.instructions.InstructionApplyActions;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.matches.MatchEthernetSource;
import org.opendaylight.genius.mdsalutil.matches.MatchEthernetType;
import org.opendaylight.genius.mdsalutil.matches.MatchIpv4Destination;
import org.opendaylight.genius.mdsalutil.matches.MatchIpv4Source;
import org.opendaylight.genius.mdsalutil.matches.MatchVlanVid;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.MefServices;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.MefService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.EtherTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.unimgr.vbr.rev170524.Vbr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.unimgr.vbr.rev170524.vbr.Subscribers;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * @author davidg
 *
 */
public class VbrHandler extends UnimgrDataTreeChangeListener<Subscribers> {

    private final IMdsalApiManager mdsalUtil;

    /**
     * @param dataBroker
     */
    public VbrHandler(DataBroker dataBroker, IMdsalApiManager mdsalUtil) {
        super(dataBroker);
        this.mdsalUtil = mdsalUtil;
        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<Subscribers> dataTreeIid = new DataTreeIdentifier<>(
                    LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(Vbr.class).child(Subscribers.class));
            dataBroker.registerDataTreeChangeListener(dataTreeIid, this);

        } catch (final Exception e) {
            throw new IllegalStateException("Subscriber listener registration failed.", e);
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
    public void add(DataTreeModification<Subscribers> newDataObject) {

        Ipv4Address ip = newDataObject.getRootNode().getDataAfter().getIp().getIpv4Address();
        Short sessionId = newDataObject.getRootNode().getDataAfter().getSessionId();
        String dpnID = SamplePacketHandler.getDpnID();

        List<MatchInfo> upstreamMatches = new ArrayList<>();
        MatchEthernetType ipMatch = new MatchEthernetType(0x800);
        upstreamMatches.add(ipMatch);
        MatchIpv4Source upstreamIpMatch = new MatchIpv4Source(new Ipv4Prefix(ip.getValue() + "/32"));
        upstreamMatches.add(upstreamIpMatch);

        List<InstructionInfo> upstreamInstructions = new ArrayList<>();
        List<ActionInfo> upstreamActionsInfos = new ArrayList<>();
        upstreamActionsInfos.add(new ActionPushVlan());
        upstreamActionsInfos.add(new ActionSetFieldVlanVid(sessionId));
        upstreamInstructions.add(new InstructionApplyActions(upstreamActionsInfos));

        FlowEntity upstreamFlow = MDSALUtil.buildFlowEntity(new BigInteger(dpnID), (short)0,
                "upstream" + ip.getValue(),
                1000, "upstream" + ip.getValue() ,0,0,BigInteger.ZERO,upstreamMatches, upstreamInstructions);
        mdsalUtil.installFlow(upstreamFlow);

        List<MatchInfo> downstreamMatches = new ArrayList<>();
        MatchIpv4Destination downstreamIpMatch = new MatchIpv4Destination(new Ipv4Prefix(ip.getValue() + "/32"));
        downstreamMatches.add(downstreamIpMatch);
        MatchVlanVid vlanIdMatch = new MatchVlanVid(sessionId);
        downstreamMatches.add(ipMatch);
        downstreamMatches.add(vlanIdMatch);

        List<InstructionInfo> downstreamInstructions = new ArrayList<>();
        List<ActionInfo> downstreamActionsInfos = new ArrayList<>();
        downstreamActionsInfos.add(new ActionPopVlan());
        downstreamInstructions.add(new InstructionApplyActions(downstreamActionsInfos));

        FlowEntity downstreamFlow = MDSALUtil.buildFlowEntity(new BigInteger(dpnID), (short)0,
                "downstream" + ip.getValue(),
                1000, "downstream" + ip.getValue() ,0,0,BigInteger.ZERO,downstreamMatches, downstreamInstructions);
        mdsalUtil.installFlow(downstreamFlow);


    }

    /* (non-Javadoc)
     * @see org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener#remove(org.opendaylight.controller.md.sal.binding.api.DataTreeModification)
     */
    @Override
    public void remove(DataTreeModification<Subscribers> removedDataObject) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener#update(org.opendaylight.controller.md.sal.binding.api.DataTreeModification)
     */
    @Override
    public void update(DataTreeModification<Subscribers> modifiedDataObject) {
        // TODO Auto-generated method stub

    }

}
