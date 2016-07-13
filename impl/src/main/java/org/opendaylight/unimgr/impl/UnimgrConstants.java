/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.impl;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

public class UnimgrConstants {

    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));

    public static final TopologyId UNI_TOPOLOGY_ID = new TopologyId(new Uri("unimgr:uni"));

    public static final TopologyId EVC_TOPOLOGY_ID = new TopologyId(new Uri("unimgr:evc"));

    public static final String UNI_PREFIX = "uni://";

    public static final String OVSDB_PREFIX = "ovsdb://";

    public static final String EVC_PREFIX = "evc://";

    public static final Integer OVSDB_PORT = new Integer(6640);

    public static final Integer OPENFLOW_PORT = new Integer(6633);

    public static final Ipv4Address LOCAL_IP = new Ipv4Address("127.0.0.1");

    public static final String DEFAULT_BRIDGE_NAME = "ovsbr0";

//    public static final String DEFAULT_BRIDGE2_NAME = "br2";

    public static final String DEFAULT_BRIDGE_NODE_ID_SUFFIX = "/bridge/";

    public static final String DEFAULT_INTERNAL_IFACE = "eth1";

    public static final String DEFAULT_TUNNEL_IFACE = "eth1";

    public static final String DEFAULT_GRE_NAME = "gre";

    public static final String DEFAULT_GRE_TUNNEL_NAME = "gre1";

    public static final String QOS_DSCP_ATTRIBUTE = "dscp";

    public static final String QOS_DSCP_ATTRIBUTE_VALUE = "0";

    public static final String QOS_MAX_RATE = "max-rate";

    public static final String QOS_PREFIX = "qos://";

    public static final String QUEUE_PREFIX = "queue://";

    public static final long OVSDB_UPDATE_TIMEOUT = 1000;
}
