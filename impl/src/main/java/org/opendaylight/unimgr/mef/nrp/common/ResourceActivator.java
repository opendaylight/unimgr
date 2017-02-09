/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.common;

import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;

/**
 * Device facing SPI for activating or deactivating a fragment of an NRP
 * ForwardingConstruct on a single device.
 */
public interface ResourceActivator {

    /**
     * Activate a service fragment on the node identified by nodeName.
     *
     * @param nodeName
     *            the name of node in network topology
     * @param outerName
     *            name of outer activation construct
     * @param innerName
     *            name of inner activation construct
     * @param flowPoint
     *            the fc-port to be activated
     * @param neighbor
     *            the neighbor fc-port
     * @param mtu
     *            the desired MTU for this forwarding construct
     */
    public void activate(String nodeName, String outerName, String innerName, FcPort flowPoint, FcPort neighbor,
            long mtu) throws TransactionCommitFailedException, ResourceActivatorException;

    /**
     * Deactivate a service fragment on the node identified by nodeName.
     *
     * @param nodeName
     *            the name of node in network topology
     * @param outerName
     *            name of outer deactivation construct
     * @param innerName
     *            name of inner deactivation construct
     * @param flowPoint
     *            the fc-port to be deactivated
     * @param neighbor
     *            the neighbor fc-port
     * @param mtu
     *            the desired MTU for this forwarding construct
     */
    public void deactivate(String nodeName, String outerName, String innerName, FcPort flowPoint, FcPort neighbor,
            long mtu) throws TransactionCommitFailedException, ResourceActivatorException;
}
