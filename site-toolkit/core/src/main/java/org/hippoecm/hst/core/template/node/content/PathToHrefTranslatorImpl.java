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
package org.hippoecm.hst.core.template.node.content;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.core.mapping.URLMapping;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathToHrefTranslatorImpl implements PathToHrefTranslator{

    /*
     * log all rewriting to the SourceRewriter interface
     */
    private Logger log = LoggerFactory.getLogger(SourceRewriter.class);
    private URLMapping mapping;
    
    public PathToHrefTranslatorImpl(URLMapping mapping) {
       this.mapping = mapping; 
    }

    public String documentPathToHref(Node node, String documentPath) {
        
        for(String prefix: Translator.EXTERNALS){
            if(documentPath.startsWith(prefix)) {
                return documentPath;
            }
        }
        if(mapping == null ) {
            log.warn("UrlMapping is null, returning original path");
            return documentPath;
        } else {
            // translate the documentPath to a URL in combination with the Node and the mapping object
            if(documentPath.startsWith("/")) {
                // absolute location, try to translate directly
                log.warn("absolute path location found in text field. Trying to rewrite the location directly");
                return mapping.rewriteLocation(documentPath);
            } else {
                // relative node, most likely a facetselect node:
                String uuid =null;
                try {
                    Node facetSelectNode = node.getNode(documentPath);
                    if(facetSelectNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                        uuid = facetSelectNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                        Session session = node.getSession();
                        Node deref = session.getNodeByUUID(uuid);
                        log.debug("rewrite '" + deref.getPath() +"' --> '" + mapping.rewriteLocation(deref) + "'");
                        return mapping.rewriteLocation(deref);
                    } else {
                        log.warn("relative node as link, but the node is not a facetselect. Unable to rewrite this to a URL");
                    }
                } catch (ItemNotFoundException e) {
                    log.error("Node with uuid '" + uuid +"' not found. Unable to rewrite link");
                } catch (PathNotFoundException e) {
                    log.error("Node '" + documentPath +"' not found. Unable to rewrite link");
                } catch (RepositoryException e) {
                    log.error("Unable to rewrite link. RepositoryException " + e.getMessage());
                }
            }
        }
        // TODO translate document path
        return documentPath;
    }

}
