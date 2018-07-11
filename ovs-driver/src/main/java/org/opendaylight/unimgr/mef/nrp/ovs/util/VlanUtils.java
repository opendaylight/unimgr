/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.util;

import com.google.common.collect.Sets;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.unimgr.mef.nrp.common.ResourceNotAvailableException;
import org.opendaylight.unimgr.mef.nrp.ovs.exception.VlanPoolExhaustedException;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.common.types.rev180321.PositiveInteger;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.NodeSvmAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.context.topology.node.ServiceVlanMapBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.Node;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.NodeSvmAugmentation;
import org.opendaylight.yang.gen.v1.urn.odl.unimgr.yang.unimgr.ext.rev170531.context.topology.node.ServiceVlanMap;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class responsible for generate Vlan ID or check if given Vlan ID is not used.
 *
 * @author marek.ryznar@amartus.com
 */
public class VlanUtils {

    private final Set<Integer> usedVlans = new HashSet<>();
    private Node node;
    private final DataBroker dataBroker;

    private final static Set<Integer> possibleVlans = IntStream.range(1, 4094).boxed().collect(Collectors.toSet());
    private final static String VLAN_POOL_EXHAUSTED_ERROR_MESSAGE = "All VLAN IDs are in use. VLAN pool exhausted.";

    private final static Logger LOG = LoggerFactory.getLogger(VlanUtils.class);


    public VlanUtils(DataBroker dataBroker, String nodeId ) throws ResourceNotAvailableException {
        this.dataBroker = dataBroker;
        try {
            node = new NrpDao(dataBroker.newReadOnlyTransaction()).getNode(new Uuid(nodeId));
            if (node == null) throw new ResourceNotAvailableException(MessageFormat.format("Node {} not found", nodeId));
        } catch (ReadFailedException e) {
            LOG.warn("Node {} not found", nodeId);
            throw new ResourceNotAvailableException(MessageFormat.format("Node {} not found", nodeId));
        }

        node.augmentation(NodeSvmAugmentation.class).getServiceVlanMap().forEach(serviceVlanMap -> usedVlans.add(serviceVlanMap.getVlanId().getValue().intValue()));
    }

    /**
     * Method return vlan ID for service if stored in node property (service-vlan-map) or generate new one.
     * @param serviceName
     */
    public Integer getVlanID(String serviceName) throws ResourceNotAvailableException, TransactionCommitFailedException {
        Optional<ServiceVlanMap> o = node.augmentation(NodeSvmAugmentation.class).getServiceVlanMap().stream().filter(serviceVlanMap -> serviceVlanMap.getServiceId().equals(serviceName)).findFirst();
        return o.isPresent()? o.get().getVlanId().getValue().intValue() : generateVid(serviceName);
    }

    private Integer generateVid(String serviceName) throws VlanPoolExhaustedException, TransactionCommitFailedException {
        Set<Integer> difference = Sets.difference(possibleVlans, usedVlans);
        if (difference.isEmpty()) {
            LOG.warn(VLAN_POOL_EXHAUSTED_ERROR_MESSAGE);
            throw new VlanPoolExhaustedException(VLAN_POOL_EXHAUSTED_ERROR_MESSAGE);
        }
        return updateNodeNewServiceVLAN(serviceName,difference.iterator().next());
    }

    private Integer updateNodeNewServiceVLAN(String serviceName, Integer vlanId) {
        List<ServiceVlanMap> list = node.augmentation(NodeSvmAugmentation.class).getServiceVlanMap();
        list.add(new ServiceVlanMapBuilder().setServiceId(serviceName).setVlanId(PositiveInteger.getDefaultInstance(vlanId.toString())).build());
        node = new NodeBuilder(node).addAugmentation(NodeSvmAugmentation.class ,new NodeSvmAugmentationBuilder().setServiceVlanMap(list).build()).build();
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        new NrpDao(tx).updateNode(node);
        tx.submit();
        return vlanId;
    }

    public void releaseServiceVlan(String serviceName) {
        List<ServiceVlanMap> list = node.augmentation(NodeSvmAugmentation.class).getServiceVlanMap();
        list.removeIf(serviceVlanMap -> serviceVlanMap.getServiceId().equals(serviceName));
        node = new NodeBuilder(node).addAugmentation(NodeSvmAugmentation.class ,new NodeSvmAugmentationBuilder().setServiceVlanMap(list).build()).build();
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        new NrpDao(tx).updateNode(node);
        tx.submit();
    }
}
