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

import java.util.HashSet;
import java.util.Set;

public class RunningConfigTest {
    @Test
    public void getUsedVcIdValuesTest() {
        Set<Integer> expected = new HashSet<Integer>();
        expected.add(3000);
        expected.add(3001);
        Assert.assertEquals(expected, config.getUsedVcIdValues());
    }

    @Test
    public void getIpAddressLoopback0Test(){
        try {
            Assert.assertEquals("1.1.1.1", config.getIpAddressLoopback0());
        } catch (IpAddressLoopbackNotFoundException e) {
            Assert.fail(e.toString());
        }
    }

    private RunningConfig config = new RunningConfig("Building configuration...\n" +
            "\n" +
            "Current configuration : 2482 bytes\n" +
            "!\n" +
            "! Last configuration change at 14:17:47 UTC Fri Oct 7 2016 by user1\n" +
            "!\n" +
            "version 15.4\n" +
            "service timestamps debug datetime msec\n" +
            "service timestamps log datetime msec\n" +
            "no platform punt-keepalive disable-kernel-core\n" +
            "platform console virtual\n" +
            "!\n" +
            "hostname testing\n" +
            "!\n" +
            "boot-start-marker\n" +
            "boot-end-marker\n" +
            "!\n" +
            "!\n" +
            "enable secret 5 _HASH_/\n" +
            "!\n" +
            "no aaa new-model\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "\n" +
            "\n" +
            "no ip domain lookup\n" +
            "ip domain name webpage.com\n" +
            "\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "subscriber templating\n" +
            "!\n" +
            "multilink bundle-name authenticated\n" +
            "!\n" +
            "!\n" +
            "license udi pid _pid_ sn _sn_\n" +
            "license boot level premium\n" +
            "spanning-tree extend system-id\n" +
            "!\n" +
            "username user1 privilege 15 secret 5 _HASH1_\n" +
            "username user2 privilege 15 secret 5 _HASH2_.\n" +
            "!\n" +
            "redundancy\n" +
            " mode none\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "cdp run\n" +
            "!\n" +
            "ip ssh version 2\n" +
            "no ip ssh server authenticate user keyboard\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "!\n" +
            "interface Loopback0\n" +
            " ip address 1.1.1.1 255.255.255.255\n" +
            "!\n" +
            "interface VirtualPortGroup0\n" +
            " ip unnumbered GigabitEthernet1\n" +
            " no mop enabled\n" +
            " no mop sysid\n" +
            "!\n" +
            "interface GigabitEthernet1\n" +
            " description <<< Management port - do not modify >>>\n" +
            " ip address dhcp\n" +
            " negotiation auto\n" +
            "!\n" +
            "interface GigabitEthernet2\n" +
            " description <<< MPLS network connection >>>\n" +
            " ip address 10.0.0.1 255.255.255.252\n" +
            " negotiation auto\n" +
            " mpls ip\n" +
            " cdp enable\n" +
            "!\n" +
            "interface GigabitEthernet3\n" +
            " no ip address\n" +
            " shutdown\n" +
            " negotiation auto\n" +
            "!\n" +
            "interface GigabitEthernet4\n" +
            " description Connection to Host\n" +
            " mtu 1522\n" +
            " no ip address\n" +
            " negotiation auto\n" +
            " service instance 1 ethernet\n" +
            " !\n" +
            " service instance 100 ethernet\n" +
            "  encapsulation dot1q 100\n" +
            "  xconnect 3.3.3.3 3000 encapsulation mpls\n" +
            "   mtu 1508\n" +
            " !\n" +
            " service instance 101 ethernet\n" +
            "  encapsulation dot1q 101\n" +
            "  xconnect 3.3.3.3 3001 encapsulation mpls\n" +
            "   mtu 1508\n" +
            " !\n" +
            "!\n" +
            "router ospf 100\n" +
            " network 1.1.1.1 0.0.0.0 area 0\n" +
            " network 10.0.0.0 0.0.0.3 area 0\n" +
            "!\n" +
            "!\n" +
            "virtual-service csr_mgmt\n" +
            " vnic gateway VirtualPortGroup0\n" +
            "  guest ip address 178.26.176.38\n" +
            " activate\n" +
            "!\n" +
            "ip forward-protocol nd\n" +
            "!\n" +
            "no ip http server\n" +
            "no ip http secure-server\n" +
            "ip route 178.26.176.38 255.255.255.255 VirtualPortGroup0\n" +
            "!\n" +
            "!\n" +
            "snmp-server community user1 RO\n" +
            "!\n" +
            "!\n" +
            "control-plane\n" +
            "!\n" +
            "!\n" +
            "line con 0\n" +
            " stopbits 1\n" +
            "line vty 0 4\n" +
            " exec-timeout 30 0\n" +
            " privilege level 15\n" +
            " login local\n" +
            " transport preferred ssh\n" +
            " transport input telnet ssh\n" +
            " transport output none\n" +
            "line vty 5 10\n" +
            " exec-timeout 30 0\n" +
            " privilege level 15\n" +
            " login local\n" +
            " transport preferred ssh\n" +
            " transport input telnet ssh\n" +
            " transport output none\n" +
            "line vty 11 98\n" +
            " login\n" +
            " transport input telnet ssh\n" +
            "!\n" +
            "netconf ssh\n" +
            "!\n" +
            "end\n");
}