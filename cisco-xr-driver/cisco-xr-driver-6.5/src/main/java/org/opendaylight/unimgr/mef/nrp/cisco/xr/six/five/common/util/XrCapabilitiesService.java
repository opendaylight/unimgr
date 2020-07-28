/*
 * Copyright (c) 2018 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.common.util;
import java.util.function.BiFunction;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.six.five.l2vpn.driver.XrDriverBuilder;
import org.opendaylight.unimgr.utils.CapabilitiesService;
import org.opendaylight.unimgr.utils.NetconfConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * @author bartosz.michalik@amartus.com
 */
public class XrCapabilitiesService extends CapabilitiesService {
    public XrCapabilitiesService(DataBroker dataBroker) {
        super(dataBroker);
    }

    public enum NodeCapability implements Capability<Node> {
        
    	 NETCONF((dbBroker, node) -> node.augmentation(NetconfNode.class) != null),
         NETCONF_CISCO_IOX_L2VPN((dbBroker, node) ->
                 checkForNetconfCapability(node, NetconfConstants.CAPABILITY_IOX_L2VPN)),
         NETCONF_CISCO_IOX_IFMGR((dbBroker, node) ->
                 checkForNetconfCapability(node,NetconfConstants.CAPABILITY_IOX_IFMGR)),
         NETCONF_CISCO_IOX_POLICYMGR((dbBroker, node) ->
                 checkForNetconfCapability(node,NetconfConstants.CAPABILITY_IOX_INFRA_POLICYMGR));

    	 private static final Logger LOG = LoggerFactory.getLogger(NodeCapability.class);
    	
    	 
		/*
		 * String CAPABILITY_IOX_L2VPN_XR65 =
		 * "(http://cisco.com/ns/yang/Cisco-IOS-XR-l2vpn-cfg?revision=2017-06-26)Cisco-IOS-XR-l2vpn-cfg";
		 * 
		 * String CAPABILITY_IOX_IFMGR_XR65 =
		 * "(http://cisco.com/ns/yang/Cisco-IOS-XR-ifmgr-cfg?revision=2017-09-07)Cisco-IOS-XR-ifmgr-cfg";
		 * 
		 * String CAPABILITY_IOX_INFRA_POLICYMGR_XR65 =
		 * "(http://cisco.com/ns/yang/Cisco-IOS-XR-infra-policymgr-cfg?revision=2017-09-07)Cisco-IOS-XR-infra-policymgr-cfg";
		 */

        private BiFunction<DataBroker, Node, Boolean> condition;

        NodeCapability(BiFunction<DataBroker, Node, Boolean> condition) {
            this.condition = condition;
        }

        @Override
        public BiFunction<DataBroker, Node, Boolean> getCondition() {
            return condition;
        }

        private static boolean checkForNetconfCapability(Node node, String netconfCapability) {
            NetconfNode netconf = node.augmentation(NetconfNode.class);
            LOG.debug(" In method checkForNetconfCapability");
            if (netconf == null) {
                return false;
            }
            if (netconf.getAvailableCapabilities() == null) {
                return false;
            }
            if (netconf.getAvailableCapabilities().getAvailableCapability() == null) {
            	
                return false;
            }
            LOG.debug(" In method checkForNetconfCapability"+netconf.getAvailableCapabilities().getAvailableCapability());
            return netconf
                    .getAvailableCapabilities()
                    .getAvailableCapability()
                    .stream()
                    .anyMatch(capability -> capability.getCapability().startsWith(netconfCapability));
        }
    }
}
