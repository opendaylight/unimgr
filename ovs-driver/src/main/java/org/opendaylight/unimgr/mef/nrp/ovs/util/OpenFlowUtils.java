/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.common.ResourceNotAvailableException;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class responsible for managing OpenFlow rules in OVS.
 *
 * @author marek.ryznar@amartus.com
 */
public class OpenFlowUtils {
    private static final Logger LOG = LoggerFactory.getLogger(OpenFlowUtils.class);
    private static final String INTERSWITCH_FLOW_ID_PREFIX = "interswitch";
    private static final String FLOW_ID_MIDDLE_PART = "vlan";
    private static final String DROP_FLOW_ID = "default-DROP";
    private static final Short FLOW_TABLE_ID = 0;
    private static final int VLAN_FLOW_PRIORITY = 20;
    private static final int INTERSWITCH_FLOW_PRIORITY = 10;
    private static final int EMPTY_VLAN_FLOW_PRIORITY = 20;
    private static final int DROP_FLOW_PRIORITY = 0;
    private static final long ETH_TYPE_LLDP = 35020;

    private static final String FLOW_TABLE_NOT_PRESENT_ERROR_MESSAGE = "Flow table is not present in node '%s'.";
    private static final String NODE_NOT_AUGMENTED_ERROR_MESSAGE = "Node '%s' does not have '%s' augmentation.";

    /**
     * Checks if flow table contains base flows.
     *
     * @param table - openflow node's table
     * @return true if table is preconfigured, false in other case
     */
    public static boolean isTablePreconfigured(Table table) {
        boolean isTablePreconfigured = false;

        for (Flow flow : table.getFlow()) {
            if (flow.getId().getValue().startsWith(INTERSWITCH_FLOW_ID_PREFIX)) {
                isTablePreconfigured = true;
                break;
            }
        }

        LOG.debug("Table is " + (isTablePreconfigured ? "" : "not") + " preconfigured with default flows.");
        return isTablePreconfigured;
    }

    /**
     * Returns flow table for the specified node.
     *
     * @param node openflow node
     * @return flow table
     * @throws ResourceNotAvailableException if node is not augmented
     *          with FlowCapableNode class or flow table is not present in node
     */
    public static Table getTable(Node node) throws ResourceNotAvailableException {
        String nodeId = node.getId().getValue();
        FlowCapableNode flowCapableNode = node.augmentation(FlowCapableNode.class);
        if (flowCapableNode == null) {
            LOG.warn(String.format(NODE_NOT_AUGMENTED_ERROR_MESSAGE, nodeId, FlowCapableNode.class.toString()));
            throw new ResourceNotAvailableException(String
                    .format(NODE_NOT_AUGMENTED_ERROR_MESSAGE, nodeId, FlowCapableNode.class.toString()));
        }

        Optional<Table> flowTable = flowCapableNode.getTable()
                                                   .stream()
                                                   .filter(table -> table.getId().equals(FLOW_TABLE_ID))
                                                   .findFirst();
        if (!flowTable.isPresent()) {
            LOG.warn(String.format(FLOW_TABLE_NOT_PRESENT_ERROR_MESSAGE, nodeId));
            throw new ResourceNotAvailableException(String.format(FLOW_TABLE_NOT_PRESENT_ERROR_MESSAGE, nodeId));
        }

        return flowTable.get();
    }

    /**
     * Returns list of flows for passing traffic using decorated S-VLAN ID via qos queue number.
     *
     * @param servicePort port on which service is activated (format: openflow:[node]:[port])
     * @param internalVlanId VLAN ID used internally in OvSwitch network
     * @param externalVlanId VLAN ID for VLAN aware services. If -1 then ignored and port-base service created.
     * @param interswitchLinks list of interswitch links for the node on which service is activated
     * @param serviceName service name (used as prefix for flow IDs)
     * @param queueNumber qos queue number
     * @return list of flows
     */
    public static List<Flow> getVlanFlows(String servicePort, int internalVlanId, Optional<Integer> externalVlanId,
                                          List<Link> interswitchLinks, String serviceName, long queueNumber) {
        List<Flow> flows = new ArrayList<>();
        flows.addAll(createVlanPassingFlows(servicePort, internalVlanId, externalVlanId,
                serviceName, interswitchLinks));
        flows.add(createVlanIngressFlow(servicePort, internalVlanId, externalVlanId,
                serviceName, interswitchLinks, queueNumber));

        return flows;
    }

    /**
     * Method is used for tagged (EVP) base tree service .
     * 
     * @param role differentiate coming endpoint is root or leaf
     * @param servicePort port on which service is activated (format: openflow:[node]:[port])
     * @param internalVlanId VLAN ID used internally in OvSwitch network
     * @param externalVlanId VLAN ID for VLAN aware services. If -1 then ignored and port-base
     *        service created.
     * @param interswitchLinks list of interswitch links for the node on which service is activated
     * @param serviceName service name (used as prefix for flow IDs)
     * @param queueNumber qos queue number
     * @param rootCount shows number of root
     * @param eTreeUtils is utility object
     * @return list of flows
     * @throws ResourceNotAvailableException if node is not augmented with FlowCapableNode class or
     *         flow table is not present in node
     * @throws TransactionCommitFailedException is raised and returned when transaction commit
     *         failed
     */
    public static List<Flow> getEvpTree(String role, String servicePort, int internalVlanId,
            Optional<Integer> externalVlanId, List<Link> interswitchLinks, String serviceName,
            long queueNumber, long rootCount, EtreeUtils eTreeUtils)
            throws ResourceNotAvailableException, TransactionCommitFailedException {
        List<Flow> flows = new ArrayList<>();

        if (rootCount > 1) {
            // Case: More then one Roots
            if (role.equalsIgnoreCase(PortRole.ROOT.getName())) {
                flows.addAll(createVlanPassingFlows(servicePort, internalVlanId, externalVlanId,
                        serviceName, interswitchLinks));
                flows.addAll(createVlanPassingFlows(servicePort, eTreeUtils.getVlanID(serviceName),
                        externalVlanId, serviceName, interswitchLinks));
                flows.add(createVlanIngressFlow(servicePort, eTreeUtils.getVlanID(serviceName),
                        externalVlanId, serviceName, interswitchLinks, queueNumber));
            } else {
                flows.addAll(createVlanPassingFlows(servicePort, eTreeUtils.getVlanID(serviceName),
                        externalVlanId, serviceName, interswitchLinks));
                flows.add(createVlanIngressFlow(servicePort, internalVlanId, externalVlanId,
                        serviceName, interswitchLinks, queueNumber));
            }
        } else {
            // Case: Only one Root and more then one Leaf
            if (role.equalsIgnoreCase(PortRole.ROOT.getName())) {
                flows.addAll(createVlanPassingFlows(servicePort, internalVlanId, externalVlanId,
                        serviceName, interswitchLinks));
                flows.add(createVlanIngressFlow(servicePort, eTreeUtils.getVlanID(serviceName),
                        externalVlanId, serviceName, interswitchLinks, queueNumber));
            } else {
                flows.addAll(createVlanPassingFlows(servicePort, eTreeUtils.getVlanID(serviceName),
                        externalVlanId, serviceName, interswitchLinks));
                flows.add(createVlanIngressFlow(servicePort, internalVlanId, externalVlanId,
                        serviceName, interswitchLinks, queueNumber));
            }
        }

        return flows;
    }


    /**
     * Method is used for port base (EP) tree service .
     * 
     * @param role differentiate coming endpoint is root or leaf
     * @param servicePort port on which service is activated (format: openflow:[node]:[port])
     * @param internalVlanId VLAN ID used internally in OvSwitch network
     * @param externalVlanId VLAN ID for VLAN aware services. If -1 then ignored and port-base
     *        service created.
     * @param interswitchLinks list of interswitch links for the node on which service is activated
     * @param serviceName service name (used as prefix for flow IDs)
     * @param queueNumber qos queue number
     * @param rootCount shows number of root
     * @param eTreeUtils is utility object
     * @param isExclusive show requested service is port or tag based
     * @return list of flows
     * @throws ResourceNotAvailableException if node is not augmented with FlowCapableNode class or
     *         flow table is not present in node
     * @throws TransactionCommitFailedException is raised and returned when transaction commit
     *         failed
     */
    public static List<Flow> getEpTree(String role, String servicePort, int internalVlanId,
            Optional<Integer> externalVlanId, List<Link> interswitchLinks, String serviceName,
            long queueNumber, long rootCount, EtreeUtils eTreeUtils, boolean isExclusive)
            throws ResourceNotAvailableException, TransactionCommitFailedException {
        List<Flow> flows = new ArrayList<>();

        EtreeUtils.cpeVlanRange();
        EtreeUtils.generatePrePopVlan();
        EtreeUtils.speVlanRange(internalVlanId);
        defaultFlows(role, flows, servicePort, internalVlanId, externalVlanId, interswitchLinks,
                serviceName, queueNumber, rootCount, eTreeUtils, isExclusive);
        unTaggedFlows(role, flows, servicePort, internalVlanId, interswitchLinks, serviceName,
                queueNumber, rootCount, eTreeUtils, isExclusive);

        return flows;
    }

    /**
     * @param role differentiate coming endpoint is root or leaf
     * @param flows object
     * @param servicePort port on which service is activated (format: openflow:[node]:[port])
     * @param internalVlanId VLAN ID used internally in OvSwitch network
     * @param externalVlanId VLAN ID for VLAN aware services. If -1 then ignored and port-base
     *        service created.
     * @param interswitchLinks list of interswitch links for the node on which service is activated
     * @param serviceName service name (used as prefix for flow IDs)
     * @param queueNumber qos queue number
     * @param rootCount shows number of root
     * @param eTreeUtils is utility object
     * @param isExclusive show requested service is port or tag based
     * @return list of flows
     */
    private static List<Flow> defaultFlows(String role, List<Flow> flows, String servicePort,
            int internalVlanId, Optional<Integer> externalVlanId, List<Link> interswitchLinks,
            String serviceName, long queueNumber, long rootCount, EtreeUtils eTreeUtils,
            boolean isExclusive) {

    	 if (rootCount > 1) {
             if (role.equalsIgnoreCase(PortRole.ROOT.getName())) {
                 flows.addAll(createVlanPassingFlows(servicePort, internalVlanId, externalVlanId,
                         serviceName, interswitchLinks));
                 flows.addAll(createVlanPassingFlows(servicePort, OVSConstant.SPEDEFAULTVAL,
                         externalVlanId, serviceName, interswitchLinks));
                 flows.add(createVlanIngressFlow(servicePort, internalVlanId, externalVlanId,
                         serviceName, interswitchLinks, queueNumber));
             } else {
                 flows.addAll(createVlanPassingFlows(servicePort, internalVlanId, externalVlanId,
                         serviceName, interswitchLinks));
                 flows.add(createVlanIngressFlow(servicePort, OVSConstant.SPEDEFAULTVAL,
                         externalVlanId, serviceName, interswitchLinks, queueNumber));
             }
         } else {
             if (role.equalsIgnoreCase(PortRole.ROOT.getName())) {
                 flows.addAll(createVlanPassingFlows(servicePort, internalVlanId, externalVlanId,
                         serviceName, interswitchLinks));
                 flows.addAll(createIngressFlow(servicePort, serviceName, interswitchLinks));
             } else {
                 flows.addAll(createPassingFlows(servicePort, serviceName, interswitchLinks));
                 flows.add(createVlanIngressFlow(servicePort, internalVlanId, externalVlanId,
                         serviceName, interswitchLinks, queueNumber));
             }
         }
        return flows;
    }

    private static List<Flow> createPassingFlows(String outputPort, String serviceName,
            List<Link> interswitchLinks) {
        return interswitchLinks.stream().map(
                link -> createPassingFlow(outputPort, link.getLinkId().getValue(), serviceName))
                .collect(Collectors.toList());
    }

    private static Flow createPassingFlow(String outputPort, String inputPort, String serviceName) {
        LOG.info(" inside createPassingFlow () outputPort{}, inputPort{} ", outputPort, inputPort);
        List<Action> actions = new ArrayList<>();
        int actionOrder = 0;

        // actions.add(ActionUtils.createPopVlanAction(actionOrder++));

        actions.add(ActionUtils.createOutputAction(outputPort, actionOrder));

        FlowId flowId = new FlowId(getVlanFlowId(serviceName, inputPort, 0));
      // return new FlowBuilder().setId(flowId).setKey(new FlowKey(flowId)).setTableId(FLOW_TABLE_ID)
        return new FlowBuilder().setId(flowId).setTableId(FLOW_TABLE_ID)
                .setPriority(EMPTY_VLAN_FLOW_PRIORITY)
                .setMatch(MatchUtils.createWithoutVlanMatch(inputPort))
                .setInstructions(ActionUtils.createInstructions(actions)).build();
    }

    /**
     * @param servicePort servicePort port on which service is activated (format:
     *        openflow:[node]:[port])
     * @param interswitchLinks list of interswitch links for the node on which service is activated
     * @param serviceName service name (used as prefix for flow IDs)
     * @param queueNumber qos queue number
     * @return list of flows
     */
    public static List<Flow> getFlows(String servicePort, List<Link> interswitchLinks,
            String serviceName, long queueNumber) {
        List<Flow> flows = new ArrayList<>();
        flows.addAll(createPassingFlows(servicePort, serviceName, interswitchLinks));
        flows.addAll(createIngressFlow(servicePort, serviceName, interswitchLinks));

        return flows;
    }

    private static List<Flow> createIngressFlow(String outputPort, String serviceName,
            List<Link> interswitchLinks) {
        return interswitchLinks.stream().map(
                link -> createIngressFlow(outputPort, link.getLinkId().getValue(), serviceName))
                .collect(Collectors.toList());
    }

    public static Flow createIngressFlow(String outputPort, String inputPort, String serviceName) {
        LOG.info(" inside createIngressFlow () outputPort{}, inputPort{} ", outputPort, inputPort);
        List<Action> actions = new ArrayList<>();
        int actionOrder = 0;

        actions.add(ActionUtils.createOutputAction(inputPort, actionOrder));

        FlowId flowId = new FlowId(getVlanFlowId(serviceName, outputPort, 0));
        return new FlowBuilder().setId(flowId).setTableId(FLOW_TABLE_ID)
                .setPriority(EMPTY_VLAN_FLOW_PRIORITY)
                .setMatch(MatchUtils.createWithoutVlanMatch(outputPort))
                .setInstructions(ActionUtils.createInstructions(actions)).build();
    }

    /**
     * @param role differentiate coming endpoint is root or leaf
     * @param flows object
     * @param servicePort port on which service is activated (format: openflow:[node]:[port])
     * @param internalVlanId VLAN ID used internally in OvSwitch network
     * @param interswitchLinks list of interswitch links for the node on which service is activated
     * @param serviceName service name (used as prefix for flow IDs)
     * @param queueNumber qos queue number
     * @param rootCount shows number of root
     * @param eTreeUtils is utility object
     * @param isExclusive show requested service is port or tag based
     * @return list of flows
     * @throws ResourceNotAvailableException if node is not augmented with FlowCapableNode class or
     *         flow table is not present in node
     * @throws TransactionCommitFailedException is raised and returned when transaction commit
     *         failed
     */
    private static List<Flow> unTaggedFlows(String role, List<Flow> flows, String servicePort,
            int internalVlanId, List<Link> interswitchLinks, String serviceName, long queueNumber,
            long rootCount, EtreeUtils eTreeUtils, boolean isExclusive)
            throws ResourceNotAvailableException, TransactionCommitFailedException {

        if (role.equalsIgnoreCase(PortRole.ROOT.getName())) {
            createRootFlows(flows, servicePort, internalVlanId, interswitchLinks, serviceName,
                    queueNumber, rootCount, eTreeUtils);
        } else {
            createLeafFlows(flows, servicePort, internalVlanId, interswitchLinks, serviceName,
                    queueNumber, eTreeUtils);
        }

        return flows;
    }

    /**
     * @param flows object
     * @param servicePort port on which service is activated (format: openflow:[node]:[port])
     * @param internalVlanId VLAN ID used internally in OvSwitch network
     * @param interswitchLinks list of interswitch links for the node on which service is activated
     * @param serviceName service name (used as prefix for flow IDs)
     * @param queueNumber qos queue number
     * @param rootCount shows number of root
     * @param eTreeUtils is utility object
     * @throws ResourceNotAvailableException if node is not augmented with FlowCapableNode class or
     *         flow table is not present in node
     * @throws TransactionCommitFailedException is raised and returned when transaction commit
     *         failed
     */
    public static void createRootFlows(List<Flow> flows, String servicePort, int internalVlanId,
            List<Link> interswitchLinks, String serviceName, long queueNumber, long rootCount,
            EtreeUtils eTreeUtils)
            throws ResourceNotAvailableException, TransactionCommitFailedException {
        int[] rng = EtreeUtils.isAvailableVlan(internalVlanId);
        Optional<Integer> externalVlanId;

        for (int i = rng[0]; i <= rng[1]; i++) {
            int epVlanID = eTreeUtils.getEpVlanID(serviceName, OVSConstant.CPETYPE);
            externalVlanId = Optional.of(Integer.valueOf(epVlanID));

            if (rootCount > 1) {
                flows.addAll(createVlanPassingFlows(servicePort, i, externalVlanId, serviceName,
                        interswitchLinks));
                flows.addAll(createVlanPassingFlows(servicePort, epVlanID, externalVlanId,
                        serviceName, interswitchLinks));
                flows.add(createVlanIngressFlow(servicePort, epVlanID, externalVlanId, serviceName,
                        interswitchLinks, queueNumber));
            } else {
                flows.addAll(createVlanPassingFlows(servicePort, i, externalVlanId, serviceName,
                        interswitchLinks));
                flows.add(createVlanIngressFlow(servicePort, epVlanID, externalVlanId, serviceName,
                        interswitchLinks, queueNumber));
            }
        }

        eTreeUtils.releaseEpTreeServiceVlan(serviceName);
    }

    /**
     * @param flows object
     * @param servicePort port on which service is activated (format: openflow:[node]:[port])
     * @param internalVlanId VLAN ID used internally in OvSwitch network
     * @param interswitchLinks list of interswitch links for the node on which service is activated
     * @param serviceName service name (used as prefix for flow IDs)
     * @param queueNumber qos queue number
     * @param eTreeUtils is utility object
     * @throws ResourceNotAvailableException if node is not augmented with FlowCapableNode class or
     *         flow table is not present in node
     * @throws TransactionCommitFailedException is raised and returned when transaction commit
     *         failed
     * @return flows for leaf
     */
    public static List<Flow> createLeafFlows(List<Flow> flows, String servicePort,
            int internalVlanId, List<Link> interswitchLinks, String serviceName, long queueNumber,
            EtreeUtils eTreeUtils)
            throws ResourceNotAvailableException, TransactionCommitFailedException {
        Optional<Integer> externalVlanId;

        for (int vlan =
                OVSConstant.CPESTARTINCLUSIVE; vlan <= OVSConstant.CPEENDEXCLUSIVE; vlan++) {
            externalVlanId = Optional.of(Integer.valueOf(vlan));
            flows.addAll(createVlanPassingFlows(servicePort, vlan, externalVlanId, serviceName,
                    interswitchLinks));
            flows.add(createVlanIngressFlow(servicePort,
                    eTreeUtils.getEpVlanID(serviceName, OVSConstant.SPETYPE), externalVlanId,
                    serviceName, interswitchLinks, queueNumber));
        }
        eTreeUtils.releaseEpTreeServiceVlan(serviceName);

        return flows;
    }

    /**
     * Returns list of flows related to service named serviceName installed in specified flow table.
     *
     * @param table flow table
     * @param serviceName service name
     * @return list of flows
     */
    public static List<Flow> getServiceFlows(Table table, String serviceName) {
        return table.getFlow().stream()
                              .filter(flow -> flow.getId().getValue().startsWith(serviceName))
                              .collect(Collectors.toList());
    }

    /**
     * Returns list of flows - full-mesh - created on the base of list of provided links and default dropping all flow.
     *
     * @param interswitchLinks list of links
     * @return list of flows
     */
    public static List<Flow> getBaseFlows(List<Link> interswitchLinks) {
        List<Flow> baseFlows = new ArrayList<>();

        baseFlows.addAll(createInterswitchFlows(interswitchLinks));
        baseFlows.add(createDefaultFlow());

        return baseFlows;
    }

    /**
     * Returns list of flows installed in flow table. Flows which match LLDP packets are filtered out.
     *
     * @param table flow table
     * @return list of flows
     */
    public static List<Flow> getExistingFlowsWithoutLLDP(Table table) {
        return table.getFlow().stream().map(x -> new FlowBuilder(x).build())
                .filter(flow -> {
                    try {
                        return ! (flow.getMatch().getEthernetMatch().getEthernetType().getType().getValue()
                                .equals(ETH_TYPE_LLDP));
                    } catch (NullPointerException npe) {
                        return true;
                    }
                })
                .collect(Collectors.toList());
    }

    private static List<Flow> createInterswitchFlows(List<Link> interswitchLinks) {
        List<Flow> flows = new ArrayList<>();

        // ports have the same names as interswitch links
        List<String> portIds = interswitchLinks.stream()
                                               .map(link -> link.getLinkId().getValue())
                                               .collect(Collectors.toList());

        for (String portId : portIds) {
            List<Action> outputActions = portIds.stream()
                    .filter(outputPortId -> !outputPortId.equalsIgnoreCase(portId))
                    .map(outputPortId -> ActionUtils.createOutputAction(outputPortId, portIds.indexOf(outputPortId)))
                    .collect(Collectors.toList());
            FlowId flowId = new FlowId(INTERSWITCH_FLOW_ID_PREFIX + "-" + portId);
            Flow flow = new FlowBuilder().setId(flowId)
                     .withKey(new FlowKey(flowId))
                     .setTableId(FLOW_TABLE_ID)
                     .setPriority(INTERSWITCH_FLOW_PRIORITY)
                     .setMatch(MatchUtils.createInPortMatch(portId))
                     .setInstructions(ActionUtils.createInstructions(outputActions))
                     .build();
            flows.add(flow);
        }

        return flows;
    }

    private static Flow createDefaultFlow() {
        FlowId dropFlowId = new FlowId(DROP_FLOW_ID);
        return new FlowBuilder().setId(dropFlowId)
                    .withKey(new FlowKey(dropFlowId))
                    .setTableId(FLOW_TABLE_ID)
                    .setPriority(DROP_FLOW_PRIORITY)
                    .setInstructions(ActionUtils
                            .createInstructions(Arrays.asList(ActionUtils.createDropAction(0))))
                    .build();
    }

    private static List<Flow> createVlanPassingFlows(String outputPort, int internalVlanId,
                                                     Optional<Integer> externalVlanId,
                                                     String serviceName, List<Link> interswitchLinks) {
        return interswitchLinks.stream()
                   .map(link -> createVlanPassingFlow(outputPort, link.getLinkId().getValue(),
                           internalVlanId, externalVlanId,serviceName))
                   .collect(Collectors.toList());
    }

    private static Flow createVlanPassingFlow(String outputPort, String inputPort, int internalVlanId,
                                              Optional<Integer> externalVlanId, String serviceName) {
        // Create list of actions and VLAN match
        List<Action> actions = new ArrayList<>();
        int actionOrder = 0;

        actions.add(ActionUtils.createPopVlanAction(actionOrder++));
        if (externalVlanId.isPresent()) {
            actions.add(ActionUtils.createPushVlanAction(actionOrder++));
            actions.add(ActionUtils.createSetVlanIdAction(externalVlanId.get(), actionOrder++));
        }
        actions.add(ActionUtils.createOutputAction(outputPort, actionOrder));

        FlowId flowId = new FlowId(getVlanFlowId(serviceName, inputPort, internalVlanId));
        return new FlowBuilder().setId(flowId)
                .withKey(new FlowKey(flowId))
                .setTableId(FLOW_TABLE_ID)
                .setPriority(VLAN_FLOW_PRIORITY)
                .setMatch(MatchUtils.createVlanMatch(internalVlanId, inputPort))
                .setInstructions(ActionUtils.createInstructions(actions))
                .build();
    }

    public static Flow createVlanIngressFlow(String inputPort, int internalVlanId,
                                             Optional<Integer> externalVlanId, String serviceName,
                                             List<Link> interswitchLinks, long queueNumber) {
        // Create list of output port IDs
        final List<String> outputPortIds = interswitchLinks.stream()
                                         .map(link -> link.getLinkId().getValue())
                                         .filter(outputPort -> !outputPort.equalsIgnoreCase(inputPort))
                                         .collect(Collectors.toList());

        // Create list of actions
        List<Action> actions = new ArrayList<>();
        int actionOrder = 0;
        // 1. Create VLAN actions performing VLAN translation when service VLAN is already used in OvSwitch network
        if (externalVlanId.isPresent()) {
            actions.add(ActionUtils.createPopVlanAction(actionOrder++));
        }
        actions.add(ActionUtils.createPushVlanAction(actionOrder++));
        actions.add(ActionUtils.createSetVlanIdAction(internalVlanId, actionOrder++));
        actions.add(ActionUtils.createSetQueueNumberAction(queueNumber, actionOrder++));

        // 2. Create output actions
        final int outputActionOrder = actionOrder;
        actions.addAll(outputPortIds.stream()
                    .map(outputPortId -> ActionUtils.createOutputAction(outputPortId,
                                outputActionOrder + outputPortIds.indexOf(outputPortId)))
                    .collect(Collectors.toList()));

        FlowId flowId = new FlowId(getVlanFlowId(serviceName, inputPort, 0));
        return new FlowBuilder().setId(flowId)
                    .withKey(new FlowKey(flowId))
                    .setTableId(FLOW_TABLE_ID)
                    .setPriority(VLAN_FLOW_PRIORITY)
//                                .setMatch(MatchUtils.createVlanMatch(externalVlanId, inputPort))
                    .setMatch(externalVlanId.isPresent() ? MatchUtils
                            .createVlanMatch(externalVlanId.get(), inputPort) : MatchUtils.createInPortMatch(inputPort))
                    .setInstructions(ActionUtils.createInstructions(actions))
                    .build();
    }

    private static String getVlanFlowId(String serviceName, String inputPort, int vlanId) {
        if (vlanId != 0) {
            return serviceName + "-" + FLOW_ID_MIDDLE_PART.concat(String.valueOf(vlanId)) + "-"
                    + inputPort;
        } else {
            return serviceName + "-" + FLOW_ID_MIDDLE_PART + "-" + inputPort;
        }

    }
}
