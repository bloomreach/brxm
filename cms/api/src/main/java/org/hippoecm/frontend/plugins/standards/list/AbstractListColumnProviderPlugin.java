/*
 *  Copyright 2010-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.list;

import java.util.Collections;
import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.markup.html.IHeaderContributor;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractListColumnProviderPlugin extends Plugin implements IListColumnProvider {

    private static final Logger log = LoggerFactory.getLogger(AbstractListColumnProviderPlugin.class);

    public AbstractListColumnProviderPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        if (!config.containsKey(IListColumnProvider.SERVICE_ID)) {
            log.warn("No service key '{}' configured; columns will not be registered", IListColumnProvider.SERVICE_ID);
            return;
        }

        context.registerService(this, config.getString(IListColumnProvider.SERVICE_ID));
    }

    public IHeaderContributor getHeaderContributor() {
        return null;
    }

    public List<ListColumn<Node>> getColumns() {
        return Collections.emptyList();
    }

    public List<ListColumn<Node>> getExpandedColumns() {
        return Collections.emptyList();
    }

}
