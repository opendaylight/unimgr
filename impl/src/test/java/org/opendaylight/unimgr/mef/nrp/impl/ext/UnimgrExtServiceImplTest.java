/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.ext;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.unimgr.mef.nrp.impl.AbstractTestWithTopo;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm_connectivity.rev170227.NaturalNumber;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp_interface.rev170227.LayerProtocol1;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.UniversalId;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.context.attrs.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapicommon.rev170227.service._interface.point.LayerProtocol;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170227.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev700101.AddSipInput;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev700101.AddSipInputBuilder;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev700101.add.sip.input.sip.type.UniSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev700101.add.sip.input.sip.type.EnniSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev700101.add.sip.input.sip.type.InniSpecBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

import java.math.BigInteger;
import java.util.function.Consumer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author bartosz.michalik@amartus.com
 */
public class UnimgrExtServiceImplTest extends AbstractTestWithTopo {
    private UnimgrExtServiceImpl extService;

    private String nodeId = "node-id";

    @Before
    public void setUp() {
        extService = new UnimgrExtServiceImpl(dataBroker);

    }

    @Test
    public void addSipNoNep() throws Exception {
        AddSipInput input = input("non-existing-nep");
        RpcResult<Void> result = extService.addSip(input).get();
        assertFalse(result.isSuccessful());
    }

    @Test
    public void addSip() throws Exception {

        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, false, nodeId, nodeId + ":1", nodeId + ":2", nodeId + ":3");
        tx.submit().checkedGet();

        AddSipInput input = input(nodeId + ":1", SipType.enni);
        RpcResult<Void> result = extService.addSip(input).get();
        assertTrue(result.isSuccessful());
        verifySipExists(nodeId + ":1", sip -> {
            LayerProtocol lp = sip.getLayerProtocol().get(0);
            LayerProtocol1 lpAug = lp.getAugmentation(LayerProtocol1.class);
            assertNotNull(lpAug);
            assertNotNull(lpAug.getNrpCgEthEnniSpec());
        });
    }



    @Test
    public void addSipFailBecauseItAlreadyExists() throws Exception {

        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, true, nodeId, nodeId + ":1", nodeId + ":2", nodeId + ":3");
        tx.submit().checkedGet();

        AddSipInput input = input(nodeId + ":1");
        RpcResult<Void> result = extService.addSip(input).get();
        assertFalse(result.isSuccessful());
        verifySipExists(nodeId + ":1");
    }

    private void verifySipExists(String nepId, Consumer<ServiceInterfacePoint> verifySip) throws ReadFailedException {

        NrpDao nrpDao = new NrpDao(dataBroker.newReadOnlyTransaction());
        OwnedNodeEdgePoint nep = nrpDao.readNep(nodeId, nepId);
        boolean hasSip = nep.getMappedServiceInterfacePoint().get(0).getValue().equals("sip:" + nepId);
        ServiceInterfacePoint sip = nrpDao.getSip("sip:" + nepId);
        assertTrue(hasSip && sip != null);
        if(verifySip != null) verifySip.accept(sip);

    }


    private void verifySipExists(String nepId) throws ReadFailedException {
        verifySipExists(nepId, null);
    }

    private AddSipInput input(String nepId) {
        return input(nepId, null);
    }

    enum SipType {enni, inni, uni}

    private AddSipInput input(String nepId, SipType type) {

        AddSipInputBuilder sipBuilder = new AddSipInputBuilder()
                .setNepId(new UniversalId(nepId))
                .setNodeId(new UniversalId(nodeId));

        if(type == null) return sipBuilder.build();

        switch (type) {
            case uni: sipBuilder.setSipType(
                    new UniSpecBuilder()
                        .setUniSpec(new org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev700101.add.sip.input.sip.type.uni.spec.UniSpecBuilder().build())
                    .build());
            break;
            case enni: sipBuilder.setSipType(
                    new EnniSpecBuilder()
                        .setEnniSpec(
                                new org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev700101.add.sip.input.sip.type.enni.spec.EnniSpecBuilder()
                                        .setMaxFrameSize(new NaturalNumber(new BigInteger("1000")))
                                        .build())
                    .build());
            break;
            case inni:
            default: sipBuilder.setSipType(
                    new InniSpecBuilder()
                    .setInniSpec(new org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev700101.add.sip.input.sip.type.inni.spec.InniSpecBuilder().build())
                    .build());
        }

        return sipBuilder.build();
    }

}