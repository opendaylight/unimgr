/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.impl;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;

import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverAmbiguousException;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverBuilder;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverNotFoundException;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriverRepoService;
import org.opendaylight.unimgr.mef.nrp.impl.ActivationTransaction;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.forwarding.constructs.ForwardingConstruct;
import org.opendaylight.yang.gen.v1.urn.onf.core.network.module.rev160630.g_forwardingconstruct.FcPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opendaylight.unimgr.mef.nrp.impl.ActivationTransaction.Result;

/**
 * @author bartosz.michalik@amartus.com
 * @author krzysztof.bijakowski@amartus.com [modifications]
 */
public class ForwardingConstructActivatorService {
    private static final Logger LOG = LoggerFactory.getLogger(ForwardingConstructActivatorService.class);
    private ActivationDriverRepoService activationRepoService;
    private final ReentrantReadWriteLock lock;

    public ForwardingConstructActivatorService(ActivationDriverRepoService activationRepoService) {
        this.activationRepoService = activationRepoService;
        lock = new ReentrantReadWriteLock();
    }

    /**
     * Activate a MEF ForwardingConstruct.
     * @param forwardingConstruct the new route to activate
     */
    public void activate(@Nonnull ForwardingConstruct forwardingConstruct, @Nonnull ForwardingConstructActivationStateTracker stateTracker) {
        if(stateTracker.isActivatable()) {
            Optional<ActivationTransaction> tx = prepareTransaction(forwardingConstruct);
            if (tx.isPresent()) {
                Result result = tx.get().activate();

                if(result.isSuccessful()) {
                    stateTracker.activated(forwardingConstruct);
                    LOG.info("Forwarding construct activated successfully, request = {} ", forwardingConstruct);
                } else {
                    stateTracker.activationFailed(forwardingConstruct);
                    LOG.warn("Forwarding construct activation failed, reason = {}, request = {}", result.getMessage(), forwardingConstruct);
                }
            } else {
                LOG.warn("No transaction for this activation request {}", forwardingConstruct);
                stateTracker.activationFailed(forwardingConstruct);
            }
        }
    }

    /**
     * Deactivate a MEF ForwardingConstruct.
     * @param forwardingConstruct the existing route to deactivate
     */
    public void deactivate(@Nonnull ForwardingConstruct forwardingConstruct, @Nonnull ForwardingConstructActivationStateTracker stateTracker) {
        if(stateTracker.isDeactivatable()) {
            Optional<ActivationTransaction> tx = prepareTransaction(forwardingConstruct);
            if (tx.isPresent()) {
                Result result = tx.get().deactivate();

                if(result.isSuccessful()) {
                    stateTracker.deactivated();
                    LOG.info("Forwarding construct deactivated successfully, request = {}", forwardingConstruct);
                } else {
                    stateTracker.deactivationFailed();
                    LOG.warn("Forwarding construct deactivation failed, reason = {}, request = {}", result.getMessage(), forwardingConstruct);
                }
            } else {
                LOG.warn("No transaction for this deactivation request {}", forwardingConstruct);
                stateTracker.deactivationFailed();
            }
        }
    }

    private Optional<ActivationTransaction> prepareTransaction(ForwardingConstruct fwdC) {
        final List<FcPort> list = fwdC.getFcPort();
        //TODO validate pre-condition
        final FcPort a = list.get(0);
        final FcPort z = list.get(1);

        return isTheSameNode(fwdC)
                ? getTxForNode(a,z, fwdC) : getTxForMultiNode(a,z, fwdC);
    }

    private boolean isTheSameNode(ForwardingConstruct forwardingConstruct) {
        final FcPort p1 = forwardingConstruct.getFcPort().get(0);
        final FcPort p2 = forwardingConstruct.getFcPort().get(1);
        return p1.getNode().equals(p2.getNode());
    }

    private Optional<ActivationTransaction> getTxForNode(FcPort portA, FcPort portZ, ForwardingConstruct fwdC) {
        lock.readLock().lock();
        try {
            final ActivationDriverBuilder.BuilderContext ctx = new ActivationDriverBuilder.BuilderContext();
            ActivationDriver activator = activationRepoService.getDriver(portA, portZ, ctx);

            activator.initialize(portA, portZ, fwdC);
            ActivationTransaction tx = new ActivationTransaction();
            tx.addDriver(activator);
            return Optional.of(tx);
        } catch (ActivationDriverNotFoundException e) {
            LOG.warn("No unique activation driver found for {} <-> {}", portA, portZ);
            return Optional.empty();
        } catch (ActivationDriverAmbiguousException e) {
            LOG.warn("Multiple activation driver found for {} <-> {}", portZ, portA);
            return Optional.empty();
        } catch (Exception e) {
            LOG.error("driver initialization exception", e);
            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    private Optional<ActivationTransaction> getTxForMultiNode(FcPort portA, FcPort portZ, ForwardingConstruct fwdC) {
        //1. find and initialize drivers
        lock.readLock().lock();
        try {

            final ActivationDriverBuilder.BuilderContext ctx = new ActivationDriverBuilder.BuilderContext();
            ctx.put(ForwardingConstruct.class.getName(), fwdC);

            Optional<ActivationDriver> aendActivator = findDriver(portA, ctx);
            Optional<ActivationDriver> zendActivator = findDriver(portZ, ctx);

            if (aendActivator.isPresent() && zendActivator.isPresent()) {
                aendActivator.get().initialize(portA, portZ, fwdC);
                zendActivator.get().initialize(portZ, portA, fwdC);

                final ActivationTransaction tx = new ActivationTransaction();
                tx.addDriver(aendActivator.get());
                tx.addDriver(zendActivator.get());

                return Optional.of(tx);
            } else {
                // ??? TODO improve comment for better traceability
                LOG.error("drivers for both ends needed");
                return Optional.empty();
            }

        } catch (Exception e) {
            LOG.error("driver initialization exception",e);
            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    protected Optional<ActivationDriver> findDriver(FcPort port, ActivationDriverBuilder.BuilderContext fwdC) {
        if (activationRepoService == null)  {
            LOG.warn("Activation Driver repo is not initialized");
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(activationRepoService.getDriver(port, fwdC));
        } catch (ActivationDriverNotFoundException e) {
            LOG.warn("No activation driver found for {}", port);
            return Optional.empty();
        } catch (ActivationDriverAmbiguousException e) {
            LOG.warn("Multiple activation driver found for {}", port);
            return Optional.empty();
        }


    }

    /**
     * Set the activation driver repository service.
     * @param activationRepoService service to use
     */
    public void setActivationRepoService(ActivationDriverRepoService activationRepoService) {
        lock.writeLock().lock();
        this.activationRepoService = activationRepoService;
        lock.writeLock().unlock();
    }

    /**
     * Unset the activation driver repository service.
     */
    public void unsetActivationRepoService() {
        lock.writeLock().lock();
        this.activationRepoService = null;
        lock.writeLock().unlock();
    }

    static final class Context {
        final FcPort portA;
        final FcPort portZ;
        final ForwardingConstruct fwC;

        public Context(FcPort portA, FcPort portZ, ForwardingConstruct fwC) {
            this.portA = portA;
            this.portZ = portZ;
            this.fwC = fwC;
        }
    }
}
