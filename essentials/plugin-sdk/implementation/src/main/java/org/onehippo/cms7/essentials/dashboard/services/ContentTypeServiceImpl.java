/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.services;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.ContentType;
import org.onehippo.cms7.essentials.dashboard.service.ContentTypeService;
import org.onehippo.cms7.essentials.dashboard.service.JcrService;
import org.onehippo.cms7.essentials.dashboard.utils.LocalizationUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ContentTypeServiceImpl implements ContentTypeService {

    private static final Logger LOG = LoggerFactory.getLogger(ContentTypeServiceImpl.class);

    @Inject private JcrService jcrService;
    @Inject private ContentBeansService beansService;

    @Override
    public List<ContentType> fetchContentTypesFromOwnNamespace(final PluginContext context, final Predicate<ContentType> filter) {
        return fetchContentTypes(context, filter, true);
    }

    @Override
    public List<ContentType> fetchContentTypes(final PluginContext context, final Predicate<ContentType> filter, final boolean ownNamespaceOnly) {
        final List<ContentType> documents = new ArrayList<>();
        final Map<String, Path> beans = beansService.findBeans(context);
        final String namespacePrefix = context.getProjectNamespacePrefix();
        final Session session = jcrService.createSession();
        try {
            final ContentTypes contentTypes = getContentTypes();
            final Map<String, Set<org.onehippo.cms7.services.contenttype.ContentType>> typesByPrefix = contentTypes.getTypesByPrefix();

            // filter on own namespace
            for (String prefix : typesByPrefix.keySet()) {
                if (!ownNamespaceOnly || prefix.equals(namespacePrefix)) {
                    for (org.onehippo.cms7.services.contenttype.ContentType ct : typesByPrefix.get(prefix)) {
                        final ContentType contentType = createContentTypeFor(ct, session, namespacePrefix, beans);
                        if (filter == null || filter.test(contentType)) {
                            documents.add(contentType);
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            LOG.error("Error fetching document types", e);
        } finally {
            jcrService.destroySession(session);
        }
        // sort documents by name:
        documents.sort((d1, d2) -> String.CASE_INSENSITIVE_ORDER.compare(d1.getName(), d2.getName()));
        return documents;
    }

    @Override
    public String jcrBasePathForContentType(final String jcrType) {
        if (jcrType.contains(":")) {
            return "/hippo:namespaces/" + jcrType.replace(':', '/');
        } else {
            return "/hippo:namespaces/system/" + jcrType;
        }
    }


    // not private for testability
    ContentTypes getContentTypes() throws RepositoryException {
        return HippoServiceRegistry.getService(org.onehippo.cms7.services.contenttype.ContentTypeService.class).getContentTypes();
    }

    private ContentType createContentTypeFor(final org.onehippo.cms7.services.contenttype.ContentType contentType,
                                             final Session session, final String prefix, final Map<String, Path> beans) {
        final ContentType documentType = new ContentType();
        final String fullName = extractFullName(contentType.getName(), prefix);

        documentType.setDisplayName(LocalizationUtils.getContentTypeDisplayName(fullName));
        documentType.setFullName(fullName);
        documentType.setPrefix(extractPrefix(contentType.getPrefix(), fullName));
        documentType.setMixin(contentType.isMixin());
        documentType.setCompoundType(contentType.isCompoundType());
        documentType.setSuperTypes(contentType.getSuperTypes());
        documentType.setName(extractName(fullName));
        documentType.setDraftMode(determineDraftMode(documentType, session));

        final Path beanPath = beans.get(fullName);
        if (beanPath != null) {
            documentType.setJavaName(beanPath.toFile().getName());
            documentType.setFullPath(beanPath.toString());
        }

        return documentType;
    }

    /**
     * ContentType#getName can, in case of an aggregated content type, return a CSV of names.
     * In that case, we return the first entry that has a prefix-match with the project's namespace.
     */
    private String extractFullName(final String name, final String prefix) {
        final Iterable<String> split = Splitter.on(",").split(name);
        for (String aName : split) {
            if (aName.startsWith(prefix)) {
                return aName;
            }
        }

        return split.iterator().next();
    }

    private String extractName(final String fullName) {
        final int idx = fullName.indexOf(':');
        if (idx != -1) {
            return fullName.substring(idx + 1);
        }
        return fullName;
    }

    private String extractPrefix(final String prefix, final String fullName) {
        if (!Strings.isNullOrEmpty(prefix)) {
            return prefix;
        }
        if (Strings.isNullOrEmpty(fullName)) {
            return null;
        }
        final int idx = fullName.indexOf(':');
        if (idx != -1) {
            return fullName.substring(0, idx);
        }
        return fullName;
    }

    private boolean determineDraftMode(final ContentType contentType, final Session session) {
        try {
            final String absPath = String.format("/hippo:namespaces/%s/%s/hipposysedit:prototypes",
                    contentType.getPrefix(), contentType.getName());
            if (session.nodeExists(absPath)) {
                final long size = session.getNode(absPath).getNodes().getSize();
                if (size > 1) {
                    return true;
                }
            }
        } catch (RepositoryException e) {
            LOG.error("Error checking draft node for document type '{}'.", contentType.getFullName(), e);
        }
        return false;
    }
}
