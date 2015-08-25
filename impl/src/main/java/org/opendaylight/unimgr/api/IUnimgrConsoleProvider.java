/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.api;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev150622.evcs.Evc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev150622.unis.Uni;

public interface IUnimgrConsoleProvider extends AutoCloseable {

    public boolean addUni(Uni uni);

    public boolean removeUni(String uuid);

    public List<Uni> listUnis(boolean isConfigurationDatastore);

    public Uni getUni(String uuid);

    public boolean removeEvc(String uuid);

    public boolean addEvc(Evc evc);

    public Evc getEvc(String uuid);
}
