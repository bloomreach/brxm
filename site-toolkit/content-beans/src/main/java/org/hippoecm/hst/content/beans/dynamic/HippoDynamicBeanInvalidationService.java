/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.dynamic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.dynamic.DynamicBeanInvalidationService;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoDynamicBeanInvalidationService implements DynamicBeanInvalidationService {
    private static final Logger log = LoggerFactory.getLogger(HippoDynamicBeanInvalidationService.class);
    private static final String CONTENT_TYPES_VERSION_CACHE_KEY = "CONTENT_TYPES_VERSION_CACHE_KEY";

    private ObjectConverter objectConverter;
    private Map<String, Long> contentTypesVersionCache = new ConcurrentHashMap<>();

    public HippoDynamicBeanInvalidationService(final ObjectConverter objectConverter) {
        this.objectConverter = objectConverter;
    }

    public void setObjectConverter(ObjectConverter objectConverter) {
        this.objectConverter = objectConverter;
    }

    @Override
    public void invalidateOnDocumentTypeModification() {
        ContentTypeService contentTypeService = HippoServiceRegistry.getService(ContentTypeService.class);
        if (contentTypeService == null) {
            log.warn("ContentTypeService hasn't been initialized yet.");
            return;
        }

        try {
            final ContentTypes contentTypes = contentTypeService.getContentTypes();
            final Long currentContentTypesVersion = contentTypesVersionCache.getOrDefault(CONTENT_TYPES_VERSION_CACHE_KEY, 0L);

            if (contentTypes.version() == currentContentTypesVersion) {
                // if there isn't any change in version number, no need to invalidate document types
                return;
            }

            synchronized (contentTypes) {
                if (currentContentTypesVersion == null) {
                    // if there isn't any record in the contentTypesVersionCache that means
                    // generating the beanless beans will be handled in ObjectCoverterImpl
                    contentTypesVersionCache.put(CONTENT_TYPES_VERSION_CACHE_KEY, 1L);
                } else if (contentTypes.version() > currentContentTypesVersion) {
                    refreshDynamicBeans(contentTypes);
                }
            }
        } catch (RepositoryException e) {
            log.error("Error on contentTypes : {}, {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unknown error on ContentTypeService : {}, {}", e.getMessage(), e);
        }
    }

    /**
     * Removes bean definitions from {@link ObjectConverter}.
     */
    private void refreshDynamicBeans(final ContentTypes contentTypes) {
        log.info("Invalidating document types for version number {}", contentTypes.version());
        contentTypesVersionCache.put(CONTENT_TYPES_VERSION_CACHE_KEY, contentTypes.version());

        contentTypes.getTypesByPrefix().values()
            .stream()
            .forEach(contentTypeSet -> {
                contentTypeSet
                    .stream()
                    .filter(contentType -> contentType.isDocumentType() || contentType.isCompoundType())
                    .forEach(contentType -> {
                        // TODO exclude internal document types
                        ObjectConverterUtils.invalidateDynamicBean(contentType.getName(), objectConverter);
                    });
        });
    }

}
