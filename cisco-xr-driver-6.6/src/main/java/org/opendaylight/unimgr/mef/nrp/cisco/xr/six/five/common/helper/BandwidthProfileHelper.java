/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.common.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.common.ServicePort;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev181122.PolicyManager;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev181122.PolicyManagerBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev181122.RateUnits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev181122.ThresholdUnits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev181122.policy.manager.PolicyMapsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev181122.policy.manager.policy.maps.PolicyMap;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev181122.policy.manager.policy.maps.PolicyMapBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev181122.policy.map.rule.PolicyMapRule;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev181122.policy.map.rule.PolicyMapRuleBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev181122.policy.map.rule.policy.map.rule.Police;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev181122.policy.map.rule.policy.map.rule.PoliceBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev181122.policy.map.rule.policy.map.rule.police.BurstBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev181122.policy.map.rule.policy.map.rule.police.ConformActionBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev181122.policy.map.rule.policy.map.rule.police.ExceedActionBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev181122.policy.map.rule.policy.map.rule.police.PeakBurstBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev181122.policy.map.rule.policy.map.rule.police.PeakRateBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev181122.policy.map.rule.policy.map.rule.police.RateBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.policymgr.cfg.rev181122.policy.map.rule.policy.map.rule.police.ViolateActionBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.common.rev180321.BwpFlow;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BandwidthProfileHelper {

    private static final Logger LOG = LoggerFactory.getLogger(BandwidthProfileHelper.class);

    private static String SEPARATOR  = "_";

    private static String CLASS_DEFAULT = "class-default";
    private final ServicePort port;

    private static class PolicyMapNameGenerator {

        static String generate(
                            String fcName,
                            BandwidthProfileComposition.BwpDirection direction,
                            BandwidthProfileComposition.BwpApplicability applicability) {
            //TODO naming convention
            return fcName
                    + SEPARATOR
                    + direction.name().toLowerCase(Locale.ROOT)
                    + SEPARATOR
                    + applicability.name().toLowerCase(Locale.ROOT);
        }
    }

    private List<PolicyMap> policyMaps;

    public BandwidthProfileHelper(ServicePort port) {
        policyMaps = new ArrayList<>();
        this.port =  port;
    }

    private Police addPolice(BwpFlow bwp) {
        assert bwp != null;
        Long cir = bwp.getCir().getValue();
        Long cbs = bwp.getCbs().getValue();
        Long pir = bwp.getEir().getValue() + cir;
        Long pbs = bwp.getCbs().getValue() + cbs;

        return new PoliceBuilder()
                // CIR configuration
                .setRate(new RateBuilder().setUnits(
                        new RateUnits("bps")).setValue(Uint32.valueOf(cir)).build())

                // CBS configuration
                .setBurst(new BurstBuilder().setUnits(
                        new ThresholdUnits("bytes")).setValue(Uint32.valueOf(cbs)).build())

                // PIR configuration
                .setPeakRate(new PeakRateBuilder().setUnits(
                        new RateUnits("bps")).setValue(Uint32.valueOf(pir)).build())

                // PBS configuration
                .setPeakBurst(new PeakBurstBuilder().setUnits(
                        new ThresholdUnits("bytes")).setValue(Uint32.valueOf(pbs)).build())

                // GREEN-marked frames action configuration
                .setConformAction(new ConformActionBuilder().setTransmit(Empty.getInstance()).build())

                // YELLOW-marked frames action configuration
                .setViolateAction(new ViolateActionBuilder().setTransmit(Empty.getInstance()).build())

                // RED-marked frames action configuration
                .setExceedAction(new ExceedActionBuilder().setDrop(Empty.getInstance()).build())

                .build();
    }

    public BandwidthProfileHelper addPolicyMap(
                                             String fcName,
                                             BandwidthProfileComposition.BwpDirection direction,
                                             BandwidthProfileComposition.BwpApplicability applicability) {
        if (BandwidthProfileComposition.BwpApplicability.UNI == applicability) {

            BwpFlow bwp = null;

            if (direction == BandwidthProfileComposition.BwpDirection.INGRESS) {
                bwp = port.getIngressBwpFlow();
            }

            if (direction == BandwidthProfileComposition.BwpDirection.EGRESS) {
                bwp = port.getEgressBwpFlow();
            }

            if (bwp == null) {
                return this;
            }

            PolicyMapRule rule = new PolicyMapRuleBuilder()
                    .setClassName(CLASS_DEFAULT)
                    .setPolice(addPolice(bwp))
                    .build();

            policyMaps.add(new PolicyMapBuilder()
                    .setName(PolicyMapNameGenerator.generate(fcName, direction, applicability))
                    .setPolicyMapRule(Collections.singletonList(rule))
                    .build()
            );

            return this;

        }

        LOG.warn("Cannot configure policy map - there are no Bandwidth Profiles defined.");
        return this;
    }

    public Optional<PolicyManager> build() {
        if (policyMaps.size() == 0) {
            return Optional.empty();
        }

        return Optional.of(new PolicyManagerBuilder()
                .setPolicyMaps(new PolicyMapsBuilder().setPolicyMap(policyMaps).build())
                .build()
        );
    }
}