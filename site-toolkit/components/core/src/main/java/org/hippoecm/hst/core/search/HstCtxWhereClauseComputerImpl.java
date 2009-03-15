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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstCtxWhereClauseComputerImpl implements HstCtxWhereClauseComputer{

    public final static Logger log = LoggerFactory.getLogger(HstCtxWhereClauseComputerImpl.class.getName()); 
    
    public String getCtxWhereClause(Node node, HstRequestContext requestContext) {
        try {
            if(!(node instanceof HippoNode)) {
                log.warn("Cannot compute a ctx where clause for a non HippoNode '{}'. Return", node.getPath());
                return null;
            }
            
            HippoNode hnode = (HippoNode)node;
            
            HippoNode canonical = (HippoNode)hnode.getCanonicalNode();
            if(canonical == null) {
                log.warn("Cannot compute a ctx where clause for a node that does not have a physical equivalence: '{}'. Return", node.getPath());
                return null;
            }
            else if (canonical.isSame(node)){
                // either the site content root node (hst:content) or just a physical node.
                if(node.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                    //
                } else {
                    // We are not searching in a virtual structure: return null, there is no context where
                    log.debug("Not a search in a virtual structure. Return null");
                    return null;
                }
                log.debug("The search is ");
            } else {
                // we are searching in a virtual node. Let's compute the context where clause to represent this in a physical search
            }
            Node rootNode = node.getSession().getRootNode();
        } catch (RepositoryException e) {
           log.warn("Unable to get Context where clause: '{}'", e);
        }
        
        
        return null;
    }

}
