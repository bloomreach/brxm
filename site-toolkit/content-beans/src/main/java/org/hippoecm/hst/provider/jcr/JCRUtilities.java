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
package org.hippoecm.hst.provider.jcr;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.uuid.UUID;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCRUtilities {

    private static final Logger log = LoggerFactory.getLogger(JCRUtilities.class);
    
    public static Node getCanonical(Node n) {
        if(n instanceof HippoNode) {
            HippoNode hnode = (HippoNode)n;
            try {
                Node canonical = hnode.getCanonicalNode();
                if(canonical == null) {
                    log.debug("Cannot get canonical node for '{}'. This means there is no phyiscal equivalence of the " +
                            "virtual node. Return null", n.getPath());
                }
                return canonical;
            } catch (RepositoryException e) {
                log.error("Repository exception while fetching canonical node. Return null" , e);
                return null;
            }
        } 
        return n;
    }


    /**
     * 
     * @param mirrorNode
     * @return the dereferenced node or <code>null</code> when no dereferenced node can be found
     */
    public static Node getDeref(Node mirrorNode) {
        String docBaseUUID = null;
        try {
            if(!mirrorNode.isNodeType(HippoNodeType.NT_FACETSELECT) && !mirrorNode.isNodeType(HippoNodeType.NT_MIRROR)) {
                log.info("Cannot deref a node that is not of (sub)type '{}' or '{}'. Return null", HippoNodeType.NT_FACETSELECT, HippoNodeType.NT_MIRROR);
                return null;
            }
            // HippoNodeType.HIPPO_DOCBASE is a mandatory property so no need to test if exists
            docBaseUUID = mirrorNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
            
            // test whether docBaseUUID can be parsed as a uuid
            try {
                UUID.fromString(docBaseUUID);
            } catch(IllegalArgumentException e) {
                log.warn("Docbase cannot be parsed to a valid uuid. Return null");
                return null;
            }
            return mirrorNode.getSession().getNodeByUUID(docBaseUUID);
        } catch (ItemNotFoundException e) {
            String path = null;
            try {
                path = mirrorNode.getPath();
            } catch (RepositoryException e1) {
                log.error("RepositoryException, cannot return deferenced node: {}", e1);
            }
            log.info("ItemNotFoundException, cannot return deferenced node because docbase uuid '{}' cannot be found. The docbase property is at '{}/hippo:docbase'. Return null", docBaseUUID, path);
        } catch (RepositoryException e) {
            log.error("RepositoryException, cannot return deferenced node: {}", e);
        }
        
        return null;
    }
}
