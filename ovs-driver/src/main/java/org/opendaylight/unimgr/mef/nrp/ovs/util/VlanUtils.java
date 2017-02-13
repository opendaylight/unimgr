/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.util;

import com.google.common.collect.Sets;
import org.opendaylight.unimgr.mef.nrp.common.ResourceNotAvailableException;
import org.opendaylight.unimgr.mef.nrp.ovs.exception.VlanPoolExhaustedException;
import org.opendaylight.unimgr.utils.NullAwareDatastoreGetter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetField;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class responsible for generate Vlan ID or check if given Vlan ID is not used.
 *
 * @author marek.ryznar@amartus.com
 */
public class VlanUtils {
    private Set<Integer> usedVlans = new HashSet<>();

    private final static Set<Integer> possibleVlans = IntStream.range(1, 4094).boxed().collect(Collectors.toSet());
    private final static String VLAN_POOL_EXHAUSTED_ERROR_MESSAGE = "All VLAN IDs are in use. VLAN pool exhausted.";

    private final static Logger LOG = LoggerFactory.getLogger(VlanUtils.class);

    public VlanUtils(List<NullAwareDatastoreGetter<Node>> nodes) throws ResourceNotAvailableException {
        getAllVlanIDs(nodes);
    }

    /**
     * Method return given vlan ID (if it is not used in OVS network) or generate new one.
     */
    public Integer generateVlanID() throws ResourceNotAvailableException {
        return generateVid();
    }

    public boolean isVlanInUse(Integer vlanId) {
        return usedVlans.contains(vlanId);
    }

    private Integer generateVid() throws VlanPoolExhaustedException {
        Set<Integer> difference = Sets.difference(possibleVlans, usedVlans);
        if (difference.isEmpty()) {
            LOG.warn(VLAN_POOL_EXHAUSTED_ERROR_MESSAGE);
            throw new VlanPoolExhaustedException(VLAN_POOL_EXHAUSTED_ERROR_MESSAGE);
        }
        return difference.iterator().next();
    }

    private void getAllVlanIDs(List<NullAwareDatastoreGetter<Node>> nodes) throws ResourceNotAvailableException {
        for (NullAwareDatastoreGetter<Node> node : nodes) {
            if (node.get().isPresent()) {
                Table table = OpenFlowUtils.getTable(node.get().get());
                if (table != null && table.getFlow() != null) {
                    for (Flow flow : table.getFlow()) {
                        checkFlows(flow);
                    }
                }
            }
        }
    }

    private void checkFlows(Flow flow) {
        getVlanFromActions(flow);
        Integer vid = getVlanFromMatch(flow.getMatch().getVlanMatch());
        if (vid != null && !usedVlans.contains(vid)) {
            usedVlans.add(vid);
        }
    }

    private Integer getVlanFromMatch(VlanMatch vlanMatch) {
        if (vlanMatch != null &&
            vlanMatch.getVlanId() != null &&
            vlanMatch.getVlanId().getVlanId() != null)
        {
            return vlanMatch.getVlanId().getVlanId().getValue();
        }
        return null;
    }

    private void getVlanFromActions(Flow flow) {
        if (flow.getInstructions() != null &&
            !flow.getInstructions().getInstruction().isEmpty())
        {
            for(Instruction instruction : flow.getInstructions().getInstruction()) {
                ApplyActionsCase applyActionsCase = (ApplyActionsCase) instruction.getInstruction();
                ApplyActions applyActions = applyActionsCase.getApplyActions();
                List<Action> actions = applyActions.getAction();
                checkActions(actions);
            }
        }
    }

    private void checkActions(List<Action> actions) {
        for( Action action:actions ) {
            org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action concreteAction = action.getAction();
            if( concreteAction instanceof SetField ) {
                SetField setField = (SetField) concreteAction;
                if( setField.getVlanMatch() != null ) {
                    Integer vid = getVlanFromMatch(setField.getVlanMatch());
                    if( vid != null && !usedVlans.contains(vid)) {
                        usedVlans.add(vid);
                    }
                }
            }
        }
    }
}
