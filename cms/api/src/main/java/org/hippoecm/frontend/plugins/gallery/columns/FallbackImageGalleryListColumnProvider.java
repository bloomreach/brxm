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

import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.markup.html.IHeaderContributor;
import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
import org.hippoecm.frontend.plugins.standards.list.IListColumnProvider;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;

public class FallbackImageGalleryListColumnProvider implements IListColumnProvider {

    @Override
    public IHeaderContributor getHeaderContributor() {
        return null;
    }

    @Override
    public List<ListColumn<Node>> getColumns() {
        final int thumbnailSize = DefaultGalleryProcessor.DEFAULT_THUMBNAIL_SIZE;
        return Arrays.asList(
                ImageGalleryColumnProviderPlugin.createIconColumn(thumbnailSize, thumbnailSize),
                ImageGalleryColumnProviderPlugin.NAME_COLUMN
        );
    }

    @Override
    public List<ListColumn<Node>> getExpandedColumns() {
        return getColumns();
    }
}
