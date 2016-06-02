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
import org.opendaylight.unimgr.mef.nrp.impl.ForwardingConstructHelper;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.GFcPort;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.GForwardingConstruct;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.fcroutelist.FcRoute;
import org.opendaylight.yang.gen.v1.uri.onf.coremodel.corenetworkmodule.objectclasses.rev160413.g_forwardingconstruct.FcPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author bartosz.michalik@amartus.com
 */
public class FcRouteActivatorService {
    private static final Logger LOG = LoggerFactory.getLogger(FcRouteActivatorService.class);
    private ActivationDriverRepoService activationRepoService;
    private final ReentrantReadWriteLock lock;

    public FcRouteActivatorService(ActivationDriverRepoService activationRepoService) {
        this.activationRepoService = activationRepoService;
        lock = new ReentrantReadWriteLock();
    }

    /**
     * Activate a MEF FcRoute.
     * @param route the new route to activate
     */
    public void activate(@Nonnull FcRoute route) {
        for (GForwardingConstruct fwdC : route.getForwardingConstruct()) {
            Optional<ActivationTransaction> tx = prepareTransaction(fwdC);
            if (tx.isPresent()) {
                tx.get().activate();
            } else {
                LOG.warn("No transaction for this activation request {}", fwdC);
            }
        }
    }

    /**
     * Deactivate a MEF FcRoute.
     * @param route the existing route to deactivate
     */
    public void deactivate(@Nonnull FcRoute route) {
        for (GForwardingConstruct fwdC : route.getForwardingConstruct()) {
            Optional<ActivationTransaction> tx = prepareTransaction(fwdC);
            if (tx.isPresent()) {
                tx.get().deactivate();
            } else {
                LOG.warn("No transaction for this deactivation request {}", fwdC);
            }
        }
    }

    private Optional<ActivationTransaction> prepareTransaction(GForwardingConstruct fwdC) {
        final List<FcPort> list = fwdC.getFcPort();
        //TODO validate pre-condition
        final GFcPort a = list.get(0);
        final GFcPort z = list.get(1);

        return ForwardingConstructHelper.isTheSameNode(fwdC)
                ? getTxForNode(a,z, fwdC) : getTxForMultiNode(a,z, fwdC);
    }

    private Optional<ActivationTransaction> getTxForNode(GFcPort portA, GFcPort portZ, GForwardingConstruct fwdC) {
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

    private Optional<ActivationTransaction> getTxForMultiNode(GFcPort portA, GFcPort portZ, GForwardingConstruct fwdC) {
        //1. find and initialize drivers
        lock.readLock().lock();
        try {

            final ActivationDriverBuilder.BuilderContext ctx = new ActivationDriverBuilder.BuilderContext();
            ctx.put(GForwardingConstruct.class.getName(), fwdC);

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

    protected Optional<ActivationDriver> findDriver(GFcPort port, ActivationDriverBuilder.BuilderContext fwdC) {
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
        final GFcPort portA;
        final GFcPort portZ;
        final GForwardingConstruct fwC;

        public Context(GFcPort portA, GFcPort portZ, GForwardingConstruct fwC) {
            this.portA = portA;
            this.portZ = portZ;
            this.fwC = fwC;
        }
    }
}
