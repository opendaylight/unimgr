/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.cli;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.IUnimgrConsoleProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Evc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;

/**
 * @author jdavid
 *
 */
public class UnimgrConsoleProviderTest implements IUnimgrConsoleProvider {

        private final List<UniAugmentation> list = new ArrayList<UniAugmentation>();
        private boolean removed = false;

        @Override
        public boolean addUni(UniAugmentation uni) {
            return (list.contains(uni)) ? false : list.add(uni);
        }

        @Override
        public boolean removeUni(IpAddress ipAddress) {
            removed = !removed;
            return removed;
        }

        @Override
        public UniAugmentation getUni(IpAddress ipAddress) {return list.get(0);}

        @Override
        public void close() throws Exception { }
        @Override
        public boolean removeEvc(String uuid) {return false;}
        @Override
        public List<UniAugmentation> listUnis(LogicalDatastoreType dataStoreType) {return null;}
        @Override
        public Evc getEvc(String uuid) {return null;}
        @Override
        public boolean addEvc(EvcAugmentation evc) {return false;}

}
