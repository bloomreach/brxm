/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.core.search;

import java.util.List;

import javax.jcr.Node;

import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * Implementations should return an xpath filter (where clause) that accounts for searching in virtual subtrees. It is AND-ed with 
 * the where clause from a normal xpath search.  
 * This is only needed for sites/application rendering and searching virtual structures. The actual implementation of
 * a <code>HstCtxWhereClauseComputer</code> is accessible through <code>{@ling org.hippoecm.hst.core.request.HstRequestContext#getHstCtxWhereClauseComputer()}</code>
 */
public interface HstCtxWhereClauseComputer {

    /**
     * This method returns the context where clause (without the '[' ']' brackets ) that is appended to the 'normal' xpath where clause for searching in virtual strutures. 
     * The scope to search from, the {@link Node} must be translated into a where clause, and also all possible ancestor filters that 
     * result in the virtual structure most be accounted for in the where clause computer. When the search is done in a non virtual <code>Node</code>,
     * the {@link #getCtxWhereClause(Node, HstRequestContext)} is allowed to simply return <code>null</code>
     * 
     * @param node the <code>{@link Node}</code> below which (in other words the scope) the search is done
     * @return the string containing the xpath where clause (without the enclosing '[' and ']') or <code>null</code> when it cannot 
     * compute one. If there is no where clause, for example because the node is the jcr root node, just "" should be returned
     * @throws HstContextualizeException 
     */
    String getCtxWhereClause(Node node) throws HstContextualizeException;
    
    /**
     * returns the context where clause as from {@link #getCtxWhereClause(Node)}, only now combines a set of scopes.
     * @param scopes
     * @param skipInvalidScopes <code>true</code> when invalid scopes are skipped
     * @return the string containing the xpath where clause (without the enclosing '[' and ']') representing all scopes or <code>null</code> when it cannot 
     * compute one. If there is no where clause, for example because the node is the jcr root node, just "" should be returned
     * @throws HstContextualizeException
     */
    String getCtxWhereClause(List<Node> scopes, boolean skipInvalidScopes) throws HstContextualizeException;
    
    /**
     * Returns a virtualizer for the scope. If the scope is physical, a virtualizer can be 
     * returned that just returns the physical node. 
     * @param scope
     * @return A virtualizer for the scope. If <code>null</code> is returned, no virtualization can be done
     * @throws HstContextualizeException
     */
    HstVirtualizer getVirtualizer(Node scope) throws HstContextualizeException;
    
    /**
    * Returns a virtualizer for the scopes. 
    * @param scopes all the scopes that was searched in
    * @param skipInvalidScopes whether to skip invalid scopes. If false, a HstContextualizeException is thrown when an invalid scope is found
    * @return A virtualizer for the scopes. If <code>null</code> is returned, no virtualization can be done
    * @throws HstContextualizeException
    */
   HstVirtualizer getVirtualizer(List<Node> scopes, boolean skipInvalidScopes) throws HstContextualizeException;

   
}
