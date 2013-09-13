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
    private String messageKey;
    private Object[] parameters;
    private Class<?> resourceBundleClass;
    private IModel<String> message;

    /**
     * Create a new violation whose resource bundle is looked up relative to the class that uses the
     * violation instead of the class that creates the violation. Since this can easily lead to various bugs and
     * difficult extensibility this constructor has been deprecated in favor of
     * {@code Violation(Set<ModelPath> fieldPaths, Class<?> resourceBundleClass, String messageKey, Object[] parameters)}
     *
     * @param fieldPaths  List of {@link ModelPath}s that led up to the violation
     * @param messageKey  The key used for translation
     * @param parameters  Optional parameters for value substitution in translations
     */
    @Deprecated
    public Violation(Set<ModelPath> fieldPaths, String messageKey, Object[] parameters) {
        this.fieldPaths = fieldPaths;
        this.messageKey = messageKey;
        this.parameters = parameters;
        this.message = new ClassResourceModel(messageKey, ValidatorMessages.class, parameters);
    }

    /**
     * Create a new violation whose resource bundle is looked up relative to the {@code resourceBundleClass} parameter.
     *
     * @param resourceBundleClass Resource bundle will be looked up relative to this class
     * @param messageKey  The key used for translation
     * @param parameters  Optional parameters for value substitution in translations
     * @param fieldPaths  List of {@link ModelPath}s that led up to the violation
     */
    @Deprecated
    public Violation(Class<?> resourceBundleClass, String messageKey, Object[] parameters, Set<ModelPath> fieldPaths) {
        this.fieldPaths = fieldPaths;
        this.messageKey = messageKey;
        this.parameters = parameters;
        this.resourceBundleClass = resourceBundleClass;
        this.message = new ClassResourceModel(messageKey, resourceBundleClass, parameters);
    }

    public Violation(final Set<ModelPath> paths, final IModel<String> messageModel) {
        this.fieldPaths = paths;
        this.message = messageModel;
    }

    @Deprecated
    public String getMessageKey() {
        return messageKey;
    }

    @Deprecated
    public Object[] getParameters() {
        return parameters;
    }

    /**
     * Returns the class of the resource bundle that contains the translation of the message.
     * Deprecated; use the {@link #getMessage()} method to get a (resolved) message.
     */
    @Deprecated
    public Class<?> getResourceBundleClass() {
        return resourceBundleClass;
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
