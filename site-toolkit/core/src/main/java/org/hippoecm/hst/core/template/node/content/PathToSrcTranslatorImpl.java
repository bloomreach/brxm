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
import javax.jcr.ValueFormatException;

import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathToSrcTranslatorImpl implements PathToSrcTranslator {
    /*
     * log all rewriting to the SourceRewriter interface
     */
    private Logger log = LoggerFactory.getLogger(SourceRewriter.class);

    public String documentPathToSrc(Node node, String documentPath) {
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

            if (documentPath.startsWith("/") && session.itemExists(documentPath)) {
                // TODO resolve these binaries though they should not be absolute in the first place
            }

            String postfix = null;
            String[] pathEls = documentPath.split("/");

            if (pathEls.length > 1) {
                // pathEls[0] refers to facetselect
                postfix = documentPath.substring(pathEls[0].length());
            }

            if (node.hasNode(pathEls[0])) {
                Node facetSelect = node.getNode(pathEls[0]);
                if (facetSelect.isNodeType(HippoNodeType.NT_FACETSELECT)
                        && facetSelect.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                    String uuid = facetSelect.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                    Node handle = session.getNodeByUUID(uuid);
                    documentPath = "/binaries" + handle.getPath();
                    if (postfix != null) {
                        documentPath += postfix;
                    } else {
                        documentPath += "/" + handle.getName();
                    }

                } else {
                    // log error
                }
            } else {
                // log error
            }
        } catch (PathNotFoundException e) {
            // also not possible because test for hasNode first
            log.error("Node not found " + e.getMessage());
        } catch (ValueFormatException e) {
            // cannot happen in principal
            log.error("ValueFormatException for docbase, expecting String value " + e.getMessage());
        } catch (ItemNotFoundException e) {
            // when this happens, the resource does not exist anymore in the repository
            log.error("The binary does not exist (anymore). " + e.getMessage());
        } catch (RepositoryException e) {
            log.error("RepositoryException " + e.getMessage());
        }

        return documentPath;
    }

}
