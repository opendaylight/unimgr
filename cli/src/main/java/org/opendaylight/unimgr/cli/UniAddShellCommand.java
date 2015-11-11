/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
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
import org.opendaylight.unimgr.command.UniCreateCommand;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Uni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed100MBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10GBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10MBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed1GBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.uni.Speed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "uni-add",
         scope = "uni",
         description = "Adds an uni to the controller.")
public class UniAddShellCommand extends OsgiCommandSupport {

    private static final Logger LOG = LoggerFactory.getLogger(UniAddShellCommand.class);
    protected IUnimgrConsoleProvider provider;

    @Option(name = "-pm",
            aliases = { "--physical-medium" },
            description = "The physical medium.\n-pm / --physical-medium <physical-medium>",
            required = false,
            multiValued = false)
    private String physicalMedium = "UNI TypeFull Duplex 2 Physical Interface";

    @Option(name = "-ma",
            aliases = { "--mac-address" },
            description = "The mac address.\n-ma / --mac-address <mac-address>",
            required = true,
            multiValued = false)
    private String macAddress = "any";

    @Option(name = "-m",
            aliases = { "--mode" },
            description = "The mode.\n-m / --mode <mode>",
            required = false,
            multiValued = false)
    private String mode = "Full Duplex";

    @Option(name = "-ml",
            aliases = { "--mac-layer" },
            description = "The mac layer.\n-ml / --mac-layer <mac-layer",
            required = false,
            multiValued = false)
    private String macLayer = "IEEE 802.3-2005";

    @Option(name = "-t",
            aliases = { "--type" },
            description = "The type.\n-t / --type <type>",
            required = false,
            multiValued = false)
    private String type = "";

    @Option(name = "-ms",
            aliases = { "--mtu-size" },
            description = "The mtu size.\n-ms / --mtu-size <mtu-size>",
            required = false,
            multiValued = false)
    private String mtuSize = "0";

    @Option(name = "-s",
            aliases = { "--speed" },
            description = "Spped.\n-s / --speed 10M/100M/1G/10G",
            required = false,
            multiValued = true)
    private String speed = "10M";

    @Option(name = "-ip",
            aliases = { "--ipAddress" },
            description = "IpAddress of the Uni",
            required = true,
            multiValued = false)
    private String ipAddress = "any";

    public UniAddShellCommand(IUnimgrConsoleProvider provider) {
        this.provider = provider;
    }

    private Object getSpeed() {

        System.out.println(speed);

        Object speedObject = null;
        if (speed.equals("10M")) {
            speedObject = new Speed10MBuilder().build();
        }
        if (speed.equals("100M")) {
            speedObject = new Speed100MBuilder().build();
        }
        if (speed.equals("1G")) {
            speedObject = new Speed1GBuilder().build();
        }
        if (speed.equals("10G")) {
            speedObject = new Speed10GBuilder().build();
        }
        System.out.println(speedObject);
        return speedObject;
    }

    @Override
    protected Object doExecute() throws Exception {
        Uni uni = new UniAugmentationBuilder()
                        .setMacAddress(new MacAddress(macAddress))
                        .setMacLayer(macLayer)
                        .setMode(mode)
                        .setMtuSize(BigInteger.valueOf(Long.valueOf(mtuSize)))
                        .setPhysicalMedium(physicalMedium)
                        .setSpeed((Speed) getSpeed())
                        .setType(type)
                        .setIpAddress(new IpAddress(ipAddress.toCharArray()))
                        .build();

        LOG.info("add uni command");
        if (provider.addUni(uni)) {
            return String.format("Uni created {}", uni.getIpAddress().getIpv4Address());
        } else {
            return new String("Error creating new uni");
        }
    }
}
