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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Evc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;

public class UnimgrConsoleProviderTest implements IUnimgrConsoleProvider {

        private final List<UniAugmentation> list = new ArrayList<UniAugmentation>();
        private boolean firstTest = true;

        @Override
        public boolean addUni(UniAugmentation uni) {
            return (list.contains(uni)) ? false : list.add(uni);
        }

        @Override
        public boolean removeUni(IpAddress ipAddress) {
            firstTest = !firstTest;
            return !firstTest;
        }

        @Override
        public List<UniAugmentation> listUnis(LogicalDatastoreType dataStoreType) {
            if(firstTest){
                firstTest = false;
                return list;
            }
            // TODO add UniAugmentation

            return list;
        }

        public void setListUnis(int amount) {
            for(int i=0; i<amount; i++){
                final UniAugmentation uniAug = mock(UniAugmentation.class);
                when(uniAug.getIpAddress().getIpv4Address().getValue()).thenReturn("192.168.1.1");
                list.add(uniAug);
            }
        }

        @Override
        public UniAugmentation getUni(IpAddress ipAddress) {return list.get(0);}

        @Override
        public void close() throws Exception { }
        @Override
        public boolean removeEvc(String uuid) {return false;}
        @Override
        public Evc getEvc(String uuid) {return null;}
        @Override
        public boolean addEvc(EvcAugmentation evc) {return false;}

}
