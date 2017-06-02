/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.template.tapi;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.unimgr.mef.nrp.impl.AbstractTestWithTopo;
import org.opendaylight.unimgr.mef.nrp.template.TemplateConstants;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapitopology.rev170227.topology.context.Topology;

import static org.junit.Assert.*;

/**
 * A simple integration test to look at the handler
 * @author bartosz.michalik@amartus.com
 */
public class TopologyDataHandlerTest extends AbstractTestWithTopo {

    private TopologyDataHandler topologyDataHandler;

    @Before
    public void testSetup() {
        topologyDataHandler = new TopologyDataHandler(dataBroker);
    }

    @Test
    public void init() throws Exception {
        //having
        topologyDataHandler.init();

        //then
        ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction();
        Topology t = new NrpDao(tx).getTopology(TapiConstants.PRESTO_SYSTEM_TOPO);
        assertNotNull(t.getNode());
        assertTrue(t.getNode().stream().anyMatch(n -> n.getUuid().getValue().equals(TemplateConstants.DRIVER_ID)));

    }

}