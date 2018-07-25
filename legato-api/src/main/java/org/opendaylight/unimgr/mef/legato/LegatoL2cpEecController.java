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
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.L2cpEecProfiles;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.L2cpEecProfilesBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.eec.profiles.Profile;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.eec.profiles.ProfileKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Arif.Hussain@Xoriant.Com
 *
 */
public class LegatoL2cpEecController  extends UnimgrDataTreeChangeListener<Profile> {

    private static final Logger LOG = LoggerFactory.getLogger(LegatoL2cpEecController.class);

    private static final InstanceIdentifier<Profile> PROFILE_ID = InstanceIdentifier
            .builder(MefGlobal.class).child(L2cpEecProfiles.class).child(Profile.class).build();

    private static final InstanceIdentifier<L2cpEecProfiles> L2CP_EEC_PROFILES_ID_OPERATIONAL = InstanceIdentifier
            .builder(MefGlobal.class).child(L2cpEecProfiles.class).build();

    private ListenerRegistration<LegatoL2cpEecController> dataTreeChangeListenerRegistration;

    public LegatoL2cpEecController(DataBroker dataBroker) {
        super(dataBroker);
        registerListener();
    }

    public void registerListener() {
        LOG.info("Initializing LegatoL2cpEecController:init() ");

        dataTreeChangeListenerRegistration = dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<Profile>(LogicalDatastoreType.CONFIGURATION, PROFILE_ID), this);
    }

    @Override
    public void add(DataTreeModification<Profile> newDataObject) {
        LOG.info("ClassName :: LegatoL2cpEecController, Method:: add(), Message:: Node Added  "
                + newDataObject.getRootNode().getIdentifier());
        addToOperationalDB(newDataObject.getRootNode().getDataAfter());
    }

    public void addToOperationalDB(Profile profileObj) {

        LOG.info(" inside addNode()");

        try {
            assert profileObj != null;

            L2cpEecProfiles l2cpEecProfiles = new L2cpEecProfilesBuilder()
                    .setProfile(Collections.singletonList(profileObj)).build();
            LegatoUtils.addToOperationalDB(l2cpEecProfiles, L2CP_EEC_PROFILES_ID_OPERATIONAL,
                    dataBroker);
        } catch (Exception e) {
            LOG.error("Error in addNode(). Err: ", e);
        }
        LOG.info(" ********** END addNode() ****************** ");
    }


    @Override
    public void update(DataTreeModification<Profile> modifiedDataObject) {
        if (modifiedDataObject.getRootNode() != null && modifiedDataObject.getRootPath() != null) {
            LOG.info("ClassName :: LegatoL2cpEecController, Method:: update(), Message:: Node modified "
                    + modifiedDataObject.getRootNode().getIdentifier());

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
        Optional<Profile> optionalProfile =
                (Optional<Profile>) LegatoUtils.readProfile(LegatoConstants.L2CP_EEC_PROFILES,
                        dataBroker, LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.create(MefGlobal.class).child(L2cpEecProfiles.class)
                                .child(Profile.class, new ProfileKey(profile.getId())));

        if (optionalProfile.isPresent()) {

            LegatoUtils.deleteFromOperationalDB(InstanceIdentifier.create(MefGlobal.class).child(L2cpEecProfiles.class)
                    .child(Profile.class, new ProfileKey(profile.getId())), dataBroker);

            addToOperationalDB(optionalProfile.get());
        }
    }


    @Override
    public void remove(DataTreeModification<Profile> removedDataObject) {
        if (removedDataObject.getRootNode() != null && removedDataObject.getRootPath() != null) {
            LOG.info(
                    "ClassName :: LegatoL2cpEecController, Method:: remove(), Message:: Node removed  "
                            + removedDataObject.getRootNode().getIdentifier());

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
            LegatoUtils
                    .deleteFromOperationalDB(
                            InstanceIdentifier.create(MefGlobal.class).child(L2cpEecProfiles.class)
                                    .child(Profile.class, new ProfileKey(profile.getId())),
                            dataBroker);
        } catch (Exception ex) {
            LOG.error("error: ", ex);
        }
    }

    @Override
    public void close() throws Exception {
        if (dataTreeChangeListenerRegistration != null) {
            dataTreeChangeListenerRegistration.close();
        }
    }


}
