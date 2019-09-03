/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.ovs.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NullAwareDatastoreGetter<T> {

    private static final Logger LOG = LoggerFactory.getLogger(NullAwareDatastoreGetter.class);

    private Optional<T> dataOptional;

    public NullAwareDatastoreGetter(T data) {
        this.dataOptional = Optional.ofNullable(data);
    }

    public NullAwareDatastoreGetter(Optional<T> dataOptional) {
        this.dataOptional = dataOptional;
    }

    public <R extends ChildOf<? super T>> NullAwareDatastoreGetter<R> collect(Function<T, Supplier<R>> function) {
        logDataOptionalStatus(function);

        return new NullAwareDatastoreGetter<>(
                dataOptional.isPresent() ? Optional
                        .ofNullable(function.apply(dataOptional.get()).get()) : Optional.empty()
        );
    }

    public <R extends Augmentation<T>> NullAwareDatastoreGetter<R> collect(Function<T, Function<Class<R>, R>> function,
                                                                           Class<R> cls) {
        logDataOptionalStatus(function);

        return new NullAwareDatastoreGetter<>(
                dataOptional.isPresent() ? Optional
                        .ofNullable(function.apply(dataOptional.get()).apply(cls)) : Optional.empty()
        );
    }

    public <R extends DataObject, C extends Collection<R>> List<NullAwareDatastoreGetter<R>> collectMany(
            Function<T, Supplier<C>> function) {
        logDataOptionalStatus(function);

        List<NullAwareDatastoreGetter<R>> result = new ArrayList<>();

        Optional<C> dataCollectionOptional = dataOptional.isPresent() ? Optional
                .ofNullable(function.apply(dataOptional.get()).get()) : Optional.empty();

        dataCollectionOptional.ifPresent(rs -> rs
                .forEach(dataObject -> result.add(new NullAwareDatastoreGetter<R>(dataObject))));

        return result;
    }

    public Optional<T> get() {
        return dataOptional;
    }

    private void logDataOptionalStatus(Function<?, ?> function) {
        if (dataOptional.isPresent()) {
            LOG.trace("Before collection of: " + function.toString()
                    + ", currently collected data is non-null: " + dataOptional.get().toString());
        } else {
            LOG.debug("Null value encountered during collection of: " + function.toString());
        }
    }
}
