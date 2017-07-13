/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.command;

import java.util.Collection;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

/**
 * DataTreeModificationHelper help to get a dummy value of DataTreeModification object as DataTreeModification
 * class is a final class.
 */
public final class DataTreeModificationHelper {

    public static DataTreeModification<Node> getUniNode(final Node node) {
        final DataTreeIdentifier<Node> uniDataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, getUniIid("10.10.1.3"));
        final DataObjectModification<Node> uniDataTreeObj = new DataObjectModification<Node>() {
            @Override
            public Collection<DataObjectModification<? extends DataObject>> getModifiedChildren() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public <C extends Identifiable<K> & ChildOf<? super Node>, K extends Identifier<C>> DataObjectModification<C> getModifiedChildListItem(
                    Class<C> arg0, K arg1) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public <C extends ChildOf<? super Node>> DataObjectModification<C> getModifiedChildContainer(Class<C> arg0) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public DataObjectModification<? extends DataObject> getModifiedChild(PathArgument arg0) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public <C extends Augmentation<Node> & DataObject> DataObjectModification<C> getModifiedAugmentation(
                    Class<C> arg0) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public ModificationType getModificationType() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public PathArgument getIdentifier() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Class<Node> getDataType() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Node getDataBefore() {
                return node;
            }

            @Override
            public Node getDataAfter() {
                return node;
            }
        };

        DataTreeModification<Node> uniNode = new DataTreeModification<Node>() {
            @Override
            public DataTreeIdentifier<Node> getRootPath() {
                return uniDataTreeIid;
            }
            @Override
            public DataObjectModification<Node> getRootNode() {
                return uniDataTreeObj;
            }
        };
        return uniNode;
    }

    private static InstanceIdentifier<Node> getUniIid(String nodeId) {
        NodeId uniNodeId = new NodeId(new NodeId(nodeId));
        InstanceIdentifier<Node> uniNodeIid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(new Uri("unimgr:uni"))))
                .child(Node.class, new NodeKey(uniNodeId));
        return uniNodeIid;
    }

    public static DataTreeModification<Link> getEvcLink(final Link link) {
        final DataTreeIdentifier<Link> evcDataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, getEvcLinkIid("1"));
        final DataObjectModification<Link> evcDataTreeObj = new DataObjectModification<Link>() {

            @Override
            public Collection<DataObjectModification<? extends DataObject>> getModifiedChildren() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public <C extends Identifiable<K> & ChildOf<? super Link>, K extends Identifier<C>> DataObjectModification<C> getModifiedChildListItem(
                    Class<C> arg0, K arg1) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public <C extends ChildOf<? super Link>> DataObjectModification<C> getModifiedChildContainer(Class<C> arg0) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public DataObjectModification<? extends DataObject> getModifiedChild(PathArgument arg0) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public <C extends Augmentation<Link> & DataObject> DataObjectModification<C> getModifiedAugmentation(
                    Class<C> arg0) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public ModificationType getModificationType() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public PathArgument getIdentifier() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Class<Link> getDataType() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Link getDataBefore() {
                return link;
            }

            @Override
            public Link getDataAfter() {
                return link;
            }
        };

        DataTreeModification<Link> evcLink = new DataTreeModification<Link>() {
            @Override
            public DataTreeIdentifier<Link> getRootPath() {
                return evcDataTreeIid;
            }

            @Override
            public DataObjectModification<Link> getRootNode() {
                return evcDataTreeObj;
            }
        };
        return evcLink;
    }

    private static InstanceIdentifier<Link> getEvcLinkIid(String linkId) {
        LinkId evcLinkId = new LinkId(new LinkId("evc://" + linkId));
        InstanceIdentifier<Link> linkPath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class,new TopologyKey(new TopologyId(new Uri("unimgr:evc"))))
                .child(Link.class, new LinkKey(evcLinkId));
        return linkPath;
    }
}
