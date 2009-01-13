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

package org.hippoecm.frontend.plugins.xinha.services;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XinhaFacetHelper {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(XinhaFacetHelper.class);

    private String facetLink;
    private boolean alreadyPresent;
    private boolean useSharedFacets;

    public XinhaFacetHelper(boolean useSharedFacets) {
        this.useSharedFacets = useSharedFacets;
    }

    public String createFacet(Node node, String link, String uuid) throws ItemExistsException, PathNotFoundException,
            NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException {
        if (uuid == null) {
            log.error("uuid is null. Should never be possible for facet");
            return "";
        }
        
        visit(node, link, uuid, 0);
        if (!alreadyPresent) {
            Node facetselect = node.addNode(facetLink, HippoNodeType.NT_FACETSELECT);
            facetselect.setProperty(HippoNodeType.HIPPO_DOCBASE, uuid);
            facetselect.setProperty(HippoNodeType.HIPPO_FACETS, new String[] {});
            facetselect.setProperty(HippoNodeType.HIPPO_MODES, new String[] {});
            facetselect.setProperty(HippoNodeType.HIPPO_VALUES, new String[] {});
            // need a node save (the draft so no problem)
            node.getSession().save();
        }
        return facetLink;
    }
    
    private void visit(Node node, String link, String uuid, int postfix) {
        try {
            String testLink = link;
            if (postfix > 0) {
                testLink += "_" + postfix;
            }
            if (node.hasNode(testLink)) {
                Node htmlLinkNode = node.getNode(testLink);
                if (htmlLinkNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                    String docbase = htmlLinkNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getValue().getString();
                    if (docbase.equals(uuid)) {
                        // we already have a link for this internal link
                        if (useSharedFacets) { //, so reuse it
                            facetLink = testLink;
                            alreadyPresent = true;
                        } else {
                            visit(node, link, uuid, ++postfix);
                        }
                    } else {
                        // we already have a link of this name, but points to different node, hence, try with another name
                        visit(node, testLink, uuid, ++postfix);
                    }
                } else {
                    // there is a node which is has the same name as the testLink, but is not a facetselect, try with another name
                    visit(node, testLink, uuid, ++postfix);
                }
            } else {
                facetLink = testLink;
                alreadyPresent = false;
            }
        } catch (RepositoryException e) {
            log.error("error occured while validating Xinha link: " + link, e);
        }
    }
}
