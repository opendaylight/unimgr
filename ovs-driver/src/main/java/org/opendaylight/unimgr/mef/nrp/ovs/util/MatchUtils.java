/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;

/**
 * Utility class providing common operations for Match objects.
 *
 * @author jakub.niezgoda@amartus.com
 */

class MatchUtils {

    static Match createInPortMatch(String inPort) {
        return new MatchBuilder().setInPort(new NodeConnectorId(inPort)).build();
    }

    static Match createVlanMatch(int vlanID, String port) {
        MatchBuilder matchBuilder = new MatchBuilder();
        VlanIdBuilder vlanIdBuilder = new VlanIdBuilder().setVlanIdPresent(true).setVlanId(new VlanId(vlanID));
        VlanMatchBuilder vlanMatchBuilder = new VlanMatchBuilder().setVlanId(vlanIdBuilder.build());

        return matchBuilder.setInPort(new NodeConnectorId(port))
                           .setVlanMatch(vlanMatchBuilder.build())
                           .build();
    }

    static Match createWithoutVlanMatch(String port) {
        MatchBuilder matchBuilder = new MatchBuilder();
       // VlanIdBuilder vlanIdBuilder = new VlanIdBuilder().setVlanIdPresent(true).setVlanId(new VlanId(vlanID));
       // VlanMatchBuilder vlanMatchBuilder = new VlanMatchBuilder().setVlanId(vlanIdBuilder.build());

        return matchBuilder.setInPort(new NodeConnectorId(port))
                           //.setVlanMatch(vlanMatchBuilder.build())
                           .build();
    }

}
