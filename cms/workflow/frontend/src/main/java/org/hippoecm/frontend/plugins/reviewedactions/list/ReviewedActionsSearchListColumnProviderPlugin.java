/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions.list;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.reviewedactions.list.comparators.StateComparator;
import org.hippoecm.frontend.plugins.reviewedactions.list.resolvers.StateIconAttributeModifier;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.list.AbstractListColumnProviderPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;

public class ReviewedActionsSearchListColumnProviderPlugin extends AbstractListColumnProviderPlugin {

    private static final long serialVersionUID = 1L;
    private static final CssResourceReference SEARCHLISTCOLUMN_SKIN = new CssResourceReference(ReviewedActionsListColumnProviderPlugin.class, "style.css");

    public ReviewedActionsSearchListColumnProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public IHeaderContributor getHeaderContributor() {
        return new IHeaderContributor() {
            @Override
            public void renderHead(final IHeaderResponse response) {
                response.render(CssHeaderItem.forReference(SEARCHLISTCOLUMN_SKIN));
            }
        };
    }

    @Override
    public List<ListColumn<Node>> getColumns() {
        List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>();

        ListColumn<Node> column = new ListColumn<Node>(new ClassResourceModel("doclisting-state", getClass()), "state");
        column.setComparator(new StateComparator());
        column.setRenderer(new EmptyRenderer<Node>());
        column.setAttributeModifier(new StateIconAttributeModifier());
        column.setCssClass("doclisting-state");
        columns.add(column);

        return columns;
    }

}
