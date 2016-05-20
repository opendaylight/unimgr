/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.mef.nrp.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.GFcPort;

/**
 * Driver builder that can provide stateful driver that are used in NRP forwarding construct transaction
 * @author bartosz.michalik@amartus.com
 */
public interface ActivationDriverBuilder {
    /**
     * Get driver for a single port
     * @param port to configure
     * @param context (de)activation context
     * @return {@link Optional#empty()} in case it cannot be instantiated for a port, driver otherwise
     */
    Optional<ActivationDriver> driverFor(GFcPort port, BuilderContext context);

    /**
     * Get driver for two ports.
     * @param aPort
     * @param zPort
     * @param context
     * @return {@link Optional#empty()} in case it cannot be instantiated for a port, driver otherwise
     */
    Optional<ActivationDriver> driverFor(GFcPort aPort, GFcPort zPort, BuilderContext context);

    /***
     * Blackboard pattern that allows for passing the context information between {@link ActivationDriverBuilder}s taking part in transaction
     */
    class BuilderContext {
        private Map<String, Object> ctx = new ConcurrentHashMap<>();

        /**
         * Get value of a certain type
         * @param key key
         * @param <T> expected type
         * @return value or empty
         */
        @SuppressWarnings("unchecked")
        public <T> Optional<T> get(String key) {
            return Optional.ofNullable((T) ctx.get(key));
        }


        /**
         * Put value to blackboard
         * @param k key
         * @param value value object
         */
        public void put(String k, Object value) {
            ctx.put(k, value);
        }

        /**
         * Remove value from blackboard
         * @param k key
         */
        public void remove(String k) {
            ctx.remove(k);
        }

        /**
         * Get all keys
         * @return
         */
        public Set<String> keys() {
            return new HashSet<>(ctx.keySet());
        }
    }

}
