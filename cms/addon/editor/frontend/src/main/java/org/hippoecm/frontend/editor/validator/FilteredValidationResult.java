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
package org.hippoecm.frontend.editor.validator;

import java.util.HashSet;
import java.util.Set;

import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.validation.FieldElement;
import org.hippoecm.frontend.validation.FieldPath;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.Violation;

/**
 * Validation result that is filtered by a particular field.  I.e. it only contains
 * violations for items that exist as part of the field, applied to a node.
 */
public class FilteredValidationResult implements IValidationResult {
    private static final long serialVersionUID = 1L;

    private final IValidationResult upstream;
    private final IFieldDescriptor field;

    public FilteredValidationResult(IValidationResult result, IFieldDescriptor field) {
        this.upstream = result;
        this.field = field;
    }

    public Set<Violation> getViolations() {
        Set<Violation> orig = upstream.getViolations();
        Set<Violation> result = new HashSet<Violation>();
        for (Violation violation : orig) {
            Set<FieldPath> paths = violation.getDependentPaths();
            for (FieldPath path : paths) {
                if (path.getElements()[0].getField().equals(field)) {
                    result.add(filterViolation(violation));
                    break;
                }
            }
        }
        return result;
    }

    public boolean isValid() {
        return upstream.isValid() || getViolations().size() == 0;
    }

    private Violation filterViolation(Violation violation) {
        Set<FieldPath> origPaths = violation.getDependentPaths();
        Set<FieldPath> newPaths = new HashSet<FieldPath>();
        for (FieldPath path : origPaths) {
            if (path.getElements()[0].getField().equals(field)) {
                FieldElement[] elements = new FieldElement[path.getElements().length - 1];
                System.arraycopy(path.getElements(), 1, elements, 0, elements.length);
                newPaths.add(new FieldPath(elements));
            }
        }
        return new Violation(newPaths, violation.getMessage());
    }
}