/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs activation over multiple @ drivers.
 *
 * @author bartosz.michalik@amartus.com
 * @author krzysztof.bijakowski@amartus.com [modifications]
 */
public class ActivationTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(ActivationTransaction.class);

    private List<ActivationDriver> drivers = new ArrayList<>();

    public void addDriver(ActivationDriver driver) {
        drivers.add(driver);
    }

    /**
     * Activate the contents of this transaction.
     * @return result
     */
    public Result activate() {
        if (drivers.isEmpty()) {
            throw new IllegalStateException("at least one driver required");
        }
        sortDrivers();
        try {
            for (ActivationDriver d: drivers) {
                d.activate();
            }
            commit();
            LOG.info("Activate transaction successful");

            return Result.success();
        } catch (Exception e) {
            //XXX add transaction identification ???
            LOG.warn("Rolling back activate transaction ", e);
            rollback();

            return Result.fail(e.getMessage(), e);
        }
    }

    /**
     * Update the contents of this transaction.
     * @return result
     */
    public Result update() {
        if (drivers.isEmpty()) {
            throw new IllegalStateException("at least one driver required");
        }
        sortDrivers();
        try {
            for (ActivationDriver d: drivers) {
                d.update();
            }
            commit();
            LOG.info("Update transaction successful");

            return Result.success();
        } catch (Exception e) {
            //XXX add transaction identification ???
            LOG.warn("Rolling back update transaction ", e);
            rollback();

            return Result.fail(e.getMessage(), e);
        }
    }

    /**
     * Deactivate the contents of this transaction.
     * @return result
     */
    public Result deactivate() {
        if (drivers.isEmpty()) {
            throw new IllegalStateException("at least one driver required");
        }
        sortDrivers();
        try {
            for (ActivationDriver d: drivers) {
                d.deactivate();
            }
            LOG.info("Deactivate transaction successful");
            commit();

            return Result.success();
        } catch (Exception e) {
            //XXX add transaction identification ???
            LOG.warn("Rolling back deactivate transaction ", e);
            rollback();

            return Result.fail(e.getMessage(), e);
        }
    }

    private void commit() {
        drivers.stream().forEach(ActivationDriver::commit);
    }

    private void rollback() {
        drivers.stream().forEach(ActivationDriver::rollback);
    }

    private void sortDrivers() {
        drivers.sort(Comparator.comparingInt(ActivationDriver::priority));
    }

    public static class Result {
        private boolean successful;

        private Optional<String> message;

        private Optional<Throwable> cause;

        private Result(boolean successful, Optional<String> message, Optional<Throwable> cause) {
            this.successful = successful;
            this.message = message;
            this.cause = cause;
        }

        public Optional<Throwable> getCause() {
            return cause;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public Optional<String> getMessage() {
            return message;
        }

        public static Result success() {
            return new Result(true, Optional.empty(), Optional.empty());
        }

        public static Result fail(String message, Throwable cause) {
            return new Result(false, Optional.of(message), Optional.of(cause));
        }
    }

}
