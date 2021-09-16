/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.utils;


public interface NetconfConstants {

    String CAPABILITY_IOX_L2VPN =
        "(http://cisco.com/ns/yang/Cisco-IOS-XR-l2vpn-cfg?revision=2018-06-15)Cisco-IOS-XR-l2vpn-cfg";

    String CAPABILITY_IOX_IFMGR =
        "(http://cisco.com/ns/yang/Cisco-IOS-XR-ifmgr-cfg?revision=2017-09-07)Cisco-IOS-XR-ifmgr-cfg";

    String CAPABILITY_IOX_INFRA_POLICYMGR =
        "(http://cisco.com/ns/yang/Cisco-IOS-XR-infra-policymgr-cfg?revision=2018-11-22)Cisco-IOS-XR-infra-policymgr-cfg";

    String NETCONF_TOPOLODY_NAME = "topology-netconf";
}
