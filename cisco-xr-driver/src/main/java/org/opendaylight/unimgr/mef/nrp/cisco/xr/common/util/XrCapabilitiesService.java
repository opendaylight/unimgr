/*
 * Copyright (c) 2018 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.unimgr.utils.CapabilitiesService;
import org.opendaylight.unimgr.utils.NetconfConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import java.util.function.BiFunction;

/**
 * @author bartosz.michalik@amartus.com
 */
public class XrCapabilitiesService extends CapabilitiesService {
    public XrCapabilitiesService(DataBroker dataBroker) {
        super(dataBroker);
    }

    public enum NodeCapability implements Capability<Node> {
        NETCONF((dbBroker, node) -> node.getAugmentation(NetconfNode.class) != null),
        NETCONF_CISCO_IOX_L2VPN((dbBroker, node) ->
                checkForNetconfCapability(node, NetconfConstants.CAPABILITY_IOX_L2VPN)),
        NETCONF_CISCO_IOX_IFMGR((dbBroker, node) ->
                checkForNetconfCapability(node,NetconfConstants.CAPABILITY_IOX_IFMGR)),
        NETCONF_CISCO_IOX_POLICYMGR((dbBroker, node) ->
                checkForNetconfCapability(node,NetconfConstants.CAPABILITY_IOX_ASR9K_POLICYMGR));

        private BiFunction<DataBroker, Node, Boolean> condition;

        NodeCapability(BiFunction<DataBroker, Node, Boolean> condition) {
            this.condition = condition;
        }

        @Override
        public BiFunction<DataBroker, Node, Boolean> getCondition() {
            return condition;
        }

        private static boolean checkForNetconfCapability(Node node, String netconf_capability) {
            NetconfNode netconf = node.getAugmentation(NetconfNode.class);
            if (netconf == null) {
                return false;
            }
            if (netconf.getAvailableCapabilities() == null) {
                return false;
            }
            if (netconf.getAvailableCapabilities().getAvailableCapability() == null) {
                return false;
            }

            return netconf
                    .getAvailableCapabilities()
                    .getAvailableCapability()
                    .stream()
                    .anyMatch(capability -> capability.getCapability().equals(netconf_capability));
        }
    }
}
