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

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextWhereClause {
    
    public static final Logger log = LoggerFactory.getLogger(ContextWhereClause.class);
    
    private final HippoNode contentContextNode;
    private final String target;
    
    public ContextWhereClause(HippoNode contentContextNode, String target) {
        this.contentContextNode = contentContextNode;
        this.target = target;
    }
    
    public String getWhereClause(){
        HippoNode searchFromNode = null;
        String contentBaseUuid = null;
        StringBuffer contextClauses = new StringBuffer();
        long start = System.currentTimeMillis();
        try {
            searchFromNode = contentContextNode;
            if(target != null && !"".equals(target)) {
                if(target.startsWith("/")) {
                    log.debug("Target is absolute path '{}'. Search will be done below /{} in the repository", target, target);
                    // TODO have to do a search with the target absolute. We only need to create a context where clause that
                    // accounts for the current filter && the absolute target path
                } else {
                    try {
                        searchFromNode = (HippoNode) contentContextNode.getNode(target);
                    } catch (PathNotFoundException e) {
                        log.warn("target '{}' is not a subnode of '{}'.", target , searchFromNode.getPath());
                        return null;
                    }
                }
            }
            
            /*
             * find all facetselects in the contentBaseNode and up untill we are at the contextNode. For every facetselect found
             * add the filter as a query
             */
            Node rootNode = searchFromNode.getSession().getRootNode();
            Node parentOfContentContextNode = contentContextNode.getParent();
            while (!searchFromNode.isSame(parentOfContentContextNode) && !searchFromNode.isSame(rootNode)) {
                
                // the contentBaseUuid will be the uuid of the first 'referenceable node' or docbase of a facetselect
                if(contentBaseUuid == null) {
                    if (searchFromNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                        contentBaseUuid = searchFromNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                        
                    } else if (searchFromNode.getCanonicalNode() != null
                            && searchFromNode.getCanonicalNode().isNodeType("mix:referenceable")) {
                        contentBaseUuid = searchFromNode.getCanonicalNode().getUUID();
                    } else {
                        log.warn("Target '"+target+"' is not pointing to a referenceable node or facetselect. Trying to search from the parent");
                    }
                }
                if (searchFromNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                    // below all mandatory multivalued props
                    Value[] modes = searchFromNode.getProperty(HippoNodeType.HIPPO_MODES).getValues();
                    Value[] facets = searchFromNode.getProperty(HippoNodeType.HIPPO_FACETS).getValues();
                    Value[] values = searchFromNode.getProperty(HippoNodeType.HIPPO_VALUES).getValues();
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
                                    contextClauses.append(" and ");
                                }
                                if ("hippostd:state".equals(facet) && "unpublished".equals(value)) {
                                    // special case
                                    contextClauses.append("(@hippostd:state='unpublished' or (@hippostd:state='published' and @hippostd:stateSummary!='changed'))");
                                } else {
                                    contextClauses.append("@").append(facet).append("='").append(value).append("'");
                                }
                            }
                        }
                    } else {
                        log
                                .warn("invalid facetselect encoutered where there are an unequal number of 'modes', 'facets' and 'values'");
                    }
                }
                searchFromNode = (HippoNode) searchFromNode.getParent();
            }

        } catch (RepositoryException e) {
            log.warn("Cannot prepare statement: RepositoryException " + e.getMessage());
            return null;
        }
        
        if(contextClauses.length() > 0) {
            contextClauses.append(" and ");
            contextClauses.append("@").append(HippoNodeType.HIPPO_PATHS).append("='").append(contentBaseUuid).append("'");
        } else {
            contextClauses.append("@").append(HippoNodeType.HIPPO_PATHS).append("='").append(contentBaseUuid).append("'");
        }
        contextClauses.append(" and not(@jcr:primaryType='nt:frozenNode')");
        log.debug("creating search context where clauses took " + (System.currentTimeMillis() - start) + " ms for clause '{}'", contextClauses.toString());
        return contextClauses.toString();
    }
}
