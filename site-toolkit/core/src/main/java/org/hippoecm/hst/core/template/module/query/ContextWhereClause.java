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
package org.hippoecm.hst.core.template.module.query;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.hst.core.Timer;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextWhereClause {
    
    public static final Logger log = LoggerFactory.getLogger(ContextWhereClause.class);
    
    private Node contextNode;
    private String target;
    
    public ContextWhereClause(Node contextNode, String target) {
        this.contextNode = contextNode;
        this.target = target;
    }
    
    public String getWhereClause(){
        HippoNode contentBaseNode = null;
        String contextBasePath = null;
        String contentBaseUuid = null;
        String contextClauses = "";
        long start = System.currentTimeMillis();
        try {
            contextBasePath = contextNode.getPath();
            try {
                contentBaseNode = (HippoNode) contextNode.getNode(target);
            } catch (PathNotFoundException e) {
                log.warn("target '" + target + "' is not a subnode of '" + contextBasePath + "'.");
                return null;
            }
            /*
             * find all facetselects in the contentBaseNode and up untill we are at the contextNode. For every facetselect found
             * add the filter as a query
             */
            Node rootNode = contentBaseNode.getSession().getRootNode();
            
            while (!contentBaseNode.isSame(contextNode) && !contentBaseNode.isSame(rootNode)) {
                
                // the contentBaseUuid will be the uuid of the first 'referenceable node' or docbase of a facetselect
                if(contentBaseUuid == null) {
                    if (contentBaseNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                        contentBaseUuid = contentBaseNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                        
                    } else if (contentBaseNode.getCanonicalNode() != null
                            && contentBaseNode.getCanonicalNode().isNodeType("mix:referenceable")) {
                        contentBaseUuid = contentBaseNode.getCanonicalNode().getUUID();
                    } else {
                        log.warn("Target '"+target+"' is not pointing to a referenceable node or facetselect. Trying to search from the parent");
                    }
                }
                if (contentBaseNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                    // below all mandatory multivalued props
                    Value[] modes = contentBaseNode.getProperty(HippoNodeType.HIPPO_MODES).getValues();
                    Value[] facets = contentBaseNode.getProperty(HippoNodeType.HIPPO_FACETS).getValues();
                    Value[] values = contentBaseNode.getProperty(HippoNodeType.HIPPO_VALUES).getValues();
                    if (modes.length == facets.length && facets.length == values.length) {
                        for (int i = 0; i < modes.length; i++) {
                            String mode = modes[i].getString();
                            String facet = facets[i].getString();
                            String value = values[i].getString();
                            if (mode.equals("clear") || mode.equals("stick")) {
                                // do not know how to handle these in a query
                                log.debug("skipping mode 'clear' or 'stick' because ambigous how to handle them");
                                continue;
                            } else {
                                if(contextClauses.length() > 0) {
                                    contextClauses += " and ";
                                }
                                if ("hippostd:state".equals(facet) && "unpublished".equals(value)) {
                                    // special case
                                    contextClauses += "(@hippostd:state='unpublished' or (@hippostd:state='published' and @hippostd:stateSummary!='changed'))";
                                } else {
                                    contextClauses += "@" + facet + "='" + value + "'";
                                }
                            }
                        }
                    } else {
                        log
                                .warn("invalid facetselect encoutered where there are an unequal number of 'modes', 'facets' and 'values'");
                    }
                }
                contentBaseNode = (HippoNode) contentBaseNode.getParent();
            }

        } catch (RepositoryException e) {
            log.error("Cannot prepare statement: RepositoryException " + e.getMessage());
            return null;
        }
        
        if(contextClauses.length() > 0) {
            contextClauses += " and " + "@" + HippoNodeType.HIPPO_PATHS + "='" + contentBaseUuid + "' and not(@jcr:primaryType='nt:frozenNode')" ;
        } else {
            contextClauses =  "@" + HippoNodeType.HIPPO_PATHS + "='" + contentBaseUuid + "' and not(@jcr:primaryType='nt:frozenNode')"; 
        }
        Timer.log.debug("creating search context where clauses took " + (System.currentTimeMillis() - start) + " ms.");
        return contextClauses;
    }
}
