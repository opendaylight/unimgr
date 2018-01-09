/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.utils;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;


public class CapabilitiesService {

    public interface Capability<T> {
        enum Mode {
            AND,
            OR;
        }

        BiFunction<DataBroker, T, Boolean> getCondition();
    }

    public interface Context<T> {
        boolean isSupporting(Capability<T> capability);
    }

    public static class NodeContext implements Context<Node> {
        public enum NodeCapability implements Capability<Node> {
            NETCONF((dbBroker, node) -> node.getAugmentation(NetconfNode.class) != null),
            NETCONF_CISCO_IOX_L2VPN((dbBroker, node) ->
                    checkForNetconfCapability(node,NetconfConstants.CAPABILITY_IOX_L2VPN)),
            NETCONF_CISCO_IOX_IFMGR((dbBroker, node) ->
                    checkForNetconfCapability(node,NetconfConstants.CAPABILITY_IOX_IFMGR)),
            NETCONF_CISCO_IOX_POLICYMGR((dbBroker, node) ->
                    checkForNetconfCapability(node,NetconfConstants.CAPABILITY_IOX_ASR9K_POLICYMGR)),
            OVSDB((dbBroker,node) -> node.getAugmentation(OvsdbBridgeAugmentation.class) != null);

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

        private CapabilitiesService service;

        private Optional<Node> nodeOpt;

        NodeContext(CapabilitiesService service, Optional<Node> nodeOpt) {
            this.service = service;
            this.nodeOpt = nodeOpt;
        }

        public Optional<Node> getNode() {
            return nodeOpt;
        }

        public boolean isSupporting(Capability<Node> capability) {
            if (!nodeOpt.isPresent()) {
                return false;
            }

            return service.checkCondition(capability, nodeOpt.get());
        }

        public boolean isSupporting(Capability.Mode mode, Capability<Node>... capabilities) {
            boolean result = (mode == Capability.Mode.AND);

            for (Capability capability : capabilities) {
                boolean isSupporting = isSupporting(capability);
                result = (mode == Capability.Mode.AND ? result && isSupporting : result || isSupporting);

                if (result ^ (mode == Capability.Mode.AND)) {
                    break;
                }
            }

            return result;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(CapabilitiesService.class);

    private DataBroker dataBroker;

    public CapabilitiesService(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public NodeContext node(Node node) {
        return new NodeContext(this, Optional.of(node));
    }

    private <T> boolean checkCondition(Capability<T> capability, T data) {
        return capability.getCondition().apply(dataBroker, data);
    }
}
