/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.common.helper;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.common.ServicePort;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceActive;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceConfigurationsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceModeEnum;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations._interface.configuration.Mtus;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev180615.InterfaceConfiguration3;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev180615.InterfaceConfiguration3Builder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev180615._interface.configurations._interface.configuration.EthernetServiceBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev180615._interface.configurations._interface.configuration.ethernet.service.Encapsulation;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev180615._interface.configurations._interface.configuration.ethernet.service.EncapsulationBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.datatypes.rev151109.Match;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.datatypes.rev151109.VlanTagOrAny;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.InterfaceConfiguration4;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615.InterfaceConfiguration4Builder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615._interface.configurations._interface.configuration.L2Transport;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev180615._interface.configurations._interface.configuration.L2TransportBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.Uint32;

/*
 * Helper, designated to support interface configuration
 *
 * @author krzysztof.bijakowski@amartus.com
 */
public class InterfaceHelper {
    private List<InterfaceConfiguration> configurations;

    public static InterfaceName getInterfaceName(ServicePort port) {
        String interfaceName = port.getTp().getValue();

        if (interfaceName.contains(":")) {
            interfaceName = interfaceName.split(":")[1];
        }

        return new InterfaceName(interfaceName);
    }

    public static InterfaceName getSubInterfaceName(ServicePort port) {
        String interfaceName = port.getTp().getValue();

        if (interfaceName.contains(":")) {
            interfaceName = interfaceName.split(":")[1];
        }
        // adding vlan id with interface name
        interfaceName = interfaceName + "." + port.getVlanId();
        return new InterfaceName(interfaceName);
    }


    public static InstanceIdentifier<InterfaceConfigurations> getInterfaceConfigurationsId() {
        return InstanceIdentifier.builder(InterfaceConfigurations.class).build();
    }

    public InterfaceHelper() {
        configurations = new LinkedList<>();
    }

    public InterfaceHelper addInterface(ServicePort port, Optional<Mtus> mtus, boolean setL2Transport) {
        return addInterface(getInterfaceName(port), mtus, setL2Transport);
    }

    public InterfaceHelper addInterface(InterfaceName name, Optional<Mtus> mtus, boolean setL2Transport) {
        InterfaceConfigurationBuilder configurationBuilder = new InterfaceConfigurationBuilder();

        configurationBuilder
            .setInterfaceName(name)
            .setActive(new InterfaceActive("act"));

        if (mtus.isPresent()) {
            configurationBuilder.setMtus(mtus.get());
        }

        if (setL2Transport) {
            setL2Configuration(configurationBuilder);
        }

        configurations.add(configurationBuilder.build());
        return this;
    }

    public InterfaceHelper addSubInterface(ServicePort port, Optional<Mtus> mtus) {
        return addSubInterface(getSubInterfaceName(port), mtus, port);
    }

    public InterfaceHelper addSubInterface(InterfaceName name, Optional<Mtus> mtus, ServicePort port) {
        InterfaceConfigurationBuilder configurationBuilder = new InterfaceConfigurationBuilder();

        configurationBuilder
            .setInterfaceName(name)
            .setActive(new InterfaceActive("act"))
            //.setShutdown(Boolean.FALSE)
            .setDescription("Create sub interface through ODL")
            .setInterfaceModeNonPhysical(InterfaceModeEnum.L2Transport);
            // set ethernet service
        setEthernetService(configurationBuilder, port);

        if (mtus.isPresent()) {
            configurationBuilder.setMtus(mtus.get());
        }
        configurations.add(configurationBuilder.build());

        return this;
    }

    private void setEthernetService(InterfaceConfigurationBuilder configurationBuilder, ServicePort port) {
        Encapsulation encapsulation = new EncapsulationBuilder()
            .setOuterRange1Low(new VlanTagOrAny(Uint32.valueOf(port.getVlanId())))
            .setOuterTagType(Match.MatchDot1q)
            .build();

        InterfaceConfiguration3 augmentation = new InterfaceConfiguration3Builder()
                .setEthernetService(new EthernetServiceBuilder()
                    .setEncapsulation(encapsulation)
                    .build()
                 )
                .build();

        configurationBuilder.addAugmentation(InterfaceConfiguration3.class, augmentation);
    }

    public InterfaceConfigurations build() {
        return new InterfaceConfigurationsBuilder()
            .setInterfaceConfiguration(configurations)
            .build();
    }

    private void setL2Configuration(InterfaceConfigurationBuilder configurationBuilder) {
        L2Transport l2transport = new L2TransportBuilder()
                    .setEnabled(Empty.getInstance())
                    .build();

        InterfaceConfiguration4 augmentation = new InterfaceConfiguration4Builder()
            .setL2Transport(l2transport)
            .build();

        configurationBuilder.addAugmentation(InterfaceConfiguration4.class, augmentation);
    }

}
