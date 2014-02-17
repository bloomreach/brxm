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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service used to read/write plugin configuration(s)
 *
 * @version "$Id: JcrPluginConfigService.java 174393 2013-08-20 13:46:56Z mmilicevic $"
 */
public class JcrPluginConfigService implements PluginConfigService {

    public static final String CONFIG_PATH = "essentials/plugins";

    private final PluginContext context;
    private final Session session;
    private final DocumentManager manager;

    public JcrPluginConfigService(final PluginContext context) {
        this.context = context;
        this.session = context.getSession();
        this.manager = new DefaultDocumentManager(context);
    }

    public PluginContext getContext() {
        return context;
    }

    @Override
    public boolean write(final Document document) {
        return manager.saveDocument(document);
    }

    @Override
    public boolean write(final Document document, final String pluginClass) {
        return manager.saveDocument(document);
    }


    @Override
    public <T extends Document> T read(final String pluginClass, final Class<T> clazz) {
        final String path = GlobalUtils.getFullConfigPath(pluginClass);
        return getConfigDocument(path, clazz);

    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Document> T read(final Class<T> clazz) {
        final String pluginClass = clazz.getName();
        final String path = GlobalUtils.getFullConfigPath(pluginClass);
        return getConfigDocument(path, clazz);

    }

    private <T extends Document> T getConfigDocument(final String path, final Class<T> clazz) {
        // NOTE: added null check so we can test dashboard without repository (CMS) running
        if (session == null) {
            return null;
        }
        return manager.fetchDocument(path, clazz);

    }


}
