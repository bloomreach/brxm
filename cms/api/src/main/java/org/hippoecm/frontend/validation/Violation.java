/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.lang.Class;
import java.lang.Object;
import java.lang.String;
import java.util.Set;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;

/**
 * Validation constraint violation.  Provides the list of {@link ModelPath}s that
 * led up to the violation and three parameters that can be used to generate a translated description of the violation.
 * These parameters are a message key, an array of parameters for value substitution in the translation and a
 * resourceBundleClass to specify the location of the resource bundle.
 */
public final class Violation implements IDetachable {

    private static final long serialVersionUID = 1L;

    private Set<ModelPath> fieldPaths;
    private IModel<String> message;

    /**
     * Create a new violation whose resource bundle is looked up relative to the {@code resourceBundleClass} parameter.
     * This constructor has been deprecated.  Validators should provide their own message translation.
     *
     * @param resourceBundleClass Resource bundle will be looked up relative to this class
     * @param messageKey  The key used for translation
     * @param parameters  Optional parameters for value substitution in translations
     * @param fieldPaths  List of {@link ModelPath}s that led up to the violation
     */
    @Deprecated
    public Violation(Class<?> resourceBundleClass, String messageKey, Object[] parameters, Set<ModelPath> fieldPaths) {
        this.fieldPaths = fieldPaths;
        this.message = new ClassResourceModel(messageKey, resourceBundleClass, parameters);
    }

    /**
     * Create a new violation with the specified message.
     *
     * @param paths  list of {@link ModelPath}s that led up to the violation
     * @param messageModel a model of the message to be shown to the user
     */
    public Violation(final Set<ModelPath> paths, final IModel<String> messageModel) {
        this.fieldPaths = paths;
        this.message = messageModel;
    }

    @Deprecated
    public String getMessageKey() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public Object[] getParameters() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the class of the resource bundle that contains the translation of the message.
     * Deprecated; use the {@link #getMessage()} method to get a (resolved) message.
     */
    @Deprecated
    public Class<?> getResourceBundleClass() {
        throw new UnsupportedOperationException();
    }

    public IModel<String> getMessage() {
        return message;
    }

    public Set<ModelPath> getDependentPaths() {
        return fieldPaths;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("paths: ");
        sb.append(fieldPaths.toString());
        sb.append(", message: ");
        sb.append(getMessage().getObject());
        return sb.toString();
    }

    public void detach() {
        for (ModelPath path : fieldPaths) {
            path.detach();
        }
    }

}
