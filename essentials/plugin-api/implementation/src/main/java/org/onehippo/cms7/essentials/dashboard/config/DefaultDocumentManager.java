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

import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.JcrPersistenceReader;
import org.onehippo.cms7.essentials.dashboard.utils.JcrPersistenceWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class DefaultDocumentManager implements DocumentManager {


    private static Logger log = LoggerFactory.getLogger(DefaultDocumentManager.class);

    private final JcrPersistenceWriter writer;
    private final JcrPersistenceReader reader;
    private final Session session;

    public DefaultDocumentManager(final PluginContext context) {
        this.session = context.createSession();
        this.writer = new JcrPersistenceWriter(session, context);
        this.reader = new JcrPersistenceReader(session, context);
    }


    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public <T extends Document> T fetchDocument(final String path, final Class<T> clazz) {
        return reader.read(path, clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Document> T fetchDocument(final String pluginClazz) {
        try {
            final Class<T> aClass = (Class<T>) Class.forName(pluginClazz);
            return fetchDocument(GlobalUtils.getFullConfigPath(pluginClazz), aClass);
        } catch (ClassNotFoundException e) {
            log.error("Error loading class", e);
        }
        return null;
    }

    @Override
    public boolean saveDocument(final Document document) {

        return writer.write(document) != null;

    }


}
