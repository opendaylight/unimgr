/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.common.util;

import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arif.hussain@xoriant.com
 */
public class CommonUtils {
    private static final Logger LOG = LoggerFactory.getLogger(CommonUtils.class);
    public static final String NETCONF_TOPOLODY_NAME = "topology-netconf";

    public static boolean isSameDevice(EndPoint endPoint, List<String> ls) {
        Uuid sip = endPoint.getEndpoint().getServiceInterfacePoint().getServiceInterfacePointId(); //sip:ciscoD1:GigabitEthernet0/0/0/1
        NodeId nodeId = new NodeId(SipHandler.getDeviceName(sip));

        if (ls.size() == 0) {
            ls.add(nodeId.getValue());
        } else if (ls.size() > 0) {
            List<String> listWithoutDuplicates =
                    ls.stream().distinct().collect(Collectors.toList());

            java.util.Optional<String> preset = listWithoutDuplicates.stream()
                    .filter(x -> x.equals(nodeId.getValue())).findFirst();

            if (preset.isPresent()) {
                return true;
            }
            ls.add(nodeId.getValue());
        }

        return false;
    }

    /**
     * Function is checking bridge domain configuration already deleted from XR-device.
     * @param endPoint
     * @param ls
     * @return boolean
     */
    public static boolean isSameInterface(EndPoint endPoint, List<Uuid> ls) {
        Uuid sip = endPoint.getEndpoint().getServiceInterfacePoint().getServiceInterfacePointId(); //sip:ciscoD1:GigabitEthernet0/0/0/1

        if (ls.size() == 0) {
            ls.add(sip);
        } else if (ls.size() > 0) {
            List<Uuid> listWithoutDuplicates =
                    ls.stream().distinct().collect(Collectors.toList());

            java.util.Optional<Uuid> preset = listWithoutDuplicates.stream()
                    .filter(x -> x.equals(sip)).findFirst();

            if (preset.isPresent()) {
                return true;
            }
            ls.add(sip);
        }

        return false;
    }

}
