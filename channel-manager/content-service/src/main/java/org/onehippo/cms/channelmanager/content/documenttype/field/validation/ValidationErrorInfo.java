/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.channelmanager.content.documenttype.field.validation;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Object conveying a document field value validation error to the client.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationErrorInfo {

    /**
     * The "required" validation is meant to indicate that a primitive field must have content. What exactly that
     * means depends on the field type. The "required" validation is *not* meant to indicate that at least one
     * instance of a multiple field must be present.
     */
    public static final String REQUIRED = "required";

    private final String validation;
    private final String message; // localized

    public ValidationErrorInfo(final String validation, final String message) {
        this.validation = validation;
        this.message = message;
    }

    public String getValidation() {
        return validation;
    }

    public String getMessage() {
        return message;
    }
}
