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

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/**
 * Service used to read/write plugin configuration(s)
 *
 * @version "$Id: JcrPluginConfigService.java 174393 2013-08-20 13:46:56Z mmilicevic $"
 */
public class JcrPluginConfigService implements PluginConfigService {

    public static final String CONFIG_PATH = "essentials/plugins";
    private static Logger log = LoggerFactory.getLogger(JcrPluginConfigService.class);
    private final PluginContext context;
    private final Session session;
    private final DocumentManager manager;

    public JcrPluginConfigService(final PluginContext context) {
        this.context = context;
        this.session = context.getSession();
        this.manager = new DefaultDocumentManager(session);
    }

    public PluginContext getContext() {
        return context;
    }

    @Override
    public boolean write(final Document document) {
        log.debug("Writing document: {}", document);
        log.debug("Writing node: {}", context);
        boolean saved =false;
        try {
            final Plugin descriptor = context.getDescriptor();
            if(descriptor==null){
                log.error("Plugin descriptor was null");
                return false;
            }
            final String configRoot = getFullConfigPath(descriptor.getPluginClass());
            document.setPath(configRoot);

            saved = manager.saveDocument(document);
            session.save();
            return saved;
        } catch (RepositoryException e) {
            log.error("Error writing configuration", e);
            GlobalUtils.refreshSession(session, false);
        }

        return saved;
    }

    @Override
    public <T extends Document> T read(final String pluginClass) {
        final String path = getFullConfigPath(pluginClass);
        return getConfigDocument(path);
    }

    @Override
    public <T extends Document> T read() {
        final String path = getFullConfigPath(context.getDescriptor().getPluginClass());
        return getConfigDocument(path);
    }

    private String getFullConfigPath(final CharSequence pluginClass) {
        final List<String> configList = Lists.newLinkedList(Splitter.on('/').split(CONFIG_PATH));
        configList.addAll(Lists.newLinkedList(Splitter.on('.').split(pluginClass)));
        return '/' + Joiner.on('/').join(configList);
    }

    private <T extends Document> T getConfigDocument(final String path) {
        // NOTE: added null check so we can test dashboard without repository (CMS) running
        if (session == null) {
            return null;
        }
        return manager.fetchDocument(path);

    }


}
