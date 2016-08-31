/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.Evc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.Unis;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.UnisBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.Uni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.UniBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.UniKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.uni.EvcUniCeVlansBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.uni.evc.uni.ce.vlans.EvcUniCeVlanBuilder;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.uni.evc.uni.ce.vlans.EvcUniCeVlanKey;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.EvcUniRoleType;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.types.rev150526.Identifier45;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan.L2vlanMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.ElanInterfaces;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElanInterfaceListener extends UnimgrDataTreeChangeListener<ElanInterfaces> {

    private static final Logger log = LoggerFactory.getLogger(ElanInterfaceListener.class);
    private ListenerRegistration<ElanInterfaceListener> elanListenerRegistration;

    public ElanInterfaceListener(final DataBroker dataBroker) {
        super(dataBroker);

        registerListener();
    }

    public void registerListener() {
        try {
            final DataTreeIdentifier<ElanInterfaces> dataTreeIid = new DataTreeIdentifier<>(
                    LogicalDatastoreType.CONFIGURATION, NetvirtUtils.getElanInterfacesInstanceIdentifier());
            elanListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIid, this);
            log.info("ElanInterfaceDataTreeChangeListener created and registered");
        } catch (final Exception e) {
            log.error("ElanInterface DataChange listener registration failed !", e);
            throw new IllegalStateException("ElanInterface registration Listener failed.", e);
        }
    }

    @Override
    public void close() throws Exception {
        elanListenerRegistration.close();
    }

    @Override
    public void add(DataTreeModification<ElanInterfaces> newDataObject) {
        log.info("org.opendaylight.unimgr.mef.netvirt.ElanInterfaceListener in add");

        handleUpdatedInterfaces(newDataObject);

    }

    private void handleUpdatedInterfaces(DataTreeModification<ElanInterfaces> newDataObject) {
        ElanInterfaces instance = newDataObject.getRootNode().getDataAfter();
        Optional<String> findFirst = instance.getElanInterface().stream().map(x -> x.getElanInstanceName()).findFirst();
        if (!findFirst.isPresent()) {
            log.info("empty - exiting");
            return;
        }
        String elanInstanceName = findFirst.get();
        if (!StringUtils.isNumericSpace(elanInstanceName)) {
            elanInstanceName = String.valueOf(elanInstanceName.hashCode());
        }

        com.google.common.base.Optional<Evc> evc = MefUtils.getEvc(dataBroker, elanInstanceName);

        if (MefUtils.isEvcAdminStateEnabled(dataBroker, elanInstanceName)) {
            log.info("The EVC {} is admin state enabled, ignoring");
            return;
        }

        final String instanceName = elanInstanceName;
        List<Interface> ifaces = instance.getElanInterface().stream()
                .map(x -> NetvirtUtils.getIetfInterface(dataBroker, x.getName()))//
                .filter(x -> x.isPresent())//
                .map(x -> x.get())//
                .collect(Collectors.toList());

        if (log.isInfoEnabled()) {
            log.info("adding unis from interfaces [{}] are not null from [{}] interfaces are: [{}]", ifaces.size(),
                    instance.getElanInterface().size(),
                    StringUtils.join(
                            instance.getElanInterface().stream().map(x -> x.getName()).collect(Collectors.toList()),
                            ", "));

        }
        List<String> trunks = ifaces.stream().filter(x -> x.getAugmentation(IfL2vlan.class) != null)//
                .filter(x -> x.getAugmentation(IfL2vlan.class).getL2vlanMode() == L2vlanMode.Trunk)//
                .map(x -> x.getName()).collect(Collectors.toList());

        Map<String, List<Integer>> vlans = ifaces.stream()//
                .filter(x -> x.getAugmentation(IfL2vlan.class) != null)//
                .filter(x -> x.getAugmentation(IfL2vlan.class)//
                        .getL2vlanMode() == L2vlanMode.TrunkMember)//
                .collect(//
                        Collectors.groupingBy(//
                                x -> x.getAugmentation(ParentRefs.class).getParentInterface(), //
                                Collectors.mapping(//
                                        x -> x.getAugmentation(IfL2vlan.class).getVlanId().getValue(), //
                                        Collectors.toList())));

        Unis unisObj = new UnisBuilder()
                .setUni(Stream.concat(trunks.stream(), vlans.keySet().stream())
                        .filter(x -> !MefUtils.EvcUniExists(dataBroker, instanceName, x))
                        .map(x -> createElanInterfaceToUni(dataBroker, x, vlans.get(x))).collect(Collectors.toList()))
                .build();
        MdsalUtils.syncUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION,
                MefUtils.getUnisInstanceIdentifier(elanInstanceName), unisObj);
    }

    private static Uni createElanInterfaceToUni(DataBroker dataBroker, String name, List<Integer> vlans) {
        if (log.isInfoEnabled()) {
            String vlansstr = "null";
            if (vlans != null) {
                vlansstr = StringUtils.join(vlans, ",");
            }
            log.info("create uni: {} setAdminStateEnabled false vlans: {}", name, vlansstr);
        }
        UniBuilder b = new UniBuilder();
        b.setAdminStateEnabled(false);
        Identifier45 _uniId = new Identifier45(name);
        b.setKey(new UniKey(_uniId));
        b.setUniId(_uniId);
        b.setRole(EvcUniRoleType.Root);
        if (vlans != null) {
            b.setEvcUniCeVlans(new EvcUniCeVlansBuilder().setEvcUniCeVlan(
                    vlans.stream().map(x -> new EvcUniCeVlanBuilder().setKey(new EvcUniCeVlanKey(x)).build())
                            .collect(Collectors.toList()))
                    .build());
        }
        return b.build();
    }

    @Override
    public void remove(DataTreeModification<ElanInterfaces> removedDataObject) {
        log.info("org.opendaylight.unimgr.mef.netvirt.ElanInterfaceListener in remove");
    }

    @Override
    public void update(DataTreeModification<ElanInterfaces> modifiedDataObject) {
        log.info("org.opendaylight.unimgr.mef.netvirt.ElanInterfaceListener in update");
        handleUpdatedInterfaces(modifiedDataObject);
    }

}