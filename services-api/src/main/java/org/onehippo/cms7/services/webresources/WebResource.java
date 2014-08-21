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
 * A web resource contains binary data that can be revisioned. There is always a current working version of the content.
 * There can be zero or more revisions of the content. Each revision is identified by an ID that is unique within
 * the revision history of this web resource.
 */
public interface WebResource {

    /**
     * @return the absolute path to this web resource, starting at web resources root.
     * The path always starts with a slash, and the path elements are also separated by slashes.
     */
    String getPath();

    /**
     * @return the name of this web resource, i.e. the last element of the path.
     */
    String getName();

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
     * @return the binary data of this content.
     */
    Binary getBinary();

    String getChecksum();

}
