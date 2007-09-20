/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
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
package org.hippoecm.repository.api;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public interface HippoNode extends Node {

    public String getDisplayName() throws RepositoryException;

    /*
     * Other (future) extensions to the interface may include:
     *
     *   public boolean isDocument();
     * Which would at this time be equivalent to
     * isNodeType(HippoNodeType.NT_DOCUMENT)
     * and could be used to check whether the Node is in fact
     * a full blown document bonzai.
     *
     *   public Node getPrimary();
     *   public Node getPrimary(Map<String,String> facets);
     * These calls can be used to look up the primary handle of
     * the document.  The handle of a document is the JCR Node
     * under which all instances of a particular document reside
     * and which should be used for reference purposes.
     * The second version of this call returns in a pre-initialized
     * map the facets which should (according to the handle definition)
     * uniquely identify the document node for which the handle was
     * requested.
     */

}
