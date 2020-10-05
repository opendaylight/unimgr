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
        "(http://cisco.com/ns/yang/Cisco-IOS-XR-l2vpn-cfg?revision=";

    String CAPABILITY_IOX_IFMGR =
        "(http://cisco.com/ns/yang/Cisco-IOS-XR-ifmgr-cfg?revision=";

    String CAPABILITY_IOX_INFRA_POLICYMGR =
        "(http://cisco.com/ns/yang/Cisco-IOS-XR-infra-policymgr-cfg?revision=";

    String NETCONF_TOPOLODY_NAME = "topology-netconf";

    String XR_VERSION_SIX_ONE = "6.1";
    String XR_VERSION_SIX_THREE = "6.3";
    String XR_VERSION_SIX_FIVE = "6.5";
    String XR_VERSION_SIX_SIX = "6.6";
    
}
