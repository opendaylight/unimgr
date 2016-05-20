package org.mef.nrp.cisco.xr;

import org.mef.nrp.impl.ActivationDriverRepoService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public class CiscoXRDriverProvider implements BindingAwareProvider, AutoCloseable {

    public CiscoXRDriverProvider() {
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {

        final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        ServiceReference<ActivationDriverRepoService> serviceRef =
                context.getServiceReference(ActivationDriverRepoService.class);
        ActivationDriverRepoService driverRepo = context.getService(serviceRef);

        L2vpnXconnectDriverBuilder l2vpnXconnectDriverBuilder = new L2vpnXconnectDriverBuilder();
        l2vpnXconnectDriverBuilder.onSessionInitialized(session);
        driverRepo.bindBuilder(l2vpnXconnectDriverBuilder);

        L2vpnBridgeDriverBuilder l2vpnBridgeDriverBuilder = new L2vpnBridgeDriverBuilder();
        l2vpnBridgeDriverBuilder.onSessionInitialized(session);
        driverRepo.bindBuilder(l2vpnBridgeDriverBuilder);
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
    }
}
