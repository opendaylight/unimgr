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
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.BwpFlowParameterProfiles;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.BwpFlowParameterProfilesBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.bwp.flow.parameter.profiles.Profile;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.bwp.flow.parameter.profiles.ProfileKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author sanket.shirode@Xoriant.com
 */
public class LegatoBwpProfileController extends UnimgrDataTreeChangeListener<Profile> {

    public LegatoBwpProfileController(DataBroker dataBroker) {
        super(dataBroker);
        registerListener();
    }

    private static final Logger LOG = LoggerFactory.getLogger(LegatoBwpProfileController.class);
    private static final InstanceIdentifier<Profile> BWP_PROFILE_IID = InstanceIdentifier.builder(MefGlobal.class)
            .child(BwpFlowParameterProfiles.class).child(Profile.class).build();
    private ListenerRegistration<LegatoBwpProfileController> dataTreeChangeListenerRegistration;

    private void registerListener() {
        LOG.info("Initializing LegatoSlsProfileController:init() ");
        dataTreeChangeListenerRegistration =
                dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(
                        LogicalDatastoreType.CONFIGURATION, BWP_PROFILE_IID), this);

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
            BwpFlowParameterProfiles bwpProfiles = new BwpFlowParameterProfilesBuilder()
                    .setProfile(Collections.singletonList(profile)).build();
            InstanceIdentifier<BwpFlowParameterProfiles> profilesTx = InstanceIdentifier
                    .create(MefGlobal.class).child(BwpFlowParameterProfiles.class);
            LegatoUtils.addToOperationalDB(bwpProfiles, profilesTx, dataBroker);
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
        try {
            assert profile != null;
            LegatoUtils.deleteFromOperationalDB(
                    InstanceIdentifier.create(MefGlobal.class).child(BwpFlowParameterProfiles.class)
                            .child(Profile.class, new ProfileKey(profile.getId())),
                    dataBroker);

        } catch (Exception ex) {
            LOG.error("error: ", ex);
        }
    }


    @Override
    public void update(DataTreeModification<Profile> modifiedDataObject) {
        // TODO Auto-generated method stub

        if (modifiedDataObject.getRootNode() != null && modifiedDataObject.getRootPath() != null) {
            LOG.info("  Node modified  " + modifiedDataObject.getRootNode().getIdentifier());

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
        InstanceIdentifier<Profile> instanceIdentifier =
                InstanceIdentifier.create(MefGlobal.class).child(BwpFlowParameterProfiles.class)
                        .child(Profile.class, new ProfileKey(profile.getId()));
        Optional<Profile> optionalProfile =
                (Optional<Profile>) LegatoUtils.readProfile(LegatoConstants.BWP_PROFILES,
                        dataBroker, LogicalDatastoreType.CONFIGURATION, instanceIdentifier);
        if (optionalProfile.isPresent()) {
            LegatoUtils.deleteFromOperationalDB(instanceIdentifier, dataBroker);
            addToOperationalDB(optionalProfile.get());
        }

    }

}
