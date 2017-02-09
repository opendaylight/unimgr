/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import com.google.common.base.Optional;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.global.rev150526.MefGlobal;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.global.rev150526.mef.global.Profiles;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.global.rev150526.mef.global.bwp.flows.group.BwpFlow;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.global.rev150526.mef.global.bwp.flows.group.BwpFlowKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.global.rev150526.mef.global.profiles.IngressBwpFlows;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.Link;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.Identifier45;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.BridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.OdlInterfaceRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniQosManager extends UnimgrDataTreeChangeListener<BwpFlow> {
    private static final Logger Log = LoggerFactory.getLogger(UniQosManager.class);
    private OdlInterfaceRpcService odlInterfaceRpcService;
    private DataBroker dataBroker;
    private final Long noLimit = 0l;
    private final static String noProfile = "";
    private ListenerRegistration<UniQosManager> bwListenerRegistration;


    // key in first map is uniId, key in second map is logical portId
    private ConcurrentHashMap<String, ConcurrentHashMap<String, BandwidthLimits>> uniPortBandwidthLimits;

    // map of current values per uni
    private ConcurrentHashMap<String, BandwidthLimits> uniBandwidthLimits;

    private ConcurrentHashMap<String, BigInteger> uniToDpn;

    public UniQosManager(final DataBroker dataBroker, OdlInterfaceRpcService odlInterfaceRpcService) {
        super(dataBroker);

        this.dataBroker = dataBroker;
        this.odlInterfaceRpcService = odlInterfaceRpcService;
        this.uniPortBandwidthLimits = new ConcurrentHashMap<>();
        this.uniBandwidthLimits = new ConcurrentHashMap<>();
        this.uniToDpn = new ConcurrentHashMap<>();
        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<BwpFlow> dataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                    getBwFlowsInstanceIdentifier());
            bwListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
            Log.info("UniQosManager created and registered");
        } catch (final Exception e) {
            Log.error("UniQosManager DataChange listener registration failed !", e);
            throw new IllegalStateException("UniQosManager registration Listener failed.", e);
        }
    }

    public synchronized void mapUniPortBandwidthLimits(String uniId, String portId, Identifier45 bwProfile) {
        Long maxKbps = noLimit;
        Long maxBurstKb = noLimit;
        if (bwProfile != null) {
            Optional<BwpFlow> bwFlowOp = MdsalUtils.read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                    getBwFlowInstanceIdentifier(bwProfile));
            if (!bwFlowOp.isPresent()) {
                Log.trace("Can't read bw profile {} for Uni {}", bwProfile, uniId);
            } else {
                // Kb per second
                maxKbps = bwFlowOp.get().getCir().getValue();
                // burst in bytes, ovs requires in Kb
                maxBurstKb = bwFlowOp.get().getCbs().getValue() * 8 / 1024;
                Log.info("Record rate limits for Uni {} Profile {}", uniId, bwProfile);
            }
        }

        mapUniPortBandwidthLimits(uniId, portId, maxKbps, maxBurstKb, replaceNull(bwProfile));
    }

    private synchronized void mapUniPortBandwidthLimits(String uniId, String portId, Long maxKbps, Long maxBurstKb,
            String profileName) {
        Log.info("Record rate limits for Uni {} port {} maxKbps {} maxBurstKb {}", uniId, portId, maxKbps, maxBurstKb);
        uniPortBandwidthLimits.putIfAbsent(uniId, new ConcurrentHashMap<>());
        ConcurrentHashMap<String, BandwidthLimits> uniMap = uniPortBandwidthLimits.get(uniId);
        uniMap.put(portId, new BandwidthLimits(maxKbps, maxBurstKb, profileName));
    }

    public void updateUni(Identifier45 uniId, Identifier45 bwProfile) {
        String bwProfileSafe = replaceNull(bwProfile);
        Log.info("Update rate limits for Uni {}", uniId.getValue());
        ConcurrentHashMap<String, BandwidthLimits> uniMap = uniPortBandwidthLimits.get(uniId.getValue());
        if (uniMap == null) {
            Log.error("Trying to update limits for non-exsting uni {}", uniId.getValue());
            return;
        }
        for (String portName : uniMap.keySet()) {
            if (uniMap.get(portName).getProfileName().equals(bwProfileSafe)) {
                continue;
            }
            if (bwProfile != null) {
                mapUniPortBandwidthLimits(uniId.getValue(), portName, new Identifier45(bwProfileSafe));
            } else {
                unMapUniPortBandwidthLimits(uniId.getValue(), portName);
            }
        }
    }

    private void updateProfile(Identifier45 bwProfile) {
        Log.info("Update rate limits for profile {}", bwProfile);
        List<String> unisWithProfile = uniBandwidthLimits.entrySet().stream()
                .filter(m -> m.getValue().profileName.equals(bwProfile.getValue())).map(m -> m.getKey())
                .collect(Collectors.toList());

        for (String uniId : unisWithProfile) {
            ConcurrentHashMap<String, BandwidthLimits> uniMap = uniPortBandwidthLimits.get(uniId);
            uniMap.forEach((k, v) -> {
                mapUniPortBandwidthLimits(uniId, k, bwProfile);
            });
        }

        for (String uniId : unisWithProfile) {
            setUniBandwidthLimits(uniId);
        }
    }

    public void deleteProfile(Identifier45 bwProfile) {
        Log.info("Delete rate limits for profile {}", bwProfile);
        List<String> unisWithProfile = uniBandwidthLimits.entrySet().stream()
                .filter(m -> m.getValue().profileName.equals(bwProfile.getValue())).map(m -> m.getKey())
                .collect(Collectors.toList());

        for (String uniId : unisWithProfile) {
            ConcurrentHashMap<String, BandwidthLimits> uniMap = uniPortBandwidthLimits.get(uniId);
            uniMap.forEach((k, v) -> {
                unMapUniPortBandwidthLimits(uniId, k, bwProfile.getValue());
            });
        }

        for (String uniId : unisWithProfile) {
            setUniBandwidthLimits(uniId);
        }
    }

    public synchronized void unMapUniPortBandwidthLimits(String uniId, String portId) {
        unMapUniPortBandwidthLimits(uniId, portId, noProfile);
    }

    public synchronized void unMapUniPortBandwidthLimits(String uniId, String portId, String profileTosave) {
        Log.info("Delete rate limits for Uni {} port {}", uniId, portId);
        ConcurrentHashMap<String, BandwidthLimits> uniMap = uniPortBandwidthLimits.get(uniId);
        if (uniMap == null) {
            Log.error("Trying to delete limits for non-exsting uni {}", uniId);
            return;
        }
        uniMap.remove(portId);
        if (uniMap.isEmpty()) {
            uniMap.put(portId, new BandwidthLimits(noLimit, noLimit, profileTosave));
        }
    }

    public void setUniBandwidthLimits(Identifier45 uniIden) {
        String uniId = uniIden.getValue();
        setUniBandwidthLimits(uniId);
    }

    private synchronized void setUniBandwidthLimits(String uniId) {
        if (!uniPortBandwidthLimits.containsKey(uniId)) {
            Log.debug("Uni {} doesn't have rate limits configured", uniId);
            return;
        }
        Iterator<String> uniPorts = uniPortBandwidthLimits.get(uniId).keySet().iterator();
        if (uniPorts == null || !uniPorts.hasNext()) {
            Log.debug("Uni {} doesn't have rate limits configured", uniId);
            return;
        }
        String logicalPort = uniPorts.next();

        BandwidthLimits newLimits = recalculateLimitsForUni(uniId, uniPortBandwidthLimits.get(uniId));
        if (newLimits.equals(uniBandwidthLimits.get(uniId))) {
            Log.debug("Uni {} rate limits has not changed", uniId);
            return;
        }

        setPortBandwidthLimits(uniId, logicalPort, newLimits.getMaxKbps(), newLimits.getMaxBurstKb());
        uniBandwidthLimits.put(uniId, newLimits);
    }

    private BandwidthLimits recalculateLimitsForUni(String uniId,
            ConcurrentHashMap<String, BandwidthLimits> uniLimits) {
        Long sumOfRate = noLimit;
        Long sumOfBurst = noLimit;
        String profileName = noProfile;
        Boolean hasNullRate = false;
        Boolean hasNullBurst = false;

        if (uniLimits == null || uniLimits.keySet() == null) {
            return new BandwidthLimits(sumOfRate, sumOfBurst, profileName);
        }

        for (BandwidthLimits v : uniLimits.values()) {
            if (v.maxKbps == null) {
                hasNullRate = true;
                break;
            }
            if (v.maxBurstKb == null) {
                hasNullBurst = true;
            }
            sumOfRate = sumOfRate + v.maxKbps;
            sumOfBurst = sumOfBurst + v.maxBurstKb;
            profileName = v.profileName;
        }
        if (hasNullRate) {
            sumOfRate = noLimit;
            sumOfBurst = noLimit;
        } else if (hasNullBurst) {
            sumOfBurst = noLimit;
        }
        return new BandwidthLimits(sumOfRate, sumOfBurst, profileName);
    }

    private void setPortBandwidthLimits(String uniId, String logicalPortId, Long maxKbps, Long maxBurstKb) {
        Log.info("Setting bandwidth limits {} {} on Port {}", maxKbps, maxBurstKb, logicalPortId);

        BigInteger dpId = BigInteger.ZERO;
        if (uniToDpn.containsKey(uniId)) {
            dpId = uniToDpn.get(uniId);
        } else {
            dpId = NetvirtUtils.getDpnForInterface(odlInterfaceRpcService, logicalPortId);
            uniToDpn.put(uniId, dpId);
        }
        if (dpId.equals(BigInteger.ZERO)) {
            Log.error("DPN ID for interface {} not found", logicalPortId);
            return;
        }

        OvsdbBridgeRef bridgeRefEntry = getBridgeRefEntryFromOperDS(dpId, dataBroker);
        Optional<Node> bridgeNode = MDSALUtil.read(LogicalDatastoreType.OPERATIONAL,
                bridgeRefEntry.getValue().firstIdentifierOf(Node.class), dataBroker);
        if (bridgeNode == null) {
            Log.error("Bridge ref for interface {} not found", logicalPortId);
            return;
        }

        String physicalPort = getPhysicalPortForUni(dataBroker, uniId);
        if (physicalPort == null) {
            Log.error("Physical port for interface {} not found", logicalPortId);
            return;
        }

        TerminationPoint tp = getTerminationPoint(bridgeNode.get(), physicalPort);
        if (tp == null) {
            Log.error("Termination point for port {} not found", physicalPort);
            return;
        }

        OvsdbTerminationPointAugmentation ovsdbTp = tp.getAugmentation(OvsdbTerminationPointAugmentation.class);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        tpAugmentationBuilder.setName(ovsdbTp.getName());
        tpAugmentationBuilder.setIngressPolicingRate(maxKbps);
        tpAugmentationBuilder.setIngressPolicingBurst(maxBurstKb);

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(tp.getKey());
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
        MdsalUtils.syncUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(SouthboundUtils.OVSDB_TOPOLOGY_ID))
                        .child(Node.class, bridgeNode.get().getKey())
                        .child(TerminationPoint.class, new TerminationPointKey(tp.getKey())),
                tpBuilder.build());
    }

    private static TerminationPoint getTerminationPoint(Node bridgeNode, String portName) {
        for (TerminationPoint tp : bridgeNode.getTerminationPoint()) {
            String tpIdStr = tp.getTpId().getValue();
            if (tpIdStr != null && tpIdStr.equals(portName))
                return tp;
        }
        return null;
    }


    private static String getPhysicalPortForUni(DataBroker dataBroker, String uniId) {
        String nodeId = null;
        try {
            Link link = MefInterfaceUtils.getLink(dataBroker, uniId, LogicalDatastoreType.OPERATIONAL);
            String parentInterfaceName = MefInterfaceUtils.getTrunkParentName(link);
            return parentInterfaceName.split(":")[1];
        } catch (Exception e) {
            Log.error("Exception when getting physical port for Uni {}", uniId, e);
        }
        return nodeId;
    }

    private static BridgeRefEntry getBridgeRefEntryFromOperDS(InstanceIdentifier<BridgeRefEntry> dpnBridgeEntryIid,
            DataBroker dataBroker) {
        Optional<BridgeRefEntry> bridgeRefEntryOptional = MdsalUtils.read(dataBroker, LogicalDatastoreType.OPERATIONAL,
                dpnBridgeEntryIid);
        if (!bridgeRefEntryOptional.isPresent()) {
            return null;
        }
        return bridgeRefEntryOptional.get();
    }

    private static OvsdbBridgeRef getBridgeRefEntryFromOperDS(BigInteger dpId, DataBroker dataBroker) {
        BridgeRefEntryKey bridgeRefEntryKey = new BridgeRefEntryKey(dpId);
        InstanceIdentifier<BridgeRefEntry> bridgeRefEntryIid = getBridgeRefEntryIdentifier(bridgeRefEntryKey);
        BridgeRefEntry bridgeRefEntry = getBridgeRefEntryFromOperDS(bridgeRefEntryIid, dataBroker);
        return (bridgeRefEntry != null) ? bridgeRefEntry.getBridgeReference() : null;
    }

    private static InstanceIdentifier<BridgeRefEntry> getBridgeRefEntryIdentifier(BridgeRefEntryKey bridgeRefEntryKey) {
        InstanceIdentifier.InstanceIdentifierBuilder<BridgeRefEntry> bridgeRefEntryInstanceIdentifierBuilder = InstanceIdentifier
                .builder(BridgeRefInfo.class).child(BridgeRefEntry.class, bridgeRefEntryKey);
        return bridgeRefEntryInstanceIdentifierBuilder.build();
    }

    private static InstanceIdentifier<BwpFlow> getBwFlowInstanceIdentifier(Identifier45 bwProfile) {
        InstanceIdentifier.InstanceIdentifierBuilder<BwpFlow> bwProfileInstanceIdentifierBuilder = InstanceIdentifier
                .builder(MefGlobal.class).child(Profiles.class).child(IngressBwpFlows.class)
                .child(BwpFlow.class, new BwpFlowKey(bwProfile));
        return bwProfileInstanceIdentifierBuilder.build();
    }

    private static InstanceIdentifier<BwpFlow> getBwFlowsInstanceIdentifier() {
        InstanceIdentifier.InstanceIdentifierBuilder<BwpFlow> bwProfileInstanceIdentifierBuilder = InstanceIdentifier
                .builder(MefGlobal.class).child(Profiles.class).child(IngressBwpFlows.class).child(BwpFlow.class);
        return bwProfileInstanceIdentifierBuilder.build();
    }

    private class BandwidthLimits {
        private final Long maxKbps;
        private final Long maxBurstKb;
        private final String profileName;

        public BandwidthLimits(Long maxKbps, Long maxBurstKb, String profileName) {
            this.maxKbps = replaceNull(maxKbps);
            this.maxBurstKb = replaceNull(maxBurstKb);
            this.profileName = profileName;
        }

        public Long getMaxKbps() {
            return maxKbps;
        }

        public Long getMaxBurstKb() {
            return maxBurstKb;
        }

        public String getProfileName() {
            return profileName;
        }

        private Long replaceNull(Long value) {
            return (value == null) ? Long.valueOf(0) : value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            BandwidthLimits other = (BandwidthLimits) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (maxBurstKb == null) {
                if (other.maxBurstKb != null)
                    return false;
            } else if (!maxBurstKb.equals(other.maxBurstKb))
                return false;
            if (maxKbps == null) {
                if (other.maxKbps != null)
                    return false;
            } else if (!maxKbps.equals(other.maxKbps))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "BandwidthLimitsBandwidthLimitsalues [maxKbps=" + maxKbps + ", maxBurstKb=" + maxBurstKb + "]";
        }

        private UniQosManager getOuterType() {
            return UniQosManager.this;
        }
    }

    private static String replaceNull(Identifier45 value) {
        return (value == null) ? noProfile : value.getValue();
    }

    @Override
    public void close() throws Exception {
        bwListenerRegistration.close();
    }

    @Override
    public void add(DataTreeModification<BwpFlow> newDataObject) {
        if (newDataObject.getRootPath() != null && newDataObject.getRootNode() != null) {
            Log.info("bw profile {} created", newDataObject.getRootNode().getIdentifier());
            updateProfile(newDataObject.getRootNode().getDataAfter().getBwProfile());
        }
    }

    @Override
    public void remove(DataTreeModification<BwpFlow> removedDataObject) {
        if (removedDataObject.getRootPath() != null && removedDataObject.getRootNode() != null) {
            Log.info("bw profile {} deleted", removedDataObject.getRootNode().getIdentifier());
            deleteProfile(removedDataObject.getRootNode().getDataBefore().getBwProfile());
        }
    }

    @Override
    public void update(DataTreeModification<BwpFlow> modifiedDataObject) {
        if (modifiedDataObject.getRootPath() != null && modifiedDataObject.getRootNode() != null) {
            Log.info("bw profile {} modified", modifiedDataObject.getRootNode().getIdentifier());
            updateProfile(modifiedDataObject.getRootNode().getDataAfter().getBwProfile());
        }
    }
}
