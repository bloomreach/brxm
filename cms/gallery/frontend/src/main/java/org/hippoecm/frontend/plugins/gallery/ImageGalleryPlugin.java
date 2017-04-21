/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.ComponentTag;
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.columns.FallbackImageGalleryListColumnProvider;
import org.hippoecm.frontend.plugins.gallery.columns.ImageGalleryColumnProviderPlugin;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.list.DocumentsProvider;
import org.hippoecm.frontend.plugins.standards.list.ExpandCollapseListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.IListColumnProvider;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.skin.DocumentListColumn;
import org.hippoecm.frontend.skin.Icon;

import static org.hippoecm.frontend.plugins.gallery.ImageGalleryPlugin.Mode.LIST;
import static org.hippoecm.frontend.plugins.gallery.ImageGalleryPlugin.Mode.THUMBNAILS;

public class ImageGalleryPlugin extends ExpandCollapseListingPlugin<Node> {

    private static final String CONFIG_GALLERY_THUMBNAIL_SIZE = "gallery.thumbnail.size";

    enum Mode {
        LIST, THUMBNAILS
    }

    private Mode mode = THUMBNAILS;

    public ImageGalleryPlugin(final IPluginContext context, final IPluginConfig config) throws RepositoryException {
        super(context, config);

        setClassName(DocumentListColumn.DOCUMENT_LIST_CSS_CLASS);

        add(CssClass.append("image-gallery"));
        add(CssClass.append(ReadOnlyModel.of(() -> mode == LIST ? "image-gallery-list" : "image-gallery-thumbnails")));

        addButton(new GalleryModeButton("listButton", LIST, Icon.LIST_UL));
        addButton(new GalleryModeButton("thumbnailsButton", THUMBNAILS, Icon.THUMBNAILS));
    }

    @Override
    protected List<ListColumn<Node>> getColumns() {
        if (mode == LIST) {
            return super.getColumns();
        } else {
            return getThumbnailModeColumns();
        }
    }

    @Override
    protected List<ListColumn<Node>> getExpandedColumns() {
        if (mode == LIST) {
            return super.getExpandedColumns();
        } else {
            return getThumbnailModeColumns();
        }
    }

    public List<ListColumn<Node>> getThumbnailModeColumns() {
        final int thumbnailSize = getPluginConfig().getAsInteger(CONFIG_GALLERY_THUMBNAIL_SIZE);
        return Arrays.asList(
                ImageGalleryColumnProviderPlugin.createIconColumn(thumbnailSize, thumbnailSize),
                ImageGalleryColumnProviderPlugin.NAME_COLUMN
        );
    }

    @Override
    protected ISortableDataProvider<Node, String> newDataProvider() {
        final DocumentListFilter filter = new DocumentListFilter(getPluginConfig());
        final Map<String, Comparator<Node>> comparators = getTableDefinition().getComparators();
        return new DocumentsProvider(getModel(), filter, comparators);
    }

    @Override
    protected IListColumnProvider getDefaultColumnProvider() {
        return new FallbackImageGalleryListColumnProvider();
    }

    private class GalleryModeButton extends AjaxLink<String> {

        private final Mode activatedMode;

        GalleryModeButton(final String id, final Mode activatedMode, final Icon icon) {
            super(id);

            this.activatedMode = activatedMode;
            setOutputMarkupId(true);

            add(HippoIcon.fromSprite("icon", icon));
        }

        @Override
        protected void onComponentTag(final ComponentTag tag) {
            if (mode == activatedMode) {
                tag.put("class", "gallery-mode-active");
            }
            super.onComponentTag(tag);
        }

        @Override
        public void onClick(final AjaxRequestTarget target) {
            mode = activatedMode;
            ImageGalleryPlugin.this.onModelChanged();
        }
    }

}
