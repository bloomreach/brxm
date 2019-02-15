/*
 *  Copyright 2009-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.validator;

import java.util.HashSet;
import java.util.Set;

import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.ModelPath;
import org.hippoecm.frontend.validation.ModelPathElement;
import org.hippoecm.frontend.validation.Violation;

/**
 * Validation result that is filtered by a particular field.  I.e. it only contains
 * violations for items that exist as part of the field, applied to a node.
 */
public class FilteredValidationResult implements IValidationResult {

    private final IValidationResult upstream;
    private final ModelPathElement element;

    public FilteredValidationResult(final IValidationResult result, final ModelPathElement element) {
        this.upstream = result;
        this.element = element;
    }

    public Set<Violation> getViolations() {
        final Set<Violation> result = new HashSet<>();
        if (upstream == null) {
            return result;
        }
        final Set<Violation> orig = upstream.getViolations();
        for (final Violation violation : orig) {
            final Set<ModelPath> paths = violation.getDependentPaths();
            for (final ModelPath path : paths) {
                if (path.getElements().length > 0) {
                    final ModelPathElement first = path.getElements()[0];
                    if (!element.equals(first)) {
                        continue;
                    }
                    result.add(filterViolation(violation));
                    break;
                }
            }
        }
        return result;
    }

    public boolean isValid() {
        return upstream == null || upstream.isValid() || getViolations().isEmpty();
    }

    private Violation filterViolation(final Violation violation) {
        final Set<ModelPath> origPaths = violation.getDependentPaths();
        final Set<ModelPath> newPaths = new HashSet<>();
        for (final ModelPath path : origPaths) {
            if (path.getElements().length > 0) {
                final ModelPathElement first = path.getElements()[0];
                if (!element.equals(first)) {
                    continue;
                }
                final ModelPathElement[] elements = new ModelPathElement[path.getElements().length - 1];
                System.arraycopy(path.getElements(), 1, elements, 0, elements.length);
                newPaths.add(new ModelPath(elements));
            }
        }
        return new Violation(newPaths, violation.getMessage(), violation.getValidationScope());
    }

    public void detach() {
        upstream.detach();
    }

}
