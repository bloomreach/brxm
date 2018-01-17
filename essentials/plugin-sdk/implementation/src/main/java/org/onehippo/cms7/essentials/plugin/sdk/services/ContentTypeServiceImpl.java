/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.services;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.essentials.plugin.sdk.rest.ContentType;
import org.onehippo.cms7.essentials.plugin.sdk.service.ContentTypeService;
import org.onehippo.cms7.essentials.plugin.sdk.service.JcrService;
import org.onehippo.cms7.essentials.plugin.sdk.service.SettingsService;
import org.onehippo.cms7.essentials.plugin.sdk.utils.LocalizationUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ContentTypeServiceImpl implements ContentTypeService {

    private static final Logger LOG = LoggerFactory.getLogger(ContentTypeServiceImpl.class);
    private static final String HIPPOSYSEDIT_SUPERTYPE = "hipposysedit:supertype";

    @Inject private JcrService jcrService;
    @Inject private ContentBeansService beansService;
    @Inject private SettingsService settingsService;

    @Override
    public List<ContentType> fetchContentTypesFromOwnNamespace() {
        return fetchContentTypes(true);
    }

    @Override
    public List<ContentType> fetchContentTypes(final boolean ownNamespaceOnly) {
        final List<ContentType> documents = new ArrayList<>();
        final Map<String, Path> beans = beansService.findBeans();
        final String namespacePrefix = settingsService.getSettings().getProjectNamespace();
        final Session session = jcrService.createSession();
        try {
            final ContentTypes contentTypes = HippoServiceRegistry
                    .getService(org.onehippo.cms7.services.contenttype.ContentTypeService.class).getContentTypes();
            final Map<String, Set<org.onehippo.cms7.services.contenttype.ContentType>> typesByPrefix = contentTypes.getTypesByPrefix();

            // filter on own namespace
            for (String prefix : typesByPrefix.keySet()) {
                if (!ownNamespaceOnly || prefix.equals(namespacePrefix)) {
                    for (org.onehippo.cms7.services.contenttype.ContentType ct : typesByPrefix.get(prefix)) {
                        documents.add(createContentTypeFor(ct, session, namespacePrefix, beans));
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

    @Override
    public String extractPrefix(final String jcrType) {
        int idx = jcrType.indexOf(':');
        return idx < 0 ? "system" : jcrType.substring(0, idx);
    }

    @Override
    public String extractShortName(final String jcrType) {
        int idx = jcrType.indexOf(':');
        return idx < 0 ? jcrType : jcrType.substring(idx + 1);
    }

    @Override
    public boolean addMixinToContentType(final String jcrContentType, final String mixinName,
                                         final Session session, final boolean updateExisting) {
        final String contentTypeNodePath = String.format("/hippo:namespaces/%s",
                jcrContentType.replace(':', '/'));

        try {
            final Node contentTypeNode = session.getNode(contentTypeNodePath);

            // add mixin as supertype
            final Node nodeTypeNode = contentTypeNode.getNode("hipposysedit:nodetype/hipposysedit:nodetype");
            final Property superTypes = nodeTypeNode.getProperty(HIPPOSYSEDIT_SUPERTYPE);
            final Set<String> augmentedSuperTypes = new HashSet<>();
            for (Value superType : superTypes.getValues()) {
                augmentedSuperTypes.add(superType.getString());
            }
            if (!augmentedSuperTypes.contains(mixinName)) {
                augmentedSuperTypes.add(mixinName);
            }
            superTypes.setValue(augmentedSuperTypes.toArray(new String[augmentedSuperTypes.size()]));

            // add mixin to prototype
            addMixin(contentTypeNode.getNode("hipposysedit:prototypes/hipposysedit:prototype"), mixinName);

            if (updateExisting) {
                addMixinToContent(session, jcrContentType, mixinName);
            }
        } catch (RepositoryException e) {
            jcrService.refreshSession(session, false);
            LOG.error("Failed to add mixin '{}' to content type '{}'.", mixinName, jcrContentType, e);
            return false;
        }

        return true;
    }

    @Override
    public String determineDefaultFieldPosition(final String jcrContentType) {
        final String editorTemplateNodePath = String.format("/hippo:namespaces/%s/editor:templates/_default_/root",
                jcrContentType.replace(':', '/'));

        final Session session = jcrService.createSession();
        if (session != null) {
            try {
                final Node root = session.getNode(editorTemplateNodePath);
                if (root.hasProperty("wicket.extensions")) {
                    final Value[] extensions = root.getProperty("wicket.extensions").getValues();
                    return root.getProperty(extensions[0].getString()).getString() + ".item";
                }
                return "${cluster.id}.field";
            } catch (RepositoryException e) {
                LOG.error("Failed to determine default field position for content type '{}'.", jcrContentType, e);
            } finally {
                jcrService.destroySession(session);
            }
        }
        return null;
    }

    private void addMixinToContent(final Session session, final String jcrContentType, final String mixinName)
            throws RepositoryException {
        final NodeIterator nodes = session
                .getWorkspace()
                .getQueryManager()
                .createQuery(String.format("//content//element(*,%s)", jcrContentType), "xpath")
                .execute()
                .getNodes();

        while (nodes.hasNext()) {
            addMixin(nodes.nextNode(), mixinName);
        }
    }

    private void addMixin(final Node node, final String mixinName) throws RepositoryException {
        for (NodeType mixin : node.getMixinNodeTypes()) {
            if (mixin.getName().equals(mixinName)) {
                return;
            }
        }
        JcrUtils.ensureIsCheckedOut(node);
        node.addMixin(mixinName);
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
        documentType.setName(extractShortName(fullName));
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
