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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class EvcDataTreeChangeListenerTest {

    private EvcDataTreeChangeListener evcDataTreeChangeListener;

    @Before
    public void setUp() throws Exception {
        evcDataTreeChangeListener = mock(EvcDataTreeChangeListener.class, Mockito.CALLS_REAL_METHODS);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEvcmgrDataTreeChangeListener() {
        Collection<DataTreeModification<Link>> collection = new ArrayList<DataTreeModification<Link>>();
        DataTreeModification<Link> evc = getDataTreeLink(ModificationType.WRITE);
        collection.add(evc);
        evc = getDataTreeLink(ModificationType.DELETE);
        collection.add(evc);
        evc = getDataTreeLink(ModificationType.SUBTREE_MODIFIED);
        collection.add(evc);
        evcDataTreeChangeListener.onDataTreeChanged(collection);
        verify(evcDataTreeChangeListener, times(1)).add(any(DataTreeModification.class));
        verify(evcDataTreeChangeListener, times(1)).remove(any(DataTreeModification.class));
        verify(evcDataTreeChangeListener, times(1)).update(any(DataTreeModification.class));
    }

    private DataTreeModification<Link> getDataTreeLink(final ModificationType modificationType) {
        final DataObjectModification<Link> evcDataObjModification = new DataObjectModification<Link>() {
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
                return modificationType;
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
                // TODO Auto-generated method stub
                return null;
            }
            @Override
            public Link getDataAfter() {
                // TODO Auto-generated method stub
                return null;
            }
        };
        DataTreeModification<Link> modifiedEvc = new DataTreeModification<Link>() {
            @Override
            public DataTreeIdentifier<Link> getRootPath() {
                return null;
            }
            @Override
            public DataObjectModification<Link> getRootNode() {
                return evcDataObjModification;
            }
        };
        return modifiedEvc;
    }

}
