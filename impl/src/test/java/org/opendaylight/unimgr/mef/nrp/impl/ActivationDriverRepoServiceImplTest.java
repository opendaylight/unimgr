package org.opendaylight.unimgr.mef.nrp.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.opendaylight.unimgr.utils.ActivationDriverMocks.prepareDriver;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverAmbiguousException;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverNotFoundException;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverRepoService;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.FcPort;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.FcPortBuilder;

/**
 * @author bartosz.michalik@amartus.com
 */
public class ActivationDriverRepoServiceImplTest {

    @Test(expected = ActivationDriverNotFoundException.class)
    public void testEmpty() throws Exception {

        ActivationDriverRepoService driverRepo = new ActivationDriverRepoServiceImpl(Collections.emptyList());
        final FcPort port = new FcPortBuilder().setId("a").build();
        driverRepo.getDriver(port, null);
    }


    @Test(expected = ActivationDriverAmbiguousException.class)
    public void testConflict() throws Exception {

        final ActivationDriver driver = mock(ActivationDriver.class);

        ActivationDriverRepoService driverRepo = new ActivationDriverRepoServiceImpl(Arrays.asList(
                prepareDriver(p -> driver), prepareDriver(p -> driver)
        ));

        final FcPort port = new FcPortBuilder().setId("a").build();
        driverRepo.getDriver(port, null);
    }

    @Test
    public void testMatching() throws Exception {

        final ActivationDriver driver = mock(ActivationDriver.class);

        ActivationDriverRepoService driverRepo = new ActivationDriverRepoServiceImpl(Collections.singletonList(
                prepareDriver(p -> driver)
        ));

        final FcPort port = new FcPortBuilder().setId("a").build();
        final ActivationDriver driverFromRepo = driverRepo.getDriver(port, null);
        assertEquals(driver, driverFromRepo);
    }
}
