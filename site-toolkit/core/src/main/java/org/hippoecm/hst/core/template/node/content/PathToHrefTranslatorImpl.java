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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.core.filters.base.HstRequestContext;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathToHrefTranslatorImpl implements PathToHrefTranslator{

    /*
     * log all rewriting to the SourceRewriter interface
     */
    private Logger log = LoggerFactory.getLogger(ContentRewriter.class);
    private HstRequestContext hstRequestContext;
    
    public PathToHrefTranslatorImpl(HstRequestContext hstRequestContext) {
       this.hstRequestContext = hstRequestContext; 
    }

    public String documentPathToHref(Node node, String documentPath) {
        
        for(String prefix: Translator.EXTERNALS){
            if(documentPath.startsWith(prefix)) {
                return documentPath;
            }
        }
        
        if(hstRequestContext.getUrlMapping() == null || node == null) {
            log.warn("UrlMapping is null, returning original path");
            return documentPath;
        } else {
            try {
                documentPath = URLDecoder.decode(documentPath,"utf-8");
            } catch (UnsupportedEncodingException e1) {
                log.warn("UnsupportedEncodingException for documentPath");
            }
            
            // translate the documentPath to a URL in combination with the Node and the mapping object
            if(documentPath.startsWith("/")) { 
                // absolute location, try to translate directly
                log.warn("Cannot rewrite absolute path '" + documentPath + "'. Expected a relative path");
            } else {
                // relative node, most likely a facetselect node:
                String uuid =null;
                try {
                    Node facetSelectNode = node.getNode(documentPath);
                    if(facetSelectNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                        uuid = facetSelectNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                        Session session = node.getSession();
                        Node deref = session.getNodeByUUID(uuid);
                        log.debug("rewrite '{}' --> '{}'", deref.getPath(), hstRequestContext.getUrlMapping().rewriteLocation(deref, hstRequestContext));
                        return hstRequestContext.getUrlMapping().rewriteLocation(deref, hstRequestContext);
                    } else {
                        log.warn("relative node as link, but the node is not a facetselect. Unable to rewrite this to a URL");
                    }
                } catch (ItemNotFoundException e) {
                    log.warn("Node with uuid '" + uuid +"' not found. Unable to rewrite link");
                } catch (PathNotFoundException e) {
                    log.warn("Node '" + documentPath +"' not found. Unable to rewrite link");
                } catch (RepositoryException e) {
                    log.warn("Unable to rewrite link. RepositoryException " + e.getMessage());
                }
            }
        }
        // TODO translate document path
        return documentPath;
    }

}
