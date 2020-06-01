/*
 * Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.browse.list;

import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.list.AbstractListColumnProviderPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeRenderer;
import org.hippoecm.frontend.skin.DocumentListColumn;

public class SearchListColumnProviderPlugin extends AbstractListColumnProviderPlugin {

    public SearchListColumnProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public List<ListColumn<Node>> getExpandedColumns() {
        return Collections.singletonList(createPathColumn());
    }

    private ListColumn<Node> createPathColumn() {
        final ClassResourceModel displayModel = new ClassResourceModel("doclisting-path", SearchViewPlugin.class);
        final ListColumn<Node> column = new ListColumn<>(displayModel, "path");

        column.setRenderer(new AbstractNodeRenderer() {

            @Override
            protected Component getViewer(String id, Node node) throws RepositoryException {
                String path = node.getPath();
                return new Label(id, path);
            }
        });

        column.setCssClass(DocumentListColumn.PATH.getCssClass());

        return column;
    }
}
