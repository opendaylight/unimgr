/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.netvirt;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataChangeListenerBase;
import org.opendaylight.genius.mdsalutil.AbstractDataChangeListener;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.MefServices;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.MefService;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.Evc;
import org.opendaylight.yang.gen.v1.http.metroethernetforum.org.ns.yang.mef.services.rev150526.mef.services.mef.service.evc.unis.uni.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.ElanInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.ElanInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.instances.ElanInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.instances.ElanInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.instances.ElanInstanceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.interfaces.ElanInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.interfaces.ElanInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.elan.rev150602.elan.interfaces.ElanInterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvcListener extends AbstractDataChangeListener<Evc> {

	private static final Logger log = LoggerFactory.getLogger(EvcListener.class);
	private DataBroker dataBroker;
	private Object evcListenerRegistration;

	public EvcListener(final DataBroker dataBroker) {
		super(Evc.class);
		this.dataBroker = dataBroker;

		registerListener();
	}

	public void registerListener() {
		try {
			evcListenerRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
					getWildCardPath(), EvcListener.this, DataChangeScope.SUBTREE);
		} catch (final Exception e) {
			log.error("Evc DataChange listener registration failed !", e);
			throw new IllegalStateException("Evc registration Listener failed.", e);
		}
	}

	private InstanceIdentifier<Evc> getWildCardPath() {
		InstanceIdentifier<Evc> instanceIdentifier = InstanceIdentifier.create(MefServices.class)
				.child(MefService.class).child(Evc.class);

		return instanceIdentifier;
	}

	@Override
	protected void remove(InstanceIdentifier<Evc> identifier, Evc del) {

	}

	@Override
	protected void update(InstanceIdentifier<Evc> identifier, Evc original, Evc update) {
	}

	@Override
	protected void add(InstanceIdentifier<Evc> identifier, Evc add) {

		try {
			// Create elan instance
			String instanceName = add.getEvcId().getValue();
			ElanInstanceBuilder einstBuilder = new ElanInstanceBuilder();
			einstBuilder.setElanInstanceName(instanceName);
			einstBuilder.setKey(new ElanInstanceKey(instanceName));

			MDSALUtil.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION,
					getElanInstanceInstanceIdentifier(instanceName), einstBuilder.build());

			int i = 0;
			// Create elan interfaces
			for (Object uni : add.getUnis().getUni()) {

				String interfaceName = instanceName + i++;
				ElanInterfaceBuilder einterfaceBuilder = new ElanInterfaceBuilder();
				einterfaceBuilder.setElanInstanceName(instanceName);
				einterfaceBuilder.setName(interfaceName);

				MDSALUtil.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION,
						getElanInterfaceInstanceIdentifier(interfaceName), einterfaceBuilder.build());
			}
		} catch (final Exception e) {
			log.error("Add evc failed !", e);
			throw new IllegalStateException("Add evc failed.", e);
		}

	}

	private InstanceIdentifier getElanInstanceInstanceIdentifier(String instanceName) {
		return InstanceIdentifier.builder(ElanInstances.class)
				.child(ElanInstance.class, new ElanInstanceKey(instanceName)).build();
	}

	private InstanceIdentifier getElanInterfaceInstanceIdentifier(String interfaceName) {
		return InstanceIdentifier.builder(ElanInterfaces.class)
				.child(ElanInterface.class, new ElanInterfaceKey(interfaceName)).build();
	}

}
