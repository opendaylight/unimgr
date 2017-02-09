/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.api;

import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.common.ResourceActivatorException;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.forwarding.constructs.ForwardingConstruct;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;

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
     * Called in case all drivers in the transaction has succeeded.
     */
    void commit();

    /**
     * Called in case any of drivers in the transaction has failed.
     */
    void rollback();

    /**
     * Set state for the driver for a (de)activation transaction.
     * @param from near end
     * @param to far end
     * @param context context
     */
    void initialize(FcPort from, FcPort to, ForwardingConstruct context);

    /**
     * Performs the activation action.
     */
    void activate() throws TransactionCommitFailedException, ResourceActivatorException;

    /**
     * Performs the deactivation action.
     */
    void deactivate() throws TransactionCommitFailedException, ResourceActivatorException;


    /**
     * Influences the order in which drivers are called within the transaction.
     * @return int priority of this driver when resoving ambiguity
     */
    int priority();
}
