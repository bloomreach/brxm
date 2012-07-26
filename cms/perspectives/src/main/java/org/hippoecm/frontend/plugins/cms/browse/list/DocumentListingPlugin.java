/*
 *  Copyright 2008 Hippo.
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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.ExpandCollapseListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.IListColumnProvider;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.comparators.TypeComparator;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentTypeIconAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TypeRenderer;
import org.hippoecm.frontend.plugins.yui.layout.IExpandableCollapsable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DocumentListingPlugin<T> extends ExpandCollapseListingPlugin<T> implements IExpandableCollapsable {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(DocumentListingPlugin.class);

    private static final String DOCUMENT_LISTING_CSS = "DocumentListingPlugin.css";

    public DocumentListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        setClassName("hippo-list-documents");
        getSettings().setAutoWidthClassName("doclisting-name");
    }

    @Override
    public void renderHead(HtmlHeaderContainer container) {
        container.getHeaderResponse().renderCSSReference(new ResourceReference(DocumentListingPlugin.class, DOCUMENT_LISTING_CSS));
        super.renderHead(container);
    }

    @Override
    protected List<IListColumnProvider> getDefaultColumnProviders() {
        List<IListColumnProvider> providers = new ArrayList<IListColumnProvider>(1);
        providers.add(new CssTypeIconListColumnProvider());
        return providers;
    }

    private static class CssTypeIconListColumnProvider implements IListColumnProvider {
        private static final long serialVersionUID = 1L;

        public List<ListColumn<Node>> getColumns() {
            List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>();

            //Type Icon
            ListColumn<Node> column = new ListColumn<Node>(new Model<String>(""), "icon");
            column.setComparator(new TypeComparator());
            column.setRenderer(new EmptyRenderer<Node>());
            column.setAttributeModifier(new DocumentTypeIconAttributeModifier());
            column.setCssClass("doclisting-icon");
            columns.add(column);

            //Name
            column = new ListColumn<Node>(new ResourceModel("doclisting-name"), "name");
            column.setComparator(new NameComparator());
            column.setAttributeModifier(new DocumentAttributeModifier());
            column.setCssClass("doclisting-name");
            columns.add(column);

            return columns;
        }

        public List<ListColumn<Node>> getExpandedColumns() {
            List<ListColumn<Node>> columns = getColumns();

            //Type
            ListColumn<Node> column = new ListColumn<Node>(new ResourceModel("doclisting-type"), "type");
            column.setComparator(new TypeComparator());
            column.setRenderer(new TypeRenderer());
            column.setCssClass("doclisting-type");
            columns.add(column);

            return columns;
        }

        public IHeaderContributor getHeaderContributor() {
            return null;
        }

    }

}
