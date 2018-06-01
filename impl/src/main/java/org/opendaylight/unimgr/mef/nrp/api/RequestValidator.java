/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.api;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceInput;

/**
 * @author bartosz.michalik@amartus.com
 */
public interface RequestValidator {
    ValidationResult checkValid(CreateConnectivityServiceInput input);
    ValidationResult checkValid(UpdateConnectivityServiceInput input);

    class ValidationResult {
        private final List<String> problems;


        public ValidationResult() {
            problems = new LinkedList<>();
        }

        public ValidationResult problem(String description) {
            this.problems.add(description);
            return this;
        }

        public List<String> getProblems() {
            return new ArrayList<>(problems);
        }

        public boolean isValid() {
            return problems.isEmpty();
        }
    }
}
