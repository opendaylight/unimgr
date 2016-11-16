/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.subnets.Subnet;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.IpvcVpn;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.IpvcVpnBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.MefServices;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.MefService;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.MefServiceKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.Evc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.Ipvc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.ipvc.Unis;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.ipvc.VpnElans;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.ipvc.VpnElansBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.ipvc.vpn.elans.Subnets;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.ipvc.choice.ipvc.vpn.elans.SubnetsBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.Identifier45;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.RetailSvcIdType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

public class MefServicesUtils {

    public static InstanceIdentifier<Unis> getIpvcUnisInstanceIdentifier() {
        return InstanceIdentifier.builder(MefServices.class).child(MefService.class).child(Ipvc.class).child(Unis.class)
                .build();
    }

    public static InstanceIdentifier<Evc> getEvcsInstanceIdentifier() {
        return getMefServiceInstanceIdentifier().child(Evc.class);
    }

    public static InstanceIdentifier<MefService> getMefServiceInstanceIdentifier() {
        return InstanceIdentifier.create(MefServices.class).child(MefService.class);
    }

    public static InstanceIdentifier<MefServices> getMefServicesInstanceIdentifier() {
        return InstanceIdentifier.create(MefServices.class);
    }

    public static InstanceIdentifier<Ipvc> getIpvcsInstanceIdentifier() {
        return InstanceIdentifier.create(MefServices.class).child(MefService.class).child(Ipvc.class);
    }

    public static InstanceIdentifier<Ipvc> getIpvcsInstanceIdentifier(RetailSvcIdType svcId) {
        return InstanceIdentifier.create(MefServices.class).child(MefService.class, new MefServiceKey(svcId))
                .child(Ipvc.class);
    }

    public static VpnElans findVpnForNetwork(Subnet newSubnet, IpvcVpn ipvcVpn) {
        return findVpnElanForNetwork(newSubnet.getUniId(), newSubnet.getIpUniId(), ipvcVpn);
    }

    public static VpnElans findVpnElanForNetwork(Identifier45 uniId, Identifier45 ipUniId, IpvcVpn ipvcVpn) {
        List<VpnElans> vpnElans = ipvcVpn != null && ipvcVpn.getVpnElans() != null ? ipvcVpn.getVpnElans()
                : Collections.emptyList();

        for (VpnElans vpnElan : vpnElans) {
            if (vpnElan.getUniId().equals(uniId) && vpnElan.getIpUniId().equals(ipUniId)) {
                return vpnElan;
            }
        }
        return null;
    }

    public static VpnElans findNetwork(Subnet newSubnet, VpnElans vpnElan) {
        String subnetStr = NetvirtVpnUtils.ipPrefixToString(newSubnet.getSubnet());
        return findNetwork(subnetStr, vpnElan);
    }

    public static VpnElans findNetwork(String newSubnet, VpnElans vpnElan) {
        List<Subnets> subnets = vpnElan != null && vpnElan.getSubnets() != null ? vpnElan.getSubnets()
                : Collections.emptyList();
        if (subnets.stream().anyMatch((s) -> s.getSubnet().equals(newSubnet))) {
            return vpnElan;
        }
        return null;
    }


    public static IpvcVpn getOperIpvcVpn(DataBroker dataBroker, InstanceIdentifier<Ipvc> identifier) {
        InstanceIdentifier<IpvcVpn> path = identifier.augmentation(IpvcVpn.class);
        Optional<IpvcVpn> ipvcVpn = MdsalUtils.read(dataBroker, LogicalDatastoreType.OPERATIONAL, path);
        if (ipvcVpn.isPresent()) {
            return ipvcVpn.get();
        } else {
            return null;
        }
    }

    public static void addOperIpvcVpnElan(InstanceIdentifier<Ipvc> identifier, String vpnId, WriteTransaction tx) {

        synchronized (vpnId.intern()) {
            InstanceIdentifier<IpvcVpn> path = identifier.augmentation(IpvcVpn.class);

            IpvcVpnBuilder ipvcVpnBuilder = new IpvcVpnBuilder();
            ipvcVpnBuilder.setVpnId(vpnId);

            tx.merge(LogicalDatastoreType.OPERATIONAL, path, ipvcVpnBuilder.build(), true);
        }
    }

    public static void addOperIpvcVpnElan(InstanceIdentifier<Ipvc> identifier, String vpnId, Identifier45 uniId,
            Identifier45 ipUniId, String elanId, String elanPortId, List<String> newSubnets, WriteTransaction tx) {

        synchronized (vpnId.intern()) {
            InstanceIdentifier<IpvcVpn> path = identifier.augmentation(IpvcVpn.class);

            IpvcVpnBuilder ipvcVpnBuilder = new IpvcVpnBuilder();
            ipvcVpnBuilder.setVpnId(vpnId);

            List<VpnElans> vpnElansEx = new ArrayList<>();
            VpnElansBuilder vpnElans = new VpnElansBuilder();
            vpnElans.setElanId(elanId);
            vpnElans.setUniId(uniId);
            vpnElans.setIpUniId(ipUniId);
            vpnElans.setElanPort(elanPortId);
            List<Subnets> subnets = new ArrayList<>();
            if (newSubnets != null) {
                newSubnets.forEach(s -> {
                    SubnetsBuilder sb = new SubnetsBuilder();
                    sb.setSubnet(s);
                    subnets.add(sb.build());
                });
            }
            vpnElans.setSubnets(subnets);
            vpnElansEx.add(vpnElans.build());
            ipvcVpnBuilder.setVpnElans(vpnElansEx);

            tx.merge(LogicalDatastoreType.OPERATIONAL, path, ipvcVpnBuilder.build(), true);
        }
    }

    public static void addOperIpvcVpnElan(DataBroker dataBroker, InstanceIdentifier<Ipvc> identifier, String vpnId,
            Identifier45 uniId, Identifier45 ipUniId, String elanId, String elanPortId, List<String> newSubnets) {
        WriteTransaction tx = MdsalUtils.createTransaction(dataBroker);
        addOperIpvcVpnElan(identifier, vpnId, uniId, ipUniId, elanId, elanPortId, newSubnets, tx);
        MdsalUtils.commitTransaction(tx);
    }

    public static void removeOperIpvcVpn(InstanceIdentifier<Ipvc> identifier, WriteTransaction tx) {
        InstanceIdentifier<IpvcVpn> path = identifier.augmentation(IpvcVpn.class);
        tx.delete(LogicalDatastoreType.OPERATIONAL, path);
    }

    public static void removeOperIpvcSubnet(DataBroker dataBroker, InstanceIdentifier<Ipvc> identifier, String vpnId,
            Identifier45 uniId, Identifier45 ipUniId, String elanId, String elanPortId, String deleteSubnet) {
        InstanceIdentifier<IpvcVpn> path = identifier.augmentation(IpvcVpn.class);
        IpvcVpn ipvcVpn = getOperIpvcVpn(dataBroker, identifier);
        if (ipvcVpn == null || ipvcVpn.getVpnElans() == null) {
            return;
        }
        IpvcVpnBuilder ipvcVpnBuilder = new IpvcVpnBuilder(ipvcVpn);
        List<VpnElans> vpnElansEx = ipvcVpnBuilder.getVpnElans();

        VpnElans vpnElans = findVpnElanForNetwork(uniId, ipUniId, ipvcVpn);
        vpnElans = findNetwork(deleteSubnet, vpnElans);

        if (vpnElans != null) {
            vpnElansEx.remove(vpnElans);
            VpnElansBuilder vpnElansB = new VpnElansBuilder(vpnElans);
            List<Subnets> exSubnets = vpnElansB.getSubnets();
            List<Subnets> newSubnets = exSubnets.stream().filter(s -> ! s.getSubnet().equals(deleteSubnet)).collect(Collectors.toList());
            vpnElansB.setSubnets(newSubnets);
            vpnElansEx.add(vpnElansB.build());
        }
        MdsalUtils.syncWrite(dataBroker, LogicalDatastoreType.OPERATIONAL, path, ipvcVpnBuilder.build());
    }

    public static void removeOperIpvcElan(DataBroker dataBroker, InstanceIdentifier<Ipvc> identifier, String vpnId,
            Identifier45 uniId, Identifier45 ipUniId, String elanId, String elanPortId) {
        InstanceIdentifier<IpvcVpn> path = identifier.augmentation(IpvcVpn.class);
        IpvcVpn ipvcVpn = getOperIpvcVpn(dataBroker, identifier);
        if (ipvcVpn == null || ipvcVpn.getVpnElans() == null) {
            return;
        }
        IpvcVpnBuilder ipvcVpnBuilder = new IpvcVpnBuilder(ipvcVpn);
        List<VpnElans> vpnElansEx = ipvcVpnBuilder.getVpnElans();

        VpnElans vpnElans = findVpnElanForNetwork(uniId, ipUniId, ipvcVpn);
        if (vpnElans != null) {
            vpnElansEx.remove(vpnElans);
        }
        MdsalUtils.syncWrite(dataBroker, LogicalDatastoreType.OPERATIONAL, path, ipvcVpnBuilder.build());
    }

}
