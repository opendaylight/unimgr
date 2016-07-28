/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.mef.unimgr.ext.rev160725.ActivationStatus;
import org.opendaylight.yang.gen.v1.urn.mef.unimgr.ext.rev160725.ForwardingConstruct1;
import org.opendaylight.yang.gen.v1.urn.mef.unimgr.ext.rev160725.ForwardingConstruct1Builder;
import org.opendaylight.yang.gen.v1.urn.mef.unimgr.ext.rev160725.forwarding.constructs.forwarding.construct.UnimgrAttrsBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.forwarding.constructs.ForwardingConstruct;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.forwarding.constructs.ForwardingConstructBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Forwarding construct activation state tracking support.
 *
 * @author krzysztof.bijakowski@amartus.com
 */
public class ForwardingConstructActivationStateTracker implements Cloneable {
    private static final Logger LOG = LoggerFactory.getLogger(ForwardingConstructActivationStateTracker.class);

    private DataBroker dataBroker;

    private InstanceIdentifier<ForwardingConstruct> fcIid;

    public ForwardingConstructActivationStateTracker(DataBroker dataBroker, InstanceIdentifier<ForwardingConstruct> fcIid) {
        this.dataBroker = dataBroker;
        this.fcIid = fcIid;
    }

    public boolean isActivatable() {
        ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction();

        try {
            CheckedFuture<Optional<ForwardingConstruct>, ReadFailedException> result = tx.read(LogicalDatastoreType.OPERATIONAL, fcIid);
            Optional<ForwardingConstruct> fcOptional = result.checkedGet();
            return !fcOptional.isPresent();
        } catch (ReadFailedException e) {
            LOG.warn("Error during forwarding construct activation state checking", e);
        }

        return false;
    }

    public boolean isDeactivatable() {
        return !isActivatable();
    }

    public void activated(ForwardingConstruct forwardingConstruct) {
        writeActivationData(forwardingConstruct, ActivationStatus.ACTIVE);
    }

    public void activationFailed(ForwardingConstruct forwardingConstruct) {
        writeActivationData(forwardingConstruct, ActivationStatus.FAILED);
    }

    public void deactivated() {
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.delete(LogicalDatastoreType.OPERATIONAL, fcIid);

        try {
            transaction.submit().checkedGet();
            LOG.debug("Forwarding construct activation state information deleted successfully");
        } catch (TransactionCommitFailedException e) {
            LOG.warn("Error during forwarding construct activation state information deletion", e);
        }
    }

    public void deactivationFailed() {
        //TODO consider how this logic should work
    }

    @Override
    public ForwardingConstructActivationStateTracker clone() throws CloneNotSupportedException {
        return (ForwardingConstructActivationStateTracker) super.clone();
    }

    private void writeActivationData(ForwardingConstruct forwardingConstruct, ActivationStatus activationStatus) {
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();

        ForwardingConstruct1 augmentation = new ForwardingConstruct1Builder()
            .setUnimgrAttrs(new UnimgrAttrsBuilder().setStatus(activationStatus).build())
            .build();

        ForwardingConstruct update = new ForwardingConstructBuilder(forwardingConstruct)
            .addAugmentation(ForwardingConstruct1.class, augmentation)
            .build();

        transaction.merge(LogicalDatastoreType.OPERATIONAL, fcIid, update);

        try {
            transaction.submit().checkedGet();
            LOG.debug("Forwarding construct activation state information wrote successfully");
        } catch (TransactionCommitFailedException e) {
            LOG.warn("Error during writing forwarding construct activation state information", e);
        }
    }
}
