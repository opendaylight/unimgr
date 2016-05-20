/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.mef.nrp.impl;

import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.GFcPort;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.GForwardingConstruct;

/**
 * Interface of a driver that maps NRP concepts to the configuration of underlying infrastructure.
 * The driver is used in following scenario
 * <ol>
 *     <li>Driver is initialized. Otherwise stop</li>
 *     <li>Driver is attached to transaction</li>
 *     <li>Driver activate/deactivate method gets called</li>
 *     <li>If all drivers within transaction succeed commit method is called. Otherwise rollback is triggered</li>
 * </ol>
 *
 * @author bartosz.michalik@amartus.com
 */
public interface ActivationDriver {

    /**
     * Called in case all drivers in the transaction has succeeded
     */
    void commit();

    /**
     * Called in case any of drivers in the transaction has failed
     */
    void rollback();

    /**
     * Set state for the driver
     * @param from near end
     * @param to far end
     * @param context context
     * @throws Exception
     */
    void initialize(GFcPort from, GFcPort to, GForwardingConstruct context) throws Exception;

    /**
     * Activates the port from
     */
    void activate() throws Exception;
    /**
     * Deactivates the port from
     */
    void deactivate() throws Exception;


    /**
     * Influences the order in which drivers are called within the transaction
     * @return
     */
    int priority();



}
