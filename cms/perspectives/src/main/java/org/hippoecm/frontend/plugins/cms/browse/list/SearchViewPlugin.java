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
package org.hippoecm.frontend.plugins.cms.browse.list;

import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.columns.FallbackImageGalleryListColumnProvider;
import org.hippoecm.frontend.plugins.gallery.columns.ImageGalleryColumnProviderPlugin;
import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.list.IListColumnProvider;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.SearchDocumentsProvider;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.plugins.standards.search.TextSearchResultModel;
import org.hippoecm.frontend.skin.DocumentListColumn;
import org.hippoecm.frontend.skin.Icon;

import static org.hippoecm.frontend.plugins.cms.browse.list.SearchViewPlugin.Mode.LIST;
import static org.hippoecm.frontend.plugins.cms.browse.list.SearchViewPlugin.Mode.THUMBNAILS;

/**
 * Optional values for <strong>gallery.thumbnail.size</strong> (default value is 60) and
 * <strong>gallery.thumbnail.box.size</strong> (default value is 32) can be configured within:
 * <p>
 * <code>/hippo:configuration/hippo:frontend/cms/cms-search-views/text/root</code>
 * </p>
 */
public final class SearchViewPlugin extends DocumentListingPlugin<BrowserSearchResult> {

    private static final String CONFIG_GALLERY_THUMBNAIL_SIZE = "gallery.thumbnail.size";
    private static final String GALLERY_THUMBNAIL_BOX_SIZE = "gallery.thumbnail.box.size";
    private static final String GALLERY_PATH = "/content/gallery";
    private static final int DEFAULT_BOX_SIZE = 32;

    enum Mode {
        LIST, THUMBNAILS
    }

    private Mode mode = THUMBNAILS;

    boolean imageSearch;

    public SearchViewPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final IModel<BrowserSearchResult> model = getModel();
        setClassName(DocumentListColumn.DOCUMENT_LIST_CSS_CLASS);
        imageSearch = isImageSearch(model);
        if (imageSearch) {
            add(CssClass.append("image-gallery"));
            add(CssClass.append(ReadOnlyModel.of(() -> mode == LIST ? "image-gallery-list" : "image-gallery-thumbnails")));
        }
        final GalleryModeButton listButton = new GalleryModeButton("listButton", LIST, Icon.LIST_UL);
        final GalleryModeButton thumbnailsButton = new GalleryModeButton("thumbnailsButton", THUMBNAILS, Icon.THUMBNAILS);
        addButton(listButton);
        addButton(thumbnailsButton);
        listButton.setVisible(imageSearch);
        thumbnailsButton.setVisible(imageSearch);
        listButton.setOutputMarkupId(true);
        thumbnailsButton.setOutputMarkupId(true);
    }

    private boolean isImageSearch(final IModel<BrowserSearchResult> model) {
        if (model instanceof TextSearchResultModel) {
            final TextSearchResultModel textSearchResultModel = (TextSearchResultModel) model;
            final String[] scope = textSearchResultModel.getScope();
            if (scope != null) {
                for (final String s : scope) {
                    if (s != null && s.startsWith(GALLERY_PATH)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected boolean isOrderable() {
        return true;
    }

    @Override
    protected ISortableDataProvider<Node, String> newDataProvider() {
        return new SearchDocumentsProvider(getModel(), getTableDefinition().getComparators());
    }


    @Override
    protected IListColumnProvider getDefaultColumnProvider() {
        return new FallbackImageGalleryListColumnProvider();
    }

    @Override
    protected List<ListColumn<Node>> getColumns() {
        if (!imageSearch) {
            return super.getColumns();
        }
        if (mode == LIST) {
            return getListThumbnailModeColumns();
        } else {
            return getThumbnailModeColumns();
        }
    }

    @Override
    protected List<ListColumn<Node>> getExpandedColumns() {
        if (!imageSearch) {
            return super.getExpandedColumns();
        }
        if (mode == LIST) {
            return super.getExpandedColumns();
        } else {
            return getThumbnailModeColumns();
        }
    }

    public List<ListColumn<Node>> getThumbnailModeColumns() {
        final int thumbnailSize = getPluginConfig().getAsInteger(CONFIG_GALLERY_THUMBNAIL_SIZE, DefaultGalleryProcessor.DEFAULT_THUMBNAIL_SIZE);
        return Arrays.asList(
                ImageGalleryColumnProviderPlugin.createIconColumn(thumbnailSize, thumbnailSize),
                ImageGalleryColumnProviderPlugin.NAME_COLUMN
        );
    }

    public List<ListColumn<Node>> getListThumbnailModeColumns() {
        final int thumbnailSize = getPluginConfig().getAsInteger(CONFIG_GALLERY_THUMBNAIL_SIZE, DefaultGalleryProcessor.DEFAULT_THUMBNAIL_SIZE);
        final int thumbnailBoxSize = getPluginConfig().getAsInteger(GALLERY_THUMBNAIL_BOX_SIZE, DEFAULT_BOX_SIZE);
        return Arrays.asList(
                ImageGalleryColumnProviderPlugin.createIconColumn(thumbnailSize, thumbnailBoxSize),
                ImageGalleryColumnProviderPlugin.NAME_COLUMN
        );
    }

    private class GalleryModeButton extends AjaxLink<String> {

        private final Mode activatedMode;

        public GalleryModeButton(final String id, final Mode activatedMode, final Icon icon) {
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
            SearchViewPlugin.this.onModelChanged();
        }
    }

}
