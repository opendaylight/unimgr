/*
 * Copyright (c) 2019 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator;

import java.util.Optional;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.InterfaceHelper;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util.MtuUtils;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations._interface.configuration.Mtus;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceActivator {

    protected InterfaceConfigurations activate(ServicePort port, ServicePort neighbor, long mtu, boolean isExclusive) {
        String interfraceName = port.getInterfaceName();
        new MtuUtils();
        Mtus mtus = MtuUtils.generateMtus(mtu, new CiscoIosXrString(interfraceName));

        // Enable L2Trasportation for port basesd service
        boolean setL2Transport = (isExclusive) ? true : false;

        return new InterfaceHelper().addInterface(port, Optional.of(mtus), setL2Transport).build();
    }

    protected InstanceIdentifier<InterfaceConfiguration> deactivate(ServicePort port, boolean isExclusive) {

        return InstanceIdentifier
                .builder(
                        InterfaceConfigurations.class)
                .child(InterfaceConfiguration.class,
                        new InterfaceConfigurationKey(new InterfaceActive("act"),
                                isExclusive == true ? InterfaceHelper.getInterfaceName(port)
                                        : InterfaceHelper.getSubInterfaceName(port)))
                .build();
    }

    protected InterfaceConfigurations buildSubInterface(ServicePort port, ServicePort neighbor, long mtu) {
        String mtuOwnerName = "sub_vlan";
        new MtuUtils();
        Mtus mtus = MtuUtils.generateMtus(mtu, new CiscoIosXrString(mtuOwnerName));

        return new InterfaceHelper().addSubInterface(port, Optional.of(mtus)).build();
    }

    protected InterfaceConfigurations activateLocalInterface(ServicePort port, ServicePort neighbor, long mtu, boolean isExclusive) {
        boolean setL2Transport = (isExclusive) ? true : false;

        return new InterfaceHelper().addInterface(port, Optional.empty(), setL2Transport)
                .addInterface(neighbor, Optional.empty(), setL2Transport).build();
    }
}
