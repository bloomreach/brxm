/*
 * Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery.columns;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.Translations;
import org.hippoecm.frontend.plugins.gallery.compare.CalendarComparator;
import org.hippoecm.frontend.plugins.gallery.compare.MimeTypeComparator;
import org.hippoecm.frontend.plugins.gallery.compare.SizeComparator;
import org.hippoecm.frontend.plugins.standards.list.render.DatePropertyRenderer;
import org.hippoecm.frontend.plugins.standards.list.render.MimeTypeIconRenderer;
import org.hippoecm.frontend.plugins.standards.list.render.SizeRenderer;
import org.hippoecm.frontend.plugins.standards.list.render.StringPropertyRenderer;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.list.AbstractListColumnProviderPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconAttributeModifier;
import org.hippoecm.frontend.skin.DocumentListColumn;

public class AssetGalleryColumnProviderPlugin extends AbstractListColumnProviderPlugin {

    private final String primaryItemName;

    public AssetGalleryColumnProviderPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        primaryItemName = config.getString("primaryItemName", "hippogallery:asset");
    }

    @Override
    public List<ListColumn<Node>> getColumns() {
        final List<ListColumn<Node>> columns = new ArrayList<>();
        columns.add(createIconColumn());
        columns.add(createNameColumn());
        return columns;
    }

    private ListColumn<Node> createIconColumn() {
        final ListColumn<Node> column = new ListColumn<>(Model.of(""), null);
        column.setRenderer(new MimeTypeIconRenderer());
        column.setAttributeModifier(new IconAttributeModifier());
        column.setComparator(new MimeTypeComparator(JcrConstants.JCR_MIMETYPE, primaryItemName));
        column.setCssClass(DocumentListColumn.ICON.getCssClass());
        return column;
    }

    private ListColumn<Node> createNameColumn() {
        final ClassResourceModel displayModel = new ClassResourceModel("assetgallery-name", Translations.class);
        final ListColumn<Node> column = new ListColumn<>(displayModel, "name");
        column.setComparator(NameComparator.getInstance());
        column.setCssClass(DocumentListColumn.NAME.getCssClass());
        column.setAttributeModifier(new AssetAttributeModifier());
        return column;
    }

    @Override
    public List<ListColumn<Node>> getExpandedColumns() {
        final List<ListColumn<Node>> columns = getColumns();
        columns.add(createSizeColumn());
        columns.add(createMimeTypeColumn());
        columns.add(createLastModifiedColumn());
        return columns;
    }

    private ListColumn<Node> createSizeColumn() {
        final ClassResourceModel displayModel = new ClassResourceModel("assetgallery-size", Translations.class);
        final ListColumn<Node> column = new ListColumn<>(displayModel, "size");
        column.setRenderer(new SizeRenderer(JcrConstants.JCR_DATA, primaryItemName));
        column.setComparator(new SizeComparator(JcrConstants.JCR_DATA, primaryItemName));
        column.setCssClass(DocumentListColumn.SIZE.getCssClass());
        return column;
    }

    private ListColumn<Node> createMimeTypeColumn() {
        final ClassResourceModel displayModel = new ClassResourceModel("assetgallery-mimetype", Translations.class);
        final ListColumn<Node> column = new ListColumn<>(displayModel, "mimetype");
        column.setRenderer(new StringPropertyRenderer(JcrConstants.JCR_MIMETYPE, primaryItemName));
        column.setComparator(new MimeTypeComparator(JcrConstants.JCR_MIMETYPE, primaryItemName));
        column.setCssClass(DocumentListColumn.MIME_TYPE.getCssClass());
        column.setAttributeModifier(new MimeTypeAttributeModifier(primaryItemName));
        return column;
    }

    private ListColumn<Node> createLastModifiedColumn() {
        final ClassResourceModel displayModel = new ClassResourceModel("assetgallery-lastmodified", Translations.class);
        final ListColumn<Node> column = new ListColumn<>(displayModel, "lastmodified");
        column.setRenderer(new DatePropertyRenderer(JcrConstants.JCR_LASTMODIFIED, primaryItemName));
        column.setComparator(new CalendarComparator(JcrConstants.JCR_LASTMODIFIED, primaryItemName));
        column.setCssClass(DocumentListColumn.DATE.getCssClass());
        return column;
    }
}
