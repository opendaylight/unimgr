/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs activation over multiple @ drivers.
 *
 * @author bartosz.michalik@amartus.com
 */
public class ActivationTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(ActivationTransaction.class);
    private List<ActivationDriver> drivers = new ArrayList<>();


    public void addDriver(ActivationDriver driver) {
        drivers.add(driver);
    }

    /**
     * Activate the contents of this transaction.
     */
    public void activate() {
        sortDrivers();
        try {
            for (ActivationDriver d: drivers) {
                d.activate();
            }
            commit();
            LOG.info("Activate transaction successful");
        } catch (Exception e) {
            //XXX add transaction identification ???
            LOG.warn("Rolling back activate transaction ", e);
            rollback();
        }
    }

    /**
     * Deactivate the contents of this transaction.
     */
    public void deactivate() {
        sortDrivers();
        try {
            for (ActivationDriver d: drivers) {
                d.deactivate();
            }
            LOG.info("Deactivate transaction successful");
            commit();
        } catch (Exception e) {
            //XXX add transaction identification ???
            LOG.warn("Rolling back deactivate transaction ", e);
            rollback();
        }
    }

    private void commit() {
        drivers.stream().forEach(ActivationDriver::commit);
    }

    private void rollback() {
        drivers.stream().forEach(ActivationDriver::rollback);
    }

    private void sortDrivers() {
        drivers.sort((driverA, driverB) -> driverA.priority() - driverB.priority());
    }

}
