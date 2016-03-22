/**
 * Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.content.service.translation;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoTranslatedContentUtils {

    private static Logger log = LoggerFactory.getLogger(HippoTranslatedContentUtils.class);

    private HippoTranslatedContentUtils() {
    }

    public static List<Node> findTranslationNodes(final Session session, final String translationId)
            throws RepositoryException {
        List<Node> translationNodes = new ArrayList<>();

        String xpath = "//element(*," + HippoTranslationNodeType.NT_TRANSLATED + ")[" + HippoTranslationNodeType.ID
                + " = '" + translationId + "']";

        @SuppressWarnings("deprecation")
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        final QueryResult result = query.execute();

        Node translationNode;
        for (NodeIterator nodeIt = result.getNodes(); nodeIt.hasNext();) {
            translationNode = nodeIt.nextNode();

            if (translationNode != null) {
                if (!translationNode.hasProperty(HippoTranslationNodeType.LOCALE)) {
                    log.debug("Skipping node '{}' because does not contain property '{}'", translationNode.getPath(),
                            HippoTranslationNodeType.LOCALE);
                    continue;
                }

                translationNodes.add(translationNode);
            }
        }

        return translationNodes;
    }
}
