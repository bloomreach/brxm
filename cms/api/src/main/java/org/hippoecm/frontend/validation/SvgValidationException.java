/*
 * Copyright 2021-2023 Bloomreach Inc. (http://www.bloomreach.com)
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

import org.hippoecm.frontend.plugins.gallery.model.GalleryException;

public class SvgValidationException extends Exception {

    private final GalleryException galleryException;

    public SvgValidationException(final GalleryException e) {
        super(e);
        this.galleryException = e;
    }

    public GalleryException getGalleryException() {
        return galleryException;
    }
}
