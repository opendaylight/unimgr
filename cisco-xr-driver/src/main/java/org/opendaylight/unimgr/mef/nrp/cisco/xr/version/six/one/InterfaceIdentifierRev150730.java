/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.xr.version.six.one;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.ServicePort;
import org.opendaylight.unimgr.mef.nrp.cisco.xr.common.helper.InterfaceHelper;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceActive;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceConfigurationsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.InterfaceModeEnum;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfigurationKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations._interface.configuration.Mtus;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109.InterfaceConfiguration2;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109.InterfaceConfiguration2Builder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109._interface.configurations._interface.configuration.EthernetServiceBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109._interface.configurations._interface.configuration.ethernet.service.Encapsulation;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.cfg.rev151109._interface.configurations._interface.configuration.ethernet.service.EncapsulationBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.datatypes.rev151109.Match;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.datatypes.rev151109.VlanTagOrAny;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.InterfaceConfiguration3;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109.InterfaceConfiguration3Builder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109._interface.configurations._interface.configuration.L2Transport;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2vpn.cfg.rev151109._interface.configurations._interface.configuration.L2TransportBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.LoadingCache;

public class InterfaceIdentifierRev150730 {

	private static final Logger LOG = LoggerFactory.getLogger(InterfaceIdentifierRev150730.class);
	 // simplyfied version of selecting
    private static Pattern gbPort = Pattern.compile(InterfaceHelper.getGbPortPattern());
    
    
    public static InstanceIdentifier<InterfaceConfigurations> getInterfaceConfigurationsId() {
            return InstanceIdentifier.builder(InterfaceConfigurations.class).build();
        }
    
    final static Predicate<InterfaceConfiguration> isNep = ic -> {
        final String name = ic.key().getInterfaceName().getValue();
        return gbPort.matcher(name).matches();
    };
    
    @SuppressWarnings("checkstyle:illegalcatch")
    public static List<OwnedNodeEdgePoint> toTp(Collection<Node> nodes, MountPointService mountService,
    		LoadingCache<NodeKey, KeyedInstanceIdentifier<Node, NodeKey>> mountIds) {
        OwnedNodeEdgePointBuilder tpBuilder = new OwnedNodeEdgePointBuilder();
        return nodes.stream().flatMap(cn -> {
            final NodeKey key = cn.key();
            try {
                KeyedInstanceIdentifier<Node, NodeKey> id = mountIds.get(key);
                Optional<MountPoint> mountPoint = mountService.getMountPoint(id);
                if (mountPoint.isPresent()) {
                    DataBroker deviceBroker = mountPoint.get().getService(DataBroker.class).get();
                    List<OwnedNodeEdgePoint> tps;
                    try (ReadTransaction tx = deviceBroker.newReadOnlyTransaction()) {
                        tps = ports(tx)
                          .filter(i -> {
                              boolean shutdown =
                                    i != null && i.getShutdown() != null;
                              return !shutdown;
                          }).filter(isNep::test).map(i -> {
                              InterfaceConfigurationKey ikey = i.key();
                              LOG.debug("found {} interface", ikey);

                              Uuid tpId = new Uuid(cn.getNodeId().getValue() + ":"
                                    + ikey.getInterfaceName().getValue());
                              return tpBuilder
                                    .setUuid(tpId)
                                    .withKey(new OwnedNodeEdgePointKey(tpId))
                                    .setLinkPortDirection(PortDirection.BIDIRECTIONAL)
                                    .setLinkPortRole(PortRole.SYMMETRIC)
                                    .setLayerProtocolName(LayerProtocolName.ETH).build();
                          }).collect(Collectors.toList());

                        return tps.stream();
                    } catch (InterruptedException e) {
                        LOG.warn("erro", e);
                    }

                } else {
                    LOG.warn("no mount point for {}", key);
                }

            } catch (ExecutionException  e) {
                LOG.warn("error while processing for {} ", key, e);
            }
            return Stream.empty();
        }).collect(Collectors.toList());
    }
    
    
    public static Stream<InterfaceConfiguration> ports(ReadTransaction tx) throws InterruptedException, ExecutionException {
        Optional<InterfaceConfigurations> interfaces = tx.read(
                                                            LogicalDatastoreType.OPERATIONAL,
                                                            getInterfaceConfigurationsId()
                                                            ).get();
        if (interfaces.isPresent()) {
            return interfaces.get().getInterfaceConfiguration().stream();
        }

        return Stream.empty();
    }

    
    public static InterfaceConfigurations addInterface(ServicePort port, long mtu, boolean setL2Transport) {
        InterfaceConfigurationsBuilder interfaceConfigurationsBuilder =  new InterfaceConfigurationsBuilder();
        InterfaceConfigurationBuilder configurationBuilder = new InterfaceConfigurationBuilder();
        Mtus mtus = MtuHelper.generateMtus(mtu, new CiscoIosXrString(port.getInterfaceName()));
        List<InterfaceConfiguration> configurationList = new LinkedList<>();

        configurationBuilder
            .setInterfaceName(InterfaceHelper.getInterfaceName(port))
            .setActive(new InterfaceActive("act"));

       if (Optional.of(mtus).isPresent()) {
           configurationBuilder.setMtus(Optional.of(mtus).get());
       }

        if (setL2Transport) { 
			setL2Configuration(configurationBuilder);
        }

        configurationList.add(configurationBuilder.build());
        interfaceConfigurationsBuilder.setInterfaceConfiguration(configurationList);
		
        return interfaceConfigurationsBuilder.build();
    }

    public static InterfaceConfigurations addSubInterface(ServicePort port, long mtu) {
        InterfaceConfigurationsBuilder interfaceConfigurationsBuilder =  new InterfaceConfigurationsBuilder();
        InterfaceConfigurationBuilder configurationBuilder = new InterfaceConfigurationBuilder();
        Mtus mtus = MtuHelper.generateMtus(mtu, new CiscoIosXrString("sub_vlan"));
        List<InterfaceConfiguration> configurationList = new LinkedList<>();

        configurationBuilder
            .setInterfaceName(InterfaceHelper.getSubInterfaceName(port))
            .setActive(new InterfaceActive("act"))
            //.setShutdown(Boolean.FALSE)
            .setDescription("Create sub interface through ODL")
            .setInterfaceModeNonPhysical(InterfaceModeEnum.L2Transport);
            // set ethernet service
            setEthernetService(configurationBuilder, port);

        if (Optional.of(mtus).isPresent()) {
            configurationBuilder.setMtus(Optional.of(mtus).get());
        }

        configurationList.add(configurationBuilder.build());
        interfaceConfigurationsBuilder.setInterfaceConfiguration(configurationList);
        return interfaceConfigurationsBuilder.build();
    }

    private static void setEthernetService(InterfaceConfigurationBuilder configurationBuilder, ServicePort port) {
        Encapsulation encapsulation = new EncapsulationBuilder()
            .setOuterRange1Low(new VlanTagOrAny(Uint32.valueOf(port.getVlanId())))
            .setOuterTagType(Match.MatchDot1q)
            .build();

        InterfaceConfiguration2 augmentation = new InterfaceConfiguration2Builder()
                .setEthernetService(new EthernetServiceBuilder()
                    .setEncapsulation(encapsulation)
                    .build()
                )
                .build();

        configurationBuilder.addAugmentation(InterfaceConfiguration2.class, augmentation);
    }

    private static void setL2Configuration(InterfaceConfigurationBuilder configurationBuilder) {
        L2Transport l2transport = new L2TransportBuilder()
                    .setEnabled(Empty.getInstance())
                    .build();

        InterfaceConfiguration3 augmentation = new InterfaceConfiguration3Builder()
            .setL2Transport(l2transport)
            .build();

        configurationBuilder.addAugmentation(InterfaceConfiguration3.class, augmentation);
    }

}
