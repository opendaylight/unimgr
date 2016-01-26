/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.EgressBw;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.IngressBw;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDestKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSourceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed100MBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10GBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10MBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed1GBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "evc-update", scope = "uni", description = "Update evc to the controller.")

public class EvcUpdateShellCommand extends OsgiCommandSupport {

    private static final Logger LOG = LoggerFactory.getLogger(EvcUpdateShellCommand.class);
    protected IUnimgrConsoleProvider provider;

    @Option(name = "-IPs", aliases = {
            "--IP-Address-source-uni" }, description = "The IP address of the source UNI.\n-IPs / --IP Address source uni", required = true, multiValued = false)
    private String IPs = "";

    @Option(name = "-IPd", aliases = {
            "--IP-Address-destenation-uni" }, description = "The IP address of the destenation UNI.\n-IPs / --IP Address destenation uni", required = true, multiValued = false)
    private String IPd = "";

    @Option(name = "-egress", aliases = {
            "--egress-speed" }, description = "egress speed.\n-s / --speed 10M/100M/1G/10G", required = false, multiValued = true)
    private String egress = "";

    @Option(name = "-ingress", aliases = {
            "--ingress-speed" }, description = "ingress speed.\n-s / --speed 10M/100M/1G/10G", required = false, multiValued = true)
    private String ingress = "";

    @Option(name = "-evcKey", aliases = { "--EVC-key" }, description = "EVC key.", required = true, multiValued = false)
    private String evcKey = "";

    private Object getSpeed(String speed) {
        Object speedObject = null;
        if (speed.equals("10M")) {
            speedObject = new Speed10MBuilder().build();
        } else if (speed.equals("100M")) {
            speedObject = new Speed100MBuilder().build();
        } else if (speed.equals("1G")) {
            speedObject = new Speed1GBuilder().build();
        } else if (speed.equals("10G")) {
            speedObject = new Speed10GBuilder().build();
        }
        return speedObject;
    }

    public EvcUpdateShellCommand(IUnimgrConsoleProvider provider) {
        this.provider = provider;
    }

    @Override
    protected Object doExecute() throws Exception {
        Short order = new Short("0");
        IpAddress ipAddreSource = new IpAddress(IPs.toCharArray());
        UniSource uniSource = new UniSourceBuilder().setIpAddress(ipAddreSource).setKey(new UniSourceKey(order))
                .setOrder(order).build();
        List<UniSource> uniSourceList = new ArrayList<UniSource>();
        uniSourceList.add(uniSource);
        IpAddress ipAddreDest = new IpAddress(IPd.toCharArray());
        UniDest uniDest = new UniDestBuilder().setOrder(order).setKey(new UniDestKey(order)).setIpAddress(ipAddreDest)
                .build();
        List<UniDest> uniDestList = new ArrayList<UniDest>();
        uniDestList.add(uniDest);
        EvcAugmentation evckey = new EvcAugmentationBuilder().setCosId(evcKey).build();
        EvcAugmentation evcAug = new EvcAugmentationBuilder().setCosId(UnimgrConstants.EVC_PREFIX + 1)
                .setEgressBw((EgressBw) getSpeed(egress)).setIngressBw((IngressBw) getSpeed(ingress))
                .setUniDest(uniDestList).setUniSource(uniSourceList).build();
        if (provider.updateEvc(evckey, evcAug, uniSource, uniDest)) {
            return new String("Evc with Source Uni " + IPs + " and destenation Uni " + IPd + " updated");
        } else {
            return new String("Error updating new Evc");
        }
    }
}
