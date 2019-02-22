/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.legato.evc;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.legato.LegatoServiceController;
import org.opendaylight.unimgr.mef.legato.util.LegatoUtils;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.Evc;
import org.opendaylight.yangtools.yang.binding.*;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Arif.Hussain@Xoriant.Com
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(LegatoUtils.class)
public class EvcDataTreeChangeListenerTest {

    private LegatoServiceController legatoServiceController;

    private static final Logger LOG = LoggerFactory
            .getLogger(EvcDataTreeChangeListenerTest.class);

    @Before
    public void setUp() throws Exception {
        legatoServiceController = mock(LegatoServiceController.class, Mockito.CALLS_REAL_METHODS);

        PowerMockito.mockStatic(LegatoUtils.class);


    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEvcServiceDataTreeChangeListener() {
        LOG.info("in side testEvcServiceDataTreeChangeListener() ");

        Collection<DataTreeModification<Evc>> collection = new ArrayList<DataTreeModification<Evc>>();
        DataTreeModification<Evc> evc = getDataTree(ModificationType.WRITE, null, mock(Evc.class));
        collection.add(evc);
        evc = getDataTree(ModificationType.DELETE, mock(Evc.class), null);
        collection.add(evc);
        evc = getDataTree(ModificationType.SUBTREE_MODIFIED, mock(Evc.class), mock(Evc.class));
        collection.add(evc);


        when(LegatoUtils.readEvc(any(DataBroker.class), any(LogicalDatastoreType.class), any())).thenReturn(Optional.absent());

        legatoServiceController.onDataTreeChanged(collection);
        verify(legatoServiceController, times(1)).add(any(DataTreeModification.class));
        verify(legatoServiceController, times(1)).remove(any(DataTreeModification.class));
        verify(legatoServiceController, times(1)).update(any(DataTreeModification.class));
    }


    private DataTreeModification<Evc> getDataTree(final ModificationType modificationType, Evc before, Evc after) {
        final DataObjectModification<Evc> evcDataObjModification = new DataObjectModification<Evc>() {
            @Override
            public Collection<DataObjectModification<? extends DataObject>> getModifiedChildren() {
                return Collections.emptyList();
            }

            @Override
            public <C extends Identifiable<K> & ChildOf<? super Evc>, K extends Identifier<C>> DataObjectModification<C> getModifiedChildListItem(
                    Class<C> arg0, K arg1) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public <C extends ChildOf<? super Evc>> DataObjectModification<C> getModifiedChildContainer(Class<C> arg0) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public DataObjectModification<? extends DataObject> getModifiedChild(PathArgument arg0) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public <C extends Augmentation<Evc> & DataObject> DataObjectModification<C> getModifiedAugmentation(
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
            public Class<Evc> getDataType() {
                return Evc.class;
            }

            @Override
            public Evc getDataBefore() {
                return before;
            }

            @Override
            public Evc getDataAfter() {
                return after;
            }
        };

        DataTreeModification<Evc> modifiedEvc = new DataTreeModification<Evc>() {
            @Override
            public DataTreeIdentifier<Evc> getRootPath() {
                return new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(Evc.class));
            }

            @Override
            public DataObjectModification<Evc> getRootNode() {
                return evcDataObjModification;
            }
        };

        return modifiedEvc;
    }

}
