/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.neutron.vpn.portip.port.data.VpnPortipToPort;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwMacListener extends UnimgrDataTreeChangeListener<VpnPortipToPort> implements IGwMacListener {
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
            final DataTreeIdentifier<VpnPortipToPort> dataTreeIid = new DataTreeIdentifier<>(
                    LogicalDatastoreType.OPERATIONAL, NetvirtVpnUtils.getVpnPortipToPortIdentifier());
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
    public void add(DataTreeModification<VpnPortipToPort> newDataObject) {
        if (newDataObject.getRootPath() != null && newDataObject.getRootNode() != null) {
            VpnPortipToPort portIpToPort = newDataObject.getRootNode().getDataAfter();
            updateMac(portIpToPort);
        }
    }

    @Override
    public void remove(DataTreeModification<VpnPortipToPort> removedDataObject) {
    }

    @Override
    public void update(DataTreeModification<VpnPortipToPort> modifiedDataObject) {
        if (modifiedDataObject.getRootPath() != null && modifiedDataObject.getRootNode() != null) {
            VpnPortipToPort portIpToPort = modifiedDataObject.getRootNode().getDataAfter();
            updateMac(portIpToPort);
        }
    }

    private void updateMac(VpnPortipToPort portIpToPort) {
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

        if (gwMacResolver.get(gwMacKey).getGwMac() == null || !gwMacResolver.get(gwMacKey).getGwMac().equals(macAddress)) {
            Log.info("Creating GW for vpn {} port {} ip {} MAC {}", vpnName, portName, ipAddress, macAddress);
            NetvirtVpnUtils.createUpdateVpnInterface(dataBroker, vpnName, portName, gwMacResolver.get(gwMacKey).getSubnet(), macAddress,
                    false, gwMacResolver.get(gwMacKey).getPortIp(), null);

            gwMacResolver.get(gwMacKey).setGwMac(macAddress);
        }
    }

    @Override
    public void resolveGwMac(String vpnName, String portName, IpAddress srcIpAddress, IpAddress dstIpAddress, String subnet) {
        String dstIpAddressStr = NetvirtVpnUtils.ipAddressToString(dstIpAddress);
        GwMacKey gwMacKey = new GwMacKey(vpnName, portName, dstIpAddressStr);

        if (!gwMacResolver.containsKey(gwMacKey)) {
            // check if IP was resolved already
            gwMacResolver.putIfAbsent(gwMacKey, new GwMacValue(NetvirtVpnUtils.ipAddressToString(srcIpAddress), subnet));
            VpnPortipToPort portIpToPort = NetvirtVpnUtils.getVpnPortFixedIp(dataBroker, vpnName, dstIpAddressStr);
            if (portIpToPort != null) {
                updateMac(portIpToPort);
            }

            Log.info("Starting GW mac resolution for vpn {} port {} GW ip {}", vpnName, portName, dstIpAddress);
            NetvirtVpnUtils.sendArpRequest(arpUtilService, srcIpAddress, dstIpAddress, portName);
        }
    }

    @Override
    public void unResolveGwMac(String vpnName, String portName, IpAddress srcIpAddress, IpAddress dstIpAddress) {
        String dstIpAddressStr = NetvirtVpnUtils.ipAddressToString(dstIpAddress);
        GwMacKey gwMacKey = new GwMacKey(vpnName, portName, dstIpAddressStr);
        if (gwMacResolver.containsKey(gwMacKey)) {
            Log.info("Stopping GW mac resolution for vpn {} port {} GW ip {}", vpnName, portName, dstIpAddress);
            gwMacResolver.remove(gwMacKey);
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

    public static class GwMacKey {
        private final String vpId;
        private final String portId;
        private final String gwIp;

        public GwMacKey(String vpId, String portId, String gwIp) {
            this.vpId = vpId;
            this.portId = portId;
            this.gwIp = gwIp;
        }

        public String getGwIp() {
            return gwIp;
        }

        public String getVpId() {
            return vpId;
        }

        public String getPortId() {
            return portId;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (gwIp == null ? 0 : gwIp.hashCode());
            result = prime * result + (portId == null ? 0 : portId.hashCode());
            result = prime * result + (vpId == null ? 0 : vpId.hashCode());
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
            if (vpId == null) {
                if (other.vpId != null) {
                    return false;
                }
            } else if (!vpId.equals(other.vpId)) {
                return false;
            }
            return true;
        }


    }

    public static class GwMacValue {
        String portIp;
        String subnet;
        String gwMac;

        public GwMacValue(String portIp, String subnet) {
            this.portIp = portIp;
            this.subnet = subnet;
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

        public String getSubnet() {
            return subnet;
        }

    }

}
