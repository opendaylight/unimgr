/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.ext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.unimgr.mef.nrp.impl.AbstractTestWithTopo;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.common.types.rev180321.NaturalNumber;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.ServiceInterfacePoint1;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.AddSipInput;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.AddSipInputBuilder;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.AddSipOutput;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.add.sip.input.sip.type.EnniSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.add.sip.input.sip.type.InniSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.add.sip.input.sip.type.UniSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePoint;
import org.opendaylight.yangtools.yang.common.RpcResult;


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
        RpcResult<AddSipOutput> result = extService.addSip(input).get();
        assertFalse(result.isSuccessful());
    }

    @Test
    public void addSip() throws InterruptedException, ExecutionException {

        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, false, new Uuid(nodeId), "d:" + nodeId, nodeId + ":1", nodeId + ":2", nodeId + ":3");
        tx.commit().get();

        AddSipInput input = input(nodeId + ":1", SipType.enni);
        RpcResult<AddSipOutput> result = extService.addSip(input).get();
        assertTrue(result.isSuccessful());
        verifySipExists(nodeId + ":1", sip -> {
            ServiceInterfacePoint1 lpAug = sip.augmentation(ServiceInterfacePoint1.class);
            assertNotNull(lpAug);
            assertNotNull(lpAug.getNrpCarrierEthEnniNResource());
        });
    }

    @Test
    public void addSipFailBecauseItAlreadyExists()
            throws InterruptedException, ExecutionException {

        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, true, new Uuid(nodeId), "d:" + nodeId, nodeId + ":1", nodeId + ":2", nodeId + ":3");
        tx.commit().get();

        AddSipInput input = input(nodeId + ":1");
        RpcResult<AddSipOutput> result = extService.addSip(input).get();
        assertFalse(result.isSuccessful());
        verifySipExists(nodeId + ":1");
    }

    private void verifySipExists(String nepId, Consumer<ServiceInterfacePoint> verifySip)
            throws InterruptedException, ExecutionException {

        NrpDao nrpDao = new NrpDao(dataBroker.newReadOnlyTransaction());
        OwnedNodeEdgePoint nep = nrpDao.readNep(nodeId, nepId);
        boolean hasSip = nep.getMappedServiceInterfacePoint().get(0).getServiceInterfacePointId().getValue()
                .equals("sip:" + nepId);
        ServiceInterfacePoint sip = nrpDao.getSip("sip:" + nepId);
        assertTrue(hasSip && sip != null);
        if (verifySip != null) {
            verifySip.accept(sip);
        }
    }

    private void verifySipExists(String nepId) throws InterruptedException, ExecutionException {
        verifySipExists(nepId, null);
    }

    private AddSipInput input(String nepId) {
        return input(nepId, null);
    }

    enum SipType { enni, inni, uni }

    private AddSipInput input(String nepId, SipType type) {

        AddSipInputBuilder sipBuilder = new AddSipInputBuilder()
                .setNepId(new Uuid(nepId))
                .setNodeId(new Uuid(nodeId));

        if (type == null) {
            return sipBuilder.build();
        }

        switch (type) {
            case uni:
                sipBuilder.setSipType(
                    new UniSpecBuilder()
                    .setUniSpec(
                            new org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.add
                            .sip.input.sip.type.uni.spec.UniSpecBuilder().build())
                    .build());
                break;
            case enni:
                sipBuilder.setSipType(
                    new EnniSpecBuilder()
                        .setEnniSpec(
                                new org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.add
                                .sip.input.sip.type.enni.spec.EnniSpecBuilder()
                                        .setMaxFrameSize(new NaturalNumber(new Long(1000)))
                                        .build())
                    .build());
                break;
            case inni:
            default:
                sipBuilder.setSipType(
                    new InniSpecBuilder()
                    .setInniSpec(
                            new org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.add
                            .sip.input.sip.type.inni.spec.InniSpecBuilder().build())
                    .build());
        }

        return sipBuilder.build();
    }

}
