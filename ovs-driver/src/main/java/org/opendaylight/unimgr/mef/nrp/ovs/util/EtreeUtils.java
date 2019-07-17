/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under 
 * the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.util;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.unimgr.mef.nrp.api.FailureResult;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.unimgr.mef.nrp.common.ResourceNotAvailableException;
import org.opendaylight.unimgr.mef.nrp.ovs.exception.VlanPoolExhaustedException;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.Sets;

/**
 * @author Arif.Hussain@Xoriant.Com
 *
 */
public class EtreeUtils {

    private static final Set<Integer> usedVlans = new HashSet<>();
    private static final Set<Integer> usedEpTreeVlans = new HashSet<>();
    private static final Map<Integer, String> usedEpTreeService = new Hashtable<Integer, String>();
    private static final Map<String, Integer> usedService = new Hashtable<String, Integer>();
    private static final Map<Integer, String> prePopMap = new Hashtable<Integer, String>();
    private static final Set<Integer> possibleRootVlans =
            IntStream.range(2048, 4094).boxed().collect(Collectors.toSet());
    private static Set<Integer> cpeRange = null;
    private static Set<Integer> speRange = null;
    private static final String VLAN_POOL_EXHAUSTED_ERROR_MESSAGE =
            "All VLAN IDs are in use. VLAN pool exhausted.";
    private ConnectivityService value;
    private static final Logger LOG = LoggerFactory.getLogger(EtreeUtils.class);


    /**
     * Method return vlan ID for e-tree service. If service is unique generate new one otherwise
     * return same vlan-ID belong to service .
     * 
     * @param serviceName
     */
    public Integer getVlanID(String serviceName)
            throws ResourceNotAvailableException {
        Optional<Integer> o = usedService.entrySet().stream()
                .filter(e -> e.getKey().equals(serviceName)).map(Map.Entry::getValue).findFirst();

        return o.isPresent() ? o.get().intValue() : generateVid(serviceName);
    }

    private Integer generateVid(String serviceName) throws VlanPoolExhaustedException {
        Set<Integer> difference = Sets.difference(possibleRootVlans, usedVlans);
        if (difference.isEmpty()) {
            LOG.warn(VLAN_POOL_EXHAUSTED_ERROR_MESSAGE);
            throw new VlanPoolExhaustedException(VLAN_POOL_EXHAUSTED_ERROR_MESSAGE);
        }

        return updateNodeNewServiceVLAN(serviceName, difference.iterator().next());
    }

    private Integer updateNodeNewServiceVLAN(String serviceName, Integer vlanId) {
        usedService.put(serviceName, vlanId);
        usedVlans.add(vlanId);

        return vlanId;
    }

    public void releaseTreeServiceVlan(String serviceName) {
        usedVlans.remove(usedService.entrySet().stream().filter(e -> e.getKey().equals(serviceName))
                .map(Map.Entry::getValue).findFirst().get());
        usedService.entrySet()
                .removeIf(serviceVlanMap -> serviceVlanMap.getKey().equals(serviceName));
    }

    /**
     * Method return cpe vlans range for e-tree service. Every service will keep same range .
     * 
     * @return spe range e.g- 1-100
     */
    public static Set<Integer> cpeVlanRange() {
        LOG.info("Avaliable cpe range ... ");
        List<Integer> range =
                IntStream.rangeClosed(OVSConstant.CPESTARTINCLUSIVE, OVSConstant.CPEENDEXCLUSIVE)
                        .boxed().collect(Collectors.toList());
        cpeRange = new TreeSet<Integer>(range);
        return cpeRange;
    }

    /**
     * Method return spe vlans range for e-tree service. If service is unique generate new one spe
     * range otherwise return same .
     * 
     * @param vlan
     * @return spe range e.g- 1-100
     */
    public static Set<Integer> speVlanRange(int vlan) {
        int[] speRG = isAvailableVlan(vlan);
        LOG.info("Avaliable spe range ... ");
        List<Integer> range =
                IntStream.rangeClosed(speRG[0], speRG[1]).boxed().collect(Collectors.toList());
        speRange = new TreeSet<Integer>(range);

        return speRange;
    }

    /**
     * Method will generate vlans map [e.g (1, 1000-1019)] by available cpe .
     */
    public static void generatePrePopVlan() {
        assert cpeRange != null;

        int count = (int) cpeRange.stream().count();
        int start = Integer.sum(OVSConstant.SPEDEFAULTVAL, count);
        int end = Integer.sum(start, count - 1);

        for (int i = 1; i < 4094; i++) {
            prePopMap.put(i, (start) + "-" + end);
            start = Integer.sum(start, count);
            end = Integer.sum(end, count);
            if (start > 4094 || end > 4094) {
                break;
            }
        }
    }


    /**
     * Method will pick available spe range by specific vlan and split into array .
     * 
     * @param vlan
     * @return int array
     */
    public static int[] isAvailableVlan(int vlan) {
        String rg[] = null;
        Optional<String> firstKey =
                prePopMap.entrySet().stream().filter(entry -> Objects.equals(entry.getKey(), vlan))
                        .map(Map.Entry::getValue).findFirst();

        if (firstKey.isPresent()) {
            LOG.info("SPE map generated successfully for ep-tree. ");
            rg = firstKey.get().split("-");
        }

        return Stream.of(rg).mapToInt(Integer::parseInt).toArray();
    }


    /**
     * Method return unique vlanID for ep-tree service.
     * 
     * @param serviceName
     * @param type
     */
    public Integer getEpVlanID(String serviceName, String type)
            throws ResourceNotAvailableException {
        Set<Integer> difference = null;

        if (type.equalsIgnoreCase(OVSConstant.CPETYPE)) {
            difference = Sets.difference(cpeRange, usedEpTreeVlans);
        } else {
            difference = Sets.difference(speRange, usedEpTreeVlans);
        }

        if (difference.isEmpty()) {
            LOG.warn(VLAN_POOL_EXHAUSTED_ERROR_MESSAGE);
            throw new VlanPoolExhaustedException(VLAN_POOL_EXHAUSTED_ERROR_MESSAGE);
        }

        return updateEpNodeNewServiceVLAN(serviceName, difference.iterator().next());
    }

    private Integer updateEpNodeNewServiceVLAN(String serviceName, Integer vlanId) {
        usedEpTreeService.put(vlanId, serviceName);
        usedEpTreeVlans.add(vlanId);

        return vlanId;
    }

    /**
     * Method release all used vlanID for ep-tree service.
     */
    public void releaseEpTreeServiceVlan(String serviceName) {
        usedEpTreeVlans.removeAll(
                usedEpTreeService.entrySet().stream().filter(e -> e.getValue().equals(serviceName))
                        .map(Map.Entry::getKey).collect(Collectors.toSet()));
        usedEpTreeService.entrySet()
                .removeIf(serviceVlanMap -> serviceVlanMap.getValue().equals(serviceName));
    }

    /**
     * Method fetch service type (tagged or port) based on the nodeId and return true or false in
     * IsExclusive flag .
     * 
     * @param dataBroker
     * @param nodeId
     * @return boolean true/false
     * @throws FailureResult
     */
    public boolean getServiceType(DataBroker dataBroker, String nodeId) throws FailureResult {
        value = new NrpDao(dataBroker.newReadOnlyTransaction())
                .getConnectivityService(new Uuid(nodeId));

        if (value == null) {
            throw new FailureResult("There is no service with id {0}", nodeId);
        }

        return value.isIsExclusive();
    }

}
