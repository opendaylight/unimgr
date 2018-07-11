/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.api;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Driver builder that can provide stateful driver that are used in NRP forwarding construct transaction.
 * @author bartosz.michalik@amartus.com
 */
public interface ActivationDriverBuilder {
    /**
     * Get driver to participate in connectivity service processing.
     * @param context (de)activation context
     * @return {@link Optional#empty()} in case it cannot be instantiated for a port, driver otherwise
     */
    Optional<ActivationDriver> driverFor(BuilderContext context);

    String getActivationDriverId();

    /**
     * Blackboard pattern that allows for passing the context information between
     * {@link ActivationDriverBuilder}s taking part in transaction.
     */
    class BuilderContext {
        private Map<String, Object> ctx = new ConcurrentHashMap<>();

        /**
         * Get value of a certain type.
         * @param key key
         * @param <T> expected type
         * @return value or empty
         */
        @SuppressWarnings("unchecked")
        public <T> Optional<T> get(String key) {
            return Optional.ofNullable((T) ctx.get(key));
        }


        /**
         * Put value to blackboard.
         * @param key key
         * @param value value object
         */
        public void put(String key, Object value) {
            ctx.put(key, value);
        }

        /**
         * Remove value from blackboard.
         * @param key key
         */
        public void remove(String key) {
            ctx.remove(key);
        }

        /**
         * Get all keys.
         * @return available keys in blackboard
         */
        public Set<String> keys() {
            return new HashSet<>(ctx.keySet());
        }
    }

}
