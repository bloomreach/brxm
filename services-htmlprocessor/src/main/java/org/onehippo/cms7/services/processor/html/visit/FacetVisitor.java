/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.processor.html.visit;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.services.processor.html.model.Model;
import org.onehippo.cms7.services.processor.html.util.FacetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FacetVisitor extends NodeVisitor {

    public static final Logger log = LoggerFactory.getLogger(FacetVisitor.class);

    public static final String ATTRIBUTE_DATA_UUID = "data-uuid";

    protected FacetVisitor(final Model<Node> nodeModel) {
        super(nodeModel);
    }

    @Override
    public void onWrite(final Tag parent, final Tag tag) throws RepositoryException {
        if (parent == null) {
            // Remove all facetselects as they are no longer in use
            try {
                FacetUtil.removeFacets(getNode());
            } catch (RepositoryException ex) {
                log.error("Error removing unused links", ex);
            }
        }
    }

    protected String findOrCreateFacetNode(final String uuid) throws RepositoryException {
        final Node node = getNode();
        String name = FacetUtil.getChildFacetNameOrNull(node, uuid);
        if (name == null) {
            name = FacetUtil.createFacet(node, uuid);
        }
        return name;
    }
}
