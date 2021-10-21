/*
 * Copyright 2015-2021 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.editor.plugins.resource;

/**
 * Exception thrown when the {@link MimeTypeHelper} validates mime type
 *
 * @version $Id$
 * @since 2015-01-28
 */
public class InvalidMimeTypeException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private String mimeType;

    public InvalidMimeTypeException(String message) {
        super(message);
    }

    public InvalidMimeTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidMimeTypeException(String message, String mimeType) {
        super(message);
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }
}
