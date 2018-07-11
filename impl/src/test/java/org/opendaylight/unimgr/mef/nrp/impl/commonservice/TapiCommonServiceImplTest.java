/*
 * Copyright (c) 2018 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.impl.commonservice;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.unimgr.mef.nrp.impl.AbstractTestWithTopo;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.Sip1;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.Sip2;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.GetServiceInterfacePointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.GetServiceInterfacePointDetailsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.GetServiceInterfacePointDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.GetServiceInterfacePointListInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.GetServiceInterfacePointListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.get.service._interface.point.list.output.Sip;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * @author bartosz.michalik@amartus.com
 */
public class TapiCommonServiceImplTest extends AbstractTestWithTopo {

    private String uuid1 = "uuid1";
    private String uuid2 = "uuid2";

    private TapiCommonServiceImpl tapiCommonService;

    @Before
    public void setUp() throws Exception {
        tapiCommonService = new TapiCommonServiceImpl();
        tapiCommonService.setBroker(dataBroker);
        tapiCommonService.init();
    }
    @Test
    public void getServiceInterfacePointList() throws Exception {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, uuid1, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");
        tx.submit().checkedGet();

        RpcResult<GetServiceInterfacePointListOutput> output = tapiCommonService.getServiceInterfacePointList(
                new GetServiceInterfacePointListInputBuilder().build()).get();

        Assert.assertTrue(output.isSuccessful());

        List<Sip> sips = output.getResult().getSip();

        Assert.assertEquals(3, sips.size());
        Sip sip = output.getResult().getSip().get(1);

        Assert.assertNotNull(sip.augmentation(Sip2.class));
    }

    @Test
    public void getServiceInterfacePointDetails() throws Exception {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, uuid1, uuid1 + ":1", uuid1 + ":2", uuid1 + ":3");
        n(tx, uuid2, uuid2 + ":1", uuid2 + ":2", uuid2 + ":3");
        tx.submit().checkedGet();


        GetServiceInterfacePointDetailsInput input = new GetServiceInterfacePointDetailsInputBuilder().setSipIdOrName("sip:" + uuid2 + ":1").build();

        RpcResult<GetServiceInterfacePointDetailsOutput> output = tapiCommonService.getServiceInterfacePointDetails(input).get();

        Assert.assertTrue(output.isSuccessful());

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.get.service._interface.point.details.output.Sip sip = output.getResult().getSip();

        Assert.assertNotNull(sip.augmentation(Sip1.class));
    }

}