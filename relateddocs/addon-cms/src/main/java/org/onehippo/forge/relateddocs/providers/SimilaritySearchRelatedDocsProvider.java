/*
 *  Copyright 2009-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.relateddocs.providers;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.forge.relateddocs.RelatedDoc;
import org.onehippo.forge.relateddocs.RelatedDocCollection;
import org.onehippo.forge.relateddocs.RelatedDocsNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Searches for Nodes (documents) that are related based on Lucene's rep:similar Filters duplicates and adds Nodes of
 * type HippoNodetype.NT_HANDLE to a {@link RelatedDocCollection}
 *
 * @author jjoachimsthal
 */
public class SimilaritySearchRelatedDocsProvider extends AbstractRelatedDocsProvider {

    public static final String SCORE = "score";
    public static final double DEFAULT_SCORE = 1.0;
    public static final String DEFAULT_SCOPE = "content";

    private static final long serialVersionUID = 1L;
    private static final int MAX_RESULTS = 25;
    private double score;

    private static final Logger log = LoggerFactory.getLogger(SimilaritySearchRelatedDocsProvider.class);

    public SimilaritySearchRelatedDocsProvider(IPluginContext context, IPluginConfig config) {
        super(context, config);
        score = config.getDouble(SCORE, DEFAULT_SCORE);
    }

    @Override
    public RelatedDocCollection getRelatedDocs(JcrNodeModel documentModel) throws RepositoryException {

        JcrNodeModel nodeModel;
        RelatedDocCollection collection = new RelatedDocCollection();
        try {
            nodeModel = new JcrNodeModel(documentModel.getNode().getNode(RelatedDocsNodeType.NT_RELATEDDOCS));
        } catch (PathNotFoundException e) {
            //I think when a new document is opened, the document doesn't contain the "relateddocs" node yet, so we *may* need to create one -- Vijay
            log.info("Relateddocs node for current document not found, returning empty docs, so creating one.{}", e.getMessage());
            nodeModel = new JcrNodeModel(documentModel.getNode().addNode(RelatedDocsNodeType.NT_RELATEDDOCS, RelatedDocsNodeType.NT_RELATEDDOCS));
        }

        RelatedDocCollection currentCollection = new RelatedDocCollection(nodeModel);
        Set<String> uuidSet = new HashSet<>();
        for (RelatedDoc r : currentCollection) {
            uuidSet.add(r.getUuid());
        }

        Node docNode = documentModel.getNode();
        Node parentNode = docNode.getParent();
        String xpathQuery = createXPathQuery(docNode);

        if (log.isDebugEnabled()) {
            log.debug("Executing query{}: ", xpathQuery);
        }
        @SuppressWarnings(value = "deprecation")
        Query query = nodeModel.getNode().getSession().getWorkspace().getQueryManager().createQuery(
                xpathQuery, Query.XPATH);

        query.setLimit(MAX_RESULTS);
        RowIterator r = query.execute().getRows();

        while (r.hasNext()) {
            // retrieve the query results from the row
            Row row = r.nextRow();
            String path = row.getValue("jcr:path").getString();
            long myScore = row.getValue("jcr:score").getLong();

            // retrieve the found document from the repository
            try {
                Node document = nodeModel.getNode().getSession().getNode(path);
                Node itsParent = ((HippoNode) document).getCanonicalNode();
                if (parentNode.isSame(itsParent)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Found parent {}", itsParent.getPath());
                    }
                    continue;
                }
                if (document.isNodeType(HippoNodeType.NT_HANDLE)) {
                    if (uuidSet.contains(document.getIdentifier())) {
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Found related document {}", document.getPath());
                    }
                    collection.add(new RelatedDoc(new JcrNodeModel(document), this.score * myScore));
                } else if (itsParent.isNodeType(HippoNodeType.NT_HANDLE)) {
                    if (uuidSet.contains(itsParent.getIdentifier())) {
                        continue;
                    }
                    // exclude prototype stuff: note need to check this
                    final JcrNodeModel jcrNodeModel = new JcrNodeModel(itsParent);
                    if (jcrNodeModel.getNode().getPath().endsWith("hippo:prototype")) {
                        continue;
                    }
                    collection.add(new RelatedDoc(jcrNodeModel, this.score * myScore));
                }
            } catch (RepositoryException e) {
                log.error("Error handling SimilaritySearch results", e.getMessage());
            }
        }
        return collection;
    }

    /* The search for related documents is within the scope of the 'root site content'. To find the root site node:
     It is the *first* node from the jcr root down to the documentModel.getNode() that has the mixin 'hippotranslation:translated'.
     If no such node is found, we do the search relative to the jcr:root/content */
    private String createXPathQuery(final Node docNode) throws RepositoryException {
        Node rootNode = docNode.getSession().getRootNode();
        Node siteContentNode = null;
        String[] docPathSegments = docNode.getPath().substring(1).split("/");
        Node crNode = rootNode;
        int position = 0;
        while (position < docPathSegments.length && crNode.hasNode(docPathSegments[position])) {
            crNode = crNode.getNode(docPathSegments[position]);
            if (!crNode.isSame(docNode) && crNode.isNodeType("hippotranslation:translated")) {
                // found the root content node
                siteContentNode = crNode;
                break;
            }
            position++;
        }

        //Default scope: /content
        if (siteContentNode == null) {
            siteContentNode = rootNode.getNode(DEFAULT_SCOPE);
        }

        StringBuilder statement = new StringBuilder("//element(*, ").append(RelatedDocsNodeType.NT_RELATABLEDOCS).append(")[rep:similar(., '");
        statement.append(docNode.getPath()).append("')");
        statement.append(" and (@hippo:availability='preview' or (@hippo:availability = 'live'))");
        statement.append(" and @hippo:paths='").append(siteContentNode.getIdentifier()).append('\'');
        statement.append("]/.. order by @jcr:score descending");

        return statement.toString();
    }
}
