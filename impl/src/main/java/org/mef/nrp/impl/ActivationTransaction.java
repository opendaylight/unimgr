/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.mef.nrp.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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

    public void activate() {
        sortDrivers();
        try {
            for(ActivationDriver d: drivers) { d.activate(); }
            commit();
            LOG.info("Activate transaction successful");
        } catch (Exception e) {
            //XXX add transaction identification ???
            LOG.warn("Rolling back activate transaction ", e);
            rollback();
        }
    }


    public void deactivate() {
        sortDrivers();
        try {
            for(ActivationDriver d: drivers) { d.deactivate(); }
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
        drivers.sort((a,b) -> a.priority() - b.priority());
    }

}
