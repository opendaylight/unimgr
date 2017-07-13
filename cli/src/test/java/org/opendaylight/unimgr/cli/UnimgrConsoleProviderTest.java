/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.cli;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.IUnimgrConsoleProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Evc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSource;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class UnimgrConsoleProviderTest implements IUnimgrConsoleProvider {

    private final List<UniAugmentation> listUni = new ArrayList<UniAugmentation>();
    private final List<EvcAugmentation> listEvc = new ArrayList<EvcAugmentation>();
    private boolean firstTest = true;

    @Override
    public boolean addUni(UniAugmentation uni) {
        return (listUni.contains(uni)) ? false : listUni.add(uni);
    }

    @Override
    public boolean removeUni(IpAddress ipAddress) {
        firstTest = !firstTest;
        return !firstTest;
    }

    @Override
    public List<UniAugmentation> listUnis(LogicalDatastoreType dataStoreType) {
        return listUni;
    }

    public void setListUnis(int amount, String ipAddress) {
        for (int i=0; i<amount; i++) {
            final UniAugmentation uniAug = mock(UniAugmentation.class);
            final IpAddress ipAddr = mock(IpAddress.class);
            final Ipv4Address ip4 = mock(Ipv4Address.class);
            when(uniAug.getIpAddress()).thenReturn(ipAddr);
            when(ipAddr.getIpv4Address()).thenReturn(ip4);
            when(ip4.getValue()).thenReturn(ipAddress);
            listUni.add(uniAug);
        }
    }

    @Override
    public UniAugmentation getUni(IpAddress ipAddress) {
        return (listUni.isEmpty()) ? null : listUni.get(0);
    }

    @Override
    public boolean addEvc(EvcAugmentation evc) {
        return (listEvc.contains(evc)) ? false : listEvc.add(evc);
    }

    @Override
    public Evc getEvc(String uuid) {
        return (listEvc.isEmpty()) ? null : listEvc.get(0);
    }

    @Override
    public boolean removeEvc(String uuid) {
        firstTest = !firstTest;
        return !firstTest;
    }

    @Override
    public void close() throws Exception { }

    @Override
    public boolean updateUni(UniAugmentation uni) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean updateEvc(InstanceIdentifier<Link> evcKey, EvcAugmentation evc, UniSource uniSource,
            UniDest uniDest) {
        // TODO Auto-generated method stub
        return false;
    }

}
