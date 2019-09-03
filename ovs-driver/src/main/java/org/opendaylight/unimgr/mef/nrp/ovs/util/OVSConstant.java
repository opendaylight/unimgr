/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.util;

/**
 * @author Arif.Hussain@Xoriant.Com
 *
 */
public class OVSConstant {

    private OVSConstant() {

        throw new IllegalStateException(OVSConstant.UTILITY1);
    }

    public static final int CPESTARTINCLUSIVE = 10;
    public static final int CPEENDEXCLUSIVE = 19;
    public static final int SPEDEFAULTVAL = 1000;
    public static final String CPETYPE = "CPE";
    public static final String SPETYPE = "SPE";
    public static final String UTILITY1 = "Utility-class";


}
