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

    public String createFacet(Node node, String link, String uuid) throws ItemExistsException, PathNotFoundException,
            NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException {
        if (uuid == null) {
            log.error("uuid is null. Should never be possible for facet");
            return "";
        }

        String linkName = newLinkName(node, link);

        Node facetselect = node.addNode(linkName, HippoNodeType.NT_FACETSELECT);
        facetselect.setProperty(HippoNodeType.HIPPO_DOCBASE, uuid);
        facetselect.setProperty(HippoNodeType.HIPPO_FACETS, new String[] {});
        facetselect.setProperty(HippoNodeType.HIPPO_MODES, new String[] {});
        facetselect.setProperty(HippoNodeType.HIPPO_VALUES, new String[] {});

        // save the document (the draft so no problem)
        Node document = node.getParent();
        while (document.getDepth() > 0) {
            if (document.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                if (document.isNew()) {
                    log.warn("Document is new, saving session");
                    document.getSession().save();
                } else {
                    document.save();
                }
                break;
            }
            document.getParent();
        }

        return linkName;
    }

    private String newLinkName(Node node, String link) throws RepositoryException {
        if (!node.hasNode(link)) {
            return link;
        }
        int postfix = 1;
        while (true) {
            String testLink = link + "_" + postfix;
            if (!node.hasNode(testLink)) {
                return testLink;
            }
            postfix++;
        }
    }

}
