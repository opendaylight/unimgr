/*
 * Copyright (c) 2019 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator;

import static org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort.toServicePort;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.MountPointHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.InterfaceHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util.CommonUtils;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.L2vpnHelper;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivator;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivatorException;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.Database;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.BridgeDomainGroups;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.BridgeDomainGroup;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.BridgeDomainGroupKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.BridgeDomains;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.bridge.domain.groups.bridge.domain.group.bridge.domains.bridge.domain.BdPseudowires;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.connectivity.service.end.point.attrs.NrpCarrierEthConnectivityEndPointResource;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ServiceType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arif.hussain@xoriant.com
 */
public abstract class AbstractL2vpnBridgeDomainActivator implements ResourceActivator {

    private static final Logger LOG =
            LoggerFactory.getLogger(AbstractL2vpnBridgeDomainActivator.class);
    private static final long MTU = 1500;

    protected DataBroker dataBroker;
    private MountPointService mountService;
    private static List<Uuid> inls = new ArrayList<Uuid>();
    private static List<String> dvls = new ArrayList<String>();

    protected AbstractL2vpnBridgeDomainActivator(DataBroker dataBroker,
            MountPointService mountService) {
        LOG.info(" L2vpn bridge domain activator initiated...");
        this.dataBroker = dataBroker;
        this.mountService = mountService;
    }

    @Override
    public void activate(List<EndPoint> endPoints, String serviceId, boolean isExclusive,
            ServiceType serviceType)
            throws ResourceActivatorException, TransactionCommitFailedException {
        ServicePort port = null;
        ServicePort neighbor = null;
        String portRole = null, neighborRole = null;
        inls.clear();
        dvls.clear();

        for (EndPoint endPoint : endPoints) {
            if (port == null) {
                port = toServicePort(endPoint, CommonUtils.NETCONF_TOPOLODY_NAME);
                NrpCarrierEthConnectivityEndPointResource attrs = endPoint.getAttrs() == null ? null
                        : endPoint.getAttrs().getNrpCarrierEthConnectivityEndPointResource();
                if (attrs != null) {
                    port.setEgressBwpFlow(attrs.getEgressBwpFlow());
                    port.setIngressBwpFlow(attrs.getIngressBwpFlow());
                }
                portRole = endPoint.getEndpoint().getRole().name();
            } else {
                neighbor = toServicePort(endPoint, CommonUtils.NETCONF_TOPOLODY_NAME);
                neighborRole = endPoint.getEndpoint().getRole().name();
            }
        }

        InterfaceConfigurations interfaceConfigurations =
                activateInterface(port, neighbor, MTU, isExclusive);
        BdPseudowires bdPseudowires = activateBdPseudowire(neighbor);
        String innerOuterName = getInnerName(serviceId);
        BridgeDomainGroups bridgeDomainGroups = activateBridgeDomain(innerOuterName, innerOuterName,
                port, neighbor, bdPseudowires, isExclusive);
        L2vpn l2vpn = activateL2Vpn(bridgeDomainGroups);

        // create sub interface for tag based service
        if (!isExclusive) {
            InterfaceConfigurations subInterfaceConfigurations =
                    createSubInterface(port, neighbor, MTU);
            createSubInterface(port.getNode().getValue(), subInterfaceConfigurations);
        }

        if (serviceType != null
                && serviceType.getName().equals(ServiceType.ROOTEDMULTIPOINTCONNECTIVITY.getName())) {
            if (!(portRole != null && portRole.equals(PortRole.LEAF.getName())
                    && neighborRole != null && neighborRole.equals(PortRole.LEAF.getName()))) {
                doActivate(port.getNode().getValue(), interfaceConfigurations, l2vpn);
            }
        } else {
            doActivate(port.getNode().getValue(), interfaceConfigurations, l2vpn);
        }
    }


    protected void doActivate(String nodeName, InterfaceConfigurations interfaceConfigurations,
            L2vpn l2vpn) throws TransactionCommitFailedException {

        Optional<DataBroker> optional = MountPointHelper.getDataBroker(mountService, nodeName);
        if (!optional.isPresent()) {
            LOG.error("Could not retrieve MountPoint for {}", nodeName);
            return;
        }

        WriteTransaction transaction = optional.get().newWriteOnlyTransaction();
        transaction.merge(LogicalDatastoreType.CONFIGURATION,
                InterfaceHelper.getInterfaceConfigurationsId(), interfaceConfigurations);
        transaction.merge(LogicalDatastoreType.CONFIGURATION, L2vpnHelper.getL2vpnId(), l2vpn);
        transaction.submit().checkedGet();
    }

    protected void createSubInterface(String nodeName,
            InterfaceConfigurations interfaceConfigurations)
            throws TransactionCommitFailedException {

        Optional<DataBroker> optional = MountPointHelper.getDataBroker(mountService, nodeName);
        if (!optional.isPresent()) {
            LOG.error("Could not retrieve MountPoint for {}", nodeName);
            return;
        }

        WriteTransaction transaction = optional.get().newWriteOnlyTransaction();
        transaction.merge(LogicalDatastoreType.CONFIGURATION,
                InterfaceHelper.getInterfaceConfigurationsId(), interfaceConfigurations);
        transaction.submit().checkedGet();
    }

    @Override
    public void deactivate(List<EndPoint> endPoints, String serviceId, boolean isExclusive,
            ServiceType serviceType)
            throws TransactionCommitFailedException, ResourceActivatorException {
        String innerOuterName = getInnerName(serviceId);
        ServicePort port = toServicePort(endPoints.stream().findFirst().get(),
                CommonUtils.NETCONF_TOPOLODY_NAME);

        InstanceIdentifier<BridgeDomain> bridgeDomainId =
                deactivateBridgeDomain(innerOuterName, innerOuterName);
        InstanceIdentifier<InterfaceConfiguration> interfaceConfigurationId =
                deactivateInterface(port, isExclusive);
        doDeactivate(port, bridgeDomainId, interfaceConfigurationId, isExclusive,
                endPoints.stream().findFirst().get());
    }

    private InstanceIdentifier<BridgeDomain> deactivateBridgeDomain(String outerName,
            String innerName) {
        return InstanceIdentifier.builder(L2vpn.class).child(Database.class)
                .child(BridgeDomainGroups.class)
                .child(BridgeDomainGroup.class,
                        new BridgeDomainGroupKey(new CiscoIosXrString(outerName)))
                .child(BridgeDomains.class)
                .child(BridgeDomain.class, new BridgeDomainKey(new CiscoIosXrString(innerName)))
                .build();
    }

    private InstanceIdentifier<InterfaceConfiguration> deactivateInterface(ServicePort port,
            boolean isExclusive) {
        return InstanceIdentifier
                .builder(
                        InterfaceConfigurations.class)
                .child(InterfaceConfiguration.class,
                        new InterfaceConfigurationKey(new InterfaceActive("act"),
                                isExclusive ? InterfaceHelper.getInterfaceName(port)
                                        : InterfaceHelper.getSubInterfaceName(port)))
                .build();
    }


    protected void doDeactivate(ServicePort port, InstanceIdentifier<BridgeDomain> bridgeDomainId,
            InstanceIdentifier<InterfaceConfiguration> interfaceConfigurationId,
            boolean isExclusive, EndPoint endpoint) throws TransactionCommitFailedException {

        Optional<DataBroker> optional =
                MountPointHelper.getDataBroker(mountService, port.getNode().getValue());
        if (!optional.isPresent()) {
            LOG.error("Could not retrieve MountPoint for {}", port.getNode().getValue());
            return;
        }

        WriteTransaction transaction = optional.get().newWriteOnlyTransaction();

        if (!CommonUtils.isSameInterface(endpoint, inls)) {
            if (!CommonUtils.isSameDevice(endpoint, dvls)) {
                transaction.delete(LogicalDatastoreType.CONFIGURATION, bridgeDomainId);
            }
            transaction.delete(LogicalDatastoreType.CONFIGURATION, interfaceConfigurationId);
        }

        transaction.submit().checkedGet();
    }

    protected abstract BdPseudowires activateBdPseudowire(ServicePort neighbor);

    protected abstract BridgeDomainGroups activateBridgeDomain(String outerName, String innerName,
            ServicePort port, ServicePort neighbor, BdPseudowires bdPseudowires,
            boolean isExclusive);

    protected abstract L2vpn activateL2Vpn(BridgeDomainGroups bridgeDomainGroups);

    protected abstract String getInnerName(String serviceId);

    protected abstract String getOuterName(String serviceId);

    protected abstract InterfaceConfigurations activateInterface(ServicePort portA,
            ServicePort portZ, long mtu, boolean isExclusive);

    protected abstract InterfaceConfigurations createSubInterface(ServicePort portA,
            ServicePort portZ, long mtu);

}
