/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.cli;

import java.math.BigInteger;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.unimgr.api.IUnimgrConsoleProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.uni.SpeedBuilder;

@Command(name = "uni-update",
scope = "uni",
description = "Updates an uni to the controller.")
public class UniUpdateShellCommand extends OsgiCommandSupport{
    @Option(name = "-ip",
            aliases = { "--ipAddress" },
            description = "IpAddress of the Uni",
            required = true,
            multiValued = false)
    private String ipAddress = "any";

    @Option(name = "-ma",
            aliases = { "--mac-address" },
            description = "The mac address.\n-ma / --mac-address <mac-address>",
            required = true,
            multiValued = false)
    private String macAddress = "any";

    @Option(name = "-ml",
            aliases = { "--mac-layer" },
            description = "The mac layer.\n-ml / --mac-layer <mac-layer",
            required = false,
            multiValued = false)
    private String macLayer = "IEEE 802.3-2005";

    @Option(name = "-m",
            aliases = { "--mode" },
            description = "The mode.\n-m / --mode <mode>",
            required = false,
            multiValued = false)
    private String mode = "Full Duplex";

    @Option(name = "-ms",
            aliases = { "--mtu-size" },
            description = "The mtu size.\n-ms / --mtu-size <mtu-size>",
            required = false,
            multiValued = false)
    private String mtuSize = "0";

    @Option(name = "-pm",
            aliases = { "--physical-medium" },
            description = "The physical medium.\n-pm / --physical-medium <physical-medium>",
            required = false,
            multiValued = false)
    private String physicalMedium = "UNI TypeFull Duplex 2 Physical Interface";

    protected IUnimgrConsoleProvider provider;

    @Option(name = "-s",
            aliases = { "--speed" },
            description = "Spped.\n-s / --speed 10M/100M/1G/10G",
            required = false,
            multiValued = false)
    private String speed = "";

    @Option(name = "-t",
            aliases = { "--type" },
            description = "The type.\n-t / --type <type>",
            required = false,
            multiValued = false)
    private String type = "";

    public UniUpdateShellCommand(IUnimgrConsoleProvider provider) {
        this.provider = provider;
    }

    @Override
    protected Object doExecute() throws Exception {
        final UniAugmentation uniAug = new UniAugmentationBuilder()
                .setMacAddress(new MacAddress(macAddress))
                .setMacLayer(macLayer)
                .setMode(mode)
                .setMtuSize(BigInteger.valueOf(Long.valueOf(mtuSize)))
                .setPhysicalMedium(physicalMedium)
                .setSpeed(new SpeedBuilder().setSpeed(Utilis.getSpeed(speed)).build())
                .setType(type)
                .setIpAddress(new IpAddress(ipAddress.toCharArray()))
                .build();
        if (provider.updateUni(uniAug)) {
            return new String("Uni with ip " +ipAddress+" updated");
        } else {
            return new String("Error updating new Uni");
        }
    }
}