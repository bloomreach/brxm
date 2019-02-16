/*
 *  Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.onehippo.cms7.services.validation.ValidationScope;

/**
 * Interface implemented by the CMS validation mechanism to enable custom validators to check values and report
 * violations.
 */
public interface IFieldValidator extends IClusterable {

    IFieldDescriptor getFieldDescriptor();

    ITypeDescriptor getFieldType();

    /**
     * Create a Violation object with field information.
     *
     * @param childModel
     * @param key
     * @throws ValidationException
     * @deprecated : use the {@link #newValueViolation(IModel, IModel, ValidationScope)} signature instead
     */
    @Deprecated
    Violation newValueViolation(IModel childModel, String key) throws ValidationException;

    /**
     * Create a Violation object with field information.
     *
     * @param childModel the JcrPropertyValueModel or JcrNodeModel of the child {@link javax.jcr.Node}
     * @param message    model containing the message to be shown to the user
     * @throws ValidationException when information to validate is not available
     * @deprecated use {@link #newValueViolation(IModel, IModel, ValidationScope)} instead
     */
    @Deprecated
    Violation newValueViolation(IModel childModel, IModel<String> message) throws ValidationException;

    /**
     * Create a Violation object with field and scope information.
     *
     * @param childModel the JcrPropertyValueModel or JcrNodeModel of the child {@link javax.jcr.Node}
     * @param message    model containing the message to be shown to the user
     * @param scope      to indicate the level the validation applies to
     * @throws ValidationException when information to validate is not available
     */
    default Violation newValueViolation(IModel childModel, IModel<String> message, ValidationScope scope)
            throws ValidationException {
        final Violation violation = newValueViolation(childModel, message);
        violation.setValidationScope(scope);
        return violation;
    }
}
