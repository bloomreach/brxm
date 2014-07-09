/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContextFactory;
import org.onehippo.cms7.essentials.dashboard.model.DocumentRestful;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.onehippo.cms7.services.contenttype.HippoContentTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by tjeger on 7/9/14.
 */
public class ContentTypeServiceUtils {
    private static final Logger log = LoggerFactory.getLogger(ContentTypeServiceUtils.class);

    public enum Type {
        ALL,
        COMPOUND,
        DOCUMENT,
        ASSET,
        GALLERY
    }

    /**
     * Fetch a list of documents of the specified type.
     *
     * Only document types of the project's own namespace are retrieved.
     *
     * @param type for filtering document types
     * @return filtered list of document types
     */
    public static List<DocumentRestful> fetchDocuments(final Type type) {
        final PluginContext context = PluginContextFactory.getContext();
        final String namespacePrefix = context.getProjectNamespacePrefix();
        final Session session = context.createSession();
        final Collection<ContentType> projectContentTypes = new HashSet<>();
        final List<DocumentRestful> documents = new ArrayList<>();
        try {
            final ContentTypeService service = new HippoContentTypeService(session);
            final ContentTypes contentTypes = service.getContentTypes();
            final SortedMap<String, Set<ContentType>> typesByPrefix = contentTypes.getTypesByPrefix();

            // filter on own namespace
            for (Map.Entry<String, Set<ContentType>> entry : typesByPrefix.entrySet()) {
                final String key = entry.getKey();
                final Set<ContentType> value = entry.getValue();
                if (key.equals(namespacePrefix)) {
                    projectContentTypes.addAll(value);
                }
            }

            // filter on document type
            for (ContentType doc : projectContentTypes) {
                Type myType = doc.isCompoundType() ? Type.COMPOUND : Type.DOCUMENT;

                if (type == Type.ALL) {
                    documents.add(new DocumentRestful(doc));
                } else if (myType == type) {
                    documents.add(new DocumentRestful(doc));
                }
            }

            // TODO remove unused field-locations code..?
            for (DocumentRestful document : documents) {
                final String path = MessageFormat.format("/hippo:namespaces/{0}/{1}/editor:templates/_default_/root", namespacePrefix, document.getName());
                if (session.nodeExists(path)) {
                    final Node node = session.getNode(path);
                    final Set<String> locations = new HashSet<>();
                    if (node.hasProperty("wicket.extensions")) {
                        final Value[] values = node.getProperty("wicket.extensions").getValues();
                        for (Value value : values) {
                            final String propVal = value.getString();
                            if (node.hasProperty(propVal)) {
                                // ".item" suffix produces value usable in document field editor template's wicket.id prop.
                                locations.add(node.getProperty(propVal).getString() + ".item");
                            }
                        }
                    }
                    if (locations.isEmpty()) {
                        locations.add("${cluster.id}.field");
                    }
                    document.setFieldLocations(locations);
                }
            }
        } catch (RepositoryException e) {
            log.error("Error fetching document types", e);
        } finally {
            GlobalUtils.cleanupSession(session);
        }
        // sort documents by name:
        Collections.sort(documents, new DocumentNameComparator());
        populateBeanPaths(context, documents);
        return documents;
    }

    private static class DocumentNameComparator implements java.util.Comparator<DocumentRestful> {
        @Override
        public int compare(final DocumentRestful first, final DocumentRestful second) {
            String name1 = first.getName();
            String name2 = second.getName();
            return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
        }
    }

    /**
     * Augment a set of document types with the corresponding bean class paths (if available)
     *
     * @param context Current plugin context
     * @param documentTypes List of to-be-processed document types.
     */
    public static void populateBeanPaths(final PluginContext context, final List<DocumentRestful> documentTypes) {
        final Path startDir = context.getBeansPackagePath();
        final Map<String, Path> existingBeans = new HashMap<>();
        final List<Path> directories = new ArrayList<>();
        GlobalUtils.populateDirectories(startDir, directories);
        final String pattern = "*.java";
        for (Path directory : directories) {
            try (final DirectoryStream<Path> stream = Files.newDirectoryStream(directory, pattern)) {
                for (Path path : stream) {
                    final String nodeJcrType = JavaSourceUtils.getNodeJcrType(path);
                    if (nodeJcrType != null) {
                        log.info("nodeJcrType {}", nodeJcrType);
                        existingBeans.put(nodeJcrType, path);
                    }
                }
            } catch (IOException e) {
                log.error("Error reading java files", e);
            }
        }
        for (DocumentRestful documentType : documentTypes) {
            final String fullName = documentType.getFullName();
            final Path beanPath = existingBeans.get(fullName);
            if (beanPath != null) {
                documentType.setJavaName(beanPath.toFile().getName());
                documentType.setFullPath(beanPath.toString());
            }
        }
    }
}
