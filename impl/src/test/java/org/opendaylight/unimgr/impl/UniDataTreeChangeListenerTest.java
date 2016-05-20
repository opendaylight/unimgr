/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
public class UniDataTreeChangeListenerTest {

    private UniDataTreeChangeListener uniDataTreeChangeListener;

    @Before
    public void setUp() throws Exception {
        uniDataTreeChangeListener = mock(UniDataTreeChangeListener.class, Mockito.CALLS_REAL_METHODS);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUnimgrDataTreeChangeListener() {
        Collection<DataTreeModification<Node>> collection = new ArrayList<DataTreeModification<Node>>();
        DataTreeModification<Node> uni = getDataTreeNode(ModificationType.WRITE);
        collection.add(uni);
        uni = getDataTreeNode(ModificationType.DELETE);
        collection.add(uni);
        uni = getDataTreeNode(ModificationType.SUBTREE_MODIFIED);
        collection.add(uni);
        uniDataTreeChangeListener.onDataTreeChanged(collection);
        verify(uniDataTreeChangeListener, times(1)).add(any(DataTreeModification.class));
        verify(uniDataTreeChangeListener, times(1)).remove(any(DataTreeModification.class));
        verify(uniDataTreeChangeListener, times(1)).update(any(DataTreeModification.class));
    }

    private DataTreeModification<Node> getDataTreeNode(final ModificationType modificationType) {
        final DataObjectModification<Node> uniDataObjModification = new DataObjectModification<Node>() {
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
                return modificationType;
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
                // TODO Auto-generated method stub
                return null;
            }
            @Override
            public Node getDataAfter() {
                // TODO Auto-generated method stub
                return null;
            }
        };
        DataTreeModification<Node> modifiedUni = new DataTreeModification<Node>() {
            @Override
            public DataTreeIdentifier<Node> getRootPath() {
                return null;
            }
            @Override
            public DataObjectModification<Node> getRootNode() {
                return uniDataObjModification;
            }
        };
        return modifiedUni;
    }
}
