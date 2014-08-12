/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.webresources;

import java.util.Calendar;

/**
 * The content of a web resource.
 */
public interface Content {

    /**
     * @return the revision ID of this content, or <code>null</code> when this content is not revisioned.
     */
    String getRevisionId();

    /**
     * @return the encoding of this content.
     */
    String getEncoding();

    /**
     * @return the last time this content has changed.
     */
    Calendar getLastModified();

    /**
     * @return the MIME type of this content.
     */
    String getMimeType();

    /**
     * @return a unique hash value of this content based on its binary data. Content with the same binary data will
     * always return the same hash. It is extremely unlikely that content with different binary data returns the same
     * hash.
     */
    String getHash();

    /**
     * @return the binary data of this content.
     */
    Binary getBinary();

}
