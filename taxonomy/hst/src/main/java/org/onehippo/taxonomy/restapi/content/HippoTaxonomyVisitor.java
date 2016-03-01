/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.taxonomy.restapi.content;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.hst.restapi.ResourceContext;
import org.hippoecm.hst.restapi.content.visitors.HippoPublicationWorkflowDocumentVisitor;
import org.hippoecm.hst.restapi.scanning.PrimaryNodeTypeNodeVisitor;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms7.services.contenttype.ContentTypeChild;

import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_CATEGORYINFOS;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_DESCRIPTION;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_KEY;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_NAME;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_SYNONYMS;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_TAXONOMY;

@SuppressWarnings("unused")
@PrimaryNodeTypeNodeVisitor
public class HippoTaxonomyVisitor extends HippoPublicationWorkflowDocumentVisitor {

    @Override
    public String getNodeType() {
        return NODETYPE_HIPPOTAXONOMY_TAXONOMY;
    }

    protected void visitNode(final ResourceContext context, final Node node, final Map<String, Object> response)
            throws RepositoryException {
        super.visitNode(context, node, response);

        final LinkedHashMap<String, Object> categories = new LinkedHashMap<>();
        for (Node child : new NodeIterable(node.getNodes())) {
            if (child.isNodeType(NODETYPE_HIPPOTAXONOMY_CATEGORY)) {
                visitCategory(context, child, categories);
            }
        }
        if (!categories.isEmpty()) {
            response.put("categories", categories);
        }
    }

    protected void visitCategory(final ResourceContext context, final Node catNode, final Map<String, Object> response)
            throws RepositoryException {
        final LinkedHashMap<String, Object> category = new LinkedHashMap<>();
        response.put(catNode.getName(), category);
        category.put("key", catNode.getProperty(HIPPOTAXONOMY_KEY).getString());

        final LinkedHashMap<String, Object> categories = new LinkedHashMap<>();
        for (Node child : new NodeIterable(catNode.getNodes())) {
            if (child.isNodeType(NODETYPE_HIPPOTAXONOMY_CATEGORY)) {
                visitCategory(context, child, categories);
            }
            else if (child.isNodeType(HIPPOTAXONOMY_CATEGORYINFOS)) {
                for (Node infoNodes : new NodeIterable(child.getNodes())) {
                    final LinkedHashMap<String, Object> localeProperties = new LinkedHashMap<>();
                    category.put(infoNodes.getName(), localeProperties);
                    localeProperties.put("name", infoNodes.getProperty(HIPPOTAXONOMY_NAME).getString());
                    if (infoNodes.hasProperty(HIPPOTAXONOMY_DESCRIPTION)) {
                        localeProperties.put("description", infoNodes.getProperty(HIPPOTAXONOMY_DESCRIPTION).getString());
                    }
                    if (infoNodes.hasProperty(HIPPOTAXONOMY_SYNONYMS)) {
                        final ArrayList<String> synonyms = new ArrayList<>();
                        for (Value value : infoNodes.getProperty(HIPPOTAXONOMY_SYNONYMS).getValues()) {
                            if (value.getString() != null) {
                                synonyms.add(value.getString());
                            }
                        }
                        if (!synonyms.isEmpty()) {
                            localeProperties.put("synonyms", synonyms);
                        }
                    }
                }
            }
        }
        if (!categories.isEmpty()) {
            category.put("categories", categories);
        }
    }

    @Override
    protected boolean skipChild(final ResourceContext context, final ContentTypeChild childType, final Node child)
            throws RepositoryException {
        return child.isNodeType(NODETYPE_HIPPOTAXONOMY_CATEGORY);
    }
}
