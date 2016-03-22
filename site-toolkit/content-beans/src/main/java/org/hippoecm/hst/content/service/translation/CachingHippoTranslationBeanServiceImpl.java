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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default Hippo Translation Content Bean service implementation by executing queries simply to find all the
 * Hippo Translation beans.
 */
public class CachingHippoTranslationBeanServiceImpl extends DefaultHippoTranslationBeanServiceImpl {

    private static Logger log = LoggerFactory.getLogger(CachingHippoTranslationBeanServiceImpl.class);

    private final HippoTranslationContentRegistry hippoTranslationContentRegistry;

    public CachingHippoTranslationBeanServiceImpl(
            final HippoTranslationContentRegistry hippoTranslationContentRegistry) {
        this.hippoTranslationContentRegistry = hippoTranslationContentRegistry;
    }

    @Override
    public List<Node> getTranslationNodes(final Session session, final String translationId)
            throws RepositoryException {
        if (StringUtils.isBlank(translationId)) {
            throw new IllegalArgumentException("Blank translation ID.");
        }

        List<Node> translationNodes = null;

        Set<String> documentHandleIds = hippoTranslationContentRegistry
                .getDocumentHandleIdsByTranslationId(translationId);

        if (documentHandleIds != null) {
            if (documentHandleIds != null) {
                translationNodes = new ArrayList<>();
                Node handleNode;

                for (String documentHandleId : documentHandleIds) {
                    handleNode = session.getNodeByIdentifier(documentHandleId);

                    if (handleNode.hasNode(handleNode.getName())) {
                        translationNodes.add(handleNode.getNode(handleNode.getName()));
                    }
                }
            }
        } else {
            translationNodes = HippoTranslatedContentUtils.findTranslationNodes(session, translationId);

            documentHandleIds = new HashSet<>();
            Node handleNode;

            for (Node translationNode : translationNodes) {
                handleNode = translationNode.getParent();

                if (handleNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                    documentHandleIds.add(handleNode.getIdentifier());
                }
            }

            hippoTranslationContentRegistry.putDocumentHandleIdsForTranslationId(translationId, documentHandleIds);
        }

        return translationNodes == null ? Collections.emptyList() : translationNodes;
    }

}
