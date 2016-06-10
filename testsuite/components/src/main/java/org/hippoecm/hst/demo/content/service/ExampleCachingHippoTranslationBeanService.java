/**
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.content.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.content.service.translation.HippoTranslationBeanService;
import org.hippoecm.hst.content.service.translation.HippoTranslationBeanServiceImpl;

/**
 * Demonstration purpose, very simple {@link HippoTranslationBeanService} component implementation,
 * which caches translation ID vs translation document node ID set mappings for performance.
 */
public class ExampleCachingHippoTranslationBeanService extends HippoTranslationBeanServiceImpl
        implements CachingHippoTranslationBeanService {

    private final Map<String, List<String>> translationIdNodeIdsCacheMap = new ConcurrentHashMap<>();

    @Override
    public List<Node> getTranslationNodes(Session session, String translationId) throws RepositoryException {
        List<String> nodeIds = translationIdNodeIdsCacheMap.get(translationId);

        if (nodeIds == null) {
            List<Node> translationNodes = super.getTranslationNodes(session, translationId);
            nodeIds = new ArrayList<>();

            for (Node node : translationNodes) {
                nodeIds.add(node.getIdentifier());
            }

            translationIdNodeIdsCacheMap.put(translationId, nodeIds);

            return translationNodes;
        } else {
            List<Node> translationNodes = new ArrayList<>();

            if (!nodeIds.isEmpty()) {
                Node node;

                for (String nodeId : nodeIds) {
                    try {
                        node = session.getNodeByIdentifier(nodeId);
                        translationNodes.add(node);
                    } catch (ItemNotFoundException ignore) {
                    }
                }
            }

            return translationNodes;
        }
    }

    @Override
    public void clearCache() {
        translationIdNodeIdsCacheMap.clear();
    }
}
