/*
 * Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.hippoecm.repository.api.HippoQuery;
import org.onehippo.forge.relateddocs.RelatedDoc;
import org.onehippo.forge.relateddocs.RelatedDocCollection;
import org.onehippo.forge.relateddocs.RelatedDocsNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferringRelatedDocsProvider extends AbstractRelatedDocsProvider {

    public static final String SCORE = "score";
    public static final double DEFAULT_SCORE = 0.5;

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ReferringRelatedDocsProvider.class);
    private static final int MAX_RESULTS = 25;

    private double score;

    public ReferringRelatedDocsProvider(IPluginContext context, IPluginConfig config) {
        super(context, config);
        score = config.getDouble(SCORE, DEFAULT_SCORE);
    }

    @Override
    public RelatedDocCollection getRelatedDocs(JcrNodeModel documentModel) throws RepositoryException {

        RelatedDocCollection collection = new RelatedDocCollection();
        JcrNodeModel nodeModel;
        try {
            nodeModel = new JcrNodeModel(documentModel.getNode().getNode(RelatedDocsNodeType.NT_RELATEDDOCS));
        } catch (PathNotFoundException e) {
            //I think when a new document is opened, the document doesn't coantin the "realteddocs" node yet, so we *may* need to create one -- Vijay
            log.info("Cannot find relateddocs node for the current node, so creating the node.", e);
            nodeModel = new JcrNodeModel(documentModel.getNode().addNode(RelatedDocsNodeType.NT_RELATEDDOCS, RelatedDocsNodeType.NT_RELATEDDOCS));
        }


        RelatedDocCollection currentCollection = new RelatedDocCollection(nodeModel);
        Set<String> uuidSet = new HashSet<String>();
        for (RelatedDoc r : currentCollection) {
            uuidSet.add(r.getUuid());
        }

        Node parentNode = documentModel.getNode().getParent();

        String xpathQuery = createXpathQuery(nodeModel);


        if (log.isDebugEnabled()) {
            log.debug("Executing query: {}" + xpathQuery);
        }

        HippoQuery query = (HippoQuery) nodeModel.getNode().getSession().getWorkspace().getQueryManager().createQuery(xpathQuery, Query.XPATH);
        query.setLimit(MAX_RESULTS);
        RowIterator r = query.execute().getRows();
        int i = 0;
        while (r.hasNext() && i < MAX_RESULTS) {
            // retrieve the query results from the row
            Row row = r.nextRow();
            String path = row.getValue("jcr:path").getString();
            try {
                // retrieve the found document from the repository
                Node document = (Node) nodeModel.getNode().getSession().getItem(path);
                Node itsParent = ((HippoNode) document).getCanonicalNode().getParent();

                // same parent? skip
                // only interested in handles, not in the documents themselves
                // if it's already chosen, then skip the document
                if (parentNode.isSame(itsParent)) {
                    continue;
                } else if (document.isNodeType(HippoNodeType.NT_HANDLE)) {
                    if (uuidSet.contains(document.getIdentifier())) {
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Found document that refers to me {}", document.getPath());
                    }
                    collection.add(new RelatedDoc(new JcrNodeModel(document), this.score));
                } else if (itsParent.isNodeType(HippoNodeType.NT_HANDLE)) {
                    if (uuidSet.contains(itsParent.getIdentifier())) {
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Found parent {}", itsParent.getPath());
                    }
                    final JcrNodeModel jcrNodeModel = new JcrNodeModel(itsParent);
                    if (jcrNodeModel.getNode().getPath().endsWith("hippo:prototype")) {
                        continue;
                    }
                    collection.add(new RelatedDoc(jcrNodeModel, this.score));
                } else {
                    continue;
                }
                i++;
            } catch (RepositoryException e) {
                log.error("Error handling Referring related documents", e);
            }
        }
        return collection;
    }

    private String createXpathQuery(JcrNodeModel nodeModel) throws RepositoryException {
        final Node node = nodeModel.getNode();
        // check if translated:

        StringBuilder statement = new StringBuilder("//element(*, ").append(RelatedDocsNodeType.NT_RELATABLEDOCS).append(')');
        statement.append('/' + RelatedDocsNodeType.NT_RELATEDDOCS);
        statement.append("/*[@hippo:docbase='");

        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            statement.append(node.getIdentifier()).append('\'');
        } else {
            statement.append(nodeModel.getParentModel().getNode().getIdentifier()).append('\'');
        }
        statement.append(" and hippo:availability='preview']");
        return statement.toString();
    }
}
