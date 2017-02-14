package org.opendaylight.unimgr.mef.nrp.cisco.xe.util;

/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Random;
import java.util.Set;

public class CliGeneratorUtil {
    private static final Logger LOG = LoggerFactory.getLogger(CliGeneratorUtil.class);

    public static String generateServiceCommands(String interfaceName, short serviceInstanceId, short cTagVlanId, String ipAddressOfPeer, int vcIdValue) throws IOException {
        StringWriter writer = new StringWriter();

        MustacheFactory mf = new DefaultMustacheFactory(CliGeneratorUtil.class.getPackage().getName().replaceAll("\\.","/"));

        Mustache mustache = mf.compile("createServiceInstance.mustache");

        LOG.debug("Compiled mustache "+mustache);

        mustache.execute(writer, new ServiceInstance(interfaceName, serviceInstanceId, cTagVlanId,ipAddressOfPeer,vcIdValue)).flush();
        return writer.toString();
    }

    public static String generateNoServiceCommands(String interfaceName, short serviceInstanceId){
        StringBuilder sb = new StringBuilder();
        sb.append("interface ").
                append(interfaceName).
                append("\n no service instance ").
                append(serviceInstanceId).
                append(" ethernet");

        return sb.toString();
    }

    public static int generateVcId(Set<Integer> idsInUse){
        Random random = new Random();
        int vcId;
        do {
            vcId = Math.abs(random.nextInt())+1;  // <1-4294967295>  possible VC ID value in command line while MAX int is 2147483647 . +1 to avoid 0
        }while (idsInUse.contains(new Integer(vcId)));

        return vcId;
    }
    private static class ServiceInstance {
        ServiceInstance(String interfaceName, short serviceInstanceId, short cTagVlanId, String ipAddressOfPeer, int vcIdValue) {
            this.interfaceName = interfaceName;
            this.serviceInstanceId = serviceInstanceId;
            this.cTagVlanId = cTagVlanId;
            this.ipAddressOfPeer = ipAddressOfPeer;
            this.vcIdValue = vcIdValue;
        }

        String interfaceName, ipAddressOfPeer;
        short cTagVlanId, serviceInstanceId ;
        int vcIdValue;
    }
}
