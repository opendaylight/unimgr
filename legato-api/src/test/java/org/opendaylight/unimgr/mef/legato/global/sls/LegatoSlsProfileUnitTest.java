/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.legato.global.sls;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.legato.LegatoSlsProfileController;
import org.opendaylight.unimgr.mef.legato.util.LegatoConstants;
import org.opendaylight.unimgr.mef.legato.util.LegatoUtils;
import org.opendaylight.unimgr.mef.legato.utils.Constants;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.MefGlobal;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.SlsProfiles;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.sls.profiles.Profile;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.sls.profiles.ProfileKey;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.Identifier1024;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.util.concurrent.FluentFuture;


@RunWith(PowerMockRunner.class)
@PrepareForTest({Optional.class})
public class LegatoSlsProfileUnitTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private WriteTransaction transaction;
    @SuppressWarnings("rawtypes")
    @Mock
    private FluentFuture checkedFuture;

    @Before
    public void setUp() throws Exception {
        mock(LegatoSlsProfileController.class, Mockito.CALLS_REAL_METHODS);
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testSlsProfileAddToOperationalDB() {
        final SlsProfiles slsProfiles = mock(SlsProfiles.class);
        final InstanceIdentifier<SlsProfiles> instanceIdentifier =
                InstanceIdentifier.builder(MefGlobal.class).child(SlsProfiles.class).build();

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).merge(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(SlsProfiles.class));
        when(transaction.commit()).thenReturn(checkedFuture);
        LegatoUtils.addToOperationalDB(slsProfiles, instanceIdentifier, dataBroker);
        verify(transaction).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(SlsProfiles.class));
        verify(transaction).commit();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSlsProfileUpdateFromOperationalDB() throws InterruptedException, ExecutionException {
        final InstanceIdentifier<Profile> profileID =
                InstanceIdentifier.create(MefGlobal.class).child(SlsProfiles.class)
                        .child(Profile.class, new ProfileKey(new Identifier1024(Constants.ONE)));

        ReadTransaction readTransaction = mock(ReadTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readTransaction);
        FluentFuture<Optional<Profile>> proFuture = mock(FluentFuture.class);

        Optional<Profile> optProfile = PowerMockito.mock(Optional.class);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(proFuture);
        when(proFuture.get()).thenReturn(optProfile);

        Optional<Profile> expectedOpt =
                (Optional<Profile>) LegatoUtils.readProfile(LegatoConstants.SLS_PROFILES,
                        dataBroker, LogicalDatastoreType.CONFIGURATION, profileID);
        verify(readTransaction).read(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class));
        assertNotNull(expectedOpt);
        assertEquals(expectedOpt, optProfile);

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class));
        when(transaction.commit()).thenReturn(checkedFuture);
        assertEquals(true, LegatoUtils.deleteFromOperationalDB(profileID, dataBroker));
        verify(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction).commit();

        final SlsProfiles slsProfiles = mock(SlsProfiles.class);
        final InstanceIdentifier<SlsProfiles> instanceIdentifier =
                InstanceIdentifier.builder(MefGlobal.class).child(SlsProfiles.class).build();

        WriteTransaction transaction2 = Mockito.mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction2);
        doNothing().when(transaction2).merge(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(SlsProfiles.class));
        when(transaction2.commit()).thenReturn(checkedFuture);
        LegatoUtils.addToOperationalDB(slsProfiles, instanceIdentifier, dataBroker);
        verify(transaction2).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(SlsProfiles.class));
        verify(transaction2).commit();
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testSlsProfileDeleteFromOperationalDB() {
        final InstanceIdentifier<Profile> profileID =
                InstanceIdentifier.create(MefGlobal.class).child(SlsProfiles.class)
                        .child(Profile.class, new ProfileKey(new Identifier1024(Constants.ONE)));

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class));
        when(transaction.commit()).thenReturn(checkedFuture);
        assertEquals(true, LegatoUtils.deleteFromOperationalDB(profileID, dataBroker));
        verify(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction).commit();
    }

}
