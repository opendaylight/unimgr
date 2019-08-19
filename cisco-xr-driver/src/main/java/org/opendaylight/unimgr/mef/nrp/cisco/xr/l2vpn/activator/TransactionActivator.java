/*
 * Copyright (c) 2019 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator;

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
import org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.helper.L2vpnHelper;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev161215.PolicyManager;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.L2vpn;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;

public class TransactionActivator {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionActivator.class);

    // for now QoS is ignored
    protected void activate(String nodeName, InterfaceConfigurations interfaceConfigurations, L2vpn l2vpn, MountPointService mountService,
            java.util.Optional<PolicyManager> qosConfig) throws TransactionCommitFailedException {

        Optional<DataBroker> optional = MountPointHelper.getDataBroker(mountService, nodeName);
        if (!optional.isPresent()) {
            LOG.error("Could not retrieve MountPoint for {}", nodeName);
            return;
        }

        WriteTransaction transaction = optional.get().newWriteOnlyTransaction();
        transaction.merge(LogicalDatastoreType.CONFIGURATION, InterfaceHelper.getInterfaceConfigurationsId(), interfaceConfigurations);
        transaction.merge(LogicalDatastoreType.CONFIGURATION, L2vpnHelper.getL2vpnId(), l2vpn);
        transaction.submit().checkedGet();
    }


    protected void activateSubInterface(String nodeName, InterfaceConfigurations interfaceConfigurations, MountPointService mountService)
            throws TransactionCommitFailedException {

        Optional<DataBroker> optional = MountPointHelper.getDataBroker(mountService, nodeName);
        if (!optional.isPresent()) {
            LOG.error("Could not retrieve MountPoint for {}", nodeName);
            return;
        }

        WriteTransaction transaction = optional.get().newWriteOnlyTransaction();
        transaction.merge(LogicalDatastoreType.CONFIGURATION, InterfaceHelper.getInterfaceConfigurationsId(), interfaceConfigurations);
        transaction.submit().checkedGet();
    }


    protected void deactivate(ServicePort port, InstanceIdentifier<?> ids, InstanceIdentifier<InterfaceConfiguration> interfaceConfigurationId,
            boolean isExclusive, EndPoint endpoint, MountPointService mountService, List<String> dvls, List<Uuid> inls) 
                    throws TransactionCommitFailedException {

        Optional<DataBroker> optional = MountPointHelper.getDataBroker(mountService, port.getNode().getValue());
        if (!optional.isPresent()) {
            LOG.error("Could not retrieve MountPoint for {}", port.getNode().getValue());
            return;
        }

        WriteTransaction transaction = optional.get().newWriteOnlyTransaction();

        if (ids.getTargetType().equals(
                org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.l2vpn.database.xconnect.groups.xconnect.group.p2p.xconnects.P2pXconnect.class)) {

            if (!ServicePort.isSameDevice(endpoint, dvls)) {
                transaction.delete(LogicalDatastoreType.CONFIGURATION, ids);
            }
            transaction.delete(LogicalDatastoreType.CONFIGURATION, interfaceConfigurationId);
        } else {

            if (!AbstractL2vpnBridgeDomainActivator.isSameInterface(endpoint, inls)) {
                if (!ServicePort.isSameDevice(endpoint, dvls)) {
                    transaction.delete(LogicalDatastoreType.CONFIGURATION, ids);
                }
                transaction.delete(LogicalDatastoreType.CONFIGURATION, interfaceConfigurationId);
            }
        }
        transaction.submit().checkedGet();
    }

}
