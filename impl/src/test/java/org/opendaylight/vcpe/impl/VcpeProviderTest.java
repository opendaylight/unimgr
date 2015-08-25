/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.vcpe.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.vcpe.command.TransactionInvoker;
import org.osgi.framework.FrameworkUtil;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest({FrameworkUtil.class})
public class VcpeProviderTest {

    @Mock private UniDataChangeListener vcpeDataChangeListener;
    @Mock private EvcDataChangeListener evcDataChangeListener;
    @Mock private VcpeDataChangeListener listener;
    @Mock private TransactionInvoker invoker;
    @Mock private DataBroker dataBroker;

    @Test
    public void testClose() throws Exception {
//        PowerMockito.mockStatic(FrameworkUtil.class);
//        BundleContext context = mock(BundleContext.class);
//        //ServiceRegistration registration = mock(ServiceRegistration.class);
//        VcpeProvider provider = new VcpeProvider();
//        Bundle bundle = mock(Bundle.class);
//        PowerMockito.when(FrameworkUtil.getBundle(any(Class.class))).thenReturn(bundle);
//        when(bundle.getBundleContext()).thenReturn(context);
//        provider.onSessionInitiated(mock(BindingAwareBroker.ProviderContext.class));
//        provider.close();
    }
}
