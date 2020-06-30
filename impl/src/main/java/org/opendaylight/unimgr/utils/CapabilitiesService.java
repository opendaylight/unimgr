/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import java.util.function.BiFunction;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;


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

            for (Capability<Node> capability : capabilities) {
                boolean isSupporting = isSupporting(capability);
                result = (mode == Capability.Mode.AND ? result && isSupporting : result || isSupporting);

                if (result ^ (mode == Capability.Mode.AND)) {
                    break;
                }
            }

            return result;
        }
    }

    protected DataBroker dataBroker;

    public CapabilitiesService(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public NodeContext node(Node node) {
        return new NodeContext(this, Optional.of(node));
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD")
    private <T> boolean checkCondition(Capability<T> capability, T data) {
        return capability.getCondition().apply(dataBroker, data);
    }
}
