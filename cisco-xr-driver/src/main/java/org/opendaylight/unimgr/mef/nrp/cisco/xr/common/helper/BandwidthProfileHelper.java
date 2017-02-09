/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.unimgr.utils.NullAwareDatastoreGetter;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.asr9k.policymgr.cfg.rev150518.PolicyManager;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.asr9k.policymgr.cfg.rev150518.PolicyManagerBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.asr9k.policymgr.cfg.rev150518.RateUnits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.asr9k.policymgr.cfg.rev150518.ThresholdUnits;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.asr9k.policymgr.cfg.rev150518.policy.manager.PolicyMapsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.asr9k.policymgr.cfg.rev150518.policy.manager.policy.maps.PolicyMap;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.asr9k.policymgr.cfg.rev150518.policy.manager.policy.maps.PolicyMapBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.asr9k.policymgr.cfg.rev150518.policy.map.rule.PolicyMapRule;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.asr9k.policymgr.cfg.rev150518.policy.map.rule.PolicyMapRuleBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.asr9k.policymgr.cfg.rev150518.policy.map.rule.policy.map.rule.Police;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.asr9k.policymgr.cfg.rev150518.policy.map.rule.policy.map.rule.PoliceBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.asr9k.policymgr.cfg.rev150518.policy.map.rule.policy.map.rule.police.*;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.bandwidth.profile.rev160630.GNRPBwpFlow;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.specs.rev160630.AdapterSpec1;
import org.opendaylight.yang.gen.v1.urn.mef.nrp.specs.rev160630.TerminationSpec1;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_layerprotocol.LpSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;


public class BandwidthProfileHelper {

    private static final Logger LOG = LoggerFactory.getLogger(BandwidthProfileHelper.class);

    private static String SEPARATOR  = "_";

    private static String CLASS_DEFAULT = "class-default";

    private static class PolicyMapNameGenerator {

        public static String generate(String fcName, BandwidthProfileComposition.BwpDirection direction, BandwidthProfileComposition.BwpApplicability applicability) {
            //TODO naming convention
            return new StringBuilder()
                    .append(fcName)
                    .append(SEPARATOR)
                    .append(direction.name().toLowerCase())
                    .append(SEPARATOR)
                    .append(applicability.name().toLowerCase())
                    .toString();
        }
    }

    private static List<BandwidthProfileComposition> retrieveBandwidthProfiles(DataBroker dataBroker, FcPort port) {
        List<BandwidthProfileComposition> bwCompositionList = new ArrayList<>();
        List<NullAwareDatastoreGetter<LpSpec>> lpSpecNadgs = new NullAwareDatastoreGetter<>(MdsalUtils.readTerminationPoint(dataBroker, CONFIGURATION, port))
                .collect(x -> x::getAugmentation, TerminationPoint1.class)
                .collect(x -> x::getLtpAttrs)
                .collectMany(x -> x::getLpList)
                .stream()
                .map(nadg -> nadg.collect(x -> x::getLpSpec))
                .collect(Collectors.toList());

        for(NullAwareDatastoreGetter<LpSpec> lpSpecNadg : lpSpecNadgs) {
            NullAwareDatastoreGetter<AdapterSpec1> adapterSpecNadg = lpSpecNadg
                    .collect(x -> x::getAdapterSpec)
                    .collect(x -> x::getAugmentation, AdapterSpec1.class);

            NullAwareDatastoreGetter<TerminationSpec1> terminationSpecNadg = lpSpecNadg
                    .collect(x -> x::getTerminationSpec)
                    .collect(x -> x::getAugmentation, TerminationSpec1.class);

            bwCompositionList.add(
                BandwidthProfileComposition.builder()
                        .defaultIngressBwProfile(adapterSpecNadg.collect(x -> x::getNrpConnAdaptSpecAttrs).collect(x -> x::getIngressBwpFlow).get())
                        .defaultEgressBwProfile(adapterSpecNadg.collect(x -> x::getNrpConnAdaptSpecAttrs).collect(x -> x::getEgressBwpFlow).get())
                        .ingressBwProfilePerEvc(adapterSpecNadg.collect(x -> x::getNrpEvcEndpointConnAdaptSpecAttrs).collect(x -> x::getIngressBwpFlow).get())
                        .egressBwProfilePerEvc(adapterSpecNadg.collect(x -> x::getNrpEvcEndpointConnAdaptSpecAttrs).collect(x -> x::getEgressBwpFlow).get())
                        .ingressBwProfilePerUni(terminationSpecNadg.collect(x -> x::getNrpUniTerminationAttrs).collect(x -> x::getIngressBwpUni).get())
                        .egressBwProfilePerUni(terminationSpecNadg.collect(x -> x::getNrpUniTerminationAttrs).collect(x -> x::getEgressBwpUni).get())
                        .build()
            );
        }

        return bwCompositionList;
    }

    private List<BandwidthProfileComposition> bandwidthProfiles;

    private List<PolicyMap> policyMaps;

    public BandwidthProfileHelper(DataBroker dataBroker, FcPort port) {
        bandwidthProfiles = BandwidthProfileHelper.retrieveBandwidthProfiles(dataBroker, port);
        policyMaps = new ArrayList<>();
    }

    public List<BandwidthProfileComposition> getBandwidthProfiles() {
        return bandwidthProfiles;
    }

    public boolean isQosEnabled() {
        for(BandwidthProfileComposition bandwidthProfileComposition : bandwidthProfiles) {
            if(bandwidthProfileComposition.hasAnyProfileDefined()) {
                return true;
            }
        }

        return false;
    }

    private Police addPolice(GNRPBwpFlow bandwidthProfile) {
        Long cir = bandwidthProfile.getCir().getValue();
        Long cbs = bandwidthProfile.getCbs().getValue();
        Long pir = bandwidthProfile.getEir().getValue() + cir;
        Long pbs = bandwidthProfile.getEbs().getValue() + cbs;

        return new PoliceBuilder()
                // CIR configuration
                .setRate(new RateBuilder().setUnits(new RateUnits("bps")).setValue(cir).build())

                // CBS configuration
                .setBurst(new BurstBuilder().setUnits(new ThresholdUnits("bytes")).setValue(cbs).build())

                // PIR configuration
                .setPeakRate(new PeakRateBuilder().setUnits(new RateUnits("bps")).setValue(pir).build())

                // PBS configuration
                .setPeakBurst(new PeakBurstBuilder().setUnits(new ThresholdUnits("bytes")).setValue(pbs).build())

                // GREEN-marked frames action configuration
                .setConformAction(new ConformActionBuilder().setTransmit(true).build())

                // YELLOW-marked frames action configuration
                .setViolateAction(new ViolateActionBuilder().setTransmit(true).build())

                // RED-marked frames action configuration
                .setExceedAction(new ExceedActionBuilder().setDrop(true).build())

                .build();
    }

    public BandwidthProfileHelper addPolicyMap(String fcName, BandwidthProfileComposition.BwpDirection direction, BandwidthProfileComposition.BwpApplicability applicability) {
        if(bandwidthProfiles.size() > 0) {
            //TODO .get(0) ?
            Optional<GNRPBwpFlow> bwProfileOptional = bandwidthProfiles.get(0).get(direction, applicability);

            if (bwProfileOptional.isPresent()) {
                List<PolicyMapRule> policyMapRules = new ArrayList<>();
                policyMapRules.add(
                        new PolicyMapRuleBuilder()
                                .setClassName(CLASS_DEFAULT)
                                .setPolice(addPolice(bwProfileOptional.get()))
                                .build()
                );


                policyMaps.add(new PolicyMapBuilder()
                        .setName(PolicyMapNameGenerator.generate(fcName, direction, applicability))
                        .setPolicyMapRule(policyMapRules)
                        .build()
                );

                return this;
            }
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