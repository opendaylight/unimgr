/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vcpe.impl.rev150622;

import org.opendaylight.vcpe.impl.VcpeProvider;

public class VcpeModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vcpe.impl.rev150622.AbstractVcpeModule {
    public VcpeModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public VcpeModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vcpe.impl.rev150622.VcpeModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final VcpeProvider vcpeProvider = new VcpeProvider();
        getBrokerDependency().registerProvider(vcpeProvider);
        return vcpeProvider;
    }

}
