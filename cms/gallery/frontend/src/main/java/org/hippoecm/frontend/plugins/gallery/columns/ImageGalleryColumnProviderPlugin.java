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
package org.hippoecm.frontend.plugins.gallery.columns;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.columns.compare.CalendarComparator;
import org.hippoecm.frontend.plugins.gallery.columns.compare.LongPropertyComparator;
import org.hippoecm.frontend.plugins.gallery.columns.compare.MimeTypeComparator;
import org.hippoecm.frontend.plugins.gallery.columns.compare.SizeComparator;
import org.hippoecm.frontend.plugins.gallery.columns.render.DatePropertyRenderer;
import org.hippoecm.frontend.plugins.gallery.columns.render.ImageIconRenderer;
import org.hippoecm.frontend.plugins.gallery.columns.render.SizeRenderer;
import org.hippoecm.frontend.plugins.gallery.columns.render.StringPropertyRenderer;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.list.AbstractListColumnProviderPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconAttributeModifier;

public class ImageGalleryColumnProviderPlugin extends AbstractListColumnProviderPlugin {
    private static final long serialVersionUID = 1L;
    public static final CssResourceReference CSS_RESOURCE_REFERENCE = new CssResourceReference(ImageGalleryColumnProviderPlugin.class, "ImageGalleryStyle.css");


    private String primaryItemName;

    public ImageGalleryColumnProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        primaryItemName = config.getString("primaryItemName", "hippogallery:original");
    }

    @Override
    public IHeaderContributor getHeaderContributor() {
        return new IHeaderContributor() {
            @Override
            public void renderHead(final IHeaderResponse response) {
                response.render(CssHeaderItem.forReference(CSS_RESOURCE_REFERENCE));
            }
        };
    }

    public List<ListColumn<Node>> getColumns() {
        List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>();

        //image icon
        ListColumn<Node> column = new ListColumn<Node>(new Model<String>(""), null);
        column.setRenderer(new ImageIconRenderer());
        column.setAttributeModifier(new IconAttributeModifier());
        column.setCssClass("image-gallery-icon");
        columns.add(column);

        //node name
        column = new ListColumn<Node>(new ClassResourceModel("gallery-name", Translations.class), "name");
        column.setComparator(new NameComparator());
        column.setCssClass("gallery-name");
        columns.add(column);

        return columns;
    }

    /**
     * We have to be careful with adding another column here; the current implementation allows for only one column that
     * can contain really long values which will be clipped so the UI doesn't break. To allow for more columns that
     * behave like this, while keeping performance acceptable we will have to go for a *real* widget.
     */
    public List<ListColumn<Node>> getExpandedColumns() {
        List<ListColumn<Node>> columns = getColumns();

        //width
        ListColumn<Node> column = new ListColumn<Node>(new ClassResourceModel("gallery-width", Translations.class), "width");
        column.setRenderer(new StringPropertyRenderer("hippogallery:width", primaryItemName) {
            @Override
            protected String getValue(Property p) throws RepositoryException {
                return super.getValue(p) + "px";
            }
        });
        column.setComparator(new LongPropertyComparator("hippogallery:width", primaryItemName));
        column.setCssClass("gallery-width");
        columns.add(column);

        //height
        column = new ListColumn<Node>(new ClassResourceModel("gallery-height", Translations.class), "height");
        column.setRenderer(new StringPropertyRenderer("hippogallery:height", primaryItemName) {
            @Override
            protected String getValue(Property p) throws RepositoryException {
                return super.getValue(p) + "px";
            }
        });
        column.setComparator(new LongPropertyComparator("hippogallery:height", primaryItemName));
        column.setCssClass("gallery-height");
        columns.add(column);

        //Mimetype
        column = new ListColumn<Node>(new ClassResourceModel("gallery-mimetype", Translations.class), "mimetype");
        column.setRenderer(new StringPropertyRenderer("jcr:mimeType", primaryItemName));
        column.setComparator(new MimeTypeComparator("jcr:mimeType", primaryItemName));
        column.setCssClass("gallery-mimetype");
        columns.add(column);

        //filesize
        column = new ListColumn<Node>(new ClassResourceModel("gallery-size", Translations.class), "size");
        column.setRenderer(new SizeRenderer("jcr:data", primaryItemName));
        column.setComparator(new SizeComparator("jcr:data", primaryItemName));
        column.setCssClass("gallery-size");
        columns.add(column);

        //Last modified date
        column = new ListColumn<Node>(new ClassResourceModel("gallery-lastmodified", Translations.class),
                "lastmodified");
        column.setRenderer(new DatePropertyRenderer("jcr:lastModified", primaryItemName));
        column.setComparator(new CalendarComparator("jcr:lastModified", primaryItemName));
        column.setCssClass("gallery-lastmodified");
        columns.add(column);

        return columns;
    }

}
