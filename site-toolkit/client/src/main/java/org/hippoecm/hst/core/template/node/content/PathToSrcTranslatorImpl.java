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
import javax.jcr.ValueFormatException;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathToSrcTranslatorImpl implements PathToSrcTranslator {
    /*
     * log all rewriting to the SourceRewriter interface
     */
    private Logger log = LoggerFactory.getLogger(ContentRewriter.class);
    HstRequestContext hstRequestContext;
    
    public PathToSrcTranslatorImpl(HstRequestContext hstRequestContext) {
       this.hstRequestContext = hstRequestContext; 
    }

    
    public String documentPathToSrc(Node node, String documentPath, boolean externalize) {
        Session session = null;
        try {
            session = node.getSession();

            if (documentPath == null || documentPath.length() == 0) {
                return "";
            }

            for (String prefix : Translator.EXTERNALS) {
                if (documentPath.startsWith(prefix)) {
                    return documentPath;
                }
            }
            
            try {
                documentPath = URLDecoder.decode(documentPath,"utf-8");
            } catch (UnsupportedEncodingException e1) {
                log.warn("UnsupportedEncodingException for documentPath");
            }

            if (documentPath.startsWith("/") && session.itemExists(documentPath)) {
                // TODO resolve these binaries though they should not be absolute in the first place
            }

            String postfix = null;
            String[] pathEls = documentPath.split("/");

            if (pathEls.length > 1) {
                // pathEls[0] refers to facetselect
                postfix = documentPath.substring(pathEls[0].length());
            }
            if(hstRequestContext.getUrlMapping() == null) {
                log.warn("UrlMapping is null, returning original path");
                return documentPath;
            }
            if (node.hasNode(pathEls[0])) {
                Node facetSelect = node.getNode(pathEls[0]);
                if (facetSelect.isNodeType(HippoNodeType.NT_FACETSELECT)
                        && facetSelect.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                    String uuid = facetSelect.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                    Node deref = session.getNodeByUUID(uuid);
                    documentPath = "/binaries" + deref.getPath();
                    if (postfix != null && !postfix.equals("/")) {
                        if(postfix.startsWith("/")) {
                            postfix = postfix.substring(1);
                        }
                        return hstRequestContext.getUrlMapping().rewriteLocation(deref.getNode(postfix), hstRequestContext, externalize).getUri();
                    } else {
                        return hstRequestContext.getUrlMapping().rewriteLocation(deref.getNode(deref.getName()), hstRequestContext, externalize).getUri();
                    }

                } else {
                    log.warn("Expected node of nodetype " + HippoNodeType.NT_FACETSELECT +". Cannot translate binary link");
                }
            } else {
                log.warn("Expected to find facetselect node '" +pathEls[0] + "' but not found. Unable to translate binary link" );
            }
        } catch (PathNotFoundException e) {
            // not possible because test for hasNode first
            log.error("Node not found " + e.getMessage());
        } catch (ValueFormatException e) {
            // cannot happen in principal
            log.error("ValueFormatException for docbase, expecting String value " + e.getMessage());
        } catch (ItemNotFoundException e) {
            // when this happens, the resource does not exist anymore in the repository
            log.warn("The binary does not exist (anymore). " + e.getMessage());
        } catch (RepositoryException e) {
            log.error("RepositoryException " + e.getMessage());
        }

        return documentPath;
    }

}
