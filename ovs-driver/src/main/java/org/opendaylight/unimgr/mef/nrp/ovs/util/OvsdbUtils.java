/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.utils.mdsal.utils.NotifyingDataChangeListener;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbQosRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbQueueRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.QosTypeLinuxHtb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Queues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QueuesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesOtherConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesOtherConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.QosEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.QosEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.QosEntryKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.CheckedFuture;

/**
 * Class responsible for managing OVSDB Nodes
 *
 * @author marek.ryznar@amartus.com
 */
public class OvsdbUtils {
    private static final TopologyId ovsdbTopoId = new TopologyId(new Uri("ovsdb:1"));
    private static final NodeId odlNodeId = new NodeId(new Uri("odl"));
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbUtils.class);
    
    /**
	 * Configures ovs egress qos shaping with following steps:
	 * <ul>
	 * 
	 * <li>Create qos queue on all port (tp + output ports)</li>
	 * <li>Create qos entry with queues created in previous step</li>
	 * <li>Add created qos entries to termination point</li> 
	 * </ul>
	 * 
	 * @param dataBroker
	 *            access to data tree store
	 * @param tpId
	 *            termination point id
	 * @param outputPorts
	 *            list of output aka interswitch/internal ports
	 * @param minRate
	 *            qos min-rate
	 * @param maxRate
	 *            qos max-rate
	 * @param serviceName
	 *            name of service
	 * @param queueNumber
	 *            qos queue number
	 */
    public static void createEgressQos(DataBroker dataBroker, String tpId, List<String> outputPorts, Long minRate, Long maxRate, String serviceName, Long queueNumber){
    	
    	String qosQueueId = serviceName + "_queue_" + queueNumber;
    	String qosEntryId = serviceName + "_qos_" + queueNumber;
    	
    	//create queue
    	Queues queue = createQueue(minRate, maxRate, qosQueueId);
        InstanceIdentifier<Queues> queueInstanceIdentifier = getQueueInstanceIdentifier(queue);
    	addQueueToConfigDatastore(dataBroker, queue, queueInstanceIdentifier);

		// create qos entry that contains ref to queue uuid, it has to be
		// present in operational config, that's why it happens separate tx
    	QosEntries qosEntry = createQosEntry(qosEntryId, queueNumber, queueInstanceIdentifier);
        InstanceIdentifier<QosEntries> qosInstanceIdentifier = getQosEntryInstanceIdentifier(qosEntry);
        addQosEntryToConfigDatastore(dataBroker, qosEntry, qosInstanceIdentifier);


  		//bind created qos to endpoint + all interswitchLinks on this this node
		Optional<TerminationPoint> otp = findTerminationPoint(dataBroker, tpId);
		Optional<Node> optNode = findBridgeNode(dataBroker, tpId);
		
		addQosEntryToTerminationPoint(dataBroker, qosInstanceIdentifier, otp, optNode);
		
		for (String outputTpId : outputPorts) {
			otp = findTerminationPoint(dataBroker, outputTpId);
			addQosEntryToTerminationPoint(dataBroker, qosInstanceIdentifier, otp, optNode);
		}
    }    

	private static void deleteQosFromConfigDatastore(DataBroker dataBroker, InstanceIdentifier<?> qosId) {
		WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
		tx.delete(LogicalDatastoreType.CONFIGURATION, qosId);
		final CheckedFuture<Void, TransactionCommitFailedException> future = tx.submit();
		try {
			future.checkedGet();
			LOG.info("Succesfully removed Qos entry from Config datastore: {}", qosId.toString());
		} catch (final TransactionCommitFailedException e) {
			LOG.warn("Failed to remove Qos entry from Config datastore: {}", qosId.toString(), e);
		}
	}
	

	private static void deleteQueueFromConfigDatastore(DataBroker dataBroker, InstanceIdentifier<?> queueId) {
		WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
		tx.delete(LogicalDatastoreType.CONFIGURATION, queueId);
		final CheckedFuture<Void, TransactionCommitFailedException> future = tx.submit();
		try {
			future.checkedGet();
			LOG.info("Succesfully removed Qos Queue from Config datastore: {}", queueId.toString());
		} catch (final TransactionCommitFailedException e) {
			LOG.warn("Failed to remove Qos Queue from Config datastore: {}", queueId.toString(), e);
		}
	}
	
	private static void deleteTerminationPointQosEntryFromConfigDatastore(DataBroker dataBroker, InstanceIdentifier<QosEntry> qosEntryId) {
		WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
		tx.delete(LogicalDatastoreType.CONFIGURATION, qosEntryId);
		final CheckedFuture<Void, TransactionCommitFailedException> future = tx.submit();
		try {
			future.checkedGet();
			LOG.info("Succesfully removed Termination Point Qos entry from  Config datastore: {}", qosEntryId.toString());
		} catch (final TransactionCommitFailedException e) {
			LOG.warn("Failed to remove Termination Point Qos entry from Config datastore: {}", qosEntryId.toString(), e);
		}
	}

	private static void addQosEntryToTerminationPoint(DataBroker dataBroker,
			InstanceIdentifier<QosEntries> qosInstanceIdentifier, Optional<TerminationPoint> otp,
			Optional<Node> optNode) {
		if (otp.isPresent() && optNode.isPresent()) {
			TerminationPoint tp = otp.get();
			tp = buildTerminationPoint(tp, qosInstanceIdentifier);
			InstanceIdentifier<TerminationPoint> tpIid = getTerminationPointInstanceIdentifier(optNode.get(), tp);
			
	    	final NotifyingDataChangeListener qosEntryToTpOperationalListener = new NotifyingDataChangeListener(
					LogicalDatastoreType.OPERATIONAL, tpIid, null);
	    	qosEntryToTpOperationalListener.registerDataChangeListener(dataBroker);
			
			WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
			tx.merge(LogicalDatastoreType.CONFIGURATION, tpIid, tp, true);
			
			
			final CheckedFuture<Void, TransactionCommitFailedException> future = tx.submit();
			try {
				future.checkedGet();
				LOG.info("Succesfully added Qos Uiid to termination point: {}", tp.getTpId().getValue());
			} catch (final TransactionCommitFailedException e) {
				LOG.warn("Failed to add Qos Uiid to termination point: {} ", tp.getTpId().getValue(), e);
			}
			
			try {
				qosEntryToTpOperationalListener.waitForUpdate();
			} catch (InterruptedException e) {
				LOG.warn("Sleep interrupted while waiting for Qos entry to Termination Point update {}", qosInstanceIdentifier, e);
			} finally {
				try {
					qosEntryToTpOperationalListener.close();
				} catch (Exception e) {
					LOG.debug(
							"Failed to properly close qosEntryToTpOperationalListener while waiting for qos entry to TerminationPoint update oper {}",
							qosInstanceIdentifier, e);
				}
			}
		}

	}

    /**
	 * Removes egress qos shaping from ovsdb for specified service:
	 * <ul>
	 * <li>Remove all qos entries to TerminationPoints assignments for specified
	 * service from config datastore</li>
	 * <li>Remove all qos entries assinged to specified TerminationPoints and
	 * service from config datastore</li>
	 * <li>Remove all queues assigned to qos entries for specified TerminationPoints
	 * and service from config datastore</li>
	 * </ul>
	 * 
	 * @param dataBroker
	 *            access to data tree store
	 * @param serviceName
	 *            name of the service
	 * @param tpsWithQos
	 *            list of TerminationPoints that might contain QoS
	 */
	public static void removeQosEntryFromTerminationPoints(DataBroker dataBroker, String serviceName,
			List<String> tpsWithQos) {
		Optional<Node> optNode = findBridgeNode(dataBroker, tpsWithQos.get(0));
		Set<InstanceIdentifier<?>> qosEntriesTodelete = new HashSet<>();
		if (optNode.isPresent()) {
			for (String tpId : tpsWithQos) {
				Optional<TerminationPoint> otp = findTerminationPoint(dataBroker, tpId);
				if (otp.isPresent()) {
					TerminationPoint tp = otp.get();
					OvsdbTerminationPointAugmentation ovsdbTpAug = tp
							.getAugmentation(OvsdbTerminationPointAugmentation.class);
					if (ovsdbTpAug != null && ovsdbTpAug.getQosEntry() != null) {
						for (QosEntry qosEntry : ovsdbTpAug.getQosEntry()) {
							OvsdbQosRef qosRef = qosEntry.getQosRef();
							if (qosRef.getValue().toString().contains(serviceName)) {
								deleteTerminationPointQosEntryFromConfigDatastore(dataBroker,
										getTerminationPointQosEntryInstanceIdentifier(optNode.get(), tp, qosEntry));
								qosEntriesTodelete.add(qosRef.getValue());
							}
						}
					}
				}
			}
		}
		for (InstanceIdentifier<?> qosEntryToDeleteId : qosEntriesTodelete) {
			QosEntries qosEntries = (QosEntries) MdsalUtils.read(dataBroker, LogicalDatastoreType.OPERATIONAL,
					qosEntryToDeleteId);
			if (qosEntries != null) {
				List<QueueList> queueList = qosEntries.getQueueList();
				deleteQosFromConfigDatastore(dataBroker, qosEntryToDeleteId);
				queueList.stream()
						.forEach(ql -> deleteQueueFromConfigDatastore(dataBroker, ql.getQueueRef().getValue()));
			}
		}
	}

	private static void addQueueToConfigDatastore(DataBroker dataBroker, Queues queue,
			InstanceIdentifier<Queues> queueInstanceIdentifier) {
		
    	final NotifyingDataChangeListener queueCreateOperationalListener = new NotifyingDataChangeListener(
				LogicalDatastoreType.OPERATIONAL, queueInstanceIdentifier, null);
		queueCreateOperationalListener.registerDataChangeListener(dataBroker);
		
		WriteTransaction createQoSQueueTx = dataBroker.newWriteOnlyTransaction();
		createQoSQueueTx.merge(LogicalDatastoreType.CONFIGURATION, queueInstanceIdentifier, queue, true);

		final CheckedFuture<Void, TransactionCommitFailedException> futureCreateQoSQueueTx = createQoSQueueTx.submit();
		try {
			futureCreateQoSQueueTx.checkedGet();
			LOG.info("Succesfully created QoS queue :{}", queueInstanceIdentifier);
		} catch (final TransactionCommitFailedException e) {
			LOG.warn("Failed to create new QoS queue: {} ", queueInstanceIdentifier, e);
		}

		try {
			queueCreateOperationalListener.waitForCreation();
		} catch (InterruptedException e) {
			LOG.warn("Sleep interrupted while waiting for Qos queue creation {}", queueInstanceIdentifier, e);
		} finally {
			try {
				queueCreateOperationalListener.close();
			} catch (Exception e) {
				LOG.debug(
						"Failed to properly close queueCreateOperationalListener while waiting for qos queue creation {}",
						queueInstanceIdentifier, e);
			}
		}

	}

	public static void addQosEntryToConfigDatastore(DataBroker dataBroker, QosEntries qosEntry, InstanceIdentifier<QosEntries> qosInstanceIdentifier) {
    	final NotifyingDataChangeListener qosEntryOperationalListener = new NotifyingDataChangeListener(
				LogicalDatastoreType.OPERATIONAL, qosInstanceIdentifier, null);
		qosEntryOperationalListener.registerDataChangeListener(dataBroker);
		
    	WriteTransaction createQosEntryTx = dataBroker.newWriteOnlyTransaction();
    	createQosEntryTx.merge(LogicalDatastoreType.CONFIGURATION, qosInstanceIdentifier, qosEntry, true);
    	
        final CheckedFuture<Void, TransactionCommitFailedException> futureCreateQosEntryTx = createQosEntryTx.submit();
        try {
        	futureCreateQosEntryTx.checkedGet();
        	LOG.info("Succesfully created QoS entry: {}", qosInstanceIdentifier);
        } catch (final TransactionCommitFailedException e) {
            LOG.warn("Failed to create new QoS entry: {} ", qosInstanceIdentifier, e);
		}
        
		try {
			qosEntryOperationalListener.waitForCreation();
		} catch (InterruptedException e) {
			LOG.warn("Sleep interrupted while waiting for qos entry creation {}", qosInstanceIdentifier, e);
		} finally {
			try {
				qosEntryOperationalListener.close();
			} catch (Exception e) {
				LOG.debug(
						"Failed to properly close qosEntryOperationalListener while waiting for qos entry creation {}",
						qosInstanceIdentifier, e);
			}
		}
    }

	private static QosEntries createQosEntry(String qosEntryId, Long queueNumber, InstanceIdentifier<Queues> queueId) {
		QosEntriesBuilder qosEntriesBuilder = new QosEntriesBuilder();
		qosEntriesBuilder.setQosId(new Uri(qosEntryId));
		qosEntriesBuilder.setQosType(QosTypeLinuxHtb.class);

		QueueListBuilder queueListBuilder = new QueueListBuilder();
		queueListBuilder.setQueueNumber(queueNumber);
		queueListBuilder.setQueueRef(new OvsdbQueueRef(queueId));
		
		List<QueueList> queueList = new LinkedList<>();
		queueList.add(queueListBuilder.build());
		
		qosEntriesBuilder.setQueueList(queueList);
		
		return qosEntriesBuilder.build();
	}

	private static Queues createQueue(Long minRate, Long maxRate, String qosQueueId) {
		QueuesBuilder queuesBuilder = new QueuesBuilder();
		queuesBuilder.setQueueId(new Uri(qosQueueId));

		LinkedList<QueuesOtherConfig> queuesOtherConfigList = new LinkedList<>();
		QueuesOtherConfigBuilder queuesMaxRateOtherConfigBuilder = new QueuesOtherConfigBuilder();
		queuesMaxRateOtherConfigBuilder.setQueueOtherConfigKey("max-rate");
		queuesMaxRateOtherConfigBuilder.setQueueOtherConfigValue(maxRate.toString());
		queuesOtherConfigList.add(queuesMaxRateOtherConfigBuilder.build());
		
		QueuesOtherConfigBuilder queuesMinRateOtherConfigBuilder = new QueuesOtherConfigBuilder();
		queuesMinRateOtherConfigBuilder.setQueueOtherConfigKey("min-rate");
		queuesMinRateOtherConfigBuilder.setQueueOtherConfigValue(minRate.toString());
		queuesOtherConfigList.add(queuesMinRateOtherConfigBuilder.build());

		queuesBuilder.setQueuesOtherConfig(queuesOtherConfigList);

		return queuesBuilder.build();
	}

	
	private static InstanceIdentifier<Queues> getQueueInstanceIdentifier(Queues queue) {
		return InstanceIdentifier.create(NetworkTopology.class)
				.child(Topology.class, new TopologyKey(ovsdbTopoId))
				.child(Node.class, new NodeKey(odlNodeId))
				.augmentation(OvsdbNodeAugmentation.class)
				.child(Queues.class, queue.getKey());
	}
	
	private static InstanceIdentifier<QosEntries> getQosEntryInstanceIdentifier(QosEntries qosEntries) {
		return InstanceIdentifier.create(NetworkTopology.class)
				.child(Topology.class, new TopologyKey(ovsdbTopoId))
				.child(Node.class, new NodeKey(odlNodeId))
				.augmentation(OvsdbNodeAugmentation.class)
				.child(QosEntries.class, qosEntries.getKey());
	}
	
    private static InstanceIdentifier<TerminationPoint> getTerminationPointInstanceIdentifier(Node bridgeNode, TerminationPoint tp){
        return InstanceIdentifier
                        .create(NetworkTopology.class)
                        .child(Topology.class,
                                new TopologyKey(ovsdbTopoId))
                        .child(Node.class, bridgeNode.getKey())
                        .child(TerminationPoint.class, tp.getKey());
    }
    
    private static InstanceIdentifier<QosEntry> getTerminationPointQosEntryInstanceIdentifier(Node bridgeNode, TerminationPoint tp, QosEntry qosEntry){
        return InstanceIdentifier
                        .create(NetworkTopology.class)
                        .child(Topology.class,
                                new TopologyKey(ovsdbTopoId))
                        .child(Node.class, bridgeNode.getKey())
                        .child(TerminationPoint.class, tp.getKey())
                        .augmentation(OvsdbTerminationPointAugmentation.class)
                        .child(QosEntry.class, qosEntry.getKey());
    }

    private static TerminationPoint buildTerminationPoint(TerminationPoint tp, InstanceIdentifier<QosEntries> qosInstanceIdentifier){
        TerminationPointBuilder terminationPointBuilder = new TerminationPointBuilder();
                 terminationPointBuilder.setKey(tp.getKey());
                terminationPointBuilder.setTpId(tp.getTpId());
        terminationPointBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class,addQosToPort(tp.getTpId().getValue(), qosInstanceIdentifier));
        return terminationPointBuilder.build();
    }

    private static OvsdbTerminationPointAugmentation addQosToPort(String name, InstanceIdentifier<QosEntries> qosInstanceIdentifier){
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();        
		List<QosEntry> qosList = new ArrayList<>();
		OvsdbQosRef qosRef = new OvsdbQosRef(qosInstanceIdentifier);
		qosList.add(new QosEntryBuilder().setKey(new QosEntryKey(new Long(SouthboundConstants.PORT_QOS_LIST_KEY)))
				.setQosRef(qosRef).build());
		ovsdbTerminationPointAugmentationBuilder.setQosEntry(qosList);

        ovsdbTerminationPointAugmentationBuilder.setName(name);


        return ovsdbTerminationPointAugmentationBuilder.build();
    }
    
    private static Optional<TerminationPoint> findTerminationPoint(DataBroker dataBroker, String port){
        List<Node> ovsdbNodes = getOvsdbNodes(dataBroker);
        Optional<TerminationPoint> terminationPoint = Optional.empty();
        if (!ovsdbNodes.isEmpty()) {
            terminationPoint = ovsdbNodes.stream()
                    .flatMap(node -> {
                        if(node.getTerminationPoint()!=null)
                            return node.getTerminationPoint().stream();
                        return Stream.empty();
                    })
                    .filter(tp -> tp.getTpId().getValue().equals(port))
                    .findFirst();
        }

        return terminationPoint;
    }

    private static Optional<Node> findBridgeNode(DataBroker dataBroker, String tpId){
        List<Node> ovsdbNodes = getOvsdbNodes(dataBroker);
        return ovsdbNodes.stream()
                .filter(node -> {
                            if(node.getTerminationPoint()!=null){
                                return node.getTerminationPoint().stream()
                                        .anyMatch(tp -> tp.getTpId().getValue().equals(tpId));
                            } else {
                                return false;
                            }
                        }
                ).findFirst();

    }

    /**
     * Retrieve a list of Ovsdb Nodes from the Operational DataStore.
     *
     * @param dataBroker The dataBroker instance to create transactions
     * @return The Ovsdb Node retrieved from the Operational DataStore
     */
    public static List<Node> getOvsdbNodes(DataBroker dataBroker) {
        final InstanceIdentifier<Topology> ovsdbTopoIdentifier = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class,
                        new TopologyKey(ovsdbTopoId));

        Topology topology = MdsalUtils.read(dataBroker,
                LogicalDatastoreType.OPERATIONAL,
                ovsdbTopoIdentifier);

        if ((topology != null) && (topology.getNode() != null)){
            return topology.getNode();
        }
        return Collections.emptyList();
    }
    
    /**
     * Retrieve a Ovsdb Odl node from the Operational DataStore.
     *
     * @param dataBroker The dataBroker instance to create transactions
     * @return The Ovsdb Odl Node retrieved from the Operational DataStore
     */
    public static Node getOdlNode(DataBroker dataBroker) {
        final InstanceIdentifier<Node> ovsdbOdlNodeIdentifier = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class,
                        new TopologyKey(ovsdbTopoId))
                .child(Node.class, new NodeKey(odlNodeId));

        return MdsalUtils.read(dataBroker,
                LogicalDatastoreType.OPERATIONAL,
                ovsdbOdlNodeIdentifier);
    }
}
