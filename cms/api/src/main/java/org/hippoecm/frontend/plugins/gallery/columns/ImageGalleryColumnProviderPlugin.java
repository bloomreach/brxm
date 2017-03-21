/*
 *  Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery.columns;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.Translations;
import org.hippoecm.frontend.plugins.gallery.compare.CalendarComparator;
import org.hippoecm.frontend.plugins.gallery.compare.LongPropertyComparator;
import org.hippoecm.frontend.plugins.gallery.compare.MimeTypeComparator;
import org.hippoecm.frontend.plugins.gallery.compare.SizeComparator;
import org.hippoecm.frontend.plugins.standards.list.render.DatePropertyRenderer;
import org.hippoecm.frontend.plugins.standards.list.render.ImageIconRenderer;
import org.hippoecm.frontend.plugins.standards.list.render.SizeRenderer;
import org.hippoecm.frontend.plugins.standards.list.render.StringPropertyRenderer;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.list.AbstractListColumnProviderPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentAttributeModifier;
import org.hippoecm.frontend.skin.DocumentListColumn;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;

public class ImageGalleryColumnProviderPlugin extends AbstractListColumnProviderPlugin {
    private static final long serialVersionUID = 1L;

    private static final String GALLERY_THUMBNAIL_SIZE = "gallery.thumbnail.size";
    private static final String GALLERY_THUMBNAIL_BOX_SIZE = "gallery.thumbnail.box.size";

    public static final ListColumn<Node> NAME_COLUMN = createNameColumn();

    private final String primaryItemName;

    public ImageGalleryColumnProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        primaryItemName = config.getString("primaryItemName", HippoGalleryNodeType.IMAGE_SET_ORIGINAL);
    }

    public List<ListColumn<Node>> getColumns() {
        final List<ListColumn<Node>> columns = new ArrayList<>();
        columns.add(createIconColumn());
        columns.add(NAME_COLUMN);
        return columns;
    }

    private static ListColumn<Node> createNameColumn() {
        final ClassResourceModel displayModel = new ClassResourceModel("gallery-name", Translations.class);
        final ListColumn<Node> column = new ListColumn<>(displayModel, "name");
        column.setComparator(NameComparator.getInstance());
        column.setCssClass(DocumentListColumn.NAME.getCssClass());
        column.setAttributeModifier(DocumentAttributeModifier.getInstance());
        return column;
    }

    private ListColumn<Node> createIconColumn() {
        final int thumbnailSize = getPluginConfig().getAsInteger(GALLERY_THUMBNAIL_SIZE);
        final int thumbnailBoxSize = getPluginConfig().getAsInteger(GALLERY_THUMBNAIL_BOX_SIZE);
        return createIconColumn(thumbnailSize, thumbnailBoxSize);
    }

    public static ListColumn<Node> createIconColumn(final int thumbnailSize, final int thumbnailBoxSize) {
        final ListColumn<Node> column = new ListColumn<>(Model.of(StringUtils.EMPTY), null);
        column.setRenderer(new ImageIconRenderer(thumbnailSize, thumbnailBoxSize));
        column.setCssClass(DocumentListColumn.ICON.getCssClass());
        return column;
    }

    public List<ListColumn<Node>> getExpandedColumns() {
        final List<ListColumn<Node>> columns = getColumns();
        columns.add(createWidthColumn());
        columns.add(createHeightColumn());
        columns.add(createMimeTypeColumn());
        columns.add(createSizeColumn());
        columns.add(createLastModifiedColumn());
        return columns;
    }

    private ListColumn<Node> createWidthColumn() {
        final ClassResourceModel displayModel = new ClassResourceModel("gallery-width", Translations.class);
        final ListColumn<Node> column = new ListColumn<>(displayModel, "width");
        column.setRenderer(new ImageDimensionRenderer(HippoGalleryNodeType.IMAGE_WIDTH, primaryItemName));
        column.setComparator(new LongPropertyComparator(HippoGalleryNodeType.IMAGE_WIDTH, primaryItemName));
        column.setCssClass(DocumentListColumn.WIDTH.getCssClass());
        return column;
    }

    private ListColumn<Node> createHeightColumn() {
        final ClassResourceModel displayModel = new ClassResourceModel("gallery-height", Translations.class);
        final ListColumn<Node> column = new ListColumn<>(displayModel, "height");
        column.setRenderer(new ImageDimensionRenderer(HippoGalleryNodeType.IMAGE_HEIGHT, primaryItemName));
        column.setComparator(new LongPropertyComparator(HippoGalleryNodeType.IMAGE_HEIGHT, primaryItemName));
        column.setCssClass(DocumentListColumn.HEIGHT.getCssClass());
        return column;
    }

    private ListColumn<Node> createMimeTypeColumn() {
        final ClassResourceModel displayModel = new ClassResourceModel("gallery-mimetype", Translations.class);
        final ListColumn<Node> column = new ListColumn<>(displayModel, "mimetype");
        column.setRenderer(new StringPropertyRenderer(JcrConstants.JCR_MIMETYPE, primaryItemName));
        column.setComparator(new MimeTypeComparator(JcrConstants.JCR_MIMETYPE, primaryItemName));
        column.setCssClass(DocumentListColumn.MIME_TYPE.getCssClass());
        return column;
    }

    private ListColumn<Node> createSizeColumn() {
        final ClassResourceModel displayModel = new ClassResourceModel("gallery-size", Translations.class);
        final ListColumn<Node> column = new ListColumn<>(displayModel, "size");
        column.setRenderer(new SizeRenderer(JcrConstants.JCR_DATA, primaryItemName));
        column.setComparator(new SizeComparator(JcrConstants.JCR_DATA, primaryItemName));
        column.setCssClass(DocumentListColumn.SIZE.getCssClass());
        return column;
    }

    private ListColumn<Node> createLastModifiedColumn() {
        final ClassResourceModel displayModel = new ClassResourceModel("gallery-lastmodified", Translations.class);
        final ListColumn<Node> column = new ListColumn<>(displayModel, "lastmodified");
        column.setRenderer(new DatePropertyRenderer(JcrConstants.JCR_LASTMODIFIED, primaryItemName));
        column.setComparator(new CalendarComparator(JcrConstants.JCR_LASTMODIFIED, primaryItemName));
        column.setCssClass(DocumentListColumn.LAST_MODIFIED_BY.getCssClass());
        return column;
    }

    private static class ImageDimensionRenderer extends StringPropertyRenderer {

        public ImageDimensionRenderer(final String propertyName, final String relPath) {
            super(propertyName, relPath);
        }

        @Override
        protected String getValue(Property p) throws RepositoryException {
            return super.getValue(p) + "px";
        }
    }

}
