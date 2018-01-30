/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.folder;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.util.DocumentUtils;
import org.onehippo.cms.channelmanager.content.document.util.FolderUtils;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.slug.SlugFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class FoldersServiceImpl implements FoldersService {

    private static final Logger log = LoggerFactory.getLogger(FoldersServiceImpl.class);

    private static final FoldersService INSTANCE = new FoldersServiceImpl();

    public static FoldersService getInstance() {
        return INSTANCE;
    }

    private FoldersServiceImpl() {
    }

    @Override
    public List<Folder> getFolders(final String path, final Session session) throws ErrorWithPayloadException {
        final List<Folder> folders = new LinkedList<>();
        if (Strings.isNullOrEmpty(path) || "/".equals(path)) {
            return folders;
        }

        final String relPath = path.startsWith("/") ? path.substring(1) : path;
        try {
            Node node = session.getRootNode();
            final String[] names = relPath.split("/");
            Folder folder = null;
            for (final String name : names) {
                if (node != null && node.hasNode(name)) {
                    node = node.getNode(name);
                    folder = createFolder(node, folder);
                } else {
                    node = null;
                    folder = createFolder(name, folder);
                }
                folders.add(folder);
            }
        } catch (RepositoryException e) {
            log.warn("Failed to get folders for path '/{}'", relPath, e);
            throw new InternalServerErrorException();
        }
        return folders;
    }

    private static Folder createFolder(final String name, final Folder parent) {
        final Folder folder = new Folder();
        final String locale = parent != null ? parent.getLocale() : null;
        final String encodedName = SlugFactory.createSlug(name, locale);
        final String path = parent != null ? parent.getPath() + "/" + encodedName : "/" + encodedName;

        folder.setName(encodedName);
        folder.setDisplayName(name);
        folder.setLocale(locale);
        folder.setPath(path);
        folder.setExists(false);
        return folder;
    }

    private static Folder createFolder(final Node node, final Folder parent) throws RepositoryException {
        final Folder folder = new Folder();
        folder.setName(node.getName());
        folder.setPath(node.getPath());
        folder.setExists(true);

        DocumentUtils.getDisplayName(node).ifPresent(folder::setDisplayName);

        String locale = FolderUtils.getLocale(node);
        if (locale == null && parent != null) {
            locale = parent.getLocale();
        }
        folder.setLocale(locale);
        return folder;
    }
}
