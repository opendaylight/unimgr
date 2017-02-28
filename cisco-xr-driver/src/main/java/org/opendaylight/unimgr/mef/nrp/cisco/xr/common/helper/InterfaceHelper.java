/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper;

import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurationsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations._interface.configuration.Mtus;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.InterfaceConfiguration3;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.InterfaceConfiguration3Builder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109._interface.configurations._interface.configuration.L2Transport;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109._interface.configurations._interface.configuration.L2TransportBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Helper, designated to support interface configuration
 *
 * @author krzysztof.bijakowski@amartus.com
 */
public class InterfaceHelper {
    private List<InterfaceConfiguration> configurations;

    public static InterfaceName getInterfaceName(FcPort port) {
        String interfaceName = port.getTp().getValue();

        if(interfaceName.contains(":")) {
            interfaceName = interfaceName.split(":")[1];
        }

        return new InterfaceName(interfaceName);
    }

    public static InstanceIdentifier<InterfaceConfigurations> getInterfaceConfigurationsId() {
        return InstanceIdentifier.builder(InterfaceConfigurations.class).build();
    }

    public InterfaceHelper() {
        configurations = new LinkedList<>();
    }

    public InterfaceHelper addInterface(FcPort port, Optional<Mtus> mtus, boolean setL2Transport) {
        return addInterface(getInterfaceName(port), mtus, setL2Transport);
    }

    public InterfaceHelper addInterface(InterfaceName name, Optional<Mtus> mtus, boolean setL2Transport) {
        InterfaceConfigurationBuilder configurationBuilder = new InterfaceConfigurationBuilder();

        configurationBuilder
            .setInterfaceName(name)
            .setActive(new InterfaceActive("act"))
            .setShutdown(Boolean.FALSE);

        if(mtus.isPresent()) {
            configurationBuilder.setMtus(mtus.get());
        }

        if(setL2Transport) {
            setL2Configuration(configurationBuilder);
        }

        configurations.add(configurationBuilder.build());
        return this;
    }

    public InterfaceConfigurations build() {
        return new InterfaceConfigurationsBuilder()
            .setInterfaceConfiguration(configurations)
            .build();
    }

    private void setL2Configuration(InterfaceConfigurationBuilder configurationBuilder) {
        L2Transport l2transport = new L2TransportBuilder()
            .setEnabled(true)
            .build();

        InterfaceConfiguration3 augmentation = new InterfaceConfiguration3Builder()
            .setL2Transport(l2transport)
            .build();

        configurationBuilder.addAugmentation(InterfaceConfiguration3.class, augmentation);
    }
}
