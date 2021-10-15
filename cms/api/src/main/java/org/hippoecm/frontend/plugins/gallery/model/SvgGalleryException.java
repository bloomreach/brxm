/*
 * Copyright 2021 Bloomreach Inc. (www.bloomreach.com)
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
package org.hippoecm.frontend.plugins.gallery.model;

import org.hippoecm.frontend.validation.SvgValidationResult;

public class SvgGalleryException extends GalleryException {

    private final SvgValidationResult validationResult;

    public SvgGalleryException(final String message, final SvgValidationResult validationResult) {
        super(message);
        this.validationResult = validationResult;
    }

    public SvgGalleryException(final String message, final Throwable cause,
                                     final SvgValidationResult validationResult) {
        super(message, cause);
        this.validationResult = validationResult;
    }

    public SvgValidationResult getValidationResult() {
        return validationResult;
    }
}
