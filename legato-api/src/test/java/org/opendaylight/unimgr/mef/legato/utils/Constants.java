/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.legato.utils;

/**
 * @author Arif.Hussain@Xoriant.Com
 *
 */
public class Constants {

    public static final String EVC_ID_TYPE = "EVC1";
    public static final String EVC_ID = "EVC_ID";
    public static final String EVC_UNI = "EVC_UNI_LIST";
    public static final String EVC_CON_TYPE = "POINTTOPOINT";
    public static final String SLS_PROFILES = "SLS_PROFILES";
    public static final String COSNAME = "EVPL";
    public static final String UNI_ID1 = "sip:ovs-node:s1:s1-eth1";
    public static final String UNI_ID2 = "sip:ovs-node:s2:s2-eth1";
    public static final String UNI_ID3 = "sip:ovs-node:s3:s3-eth1";
    public static final String UUID = "cs:162052f6bb1:73aaf0f5";
    public static final String READ_EVC = "readEvc";
    public static final String PARSE_NODES = "parseNodes";
    public static final String DELETE_NODES_FROM_OPERATIONAL = "deleteFromOperationalDB";
    public static final String UPDATE_CONNECTIVITY_INPUT = "buildUpdateConnectivityServiceInput";
    public static final String CREATE_CONNECTIVITY_INPUT = "buildCreateConnectivityServiceInput";
    public static final String NRP_CARRIER_ETH_CON_RESOURCE = "buildNrpCarrierEthConnectivityResource";
    public static final String CREATE_ENDPOINTS = "buildCreateEndpoints";
    public static final String NRP_CARRIER_ETH_CON_ENDPOINT_RESOURCE = "buildNrpCarrierEthConnectivityEndPointResource";
    public static final String CREATE_ETH_CON_ENDPOINT_AUGMENTATION = "buildCreateEthConnectivityEndPointAugmentation";
    public static final String UPDATE_ETH_CON_ENDPOINT_AUGMENTATION = "buildUpdateEthConnectivityEndPointAugmentation";
    public static final String CREATE_CON_SERVICE_AUGMENTATION = "buildCreateConServiceAugmentation";
    public static final String UPDATE_CON_SERVICE_AUGMENTATION = "buildUpdateConServiceAugmentation";
    public static final int MAXFRAME_SIZE_TYPE = 1522;
    public static final String ONE = "1";
    public static final String EPL = "epl";
    public static final String VLAN_ID = "301";
}
