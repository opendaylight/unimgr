/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xe.activator;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.cisco.rest.CiscoExecutor;
import org.opendaylight.unimgr.mef.nrp.cisco.rest.CiscoRestRunner;
import org.opendaylight.unimgr.mef.nrp.cisco.xe.util.CliGeneratorUtil;
import org.opendaylight.unimgr.mef.nrp.cisco.xe.util.IpAddressLoopbackNotFoundException;
import org.opendaylight.unimgr.mef.nrp.cisco.xe.util.RunningConfig;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivator;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivatorException;
import org.opendaylight.unimgr.mef.nrp.common.ResourceNotAvailableException;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.mef.unimgr.ext.rev160725.CTagVlanId;
import org.opendaylight.yang.gen.v1.urn.mef.unimgr.ext.rev160725.FcPort1;
import org.opendaylight.yang.gen.v1.urn.mef.unimgr.ext.rev160725.Node1;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

public class P2pConnectionActivator implements ResourceActivator {

    private static final Logger LOG = LoggerFactory.getLogger(P2pConnectionActivator.class);

    private DataBroker dataBroker;

    public P2pConnectionActivator(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public void activate(String nodeName, String outerName, String innerName, FcPort portA, FcPort portZ, long mtu) throws TransactionCommitFailedException, ResourceActivatorException {
        LOG.info("Activate\nPort A: " + portA + "\nPort Z: " + portZ);

        checkPreconditions(portA, portZ);

        Node1 node1A = getNode1augmentation(getNode(portA));
        Node1 node1Z = getNode1augmentation(getNode(portZ));

        CiscoExecutor restExecutorA = createCiscoExecutor(node1A);
    	CiscoExecutor restExecutorZ = createCiscoExecutor(node1Z);

        RunningConfig configA = new RunningConfig(getRunningConfig(restExecutorA));
        RunningConfig configZ = new RunningConfig(getRunningConfig(restExecutorZ));

        int vcId = getVcId(configA, configZ);

        LOG.info("Activating A side");
        activateSide(portA, vcId, restExecutorA, configZ);

        LOG.info("Activating Z side");
        activateSide(portZ, vcId, restExecutorZ, configA);

        LOG.info("Activation finished");
    }

    @Override
    public void deactivate(String nodeName, String outerName, String innerName, FcPort portA, FcPort portZ, long mtu)
    		throws TransactionCommitFailedException, ResourceActivatorException {
    	LOG.info("Deactivate\nPort A: " + portA + "\nPort Z: " + portZ);

        checkPreconditions(portA, portZ);

        LOG.info("Deactivating A side");
        deactivateSide(portA);

        LOG.info("Deactivating Z side");
        deactivateSide(portZ);

        LOG.info("Deactivation finished");
    }

    private void checkPreconditions(FcPort portA, FcPort portZ) throws ResourceActivatorException {
    	checkPreconditionsForPort(portA);
    	checkPreconditionsForPort(portZ);
    }

    private void checkPreconditionsForPort(FcPort port) throws ResourceActivatorException{
    	if(port.getAugmentation(FcPort1.class) == null) {
    		throw new ResourceActivatorException("VLan parameter has to be specified for port: " + port);
    	}
    }

    private int getVcId(RunningConfig configA, RunningConfig configZ){
    	Set<Integer> usedVcIds = configA.getUsedVcIdValues();
        usedVcIds.addAll(configZ.getUsedVcIdValues());
        int vcId = CliGeneratorUtil.generateVcId(usedVcIds);
        return vcId;
    }

    private void activateSide(FcPort port, int vcId, CiscoExecutor ciscoExecutor, RunningConfig oppositeSideRunningConfig) throws ResourceActivatorException{
        String commands = generateCliCommands(port, oppositeSideRunningConfig, vcId);
        setRunningConfig(ciscoExecutor, commands);
    }

    private void deactivateSide(FcPort port) throws ResourceActivatorException{
    	Node1 node1 = getNode1augmentation(getNode(port));
        CiscoExecutor restExecutor = createCiscoExecutor(node1);
        String commands = generateCliNoCommands(port);
        setRunningConfig(restExecutor, commands);
    }

    private Node getNode(FcPort port) throws ResourceNotAvailableException {

        InstanceIdentifier<Node> nodeIid = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(port.getTopology()))
                .child(Node.class, new NodeKey(port.getNode()))
                .build();

        Optional<Node> result = MdsalUtils.readOptional(dataBroker, LogicalDatastoreType.CONFIGURATION, nodeIid);

        if (result.isPresent()) {
            return result.get();
        } else {
            LOG.error("Node " + nodeIid + " does not exists");
            throw new ResourceNotAvailableException("Node " + nodeIid + " does not exists");
        }
    }

    private CiscoExecutor createCiscoExecutor(Node1 node) throws ResourceActivatorException{
    	try {
			return new CiscoRestRunner(
					InetAddress.getByName(new String(node.getConnectionSettings().getHost().getIpAddress().getValue())),
					node.getConnectionSettings().getUserName(),
					node.getConnectionSettings().getPassword());
		} catch (UnknownHostException e) {
			LOG.error("Host name cannot be resolved for: " + node.getConnectionSettings().getHost().toString());
			throw new ResourceActivatorException("Host name cannot be resolved for: " + node.getConnectionSettings().getHost().toString(),e);
		}
    }

    private Node1 getNode1augmentation(Node node) throws ResourceActivatorException {
    	Node1 node1 = node.getAugmentation(Node1.class);
        if (node1 == null) {
            LOG.error("Connection settings can not be obtained from node :" + node);
            throw new ResourceActivatorException("Connection settings can not be obtained from node :" + node);
        }
        return node1;
    }

    private String getRunningConfig(CiscoExecutor executor) throws ResourceActivatorException{
    	try {
			return executor.getRunningConfig();
		} catch (Exception e) {
			throw new ResourceActivatorException(e.getMessage(), e);
		}
    }

    private void setRunningConfig(CiscoExecutor executor, String runningConfig) throws ResourceActivatorException{
    	try {
    		executor.setRunningConfig(runningConfig);
    	} catch (Exception e) {
    		throw new ResourceActivatorException(e.getMessage(), e);
    	}
    }

    private String generateCliCommands(FcPort port, RunningConfig oppsiteSideConfig, int vcId) throws ResourceActivatorException{
    	CTagVlanId cTagVlanId = port.getAugmentation(FcPort1.class).getCTagVlanId();
    	try {
			String commands = CliGeneratorUtil.generateServiceCommands(
			         port.getTp().getValue(),
			         cTagVlanId.getValue().shortValue(),
			         cTagVlanId.getValue().shortValue(),
			         oppsiteSideConfig.getIpAddressLoopback0(),
			         vcId);
			LOG.info("Generated commands:\n" + commands);
			return commands;
		} catch (IOException e) {
			throw new ResourceActivatorException("Cannot generate commands for port: " + port, e);
		} catch (IpAddressLoopbackNotFoundException e) {
			LOG.error("Problem with getting loopback address for port: " + port, e);
			throw e;
		}
    }

    private String generateCliNoCommands(FcPort port) {
    	CTagVlanId cTagVlanId = port.getAugmentation(FcPort1.class).getCTagVlanId();
    	String commands = CliGeneratorUtil.generateNoServiceCommands(
                port.getTp().getValue(),
                cTagVlanId.getValue().shortValue());
    	LOG.debug("Generated commands:\nPort: " + port + "\nCommands: \n" + commands);
    	return commands;
    }
}
