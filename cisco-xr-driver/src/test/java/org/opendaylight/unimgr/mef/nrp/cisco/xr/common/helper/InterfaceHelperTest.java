/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util.MtuUtils;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations._interface.configuration.Mtus;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations._interface.configuration.mtus.Mtu;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.InterfaceConfiguration3;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;


/*
 * @author krzysztof.bijakowski@amartus.com
 */
public class InterfaceHelperTest {

    @Test
    public void testGetInterfaceName() {
        //given
        String interfaceName = "GigabitEthernet0/0/1";

        TpId tp = Mockito.mock(TpId.class);
        Mockito.when(tp.getValue()).thenReturn(interfaceName);

        ServicePort port = Mockito.mock(ServicePort.class);
        Mockito.when(port.getTp()).thenReturn(tp);

        InterfaceName expected = new InterfaceName(interfaceName);

        //when
        InterfaceName actual = InterfaceHelper.getInterfaceName(port);

        //then
        assertEquals(expected, actual);
    }

    @Test
    public void testGetInterfaceNameSplit() {
        //given
        String interfaceName = "xxxxxxxx:GigabitEthernet0/0/1";

        TpId tp = Mockito.mock(TpId.class);
        Mockito.when(tp.getValue()).thenReturn(interfaceName);

        ServicePort port = Mockito.mock(ServicePort.class);
        Mockito.when(port.getTp()).thenReturn(tp);

        InterfaceName expected = new InterfaceName("GigabitEthernet0/0/1");

        //when
        InterfaceName actual = InterfaceHelper.getInterfaceName(port);

        //then
        assertEquals(expected, actual);
    }

    @Test
    public void testBuildSingle() {
        //given
        String interfaceNameValue = "GigabitEthernet0/0/1";
        InterfaceName interfaceName = new InterfaceName(interfaceNameValue);
        Optional<Mtus> mtus = Optional.empty();
        boolean setL2Transport = false;

        InterfaceHelper interfaceHelper = new InterfaceHelper();

        //when
        //interfaceHelper.addInterface(interfaceName, mtus, setL2Transport);
        InterfaceConfigurations actual = interfaceHelper.build();

        //then
        assertNotNull(actual);

        List<InterfaceConfiguration> actualInterfaceConfigurationList = actual.getInterfaceConfiguration();
        assertNotNull(actualInterfaceConfigurationList);
        assertEquals(1, actualInterfaceConfigurationList.size());

        InterfaceConfiguration actualInterfaceConfiguration = actualInterfaceConfigurationList.get(0);

        assertNotNull(actualInterfaceConfiguration);
        assertEquals(interfaceName, actualInterfaceConfiguration.getInterfaceName());
        assertNull(actualInterfaceConfiguration.getMtus());
        assertNull(actualInterfaceConfiguration.augmentation(InterfaceConfiguration3.class));
    }

    @Test
    public void testBuildSingleMtuL2() {
        //given
        String interfaceNameValue = "GigabitEthernet0/0/1";
        CiscoIosXrString owner = new CiscoIosXrString("testAddCeps");
        long mtuValue = 1522L;
        InterfaceName interfaceName = new InterfaceName(interfaceNameValue);
        Optional<Mtus> mtus = Optional.of(MtuUtils.generateMtus(mtuValue, owner));
        boolean setL2Transport = true;

        InterfaceHelper interfaceHelper = new InterfaceHelper();

        //when
        //interfaceHelper.addInterface(interfaceName, mtus, setL2Transport);
        InterfaceConfigurations actual = interfaceHelper.build();

        //then
        assertNotNull(actual);

        List<InterfaceConfiguration> actualInterfaceConfigurationList = actual.getInterfaceConfiguration();
        assertNotNull(actualInterfaceConfigurationList);
        assertEquals(1, actualInterfaceConfigurationList.size());

        InterfaceConfiguration actualInterfaceConfiguration = actualInterfaceConfigurationList.get(0);

        assertNotNull(actualInterfaceConfiguration);
        assertEquals(interfaceName, actualInterfaceConfiguration.getInterfaceName());

        Mtus actualMtus = actualInterfaceConfiguration.getMtus();
        assertNotNull(actualMtus);
        List<Mtu> actualMtuList = actualMtus.getMtu();
        assertNotNull(actualMtuList);
        assertEquals(1, actualMtuList.size());
        assertNotNull(actualMtuList.get(0));
        Mtu actualMtu = actualMtuList.get(0);
        assertEquals(mtuValue, actualMtu.getMtu().longValue());
        assertEquals(owner, actualMtu.getOwner());

        InterfaceConfiguration3 l2Configuration =
                actualInterfaceConfiguration.augmentation(InterfaceConfiguration3.class);
        assertNotNull(l2Configuration);
        assertNotNull(l2Configuration.getL2Transport());
        assertNotNull(l2Configuration.getL2Transport().getEnabled());
    }

    @Test
    public void testBuildMultiple() {
        //given
        String interfaceNameValue1 = "GigabitEthernet0/0/1";
        InterfaceName interfaceName1 = new InterfaceName(interfaceNameValue1);
        String interfaceNameValue2 = "GigabitEthernet0/0/2";
        InterfaceName interfaceName2 = new InterfaceName(interfaceNameValue2);
        Optional<Mtus> mtus = Optional.empty();
        boolean setL2Transport = false;

        InterfaceHelper interfaceHelper = new InterfaceHelper();

        //when
        //interfaceHelper.addInterface(interfaceName1, mtus, setL2Transport);
        //interfaceHelper.addInterface(interfaceName2, mtus, setL2Transport);
        InterfaceConfigurations actual = interfaceHelper.build();

        //then
        assertNotNull(actual);

        List<InterfaceConfiguration> actualInterfaceConfigurationList = actual.getInterfaceConfiguration();
        assertNotNull(actualInterfaceConfigurationList);
        assertEquals(2, actualInterfaceConfigurationList.size());

        InterfaceConfiguration actualInterfaceConfiguration = actualInterfaceConfigurationList.get(0);

        assertNotNull(actualInterfaceConfiguration);
        assertEquals(interfaceName1, actualInterfaceConfiguration.getInterfaceName());
        assertNull(actualInterfaceConfiguration.getMtus());
        assertNull(actualInterfaceConfiguration.augmentation(InterfaceConfiguration3.class));

        actualInterfaceConfiguration = actualInterfaceConfigurationList.get(1);

        assertNotNull(actualInterfaceConfiguration);
        assertEquals(interfaceName2, actualInterfaceConfiguration.getInterfaceName());
        assertNull(actualInterfaceConfiguration.getMtus());
        assertNull(actualInterfaceConfiguration.augmentation(InterfaceConfiguration3.class));
    }
}
