/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.netvirt;

import java.util.Collection;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.Uni;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

public class TestHelper {
    public static final DataTreeModification<Uni> getUni(final Uni before, final Uni after, ModificationType modificationType) {
        final DataTreeIdentifier<Uni> uniDataTreeIid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, MefUtils.getEvcUniInstanceIdentifier("testUni"));
        final DataObjectModification<Uni> uniDataTreeObj = new DataObjectModification<Uni>() {
            @Override
            public Collection<DataObjectModification<? extends DataObject>> getModifiedChildren() {
                // TODO Auto-generated method stub
                return null;
            }
            @Override
            public <C extends Identifiable<K> & ChildOf<? super Uni>, K extends Identifier<C>> DataObjectModification<C> getModifiedChildListItem(
                    Class<C> arg0, K arg1) {
                // TODO Auto-generated method stub
                return null;
            }
            @Override
            public <C extends ChildOf<? super Uni>> DataObjectModification<C> getModifiedChildContainer(Class<C> arg0) {
                // TODO Auto-generated method stub
                return null;
            }
            @Override
            public DataObjectModification<? extends DataObject> getModifiedChild(PathArgument arg0) {
                // TODO Auto-generated method stub
                return null;
            }
            @Override
            public <C extends Augmentation<Uni> & DataObject> DataObjectModification<C> getModifiedAugmentation(
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
            public Class<Uni> getDataType() {
                // TODO Auto-generated method stub
                return null;
            }
            @Override
            public Uni getDataBefore() {
                return before;
            }
            @Override
            public Uni getDataAfter() {
                return after;
            }
        };
        DataTreeModification<Uni> uniUni = new DataTreeModification<Uni>() {
            @Override
            public DataTreeIdentifier<Uni> getRootPath() {
                return uniDataTreeIid;
            }
            @Override
            public DataObjectModification<Uni> getRootNode() {
                return uniDataTreeObj;
            }
        };
        return uniUni;
    }
}
