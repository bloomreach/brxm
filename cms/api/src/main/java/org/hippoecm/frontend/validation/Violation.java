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

import org.apache.wicket.model.IDetachable;

/**
 * Validation constraint violation.  Provides the list of {@link ModelPath}s that
 * led up to the violation, plus a message that describes the problem. 
 */
public final class Violation implements IDetachable {

    private static final long serialVersionUID = 1L;

    private Set<ModelPath> fieldPaths;
    private String messageKey;
    private Object[] parameters;

    public Violation(Set<ModelPath> fieldPaths, String message, Object[] parameters) {
        this.fieldPaths = fieldPaths;
        this.messageKey = message;
        this.parameters = parameters;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getParameters() {
        return parameters;
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
        sb.append(messageKey);
        return sb.toString();
    }

    public void detach() {
        for (ModelPath path : fieldPaths) {
            path.detach();
        }
    }

}
