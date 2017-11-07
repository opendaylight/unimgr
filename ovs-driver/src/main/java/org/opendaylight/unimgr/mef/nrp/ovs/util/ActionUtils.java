/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.util;

import java.util.LinkedList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetQueueActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.vlan.action._case.PopVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.vlan.action._case.PushVlanAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.vlan.action._case.PushVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.queue.action._case.SetQueueActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;

/**
 * Utility class providing common operations for Instruction and Action objects.
 *
 * @author jakub.niezgoda@amartus.com
 */

class ActionUtils {
    private static final int MAX_LENGTH = 65535;
    private static final int VLAN_TAG_ETHERNET_TYPE = 33024;

    private final static int NUMBER_OF_PORTNAME_PARTS = 3;
    private final static int INDEX_OF_PORT_NUMBER = 2;
    private final static String PORTNAME_PARTS_SEPARATOR = ":";

    private final static String INVALID_PORT_NAME_FORMAT_ERROR_MESSAGE = "Port name '%s' has invalid format.";

    static Instructions createInstructions(List<Action> actions) {
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();

        List<Instruction> instructions = new LinkedList<>();
        instructions.add(createInstruction(actions));

        return instructionsBuilder.setInstruction(instructions).build();
    }

    private static Instruction createInstruction(List<Action> actions) {
        ApplyActionsBuilder applyActionsBuilder = new ApplyActionsBuilder();
        applyActionsBuilder.setAction(actions);
        ApplyActionsCaseBuilder applyActionsCaseBuilder = new ApplyActionsCaseBuilder();
        applyActionsCaseBuilder.setApplyActions(applyActionsBuilder.build());

        InstructionBuilder instructionBuilder = new InstructionBuilder();
        instructionBuilder.setOrder(0);
        instructionBuilder.setInstruction(applyActionsCaseBuilder.build());

        return instructionBuilder.build();
    }

    static Action createPopVlanAction(int order) {
        ActionBuilder actionBuilder = new ActionBuilder();

        PopVlanActionCaseBuilder popVlanActionCaseBuilder = new PopVlanActionCaseBuilder();
        PopVlanActionBuilder popVlanActionBuilder = new PopVlanActionBuilder();
        popVlanActionCaseBuilder.setPopVlanAction(popVlanActionBuilder.build());

        actionBuilder.setOrder(order);
        actionBuilder.setAction(popVlanActionCaseBuilder.build());
        return actionBuilder.build();
    }

    static Action createDropAction(int order) {
        ActionBuilder actionBuilder = new ActionBuilder();

        DropActionCaseBuilder dropActionCaseBuilder = new DropActionCaseBuilder();
        dropActionCaseBuilder.setDropAction(new DropActionBuilder().build());

        actionBuilder.setOrder(order);
        actionBuilder.setAction(dropActionCaseBuilder.build());

        return actionBuilder.build();
    }


    static Action createOutputAction(String port, int order) {
        ActionBuilder actionBuilder = new ActionBuilder();

        OutputActionCaseBuilder outputActionCaseBuilder = new OutputActionCaseBuilder();
        OutputActionBuilder outputActionBuilder = new OutputActionBuilder();
        outputActionBuilder.setOutputNodeConnector(new Uri(getPortNumber(port)));
        outputActionBuilder.setMaxLength(MAX_LENGTH);
        outputActionCaseBuilder.setOutputAction(outputActionBuilder.build());

        actionBuilder.setOrder(order);
        actionBuilder.setAction(outputActionCaseBuilder.build());
        return actionBuilder.build();
    }


    static Action createSetVlanIdAction(int vlan, int order) {
        ActionBuilder actionBuilder = new ActionBuilder();

        VlanId vlanId = new VlanId(vlan);

        VlanIdBuilder vlanIdBuilder = new VlanIdBuilder();
        vlanIdBuilder.setVlanIdPresent(true);
        vlanIdBuilder.setVlanId(vlanId);

        VlanMatchBuilder vlanMatchBuilder = new VlanMatchBuilder();
        vlanMatchBuilder.setVlanId(vlanIdBuilder.build());

        SetFieldBuilder setFieldBuilder = new SetFieldBuilder();
        setFieldBuilder.setVlanMatch(vlanMatchBuilder.build());

        SetFieldCaseBuilder setFieldCaseBuilder = new SetFieldCaseBuilder();
        setFieldCaseBuilder.setSetField(setFieldBuilder.build());

        actionBuilder.setAction(setFieldCaseBuilder.build());
        actionBuilder.setOrder(order);

        return actionBuilder.build();
    }

    static Action createPushVlanAction(int order) {
        ActionBuilder actionBuilder = new ActionBuilder();
        actionBuilder.setOrder(order);

        PushVlanActionBuilder pushVlanActionBuilder = new PushVlanActionBuilder();
        pushVlanActionBuilder.setEthernetType(VLAN_TAG_ETHERNET_TYPE);
        PushVlanAction pushVlanAction = pushVlanActionBuilder.build();

        PushVlanActionCaseBuilder pushVlanActionCaseBuilder = new PushVlanActionCaseBuilder();
        pushVlanActionCaseBuilder.setPushVlanAction(pushVlanAction);

        actionBuilder.setAction(pushVlanActionCaseBuilder.build());

        return actionBuilder.build();
    }

    /**
     * Returns port number basing on openflow port name (e. 4 will be returned from 'openflow:3:4')
     *
     * @param portName string containing port name
     * @return port number
     */
    private static String getPortNumber(String portName) {
        String[] splittedPortName = portName.split(PORTNAME_PARTS_SEPARATOR);
        if (splittedPortName.length == NUMBER_OF_PORTNAME_PARTS) {
            return splittedPortName[INDEX_OF_PORT_NUMBER];
        } else {
            throw new IllegalArgumentException(String.format(INVALID_PORT_NAME_FORMAT_ERROR_MESSAGE, portName));
        }
    }

	static Action createSetQueueNumberAction(long queueNumber, int order) {
        ActionBuilder actionBuilder = new ActionBuilder();

        SetQueueActionBuilder setQueueActionBuilder = new SetQueueActionBuilder();
        setQueueActionBuilder.setQueueId(queueNumber);

        SetQueueActionCaseBuilder setQueueActionCaseBuilder = new SetQueueActionCaseBuilder();
        setQueueActionCaseBuilder.setSetQueueAction(setQueueActionBuilder.build());

        actionBuilder.setAction(setQueueActionCaseBuilder.build());
        actionBuilder.setOrder(order);
        return actionBuilder.build();
	}
}
