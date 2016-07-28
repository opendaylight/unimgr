/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.impl;

import org.opendaylight.yang.gen.v1.urn.mef.unimgr.ext.rev160725.ActivationStatus;
import org.opendaylight.yang.gen.v1.urn.mef.unimgr.ext.rev160725.ForwardingConstruct1;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.ForwardingConstructs;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.forwarding.constructs.ForwardingConstruct;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.forwarding.constructs.ForwardingConstructBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.forwarding.constructs.ForwardingConstructKey;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPortBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.junit.Assert;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author krzysztof.bijakowski@amartus.com
 */
class ForwardingConstructTestUtils {
    private static final ForwardingConstructKey fcKey = new ForwardingConstructKey("fc");

    static ForwardingConstructKey fcKey() {
        return fcKey;
    }

    static InstanceIdentifier<ForwardingConstruct> fcIid() {
        return InstanceIdentifier
                .builder(ForwardingConstructs.class)
                .child(ForwardingConstruct.class, fcKey)
                .build();
    }
    static ForwardingConstruct fcSingleNode() {
        return fc(
                port("a", "localhost", "80"),
                port("z", "localhost", "8080")
        );
    }

    static ForwardingConstruct fcTwoNodes() {
        return fc(
                port("a", "192.168.1.1", "80"),
                port("z", "192.168.1.2", "80")
        );
    }

    static void assertEquals(ForwardingConstruct expectedFc, ForwardingConstruct actualFc) {
        assertNotNull(expectedFc);
        assertNotNull(actualFc);

        assertNotNull(expectedFc.getFcPort());
        assertNotNull(actualFc.getFcPort());

        Set<FcPort> expectedFcPorts = new HashSet<>(expectedFc.getFcPort());
        Set<FcPort> actualFcPorts = new HashSet<>(actualFc.getFcPort());

        assertTrue(expectedFcPorts.size() == actualFcPorts.size());

        for (FcPort expectedFcPort : expectedFcPorts) {
            boolean equal = false;

            for (FcPort actualFcPort : actualFcPorts) {
                equal = compareFcPort(expectedFcPort, actualFcPort);

                if(equal) {
                    break;
                }
            }

            assertTrue(equal);
        }
        //TODO assertions for other parameters
    }

    static void assertActivationState(ForwardingConstruct fc, ActivationStatus expectedActivationStatus) {
        assertNotNull(fc.getAugmentation(ForwardingConstruct1.class));
        assertNotNull((fc.getAugmentation(ForwardingConstruct1.class).getUnimgrAttrs()));

        ActivationStatus actualActivationStatus = fc.getAugmentation(ForwardingConstruct1.class).getUnimgrAttrs().getStatus();
        assertNotNull(actualActivationStatus);

        Assert.assertEquals(expectedActivationStatus, actualActivationStatus);
    }

    private static boolean compareFcPort(FcPort expectedFcPort, FcPort actualFcPort) {
        assertNotNull(expectedFcPort);
        assertNotNull(actualFcPort);

        assertNotNull(expectedFcPort.getTopology());
        assertNotNull(expectedFcPort.getTopology().getValue());
        assertNotNull(actualFcPort.getTopology());
        assertNotNull(actualFcPort.getTopology().getValue());

        assertNotNull(expectedFcPort.getNode());
        assertNotNull(expectedFcPort.getNode().getValue());
        assertNotNull(actualFcPort.getNode());
        assertNotNull(actualFcPort.getNode().getValue());

        assertNotNull(expectedFcPort.getTp());
        assertNotNull(expectedFcPort.getTp().getValue());
        assertNotNull(actualFcPort.getTp());
        assertNotNull(actualFcPort.getTp().getValue());

        //TODO assertions for other parameters
        //TODO add possibility of null paramaters

        boolean result =
            expectedFcPort.getTopology().getValue().equals(actualFcPort.getTopology().getValue()) &&
            expectedFcPort.getNode().getValue().equals(actualFcPort.getNode().getValue()) &&
            expectedFcPort.getTp().getValue().equals(actualFcPort.getTp().getValue());

        return result;
    }

    private static ForwardingConstruct fc(FcPort... ports) {
        return new ForwardingConstructBuilder()
                .setFcPort(Arrays.asList(ports))
                .setKey(fcKey)
                .build();
    }

    private static FcPort port(String topo, String host, String port) {
        return new FcPortBuilder()
                .setTopology(new TopologyId(topo))
                .setNode(new NodeId(host))
                .setTp(new TpId(port))
                .build();
    }
}
