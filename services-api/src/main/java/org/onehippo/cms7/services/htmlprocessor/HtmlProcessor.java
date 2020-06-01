/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * Processes HTML that is meant to be read from or written to the repository. It can be used to fix malformed HTML,
 * remove unwanted elements and attributes, and transform elements into a representation needed by the CMS, e.g. images
 * and internal links.
 *
 * The process applied is:
 *
 *  1. Parse HTML into DOM tree
 *  2. Apply visitors to DOM tree
 *  3. Serialize DOM tree as string
 */
public interface HtmlProcessor extends Serializable {

    /**
     * Process stored HTML.
     *
     * @param html The stored HTML
     * @param visitors Visitors applied to the DOM tree
     * @return Processed HTML
     * @throws IOException when the DOM tree cannot be serialized
     */
    String read(final String html, final List<TagVisitor> visitors) throws IOException;

    /**
     * Process HTML to store.
     *
     * @param html The HTML to be stored
     * @param visitors Visitors applied to the DOM tree
     * @return Processed HTML
     * @throws IOException when the DOM tree cannot be serialized
     */
    String write(final String html, final List<TagVisitor> visitors) throws IOException;

}
