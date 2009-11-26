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
package org.hippoecm.hst.content.beans.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.uuid.UUID;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.util.KeyValue;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstCtxWhereClauseComputerImpl implements org.hippoecm.hst.content.beans.query.HstCtxWhereClauseComputer{

    public final static Logger log = LoggerFactory.getLogger(HstCtxWhereClauseComputerImpl.class.getName()); 
    
    /**
     * When the search was in multiple scopes, we need to rewrite the physical hits to the context aware virtual path. 
     * 
     * As we have multiple scope, we try every scope, with the deepest scopes first. Therefor, a sorted treeSet is used
     */
    public org.hippoecm.hst.content.beans.query.HstVirtualizer getVirtualizer(List<Node> scopes, boolean skipInvalidScopes) throws org.hippoecm.hst.content.beans.query.HstContextualizeException{
        // sortable set as first the deepest paths, which are the most precise, most be tried as virtualizer.
        Set<KeyValue<String, String>> pathMappers = new TreeSet<KeyValue<String, String>>();
        for(Node scope : scopes ) {
            try {
                pathMappers.add(getPathMapper(scope));
            } catch (org.hippoecm.hst.content.beans.query.HstContextualizeException e){
                if(skipInvalidScopes) {
                    log.info("Skipping invalid scope: {}", e.getMessage());
                } else {
                    throw e;
                }
            }
        }
        
        return new HstVirtualizerImpl(pathMappers);
    }
    
    public org.hippoecm.hst.content.beans.query.HstVirtualizer getVirtualizer(Node scope) throws org.hippoecm.hst.content.beans.query.HstContextualizeException{
        Set<KeyValue<String, String>> pathMappers = new HashSet<KeyValue<String, String>>();
        pathMappers.add(getPathMapper(scope));
        return new HstVirtualizerImpl(pathMappers);
    }
    
    protected Mapper getPathMapper(Node scope) throws org.hippoecm.hst.content.beans.query.HstContextualizeException{
        if(scope == null) {
            throw new org.hippoecm.hst.content.beans.query.HstContextualizeException("Cannot create a Virtualizer for a scope that is null" );
        }
        try {
            Session session = scope.getSession();
            Node contentFacetSelectNode = null;
            Node start = scope;
            Node jcrRoot = session.getRootNode();
            while(!start.isSame(jcrRoot)) {
                start = start.getParent();
                if(start.isNodeType(HstNodeTypes.NODETYPE_HST_SITE)) {
                    // get the content mirror
                   if(start.hasNode(HstNodeTypes.NODENAME_HST_CONTENTNODE)) {
                       contentFacetSelectNode = start.getNode(HstNodeTypes.NODENAME_HST_CONTENTNODE);
                       break;
                   } else {
                       throw new org.hippoecm.hst.content.beans.query.HstContextualizeException("Cannot create a Virtualizer for : " + scope.getPath());
                   }
                }
            }
            if(start.isSame(jcrRoot)) {
                return new Mapper("/", "/");
            }
            if(contentFacetSelectNode == null || !contentFacetSelectNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                throw new org.hippoecm.hst.content.beans.query.HstContextualizeException("Cannot create a Virtualizer for : " + scope.getPath());
            }
           
            String docbase = contentFacetSelectNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
            try {
                UUID.fromString(docbase);
            } catch (IllegalArgumentException e){
                throw new org.hippoecm.hst.content.beans.query.HstContextualizeException("Unable to create a Virtualizer: '{}'", e);
            }
            
            Node canonicalContentNode = session.getNodeByUUID(docbase);
            
            String canonicalContentNodePath = canonicalContentNode.getPath();
            String contentFacetSelectNodePath = contentFacetSelectNode.getPath();
            return new Mapper(canonicalContentNodePath,contentFacetSelectNodePath);
        } catch (RepositoryException e) {
           throw new org.hippoecm.hst.content.beans.query.HstContextualizeException("Unable to create a Virtualizer: '{}'", e);
        }
    }
    
    protected class Mapper implements KeyValue<String, String>, Comparable<Mapper> {
        String fromPath;
        String toPath;
        int length;

        Mapper(String fromPath, String toPath) throws org.hippoecm.hst.content.beans.query.HstContextualizeException {
            if (fromPath == null || toPath == null) {
                throw new org.hippoecm.hst.content.beans.query.HstContextualizeException("Unable to create a Virtualizer for null path");
            }
            this.fromPath = fromPath;
            this.toPath = toPath;
            this.length = fromPath.split("/").length;
        }

        public int compareTo(Mapper o) {
            if (o == null) {
                return 1;
            }
            if(o == this || o.equals(this)) {
                return 0;
            }
            
            if (this.length == o.length) {
                return 1;
            }
            
            if (this.length > o.length) {
                return -1;
            } else {
                return 1;
            }

        }

        public String getKey() {
            return this.fromPath;
        }

        public String getValue() {
            return this.toPath;
        }
        
        // only the fromPath is important because this is the key
        @Override 
        public int hashCode(){
            return fromPath.hashCode();
        }
        
       
        @Override
        public boolean equals(Object obj) {
            if(obj == null || !(obj instanceof Mapper)) {
                return false;
            }
            if(this.fromPath.equals(((Mapper)obj).fromPath)) {
                return true;
            }
            return false;
        }

        @Override
        public String toString(){
            return Mapper.class.getName() + ":" + fromPath + " --> " + toPath ;
        }
    }
    
    public String getCtxWhereClause(Node node) throws org.hippoecm.hst.content.beans.query.HstContextualizeException{
        StringBuilder facetSelectClauses = new StringBuilder();
        String path = null;
        if(node == null) {
            throw new org.hippoecm.hst.content.beans.query.HstContextualizeException("Unable to create context where clause for a node (scope) that is null.");
        }
        try {
            path = node.getPath();
            if(!(node instanceof HippoNode)) {
                throw new org.hippoecm.hst.content.beans.query.HstContextualizeException("Cannot compute a context where clause for a non HippoNode : " + node.getPath());
            }
            
            HippoNode hnode = (HippoNode)node;
            HippoNode canonical = (HippoNode)hnode.getCanonicalNode();
            
            if(canonical == null) {
                throw new org.hippoecm.hst.content.beans.query.HstContextualizeException("Cannot compute a context where clause for a node that does not have a physical equivalence : " + node.getPath());
            }
            
            if(!canonical.isNodeType("mix:referenceable") && !canonical.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                throw new  org.hippoecm.hst.content.beans.query.HstContextualizeException("Cannot create a context where clause for node '"+canonical.getPath()+"'");
            }
            if (canonical.isSame(node)){
                // either the site content root node (hst:content) or just a physical node.
                if(node.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                   String scopeUUID = node.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                   facetSelectClauses.append("@").append(HippoNodeType.HIPPO_PATHS).append("='").append(scopeUUID).append("'");
                   getFacetSelectClauses(hnode.getSession(), hnode, facetSelectClauses , false);
                } else {
                    // We are not searching in a virtual structure: the context where is the scope of the node
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
           throw new org.hippoecm.hst.content.beans.query.HstContextualizeException("Unable to get Context where clause", e);
        }
        
        if(facetSelectClauses.length() == 0) {
            throw new org.hippoecm.hst.content.beans.query.HstContextualizeException("Exception during creating ContextWhereClause. Cannot create a proper search.");
        }
        facetSelectClauses.append(" and not(@jcr:primaryType='nt:frozenNode')");
        log.debug("For node '{}' the ctxWhereClause is '{}'", path , facetSelectClauses.toString());
        return facetSelectClauses.toString();
    }
    
    // TODO HSTTWO-849 : improve the combined context computed by not repeating specific parts of all the scopes that do not add new criteria to the search
    public String getCtxWhereClause(List<Node> scopes, boolean skipInvalidScopes) throws org.hippoecm.hst.content.beans.query.HstContextualizeException {
        StringBuilder combinedCtxClauses = new StringBuilder();
        List<String> ctxes = new ArrayList<String>();
        for(Node scope : scopes) {
            try {
                String ctxForScope = getCtxWhereClause(scope);
                if(ctxForScope != null && !"".equals(ctxForScope)) {
                    ctxes.add(ctxForScope);
                }
            } catch (org.hippoecm.hst.content.beans.query.HstContextualizeException e) {
                if(skipInvalidScopes) {
                    log.info("Skipping invalid scope: {}", e.getMessage());
                } else {
                    throw e;
                }
            }
        }
        if(ctxes.size() == 0) {
            throw new org.hippoecm.hst.content.beans.query.HstContextualizeException("At least one valid scope to search in must be present. No valid scopes found. ");
        }
        
        if(ctxes.size() == 1) {
            return ctxes.get(0);
        } else {
            boolean first = true;
            for(String ctx : ctxes) {
                if(!first) {
                    combinedCtxClauses.append(" or "); 
                }
                combinedCtxClauses.append("(").append(ctx).append(")");
                first = false;
            }
            return combinedCtxClauses.toString();
        }
        
    }
    
    private void getFacetSelectClauses(Session jcrSession, HippoNode node, StringBuilder facetSelectClauses, boolean traversUp) throws org.hippoecm.hst.content.beans.query.HstContextualizeException{
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
                    throw new org.hippoecm.hst.content.beans.query.HstContextualizeException("Skipping invalid facetselect encoutered where there are an unequal number of 'modes', 'facets' and 'values'");
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
            throw new org.hippoecm.hst.content.beans.query.HstContextualizeException("RepositoryException while trying to resolve facetselect clauses. Return null", e);
        }
        
    }


}
