/*
 *  Copyright 2010 Hippo.
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

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.hippoecm.frontend.plugins.standards.list.SearchDocumentsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SearchViewPlugin extends DocumentListingPlugin<BrowserSearchResult> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(SearchViewPlugin.class);

    public SearchViewPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        setClassName("hippo-list-search");
    }

    @Override
    protected ISortableDataProvider<Node> newDataProvider() {
        return new SearchDocumentsProvider(getModel(), getTableDefinition().getComparators());
    }

}
