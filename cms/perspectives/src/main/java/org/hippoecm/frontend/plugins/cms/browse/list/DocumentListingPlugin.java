/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
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
import org.hippoecm.frontend.skin.DocumentListColumn;

public abstract class DocumentListingPlugin<T> extends ExpandCollapseListingPlugin<T> implements IExpandableCollapsable {

    public DocumentListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        setClassName(DocumentListColumn.DOCUMENT_LIST_CSS_CLASS);
    }

    @Override
    protected List<IListColumnProvider> getDefaultColumnProviders() {
        return Collections.singletonList(CssTypeIconListColumnProvider.INSTANCE);
    }

    private static class CssTypeIconListColumnProvider implements IListColumnProvider {

        private static final ListColumn<Node> NAME_COLUMN = createNameColumn();
        private static final ListColumn<Node> TYPE_COLUMN = createTypeColumn();
        private static final ListColumn<Node> TYPE_ICON_COLUMN = createTypeIconColumn();
        private static final CssTypeIconListColumnProvider INSTANCE = new CssTypeIconListColumnProvider();

        @Override
        public List<ListColumn<Node>> getColumns() {
            return Arrays.asList(TYPE_ICON_COLUMN, NAME_COLUMN);
        }

        @Override
        public List<ListColumn<Node>> getExpandedColumns() {
            return Arrays.asList(TYPE_ICON_COLUMN, NAME_COLUMN, TYPE_COLUMN);
        }

        private static ListColumn<Node> createNameColumn() {
            final ClassResourceModel displayModel = new ClassResourceModel("doclisting-name", DocumentListingPlugin.class);
            final ListColumn<Node> column = new ListColumn<>(displayModel, "name");
            column.setComparator(NameComparator.getInstance());
            column.setAttributeModifier(DocumentAttributeModifier.getInstance());
            column.setCssClass(DocumentListColumn.NAME.getCssClass());
            return column;
        }

        private static ListColumn<Node> createTypeColumn() {
            final ClassResourceModel displayModel = new ClassResourceModel("doclisting-type", DocumentListingPlugin.class);
            final ListColumn<Node> column = new ListColumn<>(displayModel, "type");
            column.setComparator(TypeComparator.getInstance());
            column.setRenderer(TypeRenderer.getInstance());
            column.setCssClass(DocumentListColumn.TYPE.getCssClass());
            return column;
        }

        private static ListColumn<Node> createTypeIconColumn() {
            final ListColumn<Node> column = new ListColumn<>(Model.of(""), "icon");
            column.setComparator(TypeComparator.getInstance());
            column.setRenderer(EmptyRenderer.getInstance());
            column.setAttributeModifier(DocumentTypeIconAttributeModifier.getInstance());
            column.setCssClass(DocumentListColumn.ICON.getCssClass());
            return column;
        }

        @Override
        public IHeaderContributor getHeaderContributor() {
            return null;
        }

    }

}
