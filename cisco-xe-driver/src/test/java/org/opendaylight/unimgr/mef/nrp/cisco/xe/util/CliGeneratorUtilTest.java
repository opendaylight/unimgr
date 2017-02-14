package org.opendaylight.unimgr.mef.nrp.cisco.xe.util;

/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class CliGeneratorUtilTest{

    @Test
    public void createServiceCommandsTest() {
        StringBuffer expectedResult = new StringBuffer();
        expectedResult
                .append("interface GigabitEthernet4\n")
                .append("mtu 1522\n")
                .append("no ip address\n")
                .append("no service instance 23 ethernet\n")
                .append("no shutdown\n")
                .append("service instance 23 ethernet\n")
                .append("encapsulation dot1q 100\n")
                .append("xconnect 3.3.3.3 3000 encapsulation mpls\n")
                .append("mtu 1508");
        try {
            Assert.assertEquals(expectedResult.toString(),CliGeneratorUtil.generateServiceCommands("GigabitEthernet4", (short) 23, (short) 100,"3.3.3.3", 3000).replaceAll("\r\n","\n"));
        } catch (IOException e) {
            Assert.fail(e.toString());
        }
    }

    @Test
    public void createNoServiceCommandsTest() {
        StringBuffer expectedResult = new StringBuffer();
        expectedResult
                .append("interface GigabitEthernet4\n")
                .append(" no service instance 23 ethernet");
        Assert.assertEquals(CliGeneratorUtil.generateNoServiceCommands("GigabitEthernet4", (short) 23),expectedResult.toString());
    }

}
