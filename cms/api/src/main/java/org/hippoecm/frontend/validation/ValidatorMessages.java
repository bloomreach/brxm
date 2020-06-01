/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.lang.String;

import org.apache.wicket.model.IModel;

/**
 * Violation messages that are available for validators.
 * They can be used to create a message model:
 * <code>
 *   IModel&lt;String&gt; message = new ClassResourceModel(key, ValidatorMessages.class);
 * </code>
 * or as keys to pass to the method {@link IFieldValidator#newValueViolation(IModel, String)}.
 * (note that such use is deprecated though, as validators should provide their own messages)
 * <p>
 * Use of these messages is deprecated.  Validators should provide their own translations.
 * e.g. using resource bundles.
 */
@Deprecated
public interface ValidatorMessages {

    String INVALID_XML = "invalid-xml";
    String HTML_IS_EMPTY = "html-is-empty";
    String REQUIRED_FIELD_NOT_PRESENT = "required-field-not-present";
    String REFERENCE_IS_EMPTY = "reference-is-empty";
    String PATH_USED_MULTIPLE_TIMES = "path-is-used-multiple-times";
}
