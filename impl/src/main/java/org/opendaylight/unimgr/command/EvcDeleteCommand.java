/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.command;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Evc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSource;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class EvcDeleteCommand extends AbstractDeleteCommand {

    private static final Logger LOG = LoggerFactory.getLogger(EvcDeleteCommand.class);

    public EvcDeleteCommand(DataBroker dataBroker,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super.changes = changes;
        super.dataBroker = dataBroker;
    }

    @Override
    public void execute() {
        Map<InstanceIdentifier<Evc>, Evc> originalEvcs = UnimgrUtils.extractOriginal(changes, Evc.class);
        Set<InstanceIdentifier<Evc>> removedEvcs = UnimgrUtils.extractRemoved(changes, Evc.class);

        Set<InstanceIdentifier<?>> removedPaths = changes.getRemovedPaths();
        if (!removedPaths.isEmpty()) {
            for (InstanceIdentifier<?> removedPath: removedPaths) {
                Class<?> type = removedPath.getTargetType();
                LOG.info("Removed paths instance identifier {}", type);
                if (type.equals(Evc.class)) {
                    LOG.info("Removed paths instance identifier {}", type);
                    for (Entry<InstanceIdentifier<Evc>, Evc> evc: originalEvcs.entrySet()) {
                        if (evc.getKey().equals(type)) {
                            Evc data = evc.getValue();
                            List<UniSource> uniSourceLst = data.getUniSource();
                            for (UniSource uniSource : uniSourceLst) {
                                InstanceIdentifier<?> iidUni = uniSource.getUni();
                                Node ovsdbNd = getUniOvsdbNode(iidUni);
                                List<TerminationPoint> termPointList = ovsdbNd.getTerminationPoint();
                                for(TerminationPoint termPoint : termPointList) {
                                    deleteTerminationPoint(termPoint, ovsdbNd);
                                }
                            }
                            LOG.info("Removed EVC Source {}", data.getUniSource());
                            List<UniDest> uniDestLst = data.getUniDest();
                            for (UniDest uniDest : uniDestLst) {
                                InstanceIdentifier<?> iidUni = uniDest.getUni();
                                Node ovsdbNd = getUniOvsdbNode(iidUni);
                                List<TerminationPoint> termPointList = ovsdbNd.getTerminationPoint();
                                for(TerminationPoint termPoint : termPointList) {
                                    deleteTerminationPoint(termPoint, ovsdbNd);
                                }
                            }
                            LOG.info("Removed EVC Destination {}", data.getUniDest());
                        }
                    }
                }
            }
        }
    }

    private Node getUniOvsdbNode(InstanceIdentifier<?> iidUni) {
        Optional<Node> nodeOpt = UnimgrUtils.readNode(dataBroker, iidUni);
        if (nodeOpt.isPresent()) {
            return  nodeOpt.get();
        }
        return null;
    }

    private boolean deleteTerminationPoint(TerminationPoint termPoint, Node ovsdbNode) {
        boolean result = false;
        InstanceIdentifier<TerminationPoint> terminationPointPath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(UnimgrConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class,ovsdbNode.getKey())
                .child(TerminationPoint.class, termPoint.getKey());
        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.delete(LogicalDatastoreType.OPERATIONAL, terminationPointPath);
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        try {
            future.checkedGet();
            result = true;
        } catch (TransactionCommitFailedException e) {
            LOG.warn("Failed to delete {} ", terminationPointPath, e);
        }
        return result;
    }
}
