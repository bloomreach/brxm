/*
 *  Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.browse.list;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.hippoecm.frontend.plugins.standards.list.DocumentsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FolderViewPlugin extends DocumentListingPlugin<Node> {

    private static final Logger log = LoggerFactory.getLogger(FolderViewPlugin.class);

    public FolderViewPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected ISortableDataProvider<Node, String> newDataProvider() {
        return new DocumentsProvider(getModel(), new DocumentListFilter(getPluginConfig()),
                getTableDefinition().getComparators());
    }

    @Override
    protected boolean isOrderable() {
        IModel<Node> model = getModel();
        try {
            Node node = model.getObject();
            return node != null && node.getPrimaryNodeType().hasOrderableChildNodes();
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return false;
    }

}
