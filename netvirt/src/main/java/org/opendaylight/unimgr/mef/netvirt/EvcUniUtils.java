/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.interfacemanager.globals.IfmConstants;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.PhysicalLayers;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.Links;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.Link;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.Uni;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.uni.EvcUniCeVlans;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.mef.service.choice.evc.choice.evc.unis.uni.evc.uni.ce.vlans.EvcUniCeVlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class EvcUniUtils {

    private static final Logger logger = LoggerFactory.getLogger(EvcUniUtils.class);

    public static Link getLink(DataBroker dataBroker, String uniId) {
        Optional<org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni> optional = MdsalUtils
                .read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                        MefUtils.getUniInstanceIdentifier(uniId));

        if (!optional.isPresent()) {
            logger.error("A matching Uni doesn't exist for EvcUni {}", uniId);
            return null;
        }

        org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.Uni uni = optional
                .get();

        PhysicalLayers physicalLayers = uni.getPhysicalLayers();
        if (physicalLayers == null) {
            logger.warn("Uni {} is missing PhysicalLayers", uniId);
            return null;
        }

        Links links = physicalLayers.getLinks();
        if (links == null || links.getLink() == null) {
            logger.warn("Uni {} is has no links", uniId);
            return null;
        }

        Link link = links.getLink().get(0);
        return link;
    }

    public static void removeUni(DataBroker dataBroker, Uni data) {
        try {
            String uniId = data.getUniId().getValue();
            WriteTransaction tx = createTransaction(dataBroker);

            logger.info("Removing trunk {}", uniId);

            NetvirtUtils.delete(uniId, tx);

            Optional<List<EvcUniCeVlan>> ceVlansOptional = getCeVlans(data);
            if (!ceVlansOptional.isPresent()) {
                return;
            }

            removeTrunkMemberInterfaces(uniId, ceVlansOptional.get(), tx);
            commitTransaction(tx);
        } catch (final Exception e) {
            logger.error("Remove uni failed !", e);
        }
    }

    public static void addUni(DataBroker dataBroker, Uni data) {
        try {
            String uniId = data.getUniId().getValue();
            WriteTransaction tx = createTransaction(dataBroker);
            Link link = EvcUniUtils.getLink(dataBroker, uniId);
            String interfaceName = uniId;
            addTrunkInterface(interfaceName, MefUtils.getTrunkParentName(link), tx);

            Optional<List<EvcUniCeVlan>> ceVlansOptional = getCeVlans(data);
            if (ceVlansOptional.isPresent()) {
                addTrunkMemberInterfaces(interfaceName, ceVlansOptional.get(), tx);
            }

            commitTransaction(tx);
        } catch (final Exception e) {
            logger.error("Add uni failed !", e);
        }
    }

    private static void addTrunkInterface(String interfaceName, String parentInterfaceName, WriteTransaction tx) {
        logger.info("Adding VLAN trunk {} ParentRef {}", interfaceName, parentInterfaceName);
        Interface trunkInterface = NetvirtUtils.createTrunkInterface(interfaceName, parentInterfaceName);
        NetvirtUtils.write(trunkInterface, tx);
    }

    private static void addTrunkMemberInterfaces(String parentInterfaceName, Iterable<EvcUniCeVlan> ceVlans,
            WriteTransaction tx) {
        for (EvcUniCeVlan ceVlan : ceVlans) {
            Object vid = ceVlan.getVid();
            if (!(vid instanceof Long)) {
                String errorMessage = String.format("vlan id {} cannot be cast to Long", vid);
                logger.error(errorMessage);
                throw new UnsupportedOperationException(errorMessage);
            }

            Long vlanId = (Long) vid;
            String interfaceName = NetvirtUtils.getInterfaceNameForVlan(parentInterfaceName, vlanId.toString());
            logger.info("Adding VLAN trunk-member {} ParentRef {}", interfaceName, parentInterfaceName);
            Interface trunkMemberInterface = NetvirtUtils.createTrunkMemberInterface(interfaceName, parentInterfaceName,
                    vlanId.intValue());
            NetvirtUtils.write(trunkMemberInterface, tx);
        }
    }

    private static void removeTrunkMemberInterfaces(String parentInterfaceName, Iterable<EvcUniCeVlan> ceVlans,
            WriteTransaction tx) {
        for (EvcUniCeVlan ceVlan : ceVlans) {
            Object vid = ceVlan.getVid();
            if (!(vid instanceof Long)) {
                String errorMessage = String.format("vlan id {} cannot be cast to Long", vid);
                logger.error(errorMessage);
                throw new UnsupportedOperationException(errorMessage);
            }

            Long vlanId = (Long) vid;
            String interfaceName = NetvirtUtils.getInterfaceNameForVlan(parentInterfaceName, vlanId.toString());
            logger.info("Removing VLAN trunk-member {}", interfaceName);
            NetvirtUtils.delete(interfaceName, tx);
        }
    }

    private static WriteTransaction createTransaction(DataBroker dataBroker) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        return tx;
    }

    private static void commitTransaction(WriteTransaction tx) {
        try {
            CheckedFuture<Void, TransactionCommitFailedException> futures = tx.submit();
            futures.get();
        } catch (Exception e) {
            logger.error("failed to commit transaction due to exception ", e);
        }
    }    

    private static Optional<List<EvcUniCeVlan>> getCeVlans(Uni uni) {
        EvcUniCeVlans ceVlans = uni.getEvcUniCeVlans();
        if (ceVlans == null) {
            return Optional.absent();
        }

        return Optional.fromNullable(ceVlans.getEvcUniCeVlan());
    }
}
