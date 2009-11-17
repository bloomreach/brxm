/*
 *  Copyright 2009 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.validation;

import java.util.Set;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;

/**
 * Validation constraint violation.  Provides the list of {@link FieldPath}s that
 * led up to the violation, plus a message that describes the problem. 
 */
public final class Violation implements IClusterable {
    private static final long serialVersionUID = 1L;

    private Set<FieldPath> fieldPaths;
    private IModel/*<String>*/message;

    public Violation(Set<FieldPath> fieldPaths, IModel/*<String>*/message) {
        this.fieldPaths = fieldPaths;
        this.message = message;
    }

    public IModel/*<String>*/getMessage() {
        return message;
    }

    public Set<FieldPath> getDependentPaths() {
        return fieldPaths;
    }

}
