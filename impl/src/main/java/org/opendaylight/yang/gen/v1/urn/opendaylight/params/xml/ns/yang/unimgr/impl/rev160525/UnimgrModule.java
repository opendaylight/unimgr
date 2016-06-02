package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.impl.rev160525;

import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.unimgr.api.IUnimgrConsoleProvider;
import org.osgi.framework.BundleContext;

public class UnimgrModule extends AbstractUnimgrModule {

    private BundleContext ctx;

    public UnimgrModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public UnimgrModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, UnimgrModule oldModule, AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final WaitingServiceTracker<IUnimgrConsoleProvider> tracker = WaitingServiceTracker.create(
                IUnimgrConsoleProvider.class, ctx);
        final IUnimgrConsoleProvider provider = tracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);
        return provider;
    }

    @Override
    public boolean canReuseInstance(AbstractUnimgrModule oldModule) {
        return true;
    }

    public void setBundleContext(BundleContext ctx) {
        this.ctx = ctx;
    }
}
