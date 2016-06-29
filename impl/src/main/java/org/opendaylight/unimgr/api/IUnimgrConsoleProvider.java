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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.Evc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSource;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface IUnimgrConsoleProvider extends AutoCloseable {

    boolean addUni(UniAugmentation uni);

    boolean updateUni(UniAugmentation uni);

    boolean removeUni(IpAddress ipAddress);

    List<UniAugmentation> listUnis(LogicalDatastoreType dataStoreType);

    UniAugmentation getUni(IpAddress ipAddress);

    boolean removeEvc(String uuid);

    boolean addEvc(EvcAugmentation evc);

    Evc getEvc(String uuid);

    boolean updateEvc(InstanceIdentifier<Link> evcKey, EvcAugmentation evc, UniSource uniSource, UniDest uniDest);
}
