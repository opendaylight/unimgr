/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.common;

/**
 * This SPI is used to create resource names when constructing device configuration.
 */
public interface ServiceNaming {

    /**
     * Return a resource name composed to include the provided id.
     *
     * @param id unique allocated id
     * @return String name
     */
    String getOuterName(String id);

    /**
     * Return a resource name composed to include the provided id.
     *
     * @param id unique allocated id
     * @return String name
     */
    String getInnerName(String id);

    /**
     * ASR 9000 can't accept colon in xconnect and bridgeDomain group name, so it have to be replaced with
     * underscore. If any other restriction will be found, this is a good place to change serviceId
     * name.
     *
     * @param id unique allocated id
     * @return new id
     */
    String replaceForbidenCharacters(String id);
}
