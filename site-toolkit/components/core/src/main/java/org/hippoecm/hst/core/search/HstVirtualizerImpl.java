
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
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.LoggerFactory;

public class HstVirtualizerImpl implements HstVirtualizer {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstVirtualizerImpl.class);
    
    private String canonicalContentNodePath;
    private String contentFacetSelectNodePath;
    
    public HstVirtualizerImpl(String canonicalContentNodePath, String contentFacetSelectNodePath) {
        this.canonicalContentNodePath = canonicalContentNodePath;
        this.contentFacetSelectNodePath = contentFacetSelectNodePath;
    }

    public Node virtualize(Node canonical)  throws HstContextualizeException{
        
        String path = null;
        try {
             /*
              * If for nodes below a hippo:handle, we cannot take the path directly, as the path may look like: /foo/foo[2], where
              * the virtualized path does not contain [2]
              */
             if(canonical.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                 Node parent = canonical.getParent();
                 path = parent.getPath()+"/"+parent.getName();
             }else {
                 path = canonical.getPath();
             }
             if(canonicalContentNodePath == null || contentFacetSelectNodePath == null) {
                 throw new HstContextualizeException("Unable to virtualize node '"+path+"' ");
             }
             if(!path.startsWith(canonicalContentNodePath)) {
                 throw new HstContextualizeException("Unable to virtualize node '"+path+"' because does not start with expected path '"+canonicalContentNodePath+"'");
             }
             String virtualizedPath = contentFacetSelectNodePath + path.substring(canonicalContentNodePath.length());
             return (Node)canonical.getSession().getItem(virtualizedPath);
        } catch(PathNotFoundException e) {
            log.warn("The node '{}' cannot be virtualized. Return null", path);
            return null;
        }
        catch (RepositoryException e) {
            throw new HstContextualizeException("Unable to virtualize node '"+path+"' " , e);
        }
    }

}
