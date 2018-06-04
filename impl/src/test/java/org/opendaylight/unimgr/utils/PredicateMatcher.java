/*
 * Copyright (c) 2018 Amartus and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.utils;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.util.function.Predicate;

/**
 * @author bartosz.michalik@amartus.com
 */
public class PredicateMatcher<T> extends BaseMatcher<T> {

    private final Predicate<T> predicate;

    public PredicateMatcher(Predicate<T> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean matches(Object item) {
        return predicate.test((T) item);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Predicate not fulfiled");
    }

    public static <U> PredicateMatcher<U> fromPredicate(Predicate<U> predicate) {
        return new PredicateMatcher<>(predicate);
    }
}
