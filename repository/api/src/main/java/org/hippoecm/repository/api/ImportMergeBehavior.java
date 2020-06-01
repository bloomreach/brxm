/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.api;

/**
 * <b>This call is not (yet) part of the API, but under evaluation.</b><p/>
 * The possible actions specified by the <code>mergeBehavior</code>
 * parameter in {@link HippoSession#importDereferencedXML}.
 * When a node already exists in the repository on the same path during import, the value specified by the mergeBehavior governs what to do in such a case.
 * The mergeBehavior parameter must be just one of these values.
 * @deprecated there is no substitution for this class, content merging is done using enhanced system view xml semantics
 */
@Deprecated
public interface ImportMergeBehavior {

    /**
     * When a node already exists in the repository on the same path, skip the node in the import and its subtree.
     */
    public static final int IMPORT_MERGE_SKIP = 0;
    /**
     * When a node already exits, drop the existing node and import th enew node.
     */
    public static final int IMPORT_MERGE_OVERWRITE = 1;
    /**
     * When a node already exist, try to add a new node as a same-name sibling.  If the parenting node type definition does not allow same-name siblings of that name, revert to skip behavior.
     */
    public static final int IMPORT_MERGE_ADD_OR_SKIP = 2; // try add first else skip
    /**
     * When a node already exist, try to add a new node as a same-name sibling.  If the parenting node type definition does not allow same-name siblings of that name, revert to overwrite behavior.
     */
    public static final int IMPORT_MERGE_ADD_OR_OVERWRITE = 3; //try add first else overwrite
    /**
     * When a node alread exist, throw an {@link javax.jcr.ItemExistsException} exception and abort the import.  The import so far is kept in session transient state.
     */
    public static final int IMPORT_MERGE_THROW = 4;
    /**
     * Disable merging with existing nodes.
     */
    public static final int IMPORT_MERGE_DISABLE = 5;
}
