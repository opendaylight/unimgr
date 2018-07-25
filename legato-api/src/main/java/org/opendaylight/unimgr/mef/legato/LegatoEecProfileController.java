/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.legato;

import com.google.common.base.Optional;

import java.util.Collections;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.unimgr.mef.legato.util.LegatoConstants;
import org.opendaylight.unimgr.mef.legato.util.LegatoUtils;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.MefGlobal;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.EecProfiles;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.EecProfilesBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.eec.profiles.Profile;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.eec.profiles.ProfileKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author sanket.shirode@Xoriant.com
 */

public class LegatoEecProfileController extends UnimgrDataTreeChangeListener<Profile> {

    private static final Logger LOG = LoggerFactory.getLogger(LegatoEecProfileController.class);
    private ListenerRegistration<LegatoEecProfileController> dataTreeChangeListenerRegistration;
    private static final InstanceIdentifier<Profile> EEC_PROFILE_IID = InstanceIdentifier.builder(MefGlobal.class)
            .child(EecProfiles.class).child(Profile.class).build();

    public LegatoEecProfileController(DataBroker dataBroker) {
        super(dataBroker);
        registerListener();
    }

    private void registerListener() {
        LOG.info("Initializing LegatoSlsProfileController:init() ");

        dataTreeChangeListenerRegistration = dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<Profile>(LogicalDatastoreType.CONFIGURATION, EEC_PROFILE_IID), this);

    }

    @Override
    public void close() throws Exception {
        if (dataTreeChangeListenerRegistration != null) {
            dataTreeChangeListenerRegistration.close();
        }
    }

    @Override
    public void add(DataTreeModification<Profile> newDataObject) {
        LOG.info("  Node Added  " + newDataObject.getRootNode().getIdentifier());
        addToOperationalDB(newDataObject.getRootNode().getDataAfter());
    }

    public void addToOperationalDB(Profile profile) {
        try {
            assert profile != null;
            EecProfiles eecProfiles = new EecProfilesBuilder().setProfile(Collections.singletonList(profile)).build();
            InstanceIdentifier<EecProfiles> profilesTx = InstanceIdentifier.create(MefGlobal.class)
                    .child(EecProfiles.class);
            LegatoUtils.addToOperationalDB(eecProfiles, profilesTx, dataBroker);
        } catch (Exception ex) {
            LOG.error("error: ", ex);
        }
    }

    @Override
    public void remove(DataTreeModification<Profile> removedDataObject) {
        if (removedDataObject.getRootNode() != null && removedDataObject.getRootPath() != null) {
            LOG.info("  Node removed  " + removedDataObject.getRootNode().getIdentifier());
            try {
                assert removedDataObject.getRootNode().getDataBefore() != null;
                deleteFromOperationalDB(removedDataObject.getRootNode().getDataBefore());
            } catch (Exception ex) {
                LOG.error("error: ", ex);
            }
        }
    }

    public void deleteFromOperationalDB(Profile profile) {
        assert profile != null;
        LegatoUtils.deleteFromOperationalDB(
                InstanceIdentifier.create(MefGlobal.class).child(EecProfiles.class)
                        .child(Profile.class, new ProfileKey(profile.getId())), dataBroker);
    }


    @Override
    public void update(DataTreeModification<Profile> modifiedDataObject) {
        if (modifiedDataObject.getRootNode() != null && modifiedDataObject.getRootPath() != null) {
            LOG.info("  Node modified  " + modifiedDataObject.getRootNode().getIdentifier());
            LOG.info(" inside updateNode()");
            try {
                assert modifiedDataObject.getRootNode().getDataAfter() != null;
                updateFromOperationalDB(modifiedDataObject.getRootNode().getDataAfter());
            } catch (Exception ex) {
                LOG.error("error: ", ex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void updateFromOperationalDB(Profile profile) {
        assert profile != null;
        InstanceIdentifier<Profile> instanceIdentifier = InstanceIdentifier.create(MefGlobal.class)
                .child(EecProfiles.class).child(Profile.class, new ProfileKey(profile.getId()));
        Optional<Profile> optionalProfile =
                (Optional<Profile>) LegatoUtils.readProfile(LegatoConstants.EEC_PROFILES,
                        dataBroker, LogicalDatastoreType.CONFIGURATION, instanceIdentifier);
        if (optionalProfile.isPresent()) {
            LegatoUtils.deleteFromOperationalDB(instanceIdentifier, dataBroker);
            addToOperationalDB(optionalProfile.get());
        }
    }

}
