/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.legato;

import java.util.Collections;
import java.util.Optional;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.unimgr.mef.legato.util.LegatoConstants;
import org.opendaylight.unimgr.mef.legato.util.LegatoUtils;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.MefGlobal;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.SlsProfiles;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.SlsProfilesBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.sls.profiles.Profile;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.sls.profiles.ProfileKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Arif.Hussain@Xoriant.Com
 */

public class LegatoSlsProfileController  extends UnimgrDataTreeChangeListener<Profile> {

    public LegatoSlsProfileController(final DataBroker dataBroker) {
        super(dataBroker);
        registerListener();
    }

    private static final Logger LOG = LoggerFactory
            .getLogger(LegatoSlsProfileController.class);


    private static final InstanceIdentifier<Profile> PROFILE_IID = InstanceIdentifier
            .builder(MefGlobal.class).child(SlsProfiles.class).child(Profile.class).build();

    private static final InstanceIdentifier<SlsProfiles> SLS_PROFILES_IID_OPERATIONAL = InstanceIdentifier
            .builder(MefGlobal.class).child(SlsProfiles.class).build();

    private ListenerRegistration<LegatoSlsProfileController> dataTreeChangeListenerRegistration;

    public void registerListener() {
        LOG.info("Initializing LegatoSlsProfileController:init() ");

        dataTreeChangeListenerRegistration = dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION, PROFILE_IID), this);

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


    public void addToOperationalDB(Profile profileObj) {
        LOG.info(" inside addNode()");
        try {
            assert profileObj != null;

            SlsProfiles profile = new SlsProfilesBuilder().setProfile(Collections.singletonList(profileObj)).build();
            LegatoUtils.addToOperationalDB(profile, SLS_PROFILES_IID_OPERATIONAL, dataBroker);
        } catch (Exception e) {
            LOG.error("Error in addNode(). Err: ", e);
        }
        LOG.info(" ********** END addNode() ****************** ");
    }


    @Override
    public void update(DataTreeModification<Profile> modifiedDataObject) {
        if (modifiedDataObject.getRootNode() != null && modifiedDataObject.getRootPath() != null) {
            LOG.info("  Node modified  " + modifiedDataObject.getRootNode().getIdentifier());
            try {
                assert modifiedDataObject.getRootNode().getDataAfter() != null;
                updateNode(modifiedDataObject.getRootNode().getDataAfter());
            } catch (Exception ex) {
                LOG.error("error: ", ex);
            }
        }
    }


    @SuppressWarnings("unchecked")
    public void updateNode(Profile profile) {
        LOG.info(" inside updateNode()");

        assert profile != null;
        Optional<Profile> optionalProfile =
                (Optional<Profile>) LegatoUtils.readProfile(LegatoConstants.SLS_PROFILES,
                        dataBroker, LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.create(MefGlobal.class).child(SlsProfiles.class)
                                .child(Profile.class, new ProfileKey(profile.getId())));

        if (optionalProfile.isPresent()) {

            LegatoUtils
                    .deleteFromOperationalDB(
                            InstanceIdentifier.create(MefGlobal.class).child(SlsProfiles.class)
                                    .child(Profile.class, new ProfileKey(profile.getId())),
                            dataBroker);

            addToOperationalDB(optionalProfile.get());
        }
        LOG.info(" ********** END updateNode() ****************** ");
    }


    @Override
    public void remove(DataTreeModification<Profile> removedDataObject) {
        if (removedDataObject.getRootNode() != null && removedDataObject.getRootPath() != null) {
            LOG.info("  Node removed  " + removedDataObject.getRootNode().getIdentifier());

            deleteNode(removedDataObject.getRootNode().getDataBefore());
        }
    }

    public void deleteNode(Profile profileObj) {
        LOG.info(" inside deleteNode()");

        try {
            assert profileObj != null;
            LegatoUtils
                    .deleteFromOperationalDB(
                            InstanceIdentifier.create(MefGlobal.class).child(SlsProfiles.class)
                                    .child(Profile.class, new ProfileKey(profileObj.getId())),
                            dataBroker);

        } catch (Exception ex) {
            LOG.error("error: ", ex);
        }

        LOG.info(" ********** END deleteNode() ****************** ");
    }

}
