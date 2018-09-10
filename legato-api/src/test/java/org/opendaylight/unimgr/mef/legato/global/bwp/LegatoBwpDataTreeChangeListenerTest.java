/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.legato.global.bwp;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.unimgr.mef.legato.LegatoBwpProfileController;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.bwp.flow.parameter.profiles.Profile;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author Arif.Hussain@Xoriant.Com
 *
 */
@RunWith(PowerMockRunner.class)
public class LegatoBwpDataTreeChangeListenerTest {

    private LegatoBwpProfileController legatoBwpProfileController;

    @Before
    public void setUp() throws Exception {
        legatoBwpProfileController = mock(LegatoBwpProfileController.class, Mockito.CALLS_REAL_METHODS);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBwpDataTreeChangeListener() {
        Collection<DataTreeModification<Profile>> collection = new ArrayList<DataTreeModification<Profile>>();
        DataTreeModification<Profile> profile = getDataTree(ModificationType.WRITE);
        collection.add(profile);
        profile = getDataTree(ModificationType.DELETE);
        collection.add(profile);
        profile = getDataTree(ModificationType.SUBTREE_MODIFIED);
        collection.add(profile);
        legatoBwpProfileController.onDataTreeChanged(collection);
        verify(legatoBwpProfileController, times(1)).add(any(DataTreeModification.class));
        verify(legatoBwpProfileController, times(1)).remove(any(DataTreeModification.class));
        verify(legatoBwpProfileController, times(1)).update(any(DataTreeModification.class));
    }

    private DataTreeModification<Profile> getDataTree(final ModificationType modificationType) {
        final DataObjectModification<Profile> proDataObjModification = new DataObjectModification<Profile>() {
            @Override
            public Collection<DataObjectModification<? extends DataObject>> getModifiedChildren() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public <C extends Identifiable<K> & ChildOf<? super Profile>, K extends Identifier<C>> DataObjectModification<C> getModifiedChildListItem(
                    Class<C> arg0, K arg1) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public <C extends ChildOf<? super Profile>> DataObjectModification<C> getModifiedChildContainer(Class<C> arg0) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public DataObjectModification<? extends DataObject> getModifiedChild(PathArgument arg0) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public <C extends Augmentation<Profile> & DataObject> DataObjectModification<C> getModifiedAugmentation(
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
            public Class<Profile> getDataType() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Profile getDataBefore() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Profile getDataAfter() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public <C extends ChildOf<? super Profile>> Collection<DataObjectModification<C>> getModifiedChildren(
                    Class<C> childType) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public <H extends ChoiceIn<? super Profile> & DataObject, C extends ChildOf<? super H>> Collection<DataObjectModification<C>> getModifiedChildren(
                    Class<H> caseType, Class<C> childType) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public <H extends ChoiceIn<? super Profile> & DataObject, C extends ChildOf<? super H>> DataObjectModification<C> getModifiedChildContainer(
                    Class<H> caseType, Class<C> child) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public <H extends ChoiceIn<? super Profile> & DataObject, C extends Identifiable<K> & ChildOf<? super H>, K extends Identifier<C>> DataObjectModification<C> getModifiedChildListItem(
                    Class<H> caseType, Class<C> listItem, K listKey) {
                // TODO Auto-generated method stub
                return null;
            }
        };

        DataTreeModification<Profile> modifiedPro = new DataTreeModification<Profile>() {
            @Override
            public DataTreeIdentifier<Profile> getRootPath() {
                return null;
            }

            @Override
            public DataObjectModification<Profile> getRootNode() {
                return proDataObjModification;
            }
        };

        return modifiedPro;
    }

}
