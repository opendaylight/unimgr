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

import org.mef.nrp.impl.ActivationDriver;
import org.mef.nrp.impl.ActivationDriverAmbiguousException;
import org.mef.nrp.impl.ActivationDriverBuilder;
import org.mef.nrp.impl.ActivationDriverNotFoundException;
import org.mef.nrp.impl.ActivationDriverRepoService;
import org.mef.nrp.impl.ActivationTransaction;
import org.mef.nrp.impl.ForwardingConstructHelper;
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
    private ReentrantReadWriteLock lock;

    public FcRouteActivatorService() {
        lock = new ReentrantReadWriteLock();
    }

    public void activate(FcRoute route) {
        for(GForwardingConstruct fwdC : route.getForwardingConstruct()) {
            Optional<ActivationTransaction> tx = prepareTransaction(fwdC);
            if (tx.isPresent()) {
                tx.get().activate();
            } else {
                LOG.warn("No transaction for this activation request {}", fwdC);
            }
        }
    }

    public void deactivate(FcRoute route) {
        for(GForwardingConstruct fwdC : route.getForwardingConstruct()) {
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

        return ForwardingConstructHelper.isTheSameNode(fwdC) ?  getTxForNode(a,z, fwdC) :
                getTxForMultiNode(a,z, fwdC);
    }

    private Optional<ActivationTransaction> getTxForMultiNode(GFcPort a, GFcPort z, GForwardingConstruct fwdC) {
        lock.readLock().lock();
        try {
            final ActivationDriverBuilder.BuilderContext ctx = new ActivationDriverBuilder.BuilderContext();
            ActivationDriver activator = activationRepoService.getDriver(a, z, ctx);

            activator.initialize(a, z, fwdC);
            ActivationTransaction tx = new ActivationTransaction();
            tx.addDriver(activator);
            return Optional.of(tx);
        } catch(ActivationDriverNotFoundException e) {
            LOG.warn("No unique activation driver found for {} <-> {}", a);
            return Optional.empty();
        } catch(ActivationDriverAmbiguousException e) {
            LOG.warn("Multiple activation driver found for {} <-> {}", z);
            return Optional.empty();
        } catch (Exception e) {
            LOG.error("driver initialization exception",e);
            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    private Optional<ActivationTransaction> getTxForNode(GFcPort a, GFcPort z, GForwardingConstruct fwdC) {
        //1. find and initialize drivers
        Optional<ActivationDriver> aActivator;
        Optional<ActivationDriver> zActivator;
        lock.readLock().lock();
        try {

            final ActivationDriverBuilder.BuilderContext ctx = new ActivationDriverBuilder.BuilderContext();
            ctx.put(GForwardingConstruct.class.getName(), fwdC);

            aActivator = findDriver(a, ctx);
            zActivator = findDriver(z, ctx);

            if (aActivator.isPresent() && zActivator.isPresent()) {
                aActivator.get().initialize(a, z, fwdC);
                zActivator.get().initialize(z, a, fwdC);
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

        final ActivationTransaction tx = new ActivationTransaction();
        tx.addDriver(aActivator.get());
        tx.addDriver(zActivator.get());

        return Optional.of(tx);
    }

    /***
     *
     * @param port
     * @param fwdC
     * @return activation driver or empty
     */
    protected Optional<ActivationDriver> findDriver(GFcPort port, ActivationDriverBuilder.BuilderContext fwdC) {
        if(activationRepoService == null)  {
            LOG.warn("Activation Driver repo is not initialized");
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(activationRepoService.getDriver(port, fwdC));
        } catch(ActivationDriverNotFoundException e) {
            LOG.warn("No unique activation driver found for {}", port);
            return Optional.empty();
        } catch(ActivationDriverAmbiguousException e) {
            LOG.warn("Multiple activation driver found for {}", port);
            return Optional.empty();
        }


    }

    public void setActivationRepoService(ActivationDriverRepoService activationRepoService) {
        lock.writeLock().lock();
        this.activationRepoService = activationRepoService;
        lock.writeLock().unlock();
    }


    public void unsetActivationRepoService() {
        lock.writeLock().lock();
        this.activationRepoService = null;
        lock.writeLock().unlock();
    }

    static final class Context {
        final GFcPort a;
        final GFcPort z;
        final GForwardingConstruct fwC;

        public Context(GFcPort a, GFcPort z, GForwardingConstruct fwC) {
            this.a = a;
            this.z = z;
            this.fwC = fwC;
        }
    }
}
