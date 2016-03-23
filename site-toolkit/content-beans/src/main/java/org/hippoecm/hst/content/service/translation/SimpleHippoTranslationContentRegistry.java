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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;

public class SimpleHippoTranslationContentRegistry implements HippoTranslationContentRegistry {

    private Object lock = new Object();

    private Map<String, Set<String>> documentHandleIdsByTranslationIdCache = new ConcurrentHashMap<>();

    private Map<String, String> translationIdByDocumentHandleIdCache = new ConcurrentHashMap<>();

    @Override
    public Set<String> getDocumentHandleIdsByTranslationId(String translationId) {
        if (StringUtils.isBlank(translationId)) {
            throw new IllegalArgumentException("Invalid translation ID.");
        }

        Set<String> handleIds = documentHandleIdsByTranslationIdCache.get(translationId);
        return handleIds;
    }

    @Override
    public void putDocumentHandleIdsForTranslationId(String translationId, Set<String> documentHandleIds) {
        if (StringUtils.isBlank(translationId)) {
            throw new IllegalArgumentException("Invalid translation ID.");
        }

        if (documentHandleIds == null) {
            throw new IllegalArgumentException("Invalid documentHandleIds.");
        }

        synchronized (lock) {
            documentHandleIdsByTranslationIdCache.put(translationId, documentHandleIds);

            for (String documentHandleId : documentHandleIds) {
                translationIdByDocumentHandleIdCache.put(documentHandleId, translationId);
            }
        }
    }

    @Override
    public boolean removeDocumentHandleIdsByTranslationId(String translationId) {
        if (StringUtils.isBlank(translationId)) {
            throw new IllegalArgumentException("Invalid translation ID.");
        }

        boolean removed = false;

        synchronized (lock) {
            Set<String> documentHandleIds = documentHandleIdsByTranslationIdCache.remove(translationId);
            removed = documentHandleIds != null;

            for (String documentHandleId : documentHandleIds) {
                translationIdByDocumentHandleIdCache.remove(documentHandleId);
            }
        }

        return removed;
    }

    @Override
    public boolean removeDocumentHandleId(String documentHandleId) {
        boolean removed = false;

        synchronized (lock) {
            String translationId = translationIdByDocumentHandleIdCache.remove(documentHandleId);
            removed = translationId != null;

            if (removed) {
                Set<String> documentHandleIds = documentHandleIdsByTranslationIdCache.get(translationId);

                if (documentHandleIds != null) {
                    if (documentHandleIds.isEmpty()
                            || (documentHandleIds.size() == 1 && documentHandleIds.contains(documentHandleId))) {
                        documentHandleIdsByTranslationIdCache.remove(translationId);
                    }
                }
            }
        }

        return removed;
    }

}
