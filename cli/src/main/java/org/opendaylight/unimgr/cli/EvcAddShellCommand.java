/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.cli;

import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.unimgr.api.IUnimgrConsoleProvider;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.EgressBwBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.IngressBwBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDestKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSourceKey;

@Command(name = "evc-add",
    scope = "uni",
    description = "Add evc to the controller.")
public class EvcAddShellCommand extends OsgiCommandSupport {

    protected IUnimgrConsoleProvider provider;

    @Option(name = "-IPs",
            aliases = { "--IP-Address-source-uni" },
            description = "The IP address of the source UNI.\n-IPs / --IP Address source uni",
            required = true,
            multiValued = false)
    private String ipSource = "";

    @Option(name = "-IPd",
            aliases = { "--IP-Address-destenation-uni" },
            description = "The IP address of the destenation UNI.\n-IPs / --IP Address destenation uni",
            required = true,
            multiValued = false)
    private String ipDestination = "";

    @Option(name = "-egress",
            aliases = { "--egress-speed" },
            description = "egress speed.\n-s / --speed 10M/100M/1G/10G",
            required = false,
            multiValued = false)
    private String egress = "1G";

    @Option(name = "-ingress",
            aliases = { "--ingress-speed" },
            description = "ingress speed.\n-s / --speed 10M/100M/1G/10G",
            required = false,
            multiValued = false)
    private String ingress = "1G";

    public EvcAddShellCommand(IUnimgrConsoleProvider provider) {
        this.provider = provider;
    }

    @Override
    protected Object doExecute() throws Exception {
        Short order = new Short("0");
        IpAddress ipAddreSource = new IpAddress(ipSource.toCharArray());
        UniSource uniSource = new UniSourceBuilder()
                                  .setIpAddress(ipAddreSource)
                                  .setKey(new UniSourceKey(order))
                                  .setOrder(order)
                                  .build();
        List<UniSource> uniSourceList = new ArrayList<UniSource>();
        uniSourceList.add(uniSource);
        IpAddress ipAddreDest = new IpAddress(ipDestination.toCharArray());
        UniDest uniDest = new UniDestBuilder()
                          .setOrder(order)
                          .setKey(new UniDestKey(order))
                          .setIpAddress(ipAddreDest)
                          .build();
        List<UniDest> uniDestList = new ArrayList<UniDest>();
        uniDestList.add(uniDest);
        EvcAugmentation evcAug = new EvcAugmentationBuilder()
                                     .setCosId(UnimgrConstants.EVC_PREFIX + 1)
                                     .setEgressBw(new EgressBwBuilder().setSpeed(Utils.getSpeed(egress)).build())
                                     .setIngressBw(new IngressBwBuilder().setSpeed(Utils.getSpeed(ingress)).build())
                                     .setUniDest(uniDestList)
                                     .setUniSource(uniSourceList)
                                     .build();
        if (provider.addEvc(evcAug)) {
            return new String("Evc with Source Uni " + ipSource + " and destenation Uni " + ipDestination + " created");
        } else {
            return new String("Error creating new Evc");
        }
    }
}
