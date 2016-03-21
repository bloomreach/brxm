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
package org.hippoecm.hst.content.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.cache.CacheElement;
import org.hippoecm.hst.cache.HstCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default Hippo Translation Content Bean service implementation by executing queries simply to find all the
 * Hippo Translation beans.
 */
public class CachingHippoTranslationBeanServiceImpl extends DefaultHippoTranslationBeanServiceImpl {

    private static Logger log = LoggerFactory.getLogger(CachingHippoTranslationBeanServiceImpl.class);

    private final HstCache handleIdsOfTranslationIdCache;

    public CachingHippoTranslationBeanServiceImpl(final HstCache handleIdsOfTranslationIdCache) {
        this.handleIdsOfTranslationIdCache = handleIdsOfTranslationIdCache;
    }

    @Override
    public List<Node> getTranslationNodes(final Session session, final String translationId)
            throws RepositoryException {
        if (StringUtils.isBlank(translationId)) {
            throw new IllegalArgumentException("Blank translation ID.");
        }

        List<Node> translationNodes = null;

        CacheElement cacheElem = handleIdsOfTranslationIdCache.get(translationId);

        if (cacheElem != null) {
            List<String> handleNodeIds = (List<String>) cacheElem.getContent();

            if (handleNodeIds != null) {
                translationNodes = new ArrayList<>();
                Node handleNode;

                for (String handleNodeId : handleNodeIds) {
                    handleNode = session.getNodeByIdentifier(handleNodeId);

                    if (handleNode.hasNode(handleNode.getName())) {
                        translationNodes.add(handleNode.getNode(handleNode.getName()));
                    }
                }
            }
        } else {
            translationNodes = super.getTranslationNodes(session, translationId);

            List<String> handleNodeIds = new ArrayList<String>();
            Node handleNode;
            for (Node translationNode : translationNodes) {
                handleNode = translationNode.getParent();
                handleNodeIds.add(handleNode.getIdentifier());
            }

            cacheElem = handleIdsOfTranslationIdCache.createElement(translationId, handleNodeIds);
            handleIdsOfTranslationIdCache.put(cacheElem);
        }

        return translationNodes == null ? Collections.emptyList() : translationNodes;
    }

}
