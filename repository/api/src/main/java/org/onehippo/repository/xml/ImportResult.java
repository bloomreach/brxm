/*
 *  Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.xml;

import java.util.Collection;

import javax.jcr.Node;

/**
 * Encapsulates different aspects of the result of importing an enhanced system view xml document.
 * @see org.hippoecm.repository.api.HippoSession#importEnhancedSystemViewXML(String, java.io.InputStream, int, int, ContentResourceLoader)
 */
public interface ImportResult {

    /**
     * A context path is either the path to the root node of an added subtree
     * or the path to a node the imported xml merged with. The concept of context
     * paths is important when the xml needs to be reapplied due to a reload later.
     */
    Collection<String> getContextPaths();

    /**
     * Return either the root of the subtree that was added
     * or the root of the subtree that was merged with in the case of a delta combine.
     */
    Node getBaseNode();

}
