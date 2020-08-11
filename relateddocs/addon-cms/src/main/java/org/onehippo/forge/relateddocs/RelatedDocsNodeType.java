/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.relateddocs;

/**
 * Interface that contains the statics for RelatedDocs
 */
public interface RelatedDocsNodeType {
    /**
     * jcrMixinType for Nodes that can act as related documents
     */
    public static final String NT_RELATABLEDOCS = "relateddocs:relatabledocs";

    /**
     * Child node to hold all the related docs.
     */
    public static final String NT_RELATEDDOCS = "relateddocs:docs";

    /**
     * Name of child node to hold facetselects to related documents.
     */
    public static final String RELATEDDOCS_RELDOC = "relateddocs:reldoc";

}
