/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.api;

import java.util.List;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Evc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;

public interface IUnimgrConsoleProvider extends AutoCloseable {

    boolean addUni(UniAugmentation uni);

    boolean removeUni(IpAddress ipAddress);

    List<UniAugmentation> listUnis(LogicalDatastoreType dataStoreType);

    UniAugmentation getUni(IpAddress ipAddress);

    boolean removeEvc(String uuid);

    boolean addEvc(EvcAugmentation evc);

    Evc getEvc(String uuid);
}
