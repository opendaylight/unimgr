/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.command;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.impl.UnimgrMapper;
import org.opendaylight.unimgr.utils.EvcUtils;
import org.opendaylight.unimgr.utils.MdsalUtils;
import org.opendaylight.unimgr.utils.OvsdbUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.EvcAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.evc.UniSource;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EvcUtils.class, MdsalUtils.class, OvsdbUtils.class, UnimgrMapper.class})
public class EvcRemoveCommandTest {

    private EvcRemoveCommand evcRemoveCommand;
    private DataTreeModification<Link> evcLink;
    private Link link;
    private DataBroker dataBroker;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(MdsalUtils.class);
        PowerMockito.mockStatic(EvcUtils.class);
        PowerMockito.mockStatic(OvsdbUtils.class);
        PowerMockito.mockStatic(UnimgrMapper.class);
        dataBroker = mock(DataBroker.class);
        link = mock(Link.class);
        evcLink = DataTreeModificationHelper.getEvcLink(link);
        evcRemoveCommand = new EvcRemoveCommand(dataBroker, evcLink);
    }

    /**
     * Test method for {@link org.opendaylight.unimgr.command.EvcRemoveCommand#execute()}.
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testEvcRemoveCommand() throws Exception {
        final List<UniSource> unisSource = new ArrayList<UniSource>();
        final UniSource uniSource = mock(UniSource.class);
        final List<UniDest> unisDest = new ArrayList<UniDest>();
        final UniDest uniDest = mock(UniDest.class);
        final EvcAugmentation evcAugmentation = mock(EvcAugmentation.class);
        final Optional<Node> optionalNode = mock(Optional.class);
        final InstanceIdentifier instanceOfNode = mock(InstanceIdentifier.class);
        unisSource.add(uniSource);
        unisDest.add(uniDest);

        when(link.getAugmentation(EvcAugmentation.class)).thenReturn(evcAugmentation);
        when(evcAugmentation.getUniSource()).thenReturn(unisSource);
        when(evcAugmentation.getUniDest()).thenReturn(unisDest);
        when(uniSource.getUni()).thenReturn(instanceOfNode);
        when(uniDest.getUni()).thenReturn(instanceOfNode);
        when(MdsalUtils.read(any(DataBroker.class), any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class))).thenReturn(evcAugmentation);
        when(MdsalUtils.readNode(any(DataBroker.class), any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class))).thenReturn(optionalNode);
        PowerMockito.doNothing().when(EvcUtils.class, "deleteEvcData",
                any(DataBroker.class), any(Optional.class));
        when(MdsalUtils.deleteNode(any(DataBroker.class), any(InstanceIdentifier.class),
                any(LogicalDatastoreType.class))).thenReturn(true);

        evcRemoveCommand.execute();
        PowerMockito.verifyStatic(times(2));
        EvcUtils.deleteEvcData(any(DataBroker.class), any(Optional.class));
        PowerMockito.verifyStatic(times(1));
        MdsalUtils.deleteNode(any(DataBroker.class), any(InstanceIdentifier.class),
                any(LogicalDatastoreType.class));
    }

}
