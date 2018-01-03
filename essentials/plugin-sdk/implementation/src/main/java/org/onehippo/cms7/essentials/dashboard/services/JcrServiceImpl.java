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

package org.onehippo.cms7.essentials.dashboard.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.inject.Singleton;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.onehippo.cms7.essentials.dashboard.service.JcrService;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TranslationsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@Singleton
public class JcrServiceImpl implements JcrService {
    private static final Logger LOG = LoggerFactory.getLogger(JcrServiceImpl.class);

    @Override
    public Session createSession() {
        try {
            final HippoRepository repository = HippoRepositoryFactory.getHippoRepository("vm://");
            return repository.login("admin", "admin".toCharArray());
        } catch (RepositoryException e) {
            LOG.error("Error creating repository connection: ", e);
        }
        return null;
    }

    @Override
    public void refreshSession(final Session session, final boolean keepChanges) {
        try {
            session.refresh(keepChanges);
        } catch (RepositoryException e) {
            LOG.error("Error refreshing session: ", e);
        }
    }

    @Override
    public void destroySession(final Session session) {
        session.logout();
    }

    @Override
    public boolean importResource(final Node targetNode, final String resourcePath, final Map<String, Object> placeholderData) {
        String interpolated = TemplateUtils.injectTemplate(resourcePath, placeholderData);
        if (interpolated == null) {
            LOG.error("Can't read resource '{}'.", resourcePath);
            return false;
        }

        InputStream in = null;
        try {
            in = new ByteArrayInputStream(interpolated.getBytes("UTF-8"));
            targetNode.getSession().importXML(targetNode.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
        } catch (IOException | RepositoryException e) {
            LOG.error("Failed to import resource '{}'.", resourcePath, e);
            return false;
        } finally {
            IOUtils.closeQuietly(in);
        }
        return true;
    }

    @Override
    public boolean importTranslationsResource(final Session session, final String resourcePath, final Map<String, Object> placeholderData) {
        String interpolated = TemplateUtils.injectTemplate(resourcePath, placeholderData);
        if (interpolated == null) {
            LOG.error("Can't read resource '{}'.", resourcePath);
            return false;
        }

        try {
            TranslationsUtils.importTranslations(interpolated, session);
        } catch (RepositoryException | IOException e) {
            LOG.error("Failed to import translations '{}'.", resourcePath, e);
            return false;
        }
        return true;
    }
}
