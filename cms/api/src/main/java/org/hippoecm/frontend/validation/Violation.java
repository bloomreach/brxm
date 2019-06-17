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
package org.hippoecm.frontend.validation;

import java.util.Set;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;

/**
 * Validation constraint violation.  Provides the list of {@link ModelPath}s that
 * led up to the violation and three parameters that can be used to generate a translated description of the violation.
 * These parameters are a message key, an array of parameters for value substitution in the translation and a
 * resourceBundleClass to specify the location of the resource bundle.
 */
public final class Violation implements IDetachable {

    private final Set<ModelPath> fieldPaths;
    private final IModel<String> message;
    private ValidationScope validationScope;

    /**
     * Create a new violation with the specified message. The scope of this violation will be {@code
     * ValidationScope.DOCUMENT}
     *
     * @param paths        list of {@link ModelPath}s that led up to the violation
     * @param messageModel a model of the message to be shown to the user
     */
    public Violation(final Set<ModelPath> paths, final IModel<String> messageModel) {
        this.fieldPaths = paths;
        this.message = messageModel;
        this.validationScope = ValidationScope.DOCUMENT;
    }

    public Violation(final Set<ModelPath> fieldPaths, final IModel<String> message,
                     final ValidationScope validationScope) {
        this.fieldPaths = fieldPaths;
        this.message = message;
        this.validationScope = validationScope;
    }

    public IModel<String> getMessage() {
        return message;
    }

    public Set<ModelPath> getDependentPaths() {
        return fieldPaths;
    }

    public ValidationScope getValidationScope() {
        return validationScope;
    }

    public void setValidationScope(final ValidationScope validationScope) {
        this.validationScope = validationScope;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("paths: ");
        sb.append(fieldPaths.toString());
        sb.append(", message: ");
        sb.append(getMessage().getObject());
        return sb.toString();
    }

    public void detach() {
        fieldPaths.forEach(ModelPath::detach);
    }

}
