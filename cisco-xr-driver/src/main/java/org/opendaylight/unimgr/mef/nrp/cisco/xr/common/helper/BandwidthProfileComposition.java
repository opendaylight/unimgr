/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper;

import org.opendaylight.yang.gen.v1.urn.mef.nrp.bandwidth.profile.rev160630.GNRPBwpFlow;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.specs.rev160630.g_nrp_connadaptspec.EgressBwpFlow;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.specs.rev160630.g_nrp_connadaptspec.IngressBwpFlow;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.specs.rev160630.g_nrp_uni_terminationspec.EgressBwpUni;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.specs.rev160630.g_nrp_uni_terminationspec.IngressBwpUni;

import java.util.Optional;

public class BandwidthProfileComposition {

    public enum BwpDirection {
        INGRESS,
        EGRESS
    }

    public enum BwpApplicability {
        DEFAULT,
        EVC,
        UNI
    }

    public static BandwidthProfileCompositionBuilder builder() {
        return new BandwidthProfileCompositionBuilder();
    }

    private static Optional<GNRPBwpFlow> convert(Optional<? extends GNRPBwpFlow> input) {
        if(input.isPresent()) {
            return Optional.of(input.get());
        }

        return Optional.empty();
    }

    private Optional<IngressBwpFlow> ingressBwProfilePerEvc;

    private Optional<EgressBwpFlow> egressBwProfilePerEvc;

    private Optional<IngressBwpUni> ingressBwProfilePerUni;

    private Optional<EgressBwpUni> egressBwProfilePerUni;

    private Optional<IngressBwpFlow> defaultIngressBwProfile;

    private Optional<EgressBwpFlow> defaultEgressBwProfile;

    private BandwidthProfileComposition(BandwidthProfileCompositionBuilder builder) {
        this.ingressBwProfilePerEvc = builder.ingressBwProfilePerEvc;
        this.egressBwProfilePerEvc = builder.egressBwProfilePerEvc;
        this.ingressBwProfilePerUni = builder.ingressBwProfilePerUni;
        this.egressBwProfilePerUni = builder.egressBwProfilePerUni;
        this.defaultIngressBwProfile = builder.defaultIngressBwProfile;
        this.defaultEgressBwProfile = builder.defaultEgressBwProfile;
    }

    public Optional<IngressBwpFlow> getIngressBwProfilePerEvc() {
        return ingressBwProfilePerEvc;
    }

    public Optional<EgressBwpFlow> getEgressBwProfilePerEvc() {
        return egressBwProfilePerEvc;
    }

    public Optional<IngressBwpUni> getIngressBwProfilePerUni() {
        return ingressBwProfilePerUni;
    }

    public Optional<EgressBwpUni> getEgressBwProfilePerUni() {
        return egressBwProfilePerUni;
    }

    public Optional<IngressBwpFlow> getDefaultIngressBwProfile() {
        return defaultIngressBwProfile;
    }

    public Optional<EgressBwpFlow> getDefaultEgressBwProfile() {
        return defaultEgressBwProfile;
    }

    public Optional<GNRPBwpFlow> get(BwpDirection direction, BwpApplicability applicability) {
        switch(direction) {
            case INGRESS:
                switch(applicability) {
                    case DEFAULT:
                        return convert(defaultIngressBwProfile);
                    case EVC:
                        return convert(ingressBwProfilePerEvc);
                    case UNI:
                        return convert(ingressBwProfilePerUni);
                    default:
                        return Optional.empty();
                }
            case EGRESS:
                switch(applicability) {
                    case DEFAULT:
                        return convert(defaultEgressBwProfile);
                    case EVC:
                        return convert(egressBwProfilePerEvc);
                    case UNI:
                        return convert(egressBwProfilePerUni);
                    default:
                        return Optional.empty();
                }
            default:
                return Optional.empty();
        }
    }

    public boolean hasAnyProfileDefined() {
        return ingressBwProfilePerEvc.isPresent() ||
               egressBwProfilePerEvc.isPresent() ||
               ingressBwProfilePerUni.isPresent() ||
               egressBwProfilePerUni.isPresent() ||
               defaultIngressBwProfile.isPresent() ||
               defaultEgressBwProfile.isPresent();
    }

    public static class BandwidthProfileCompositionBuilder {
        private Optional<IngressBwpFlow> ingressBwProfilePerEvc;

        private Optional<EgressBwpFlow> egressBwProfilePerEvc;

        private Optional<IngressBwpUni> ingressBwProfilePerUni;

        private Optional<EgressBwpUni> egressBwProfilePerUni;

        private Optional<IngressBwpFlow> defaultIngressBwProfile;

        private Optional<EgressBwpFlow> defaultEgressBwProfile;

        private BandwidthProfileCompositionBuilder() {
            ingressBwProfilePerEvc = Optional.empty();
            egressBwProfilePerEvc = Optional.empty();
            ingressBwProfilePerUni = Optional.empty();
            egressBwProfilePerUni = Optional.empty();
            defaultIngressBwProfile = Optional.empty();
            defaultEgressBwProfile = Optional.empty();
        }

        public BandwidthProfileCompositionBuilder ingressBwProfilePerEvc(Optional<IngressBwpFlow> ingressBwProfilePerEvc) {
            this.ingressBwProfilePerEvc = ingressBwProfilePerEvc;
            return this;
        }

        public BandwidthProfileCompositionBuilder egressBwProfilePerEvc(Optional<EgressBwpFlow> egressBwProfilePerEvc) {
            this.egressBwProfilePerEvc = egressBwProfilePerEvc;
            return this;
        }

        public BandwidthProfileCompositionBuilder ingressBwProfilePerUni(Optional<IngressBwpUni> ingressBwProfilePerUni) {
            this.ingressBwProfilePerUni = ingressBwProfilePerUni;
            return this;
        }

        public BandwidthProfileCompositionBuilder egressBwProfilePerUni(Optional<EgressBwpUni> egressBwProfilePerUni) {
            this.egressBwProfilePerUni = egressBwProfilePerUni;
            return this;
        }

        public BandwidthProfileCompositionBuilder defaultIngressBwProfile(Optional<IngressBwpFlow> defaultIngressBwProfile) {
            this.defaultIngressBwProfile = defaultIngressBwProfile;
            return this;
        }

        public BandwidthProfileCompositionBuilder defaultEgressBwProfile(Optional<EgressBwpFlow> defaultEgressBwProfile) {
            this.defaultEgressBwProfile = defaultEgressBwProfile;
            return this;
        }

        public BandwidthProfileComposition build() {
            return new BandwidthProfileComposition(this);
        }
    }
}