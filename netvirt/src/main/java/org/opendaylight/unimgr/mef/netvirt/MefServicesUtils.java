/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
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
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.Identifier45;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.RetailSvcIdType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

public class MefServicesUtils {

    public static InstanceIdentifier<Unis> getIpvcUnisInstanceIdentifier() {
        return InstanceIdentifier.builder(MefServices.class).child(MefService.class).child(Ipvc.class).child(Unis.class).build();
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
        return InstanceIdentifier.create(MefServices.class).child(MefService.class, new MefServiceKey(svcId)).child(Ipvc.class);
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

    public static void setOperIpvcVpnElan(DataBroker readDataBroker, InstanceIdentifier<Ipvc> identifier, String vpnId, Identifier45 uniId,
            Identifier45 ipUniId, String elanId, String elanPortId, WriteTransaction tx) {
        IpvcVpn ipvcVpnCur = getOperIpvcVpn(readDataBroker, identifier);

        synchronized (vpnId.intern()) {
            InstanceIdentifier<IpvcVpn> path = identifier.augmentation(IpvcVpn.class);
            IpvcVpnBuilder ipvcVpnBuilder = null;
            if (ipvcVpnCur != null) {
                ipvcVpnBuilder = new IpvcVpnBuilder(ipvcVpnCur);
            } else {
                ipvcVpnBuilder = new IpvcVpnBuilder();

            }
            ipvcVpnBuilder.setVpnId(vpnId);

            List<VpnElans> vpnElansEx = (ipvcVpnBuilder.getVpnElans() != null) ? ipvcVpnBuilder.getVpnElans()
                    : new ArrayList<>();
            VpnElansBuilder vpnElans = new VpnElansBuilder();
            vpnElans.setElanId(elanId);
            vpnElans.setUniId(uniId);
            vpnElans.setIpUniId(ipUniId);
            vpnElans.setElanPort(elanPortId);
            vpnElansEx.add(vpnElans.build());
            ipvcVpnBuilder.setVpnElans(vpnElansEx);

            tx.merge(LogicalDatastoreType.OPERATIONAL, path, ipvcVpnBuilder.build(), true);
        }
    }   
}
