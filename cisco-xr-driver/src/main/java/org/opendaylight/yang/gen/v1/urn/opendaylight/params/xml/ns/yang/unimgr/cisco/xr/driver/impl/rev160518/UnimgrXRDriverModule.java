package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.cisco.xr.driver.impl.rev160518;

import org.mef.nrp.cisco.xr.CiscoXRDriverProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;

public class UnimgrXRDriverModule extends
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.cisco.xr.driver.impl.rev160518.AbstractUnimgrXRDriverModule {

    public UnimgrXRDriverModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public UnimgrXRDriverModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.cisco.xr.driver.impl.rev160518.UnimgrXRDriverModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

        final CiscoXRDriverProvider provider = new CiscoXRDriverProvider();

        BindingAwareBroker broker = getBrokerDependency();
        broker.registerProvider(provider);

        return provider;
    }
}
