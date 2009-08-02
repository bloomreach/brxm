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
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.uuid.UUID;
import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstCtxWhereClauseComputerImpl implements HstCtxWhereClauseComputer{

    public final static Logger log = LoggerFactory.getLogger(HstCtxWhereClauseComputerImpl.class.getName()); 
    
    public HstVirtualizer getVirtualizer(Node ctxAwareNode) throws HstContextualizeException{
        try {
            Session session = ctxAwareNode.getSession();
            Node contentFacetSelectNode = null;
            Node start = ctxAwareNode;
            Node jcrRoot = session.getRootNode();
            while(!start.isSame(jcrRoot)) {
                start = start.getParent();
                if(start.isNodeType(Configuration.NODETYPE_HST_SITE)) {
                    // get the content mirror
                   if(start.hasNode(Configuration.NODENAME_HST_CONTENTNODE)) {
                       contentFacetSelectNode = start.getNode(Configuration.NODENAME_HST_CONTENTNODE);
                       break;
                   } else {
                       throw new HstContextualizeException("Cannot create a Virtualizer for : " + ctxAwareNode.getPath());
                   }
                }
            }
            if(start.isSame(jcrRoot)) {
                // it was not a search in a virtual tree in the first place. Return a NOOP virtualizer
                return new HstVirtualizer(){
                    public Node virtualize(Node canonical) throws HstContextualizeException {
                        return canonical;
                    } 
                };
            }
            if(contentFacetSelectNode == null || !contentFacetSelectNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                throw new HstContextualizeException("Cannot create a Virtualizer for : " + ctxAwareNode.getPath());
            }
           
            String docbase = contentFacetSelectNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
            try {
                UUID.fromString(docbase);
            } catch (IllegalArgumentException e){
                throw new HstContextualizeException("Unable to create a Virtualizer: '{}'", e);
            }
            
            Node canonicalContentNode = session.getNodeByUUID(docbase);
            
            String canonicalContentNodePath = canonicalContentNode.getPath();
            String contentFacetSelectNodePath = contentFacetSelectNode.getPath();
            return new HstVirtualizerImpl(canonicalContentNodePath, contentFacetSelectNodePath);
        } catch (RepositoryException e) {
           throw new HstContextualizeException("Unable to create a Virtualizer: '{}'", e);
        }
        
    }
    
    public String getCtxWhereClause(Node node) throws HstContextualizeException{
        StringBuffer facetSelectClauses = new StringBuffer();
        String path = null;
        try {
            path = node.getPath();
            if(!(node instanceof HippoNode)) {
                throw new HstContextualizeException("Cannot compute a ctx where clause for a non HippoNode : " + node.getPath());
            }
            
            HippoNode hnode = (HippoNode)node;
            HippoNode canonical = (HippoNode)hnode.getCanonicalNode();
            
            if(canonical == null) {
                throw new HstContextualizeException("Cannot compute a ctx where clause for a node that does not have a physical equivalence : " + node.getPath());
            }
            
            if(!canonical.isNodeType("mix:referenceable") && !canonical.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                throw new  HstContextualizeException("Cannot create a context where clause for node '"+canonical.getPath()+"'");
             }
            if (canonical.isSame(node)){
                // either the site content root node (hst:content) or just a physical node.
                if(node.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                   String scopeUUID = node.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                   facetSelectClauses.append("@").append(HippoNodeType.HIPPO_PATHS).append("='").append(scopeUUID).append("'");
                   getFacetSelectClauses(hnode.getSession(), hnode, facetSelectClauses , false);
                } else {
                    // We are not searching in a virtual structure: return "" , there is no context where, only a where on the scope of the node
                    if(log.isDebugEnabled()) {
                        log.debug("Not a search in a virtual structure. Return the scope for the node '{}' only.", node.getPath());
                    }
                    String scopeUUID =  canonical.getUUID();
                    facetSelectClauses.append("@").append(HippoNodeType.HIPPO_PATHS).append("='").append(scopeUUID).append("'");
                }
            } else {
                // we are searching in a virtual node. Let's compute the context where clause to represent this in a physical search
                // when we can get a canonical, we know for sure it is referenceable
                String scopeUUID =  canonical.getUUID();
                facetSelectClauses.append("@").append(HippoNodeType.HIPPO_PATHS).append("='").append(scopeUUID).append("'");
                getFacetSelectClauses(hnode.getSession(), hnode, facetSelectClauses , true);
            }
        } catch (RepositoryException e) {
           log.warn("Unable to get Context where clause: '{}'", e);
           throw new HstContextualizeException("Unable to get Context where clause", e);
        }
        
        if(facetSelectClauses.length() == 0) {
            throw new HstContextualizeException("Exception during creating ContextWhereClause. Cannot create a proper search.");
        }
        facetSelectClauses.append(" and not(@jcr:primaryType='nt:frozenNode')");
        log.debug("For node '{}' the ctxWhereClause is '{}'", path , facetSelectClauses.toString());
        return facetSelectClauses.toString();
    }
    
    private void getFacetSelectClauses(Session jcrSession, HippoNode node, StringBuffer facetSelectClauses, boolean traversUp) throws HstContextualizeException{
        try {
            if(node == null || node.isSame(jcrSession.getRootNode())) {
                return;
            }
            if (node.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                Value[] modes = node.getProperty(HippoNodeType.HIPPO_MODES).getValues();
                Value[] facets = node.getProperty(HippoNodeType.HIPPO_FACETS).getValues();
                Value[] values = node.getProperty(HippoNodeType.HIPPO_VALUES).getValues();

                if (modes.length == facets.length && facets.length == values.length) {
                    for (int i = 0; i < modes.length; i++) {
                        String mode = modes[i].getString();
                        String facet = facets[i].getString();
                        String value = values[i].getString();
                        if (mode.equals("clear") || mode.equals("stick")) {
                            log.debug("skipping mode 'clear' or 'stick' because ambigous how to handle them");
                            continue;
                        } else {
                            if (facetSelectClauses.length() > 0) {
                                facetSelectClauses.append(" and ");
                            }
                            if ("hippostd:state".equals(facet) && "unpublished".equals(value)) {
                                // special case
                                facetSelectClauses.append("(@hippostd:state='unpublished' or (@hippostd:state='published' and @hippostd:stateSummary!='changed'))");
                            } else {
                                facetSelectClauses.append("@").append(facet).append("='").append(value).append("'");
                            }
                        }
                    }
                } else {
                    log.warn("Skipping invalid facetselect encoutered where there are an unequal number of 'modes', 'facets' and 'values'");
                    throw new HstContextualizeException("Skipping invalid facetselect encoutered where there are an unequal number of 'modes', 'facets' and 'values'");
                }
            }
            
            if(traversUp) {
                HippoNode parent = (HippoNode)node.getParent();
                Node canonicalParent = parent.getCanonicalNode();
                if(canonicalParent != null) {
                    // only iterate up when we do have a canonical parent. 
                    if(parent.isSame(canonicalParent)) {
                    // if the parent is physical, we do need to further traverse up
                        if(parent.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                            getFacetSelectClauses(jcrSession,(HippoNode)node.getParent(),  facetSelectClauses, false);
                        }
                    } else {
                        getFacetSelectClauses(jcrSession, (HippoNode)node.getParent(),  facetSelectClauses, traversUp);
                    }
                } 
                
            }
        } catch (RepositoryException e) {
            log.warn("RepositoryException while trying to resolve facetselect clauses. Return null");
            throw new HstContextualizeException("RepositoryException while trying to resolve facetselect clauses. Return null", e);
        }
        
    }

}
