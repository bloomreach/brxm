/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.yui.upload.validation;

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.util.lang.Bytes;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.ValidationException;

public interface FileUploadValidationService extends IValidationService {

    String DEFAULT_MAX_FILE_SIZE = "10mb";
    String[] DEFAULT_EXTENSIONS_ALLOWED = new String[0];

    void validate(FileUpload upload) throws ValidationException;

    void addViolation(final String key, final Object... params);

    //TODO: here for client side validation - move to config class?
    String[] getAllowedExtensions();

    //TODO: here for backwards compatibility
    void setAllowedExtensions(String[] extensions);

    //TODO: here for client side validation - move to config class?
    Bytes getMaxFileSize();

}
