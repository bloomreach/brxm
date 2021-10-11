/*
 * Copyright 2021 Bloomreach Inc. (http://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.validation;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.frontend.plugins.gallery.model.SvgOnLoadGalleryException;
import org.hippoecm.frontend.plugins.gallery.model.SvgScriptGalleryException;

public class SvgValidator {

    public SvgValidator() {
    }

    public static void validate(final String upload) throws
            SvgValidationException {
        final String svgContent = new String(upload.getBytes());
        if (StringUtils.containsIgnoreCase(svgContent, "<script")) {
            throw new SvgValidationException(
                    new SvgScriptGalleryException("SVG images with embedded script are not supported."));
        }
        if (StringUtils.containsIgnoreCase(svgContent, "onload=")) {
            throw new SvgValidationException(
                    new SvgOnLoadGalleryException("SVG images with onload attribute are not supported."));
        }
    }
}
