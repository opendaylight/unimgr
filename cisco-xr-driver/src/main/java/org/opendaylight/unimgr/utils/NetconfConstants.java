/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.utils;


public interface NetconfConstants {

    /*
     * The YANG models identified by these constants must match those
     * supported by the XR devices to which unimgr connects.
     */

    String NETCONF_TOPOLODY_NAME = "topology-netconf";

    /*
     * Cisco IOS XR 6.2.1

    String CAPABILITY_IOX_L2VPN =
        "(http://cisco.com/ns/yang/Cisco-IOS-XR-l2vpn-cfg?revision=2015-11-09)Cisco-IOS-XR-l2vpn-cfg";

    String CAPABILITY_IOX_IFMGR =
        "(http://cisco.com/ns/yang/Cisco-IOS-XR-ifmgr-cfg?revision=2015-07-30)Cisco-IOS-XR-ifmgr-cfg";

    String CAPABILITY_IOX_INFRA_POLICYMGR =
        "(http://cisco.com/ns/yang/Cisco-IOS-XR-infra-policymgr-cfg?revision=2016-12-15)Cisco-IOS-XR-infra-policymgr-cfg";
    */

    /*
     * Cisco IOS XR 6.4.1
     */
    String CAPABILITY_IOX_L2VPN =
        "(http://cisco.com/ns/yang/Cisco-IOS-XR-l2vpn-cfg?revision=2017-06-26)Cisco-IOS-XR-l2vpn-cfg";

    String CAPABILITY_IOX_IFMGR =
        "(http://cisco.com/ns/yang/Cisco-IOS-XR-ifmgr-cfg?revision=2017-09-07)Cisco-IOS-XR-ifmgr-cfg";

    String CAPABILITY_IOX_INFRA_POLICYMGR =
        "(http://cisco.com/ns/yang/Cisco-IOS-XR-infra-policymgr-cfg?revision=2017-12-12)Cisco-IOS-XR-infra-policymgr-cfg";

    /*
     * Cisco IOS XR 6.5.3

    String CAPABILITY_IOX_L2VPN =
        "(http://cisco.com/ns/yang/Cisco-IOS-XR-l2vpn-cfg?revision=2017-06-26)Cisco-IOS-XR-l2vpn-cfg";

    String CAPABILITY_IOX_IFMGR =
        "(http://cisco.com/ns/yang/Cisco-IOS-XR-ifmgr-cfg?revision=2017-09-07)Cisco-IOS-XR-ifmgr-cfg";

    String CAPABILITY_IOX_INFRA_POLICYMGR =
        "(http://cisco.com/ns/yang/Cisco-IOS-XR-infra-policymgr-cfg?revision=2018-09-30)Cisco-IOS-XR-infra-policymgr-cfg";
    */
}
