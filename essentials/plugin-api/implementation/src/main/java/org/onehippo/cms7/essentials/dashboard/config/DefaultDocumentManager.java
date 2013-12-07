/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.AnnotationMapperImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public class DefaultDocumentManager implements DocumentManager {

    public static final char PATH_SEPARATOR = '/';
    private static Logger log = LoggerFactory.getLogger(DefaultDocumentManager.class);
    private final Session session;
    private final Map<String, Class<? extends Document>> contentTypeMap = new HashMap<>();

    public DefaultDocumentManager(final Session session) {
        this.session = session;
        initContentTypes();
    }

    protected void initContentTypes() {
        contentTypeMap.put(getContentType(BaseDocument.class), BaseDocument.class);
        contentTypeMap.put(getContentType(BaseFolder.class), BaseFolder.class);
        contentTypeMap.put(getContentType(ProjectSettingsBean.class), ProjectSettingsBean.class);
        contentTypeMap.put(getContentType(RecentlyInstalledPlugin.class), RecentlyInstalledPlugin.class);
    }

    protected String getContentType(Class<? extends Document> clazz) {
        final DocumentType ct = clazz.getAnnotation(DocumentType.class);
        if (ct != null) {
            return ct.value();
        } else {
            return clazz.getSimpleName();
        }
    }

    @Override
    public <T extends Document> T fetchDocument(final String path, final Class<T> clazz) {
        final ObjectContentManager manager = createManager(session);
        @SuppressWarnings("unchecked")
        final T document = (T) manager.getObject(clazz, path);
        log.info("Loaded Content: {}", document);
        return document;


    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Document> T fetchDocument(final String path, final String documentType) {
        final Class<? extends Document> clazz = contentTypeMap.get(documentType);
        if (clazz != null) {
            return (T) fetchDocument(path, clazz);
        } else {
            return fetchDocument(path);
        }
    }

    @Override
    public <T extends Document> T fetchDocument(final String path) {
        final ObjectContentManager manager = createManager(session);
        @SuppressWarnings("unchecked")
        final T document = (T) manager.getObject(path);
        log.info("Loaded document: {}", document);
        return document;


    }

    @Override
    public boolean saveDocument(final Document document) {
        if (document == null || Strings.isNullOrEmpty(document.getPath())) {
            throw new IllegalArgumentException("Cannot save document which is null or has no path");
        }
        if (document.getPath().charAt(0) != PATH_SEPARATOR) {
            throw new IllegalArgumentException("Cannot save document with relative path: " + document.getPath());
        }

        ObjectContentManager manager = createManager(session);
        if (manager == null) {
            return false;
        }
        if (manager.objectExists(document.getPath())) {
            manager.update(document);
        } else {
            createSubfolders(manager, document.getPath());
            manager.insert(document);
        }
        manager.save();
        return true;

    }

    /**
     * Create subfolders
     *
     * @param manager instance of ObjectContentManager
     * @param path    path provided
     */
    private void createSubfolders(ObjectContentManager manager, String path) {

        final String[] pathParts = StringUtils.split(path, PATH_SEPARATOR);
        final int length = pathParts.length - 1;
        final StringBuilder parent = new StringBuilder();
        for (int i = 0; i < length; i++) {
            parent.append(PATH_SEPARATOR);
            String folderPath = parent.append(pathParts[i]).toString();
            if (manager.objectExists(folderPath)) {
                continue;
            }
            final Folder folder = new BaseFolder(folderPath);
            manager.insert(folder);
        }
    }

    private ObjectContentManager createManager(final Session session) {
        @SuppressWarnings("rawtypes")
        final List<Class> classes = new ArrayList<>();
        for (Class<? extends Document> clazz : contentTypeMap.values()) {
            classes.add(clazz);
        }
        Mapper mapper = new AnnotationMapperImpl(classes);
        return new ObjectContentManagerImpl(session, mapper);
    }


}
