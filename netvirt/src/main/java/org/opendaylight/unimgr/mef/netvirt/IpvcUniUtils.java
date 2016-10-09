/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.interfaces.rev150526.mef.interfaces.unis.uni.physical.layers.links.Link;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.ipvc.unis.Uni;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.CheckedFuture;

public class IpvcUniUtils {

    private static final Logger logger = LoggerFactory.getLogger(IpvcUniUtils.class);

    public static void addUni(DataBroker dataBroker, Uni data) {
        try {
            String uniId = data.getIpUniId().getValue();
            WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
            Link link = EvcUniUtils.getLink(dataBroker, uniId);
            String interfaceName = uniId;
            addTrunkInterface(interfaceName, MefUtils.getTrunkParentName(link), tx);

            commitTransaction(tx);
        } catch (final Exception e) {
            logger.error("Add uni failed !", e);
        }
    }

    private static void addTrunkInterface(String interfaceName, String parentInterfaceName, WriteTransaction tx) {
        logger.info("Adding VLAN trunk {} ParentRef {}", interfaceName, parentInterfaceName);
        Interface trunkInterface = NetvirtUtils.createTrunkInterface(interfaceName, parentInterfaceName);
        NetvirtUtils.writeInterface(trunkInterface, tx);
    }

    private static void commitTransaction(WriteTransaction tx) {
        try {
            CheckedFuture<Void, TransactionCommitFailedException> futures = tx.submit();
            futures.get();
        } catch (Exception e) {
            logger.error("failed to commit transaction due to exception ", e);
        }
    }
}
