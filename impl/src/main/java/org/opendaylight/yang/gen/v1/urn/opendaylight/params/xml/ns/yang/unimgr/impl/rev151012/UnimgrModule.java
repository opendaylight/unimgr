package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.impl.rev151012;

import org.opendaylight.unimgr.impl.UnimgrProvider;

public class UnimgrModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.impl.rev151012.AbstractUnimgrModule {

    public UnimgrModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public UnimgrModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.impl.rev151012.UnimgrModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final UnimgrProvider unimgrProvider = new UnimgrProvider();
        getBrokerDependency().registerProvider(unimgrProvider);
        return unimgrProvider;
    }

}
