/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.utils;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.impl.UnimgrConstants;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDestKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSourceKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class EvcUtils {

    private static final Logger LOG = LoggerFactory.getLogger(EvcUtils.class);

    /**
     * Delete EVC data from configuration datastore.
     * @param dataBroker The dataBroker instance to create transactions
     * @param optionalUni Optional Uni Node
     */
    public static void deleteEvcData(final DataBroker dataBroker, final Optional<Node> optionalUni) {
        if (optionalUni.isPresent()) {
            final UniAugmentation uniAugmentation =
                                optionalUni
                                    .get()
                                    .getAugmentation(UniAugmentation.class);
            final InstanceIdentifier<Node> ovsdbNodeIid =
                                              uniAugmentation
                                             .getOvsdbNodeRef()
                                             .getValue()
                                             .firstIdentifierOf(Node.class);
            final Optional<Node> optionalOvsdNode =
                    MdsalUtils.readNode(dataBroker,
                                         LogicalDatastoreType.OPERATIONAL,
                                         ovsdbNodeIid);
            if (optionalOvsdNode.isPresent()) {
                final Node ovsdbNode = optionalOvsdNode.get();
                final OvsdbNodeAugmentation ovsdbNodeAugmentation =
                        ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
                for (final ManagedNodeEntry managedNodeEntry: ovsdbNodeAugmentation.getManagedNodeEntry()) {
                    final InstanceIdentifier<Node> bridgeIid = managedNodeEntry
                                                             .getBridgeRef()
                                                             .getValue()
                                                             .firstIdentifierOf(Node.class);
                    final Optional<Node> optBridgeNode = MdsalUtils.readNode(dataBroker, bridgeIid);
                    if (optBridgeNode.isPresent()) {
                        final Node bridgeNode = optBridgeNode.get();
                        final InstanceIdentifier<TerminationPoint> iidGreTermPoint =
                                UnimgrMapper.getTerminationPointIid(bridgeNode,
                                        UnimgrConstants.DEFAULT_GRE_TUNNEL_NAME);
                        final InstanceIdentifier<TerminationPoint> iidEthTermPoint =
                                UnimgrMapper.getTerminationPointIid(bridgeNode,
                                        UnimgrConstants.DEFAULT_TUNNEL_IFACE);
                        MdsalUtils.deleteNode(dataBroker, iidGreTermPoint, LogicalDatastoreType.CONFIGURATION);
                        MdsalUtils.deleteNode(dataBroker, iidEthTermPoint, LogicalDatastoreType.CONFIGURATION);
                    }
                }
            }
        } else {
            LOG.info("Unable to retrieve UNI from the EVC.");
        }
    }

    /**
     * Retrieve the list of links in the Operational DataStore.
     * @param dataBroker The dataBroker instance to create transactions
     * @return A list of Links retrieved from the Operational DataStore
     */
    public static List<Link> getEvcLinks(final DataBroker dataBroker) {
        final List<Link> evcLinks = new ArrayList<>();
        final InstanceIdentifier<Topology> evcTopology = UnimgrMapper.getEvcTopologyIid();
        final Topology topology = MdsalUtils.read(dataBroker,
                                             LogicalDatastoreType.OPERATIONAL,
                                             evcTopology);
        if ((topology != null) && (topology.getLink() != null)) {
            for (final Link link : topology.getLink()) {
                final EvcAugmentation evcAugmentation = link.getAugmentation(EvcAugmentation.class);
                if (evcAugmentation != null) {
                    evcLinks.add(link);
                }
            }
        }
        return evcLinks;
    }

    /**
     * Updates a specific EVC into a specific DataStore type.
     * @param dataStore The datastore type
     * @param evcKey The EVC key
     * @param evcAugmentation The EVC's data
     * @param sourceUniIid The Source Uni Instance Identifier
     * @param destinationUniIid The destination Uni Instance Identifier
     * @param dataBroker The dataBroker instance to create transactions
     * @return true if evc is updated
     */
    public static boolean updateEvcNode(final LogicalDatastoreType dataStore,
                                     final InstanceIdentifier<?> evcKey,
                                     final EvcAugmentation evcAugmentation,
                                     final InstanceIdentifier<?> sourceUniIid,
                                     final InstanceIdentifier<?> destinationUniIid,
                                     final DataBroker dataBroker) {
        final EvcAugmentationBuilder updatedEvcBuilder = new EvcAugmentationBuilder(evcAugmentation);
        if ((sourceUniIid != null) && (destinationUniIid != null)) {
            final List<UniSource> sourceList = new ArrayList<UniSource>();
            final UniSource evcUniSource = evcAugmentation.getUniSource().iterator().next();
            final UniSourceKey sourceKey = evcUniSource.getKey();
            final short sourceOrder = evcUniSource.getOrder();
            final IpAddress sourceIp = evcUniSource.getIpAddress();
            final UniSource uniSource = new UniSourceBuilder()
                                          .setOrder(sourceOrder)
                                          .setKey(sourceKey)
                                          .setIpAddress(sourceIp)
                                          .setUni(sourceUniIid)
                                          .build();
            sourceList.add(uniSource);
            updatedEvcBuilder.setUniSource(sourceList);

            final List<UniDest> destinationList = new ArrayList<UniDest>();
            final UniDest evcUniDest = evcAugmentation.getUniDest().iterator().next();
            final UniDestKey destKey = evcUniDest.getKey();
            final short destOrder = evcUniDest.getOrder();
            final IpAddress destIp = evcUniDest.getIpAddress();
            final UniDest uniDest = new UniDestBuilder()
                                      .setIpAddress(destIp)
                                      .setOrder(destOrder)
                                      .setKey(destKey)
                                      .setUni(destinationUniIid)
                                      .build();
            destinationList.add(uniDest);
            updatedEvcBuilder.setUniDest(destinationList);
            final Optional<Link> optionalEvcLink = MdsalUtils.readLink(dataBroker,
                                                      LogicalDatastoreType.CONFIGURATION,
                                                      evcKey);
            if (optionalEvcLink.isPresent()) {
                final Link link = optionalEvcLink.get();
                final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
                final LinkBuilder linkBuilder = new LinkBuilder();
                linkBuilder.setKey(link.getKey());
                linkBuilder.setLinkId(link.getLinkId());
                linkBuilder.setDestination(link.getDestination());
                linkBuilder.setSource(link.getSource());
                linkBuilder.addAugmentation(EvcAugmentation.class, updatedEvcBuilder.build());
                transaction.put(dataStore, evcKey.firstIdentifierOf(Link.class), linkBuilder.build());
                transaction.submit();
                return true;
            } else {
                LOG.info("EvcLink is not present: " + optionalEvcLink.get().getKey());
            }
        } else {
            LOG.info("Invalid instance identifiers for sourceUni and destUni.");
        }
        return false;
    }

    private EvcUtils() {
        throw new AssertionError("Instantiating utility class.");
    }
}
