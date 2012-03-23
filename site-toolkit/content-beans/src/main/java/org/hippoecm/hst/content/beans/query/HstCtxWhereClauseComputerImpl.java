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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.util.KeyValue;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstCtxWhereClauseComputerImpl implements HstCtxWhereClauseComputer{

    public final static Logger log = LoggerFactory.getLogger(HstCtxWhereClauseComputerImpl.class.getName()); 
    
    /**
     * When the search was in multiple scopes, we need to rewrite the physical hits to the context aware virtual path. 
     * 
     * As we have multiple scope, we try every scope, with the deepest scopes first. Therefor, a sorted treeSet is used
     */
    public HstVirtualizer getVirtualizer(List<Node> scopes, boolean skipInvalidScopes) throws HstContextualizeException{
        // sortable set as first the deepest paths, which are the most precise, most be tried as virtualizer.
        Set<KeyValue<String, String>> pathMappers = new TreeSet<KeyValue<String, String>>();
        for(Node scope : scopes ) {
            try {
                pathMappers.add(getPathMapper(scope));
            } catch (HstContextualizeException e){
                if(skipInvalidScopes) {
                    log.info("Skipping invalid scope: {}", e.getMessage());
                } else {
                    throw e;
                }
            }
        }
        
        return new HstVirtualizerImpl(pathMappers);
    }
    
    public HstVirtualizer getVirtualizer(Node scope) throws HstContextualizeException{
        Set<KeyValue<String, String>> pathMappers = new TreeSet<KeyValue<String, String>>();
        pathMappers.add(getPathMapper(scope));
        return new HstVirtualizerImpl(pathMappers);
    }
    
    protected Mapper getPathMapper(Node scope) throws HstContextualizeException{
        if(scope == null) {
            throw new HstContextualizeException("Cannot create a Virtualizer for a scope that is null" );
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
                       throw new HstContextualizeException("Cannot create a Virtualizer for : " + scope.getPath());
                   }
                }
            }
            if(start.isSame(jcrRoot)) {
                return new Mapper("/", "/");
            }
            if(contentFacetSelectNode == null || !contentFacetSelectNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                throw new HstContextualizeException("Cannot create a Virtualizer for : " + scope.getPath());
            }
           
            String docbase = contentFacetSelectNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
            try {
                UUID.fromString(docbase);
            } catch (IllegalArgumentException e){
                throw new HstContextualizeException("Unable to create a Virtualizer: node '"
                        + contentFacetSelectNode.getPath() + "' has illegal hippo:docbase '" + docbase + "'", e);
            }
            
            Node canonicalContentNode = session.getNodeByIdentifier(docbase);
            
            String canonicalContentNodePath = canonicalContentNode.getPath();
            String contentFacetSelectNodePath = contentFacetSelectNode.getPath();
            return new Mapper(canonicalContentNodePath,contentFacetSelectNodePath);
        } catch (RepositoryException e) {
           throw new HstContextualizeException("Unable to create a Virtualizer", e);
        }
    }
    
    protected static class Mapper implements KeyValue<String, String>, Comparable<Mapper> {
        String fromPath;
        String toPath;
        int length;

        Mapper(String fromPath, String toPath) throws HstContextualizeException {
            if (fromPath == null || toPath == null) {
                throw new HstContextualizeException("Unable to create a Virtualizer for null path");
            }
            this.fromPath = fromPath;
            this.toPath = toPath;
            this.length = fromPath.split("/").length;
        }

        public int compareTo(Mapper o) {
            if (o == null) {
                throw new NullPointerException("Not allowed to compare with null");
            }
            if(o == this || o.equals(this)) {
                return 0;
            }

            if (this.length > o.length) {
                // if the length, and thus the number of slashes is larger for this,
                // this Mapper should be tried as first as it is a deeper scoped mapper: Therefor
                // return -1
                return -1;
            }
            if(this.length < o.length) {
                return 1;
            }
            if(fromPath.length() > o.fromPath.length()) {
                return -1;
            }
            if(fromPath.length() < o.fromPath.length()) {
                return 1;
            }

            if(toPath.length() > o.toPath.length()) {
                return -1;
            }
            if(toPath.length() < o.toPath.length()) {
                return 1;
            }
            return 0;

        }

        public String getKey() {
            return this.fromPath;
        }

        public String getValue() {
            return this.toPath;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Mapper mapper = (Mapper) o;

            if (length != mapper.length) {
                return false;
            }
            if (!fromPath.equals(mapper.fromPath)) {
                return false;
            }
            if (!toPath.equals(mapper.toPath)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = fromPath.hashCode();
            result = 31 * result + toPath.hashCode();
            result = 31 * result + length;
            return result;
        }
        @Override
        public String toString(){
            return Mapper.class.getName() + ":" + fromPath + " --> " + toPath ;
        }
    }
    
    public String getCtxWhereClause(Node node) throws HstContextualizeException{
        StringBuilder facetSelectClauses = new StringBuilder();
        String path;
        if(node == null) {
            throw new HstContextualizeException("Unable to create context where clause for a node (scope) that is null.");
        }
        try {
            path = node.getPath();
            if(!(node instanceof HippoNode)) {
                throw new HstContextualizeException("Cannot compute a context where clause for a non HippoNode : " + node.getPath());
            }
            
            HippoNode currentNode = (HippoNode)node;
            Node canonical = currentNode.getCanonicalNode();
            
            
            while(canonical == null) {
                // the current <code>node</code> is a virtual only node. Take the parent until there is a canonical version.
                // before we hit the jcr root, we certainly get a non null canonical node
                log.debug("The current scope '{}' is part of a virtual only node. We take the first ancestor that has a canonical version as scope." , node.getPath());
                currentNode = (HippoNode)currentNode.getParent();
                canonical = currentNode.getCanonicalNode();
            }
            
            while(!canonical.isNodeType("mix:referenceable") && !canonical.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                log.info("The current scope '{}' does not point to a referenceable node. Take the first referenceable ancestor.", canonical.getPath());
                if(canonical.isSame(canonical.getSession().getRootNode())) {
                    log.info("First referenceable node for canonical is the jcr root node. Take the root node as scope");
                    break;
                }
                canonical = canonical.getParent();
            }
            
            if (canonical.isSame(currentNode)){
                // either the site content root node (hst:content) or just a physical node.
                if(currentNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                   String scopeUUID = currentNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                   facetSelectClauses.append("@").append(HippoNodeType.HIPPO_PATHS).append("='").append(scopeUUID).append("'");
                   appendFacetSelectClauses(currentNode.getSession(), currentNode, facetSelectClauses , false);
                } else {
                    // We are not searching in a virtual structure: the context where is the scope of the node
                    if(log.isDebugEnabled()) {
                        log.debug("Not a search in a virtual structure. Return the scope for the node '{}' only.", currentNode.getPath());
                    }
                    String scopeUUID =  canonical.getIdentifier();
                    facetSelectClauses.append("@").append(HippoNodeType.HIPPO_PATHS).append("='").append(scopeUUID).append("'");
                }
            } else {
                // we are searching in a virtual node. Let's compute the context where clause to represent this in a physical search
                // when we can get a canonical, we know for sure it is referenceable
                String scopeUUID =  canonical.getIdentifier();
                facetSelectClauses.append("@").append(HippoNodeType.HIPPO_PATHS).append("='").append(scopeUUID).append("'");
                appendFacetSelectClauses(currentNode.getSession(), currentNode, facetSelectClauses , true);
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
    
    // TODO HSTTWO-849 : improve the combined context computed by not repeating specific parts of all the scopes that do not add new criteria to the search
    public String getCtxWhereClause(List<Node> scopes, boolean skipInvalidScopes) throws HstContextualizeException {
        StringBuilder combinedCtxClauses = new StringBuilder();
        List<String> ctxes = new ArrayList<String>();
        for(Node scope : scopes) {
            try {
                String ctxForScope = getCtxWhereClause(scope);
                if(ctxForScope != null && !"".equals(ctxForScope)) {
                    ctxes.add(ctxForScope);
                }
            } catch (HstContextualizeException e) {
                if(skipInvalidScopes) {
                    log.info("Skipping invalid scope: {}", e.getMessage());
                } else {
                    throw e;
                }
            }
        }
        if(ctxes.size() == 0) {
            throw new HstContextualizeException("At least one valid scope to search in must be present. No valid scopes found. ");
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
    
    private void appendFacetSelectClauses(Session jcrSession, HippoNode node, StringBuilder facetSelectClauses, boolean traverseUp) throws HstContextualizeException{
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
                            // continue to next
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
            
            if(traverseUp) {
                HippoNode parent = (HippoNode)node.getParent();
                Node canonicalParent = parent.getCanonicalNode();
                if(canonicalParent != null) {
                    // only iterate up when we do have a canonical parent. 
                    if(parent.isSame(canonicalParent)) {
                    // if the parent is physical, we do need to further traverse up
                        if(parent.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                            appendFacetSelectClauses(jcrSession,(HippoNode)node.getParent(),  facetSelectClauses, false);
                        }
                    } else {
                        appendFacetSelectClauses(jcrSession, (HippoNode)node.getParent(),  facetSelectClauses, traverseUp);
                    }
                } 
                
            }
        } catch (RepositoryException e) {
            log.warn("RepositoryException while trying to resolve facetselect clauses. Return null");
            throw new HstContextualizeException("RepositoryException while trying to resolve facetselect clauses. Return null", e);
        }
        
    }


}
