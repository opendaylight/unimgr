/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.OdlArputilService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.learnt.vpn.vip.to.port.data.LearntVpnVipToPort;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwMacListener extends UnimgrDataTreeChangeListener<LearntVpnVipToPort> implements IGwMacListener {
    private static final Logger Log = LoggerFactory.getLogger(GwMacListener.class);
    private ListenerRegistration<GwMacListener> gwMacListenerRegistration;
    private final OdlArputilService arpUtilService;
    private final ExecutorService retriesHandler;
    private final Short sleepInterval;

    private final ConcurrentHashMap<GwMacKey, GwMacValue> gwMacResolver;

    public GwMacListener(final DataBroker dataBroker, final OdlArputilService arputilService, Short sleepInterval) {
        super(dataBroker);
        this.arpUtilService = arputilService;
        this.gwMacResolver = new ConcurrentHashMap<>();
        this.sleepInterval = sleepInterval;
        retriesHandler = Executors.newSingleThreadExecutor();
        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<LearntVpnVipToPort> dataTreeIid = new DataTreeIdentifier<>(
                    LogicalDatastoreType.OPERATIONAL, NetvirtVpnUtils.getLearntVpnVipToPortIdentifier());
            gwMacListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
            startRetriesThread();
            Log.info("GwMacListener created and registered");
        } catch (final Exception e) {
            Log.error("GwMacListener listener registration failed !", e);
            throw new IllegalStateException("GwMacListener Listener failed.", e);
        }
    }

    @Override
    public void close() throws Exception {
        gwMacListenerRegistration.close();
        retriesHandler.shutdown();
        if (!retriesHandler.awaitTermination(10, TimeUnit.SECONDS)) {
            retriesHandler.shutdownNow();
        }
    }

    @Override
    public void add(DataTreeModification<LearntVpnVipToPort> newDataObject) {
        if (newDataObject.getRootPath() != null && newDataObject.getRootNode() != null) {
            LearntVpnVipToPort portIpToPort = newDataObject.getRootNode().getDataAfter();
            updateMac(portIpToPort);
        }
    }

    @Override
    public void remove(DataTreeModification<LearntVpnVipToPort> removedDataObject) {
    }

    @Override
    public void update(DataTreeModification<LearntVpnVipToPort> modifiedDataObject) {
        if (modifiedDataObject.getRootPath() != null && modifiedDataObject.getRootNode() != null) {
            LearntVpnVipToPort portIpToPort = modifiedDataObject.getRootNode().getDataAfter();
            updateMac(portIpToPort);
        }
    }

    private synchronized void updateMac(LearntVpnVipToPort portIpToPort) {
        String portName = portIpToPort.getPortName();
        String macAddress = portIpToPort.getMacAddress();
        String vpnName = portIpToPort.getVpnName();
        String ipAddress = portIpToPort.getPortFixedip();
        GwMacKey gwMacKey = new GwMacKey(vpnName, portName, ipAddress);

        if (!gwMacResolver.containsKey(gwMacKey)) {
            Log.debug("Ignoring MAC update for vpn {} port {} ip {}", vpnName, portName, ipAddress);
            return;
        }

        if (macAddress != null && vpnName != null) {
            Log.trace("Updating vpn {} port {} with IP {} MAC {}", vpnName, portName, ipAddress, macAddress);
        }

        if (gwMacResolver.get(gwMacKey).getGwMac() == null
                || !gwMacResolver.get(gwMacKey).getGwMac().equals(macAddress)) {
            String portIp = gwMacResolver.get(gwMacKey).getPortIp();
            for (String subnet : gwMacResolver.get(gwMacKey).getSubnets()) {
                Log.info("Creating GW for vpn {} port {} ip {} subnet {} MAC {}", vpnName, portName, ipAddress, subnet,
                        macAddress);

                NetvirtVpnUtils.createUpdateVpnInterface(dataBroker, vpnName, portName, subnet, macAddress, false,
                        portIp, null);
            }

            gwMacResolver.get(gwMacKey).setGwMac(macAddress);
        }
    }

    private void forceUpdateSubnet(GwMacKey gwMacKey, String subnet) {
        if (!gwMacResolver.containsKey(gwMacKey)) {
            return;
        }
        String portIp = gwMacResolver.get(gwMacKey).getPortIp();
        String macAddress = gwMacResolver.get(gwMacKey).getGwMac();
        if (macAddress == null) {
            return;
        }
        Log.info("Creating GW for vpn {} port {} ip {} subnet {} MAC {}", gwMacKey.vpnId, gwMacKey.portId,
                gwMacKey.gwIp, subnet, macAddress);
        NetvirtVpnUtils.createUpdateVpnInterface(dataBroker, gwMacKey.vpnId, gwMacKey.portId, subnet, macAddress, false,
                portIp, null);

    }

    @Override
    public synchronized void resolveGwMac(String vpnName, String portName, IpAddress srcIpAddress,
            IpAddress dstIpAddress, String subnet) {
        String dstIpAddressStr = NetvirtVpnUtils.ipAddressToString(dstIpAddress);
        GwMacKey gwMacKey = new GwMacKey(vpnName, portName, dstIpAddressStr);

        if (!gwMacResolver.containsKey(gwMacKey)) {
            gwMacResolver.putIfAbsent(gwMacKey,
                    new GwMacValue(NetvirtVpnUtils.ipAddressToString(srcIpAddress), subnet));
            Log.info("Starting GW mac resolution for vpn {} port {} GW ip {}", vpnName, portName, dstIpAddress);
            NetvirtVpnUtils.sendArpRequest(arpUtilService, srcIpAddress, dstIpAddress, portName);
        } else {
            forceUpdateSubnet(gwMacKey, subnet);
            gwMacResolver.get(gwMacKey).getSubnets().add(subnet);
        }

        LearntVpnVipToPort portIpToPort = NetvirtVpnUtils.getLearntVpnVipToPort(dataBroker, vpnName, dstIpAddressStr);
        if (portIpToPort != null) {
            updateMac(portIpToPort);
        }

    }

    @Override
    public synchronized void unResolveGwMac(String vpnName, String portName, IpAddress srcIpAddress,
            IpAddress dstIpAddress, String subnet) {
        String dstIpAddressStr = NetvirtVpnUtils.ipAddressToString(dstIpAddress);
        GwMacKey gwMacKey = new GwMacKey(vpnName, portName, dstIpAddressStr);
        if (gwMacResolver.containsKey(gwMacKey)) {
            Log.info("Stopping GW mac resolution for vpn {} port {} GW ip {} Subnet {}", vpnName, portName,
                    dstIpAddress, subnet);
            gwMacResolver.get(gwMacKey).getSubnets().remove(subnet);

            if (gwMacResolver.get(gwMacKey).getSubnets().isEmpty()) {
                gwMacResolver.remove(gwMacKey);
            }
        }
    }

    private void resolveRetry() {
        gwMacResolver.forEach((k, v) -> {
            if (v.getGwMac() == null) {
                IpAddress dstIpAddress = new IpAddress(k.gwIp.toCharArray());
                IpAddress srcIpAddress = new IpAddress(v.portIp.toCharArray());
                Log.debug("Resending ARP for IP {} port {}", dstIpAddress, k.getGwIp());

                NetvirtVpnUtils.sendArpRequest(arpUtilService, srcIpAddress, dstIpAddress, k.portId);
            }
        });
    }

    void startRetriesThread() {
        retriesHandler.submit(() -> {
            Thread t = Thread.currentThread();
            t.setName("ResolveSubnetGW");
            Log.info("ResolveSubnetGW: started {}", t.getName());
            while (true) {
                NetvirtUtils.safeSleep(sleepInterval);
                resolveRetry();
            }
        });
        Log.debug("Subnet GW Arp Retries");
    }

    private static class GwMacKey {
        private final String vpnId;
        private final String portId;
        private final String gwIp;

        public GwMacKey(String vpId, String portId, String gwIp) {
            this.vpnId = vpId;
            this.portId = portId;
            this.gwIp = gwIp;
        }

        public String getGwIp() {
            return gwIp;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (gwIp == null ? 0 : gwIp.hashCode());
            result = prime * result + (portId == null ? 0 : portId.hashCode());
            result = prime * result + (vpnId == null ? 0 : vpnId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            GwMacKey other = (GwMacKey) obj;
            if (gwIp == null) {
                if (other.gwIp != null) {
                    return false;
                }
            } else if (!gwIp.equals(other.gwIp)) {
                return false;
            }
            if (portId == null) {
                if (other.portId != null) {
                    return false;
                }
            } else if (!portId.equals(other.portId)) {
                return false;
            }
            if (vpnId == null) {
                if (other.vpnId != null) {
                    return false;
                }
            } else if (!vpnId.equals(other.vpnId)) {
                return false;
            }
            return true;
        }

    }

    private static class GwMacValue {
        private String portIp;
        private Set<String> subnets;
        private String gwMac;

        public GwMacValue(String portIp, String subnet) {
            this.portIp = portIp;
            this.subnets = new HashSet<String>();
            this.subnets.add(subnet);
        }

        public String getGwMac() {
            return gwMac;
        }

        public void setGwMac(String gwMac) {
            this.gwMac = gwMac;
        }

        public String getPortIp() {
            return portIp;
        }

        public Set<String> getSubnets() {
            return subnets;
        }

    }

}
