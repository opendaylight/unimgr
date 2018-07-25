/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.legato.global.l2cp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.unimgr.mef.legato.LegatoL2cpEecController;
import org.opendaylight.unimgr.mef.legato.util.LegatoConstants;
import org.opendaylight.unimgr.mef.legato.util.LegatoUtils;
import org.opendaylight.unimgr.mef.legato.utils.Constants;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.MefGlobal;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.L2cpEecProfiles;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.eec.profiles.Profile;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.eec.profiles.ProfileKey;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.Identifier1024;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.modules.junit4.PowerMockRunner;


@SuppressWarnings("deprecation")
@RunWith(PowerMockRunner.class)
public class LegatoL2cpEecProfileUnitTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private WriteTransaction transaction;
    @SuppressWarnings("rawtypes")
    @Mock
    private CheckedFuture checkedFuture;

    @Before
    public void setUp() throws Exception {
        mock(LegatoL2cpEecController.class, Mockito.CALLS_REAL_METHODS);
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testL2cpEecAddToOperationalDB() {
        final L2cpEecProfiles l2cpEecProfiles = mock(L2cpEecProfiles.class);
        final InstanceIdentifier<L2cpEecProfiles> instanceIdentifier =
                InstanceIdentifier.builder(MefGlobal.class).child(L2cpEecProfiles.class).build();

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).merge(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(L2cpEecProfiles.class));
        when(transaction.submit()).thenReturn(checkedFuture);
        LegatoUtils.addToOperationalDB(l2cpEecProfiles, instanceIdentifier, dataBroker);
        verify(transaction).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(L2cpEecProfiles.class));
        verify(transaction).submit();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testL2cpEecUpdateFromOperationalDB() throws ReadFailedException {
        final InstanceIdentifier<Profile> profileID =
                InstanceIdentifier.create(MefGlobal.class).child(L2cpEecProfiles.class)
                        .child(Profile.class, new ProfileKey(new Identifier1024(Constants.ONE)));

        ReadOnlyTransaction readTransaction = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readTransaction);
        CheckedFuture<Optional<Profile>, ReadFailedException> proFuture = mock(CheckedFuture.class);

        Optional<Profile> optProfile = mock(Optional.class);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(proFuture);
        when(proFuture.checkedGet()).thenReturn(optProfile);

        Optional<Profile> expectedOpt =
                (Optional<Profile>) LegatoUtils.readProfile(LegatoConstants.L2CP_EEC_PROFILES,
                        dataBroker, LogicalDatastoreType.CONFIGURATION, profileID);
        verify(readTransaction).read(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class));
        assertNotNull(expectedOpt);
        assertEquals(expectedOpt, optProfile);

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class));
        when(transaction.submit()).thenReturn(checkedFuture);
        assertEquals(true, LegatoUtils.deleteFromOperationalDB(profileID, dataBroker));
        verify(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction).submit();

        final L2cpEecProfiles l2cpEecProfiles = mock(L2cpEecProfiles.class);
        final InstanceIdentifier<L2cpEecProfiles> instanceIdentifier =
                InstanceIdentifier.builder(MefGlobal.class).child(L2cpEecProfiles.class).build();

        WriteTransaction transaction2 = Mockito.mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction2);
        doNothing().when(transaction2).merge(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(L2cpEecProfiles.class));
        when(transaction2.submit()).thenReturn(checkedFuture);
        LegatoUtils.addToOperationalDB(l2cpEecProfiles, instanceIdentifier, dataBroker);
        verify(transaction2).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(L2cpEecProfiles.class));
        verify(transaction2).submit();
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testL2cpEecDeleteFromOperationalDB() {
        final InstanceIdentifier<Profile> profileID =
                InstanceIdentifier.create(MefGlobal.class).child(L2cpEecProfiles.class)
                        .child(Profile.class, new ProfileKey(new Identifier1024(Constants.ONE)));

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class));
        when(transaction.submit()).thenReturn(checkedFuture);
        assertEquals(true, LegatoUtils.deleteFromOperationalDB(profileID, dataBroker));
        verify(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction).submit();

    }

}
