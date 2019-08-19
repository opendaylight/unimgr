/*
 * Copyright (c) 2019 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.l2vpn.activator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.MountPointHelper;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivatorException;
import org.opendaylight.unimgr.mef.nrp.common.TapiUtils;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ConnectivityServiceEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ServiceType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.end.point.ServiceInterfacePoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


/**
 * @author Om.SAwasthi@Xoriant.Com
 *
 */
@PrepareForTest({AbstractL2vpnActivator.class, MountPointHelper.class})
@RunWith(PowerMockRunner.class)
public class AbstractL2vpnBridgeDomainActivatorTest {

    public static final String UUID1 = "sip:ciscoD1:GigabitEthernet0/0/0/1";
    public static final String UUID2 = "sip:ciscoD2:GigabitEthernet0/0/0/1";
    private EndPoint ep1;
    private EndPoint ep2;
    private AbstractL2vpnBridgeDomainActivator absl2vpnBridgeDomain;
    private ArrayList<EndPoint> endPoints;
    private String serviceId = "";
    private Boolean isExclusive;
    private ServiceType serviceType;
    private Optional<DataBroker> optBroker;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {

        absl2vpnBridgeDomain =
                Mockito.mock(AbstractL2vpnBridgeDomainActivator.class, Mockito.CALLS_REAL_METHODS);
        endPoints = new ArrayList<EndPoint>();
        isExclusive = true;
        serviceType = ServiceType.POINTTOPOINTCONNECTIVITY;

        ConnectivityServiceEndPoint cep =
                new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.EndPointBuilder()
                        .setServiceInterfacePoint(
                                TapiUtils.toSipRef(new Uuid(UUID1), ServiceInterfacePoint.class))
                        .setDirection(PortDirection.BIDIRECTIONAL).setRole(PortRole.LEAF).build();
        // .setbuild();

        ep1 = new EndPoint(cep, null);
        ConnectivityServiceEndPoint cep1 =
                new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.EndPointBuilder()
                        .setServiceInterfacePoint(
                                TapiUtils.toSipRef(new Uuid(UUID2), ServiceInterfacePoint.class))
                        .setDirection(PortDirection.BIDIRECTIONAL).setRole(PortRole.LEAF).build();
        ep2 = new EndPoint(cep1, null);
        endPoints.add(ep1);
        endPoints.add(ep2);
        optBroker = mock(Optional.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void activateTest() throws TransactionCommitFailedException, ResourceActivatorException {


        MemberModifier.suppress(MemberMatcher.method(MountPointHelper.class, "getDataBroker"));
        PowerMockito.mockStatic(MountPointHelper.class, Mockito.CALLS_REAL_METHODS);
        PowerMockito.mockStatic(MountPointHelper.class);
        PowerMockito.when(MountPointHelper.getDataBroker(Mockito.anyObject(), Mockito.anyString()))
                .thenReturn(optBroker);
        PowerMockito.when(optBroker.isPresent()).thenReturn(true);
        WriteTransaction transaction = Mockito.mock(WriteTransaction.class);
        DataBroker databroker = Mockito.mock(DataBroker.class);
        PowerMockito.when(optBroker.get()).thenReturn(databroker);
        PowerMockito.when(databroker.newWriteOnlyTransaction()).thenReturn(transaction);

        PowerMockito.doNothing().when(transaction).merge(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(InterfaceConfigurations.class));
        PowerMockito.doNothing().when(transaction).merge(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), Mockito.anyObject());
        @SuppressWarnings("rawtypes")
        CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
        PowerMockito.when(transaction.submit()).thenReturn(checkedFuture);

        absl2vpnBridgeDomain.activate(endPoints, serviceId, isExclusive, serviceType);

    }

    @Test
    public void deactivateTest()
            throws TransactionCommitFailedException, ResourceActivatorException {
        serviceId = "cs:16b9e18aa84:-364cc8e5";

        PowerMockito.when(absl2vpnBridgeDomain.getInnerName(serviceId)).thenReturn(serviceId);
        PowerMockito.when(absl2vpnBridgeDomain.getOuterName(serviceId)).thenReturn(serviceId);
        PowerMockito.mockStatic(MountPointHelper.class);
        PowerMockito.when(MountPointHelper.getDataBroker(Mockito.anyObject(), Mockito.anyString()))
                .thenReturn(optBroker);
        PowerMockito.when(optBroker.isPresent()).thenReturn(true);
        WriteTransaction transaction = Mockito.mock(WriteTransaction.class);
        DataBroker databroker = Mockito.mock(DataBroker.class);
        PowerMockito.when(optBroker.get()).thenReturn(databroker);
        PowerMockito.when(databroker.newWriteOnlyTransaction()).thenReturn(transaction);
        PowerMockito.doNothing().when(transaction).delete(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class));
        PowerMockito.doNothing().when(transaction).delete(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class));
        CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
        PowerMockito.when(transaction.submit()).thenReturn(checkedFuture);

        absl2vpnBridgeDomain.deactivate(endPoints, serviceId, isExclusive, serviceType);
    }

    @Test
    public void isSameInterfaceTest() {

        List<Uuid> ls = new ArrayList<Uuid>();
        assertFalse(absl2vpnBridgeDomain.isSameInterface(ep1, ls));

        ls.add(new Uuid(UUID1));
        assertTrue(absl2vpnBridgeDomain.isSameInterface(ep1, ls));

    }
}
