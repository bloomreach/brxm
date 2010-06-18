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
package org.hippoecm.frontend.plugins.gallery;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.AbstractListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.yui.JsFunction;
import org.hippoecm.frontend.plugins.yui.tables.TableHelperBehavior;
import org.hippoecm.frontend.plugins.yui.widget.WidgetBehavior;
import org.hippoecm.frontend.plugins.yui.widget.WidgetSettings;
import org.hippoecm.frontend.widgets.LabelWithTitle;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageGalleryPlugin extends AbstractListingPlugin implements IHeaderContributor {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final static Logger log = LoggerFactory.getLogger(ImageGalleryPlugin.class);
    private static final String IMAGE_GALLERY_CSS = "ImageGalleryPlugin.css";
    private static final String TOGGLE_LIST_IMG = "toggle_list.png";
    private static final String TOGGLE_THUMBNAIL_IMG = "toggle_thumb.png";

    private static final String IMAGE_FOLDER_TYPE = "hippogallery:stdImageGallery";

    private String viewMode = "LIST";

    private WebMarkupContainer galleryList;
    private AjaxLink<String> toggleLink;
    private Image toggleImage;

    public ImageGalleryPlugin(final IPluginContext context, final IPluginConfig config) throws RepositoryException {
        super(context, config);

        dataTable.setOutputMarkupId(true);

        add(galleryList = new WebMarkupContainer("gallery-list"));
        galleryList.setOutputMarkupId(true);
        galleryList.setVisible(false);
        galleryList.add(new GalleryItemView("gallery-item"));

        WidgetSettings settings = new WidgetSettings();
        settings.setCalculateWidthAndHeight(new JsFunction(
                "function(sizes) {return {width: sizes.wrap.w, height: sizes.wrap.h-25};}"));
        galleryList.add(new WidgetBehavior(settings));

        add(toggleLink = new AjaxLink<String>("toggle", new Model<String>()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                viewMode = "LIST".equals(viewMode) ? "THUMBNAILS" : "LIST";
                redraw();

            }
        });
        toggleLink.setOutputMarkupId(true);

        toggleImage = new Image("toggleimg", TOGGLE_LIST_IMG);
        toggleImage.setOutputMarkupId(true);
        toggleLink.add(toggleImage);
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        if (viewMode.equals("LIST")) {
            this.dataTable.setVisible(true);
            this.galleryList.setVisible(false);
            toggleImage = new Image("toggleimg", TOGGLE_LIST_IMG);
        } else {
            this.dataTable.setVisible(false);
            this.galleryList.setVisible(true);
            toggleImage = new Image("toggleimg", TOGGLE_THUMBNAIL_IMG);
        }

        toggleLink.replace(toggleImage);
    }

    @Override
    public TableDefinition<Node> getTableDefinition() {
        List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>();

        ListColumn<Node> column = new ListColumn<Node>(new Model<String>(""), null);
        column.setRenderer(new EmptyRenderer<Node>());
        column.setAttributeModifier(new GalleryFolderAttributeModifier());
        columns.add(column);

        column = new ListColumn<Node>(new StringResourceModel("gallery-name", this, null), "name");
        column.setComparator(new NameComparator());
        columns.add(column);

        return new TableDefinition<Node>(columns);
    }

    @Override
    protected ListDataTable<Node> getListDataTable(String id, TableDefinition<Node> tableDefinition,
            ISortableDataProvider<Node> dataProvider, TableSelectionListener<Node> selectionListener, boolean triState,
            ListPagingDefinition pagingDefinition) {
        ListDataTable<Node> ldt = super.getListDataTable(id, tableDefinition, dataProvider, selectionListener,
                triState, pagingDefinition);
        ldt.add(new TableHelperBehavior());
        return ldt;
    }

    public void renderHead(IHeaderResponse response) {
        ResourceReference cssResourceReference = new ResourceReference(ImageGalleryPlugin.class, IMAGE_GALLERY_CSS);
        response.renderCSSReference(cssResourceReference);
    }

    @Override
    protected void onSelectionChanged(IModel<Node> model) {
        AjaxRequestTarget target = AjaxRequestTarget.get();
        if (target != null && viewMode.equals("THUMBNAILS")) {
            target.addComponent(galleryList);
        }
    }

    private class GalleryItemView extends RefreshingView<Node> {

        public GalleryItemView(String id) {
            super(id);

            setOutputMarkupId(true);

            setItemReuseStrategy(new ReuseIfModelsEqualStrategy());
        }

        @Override
        protected Iterator<IModel<Node>> getItemModels() {
            ArrayList<IModel<Node>> nodeModels = new ArrayList<IModel<Node>>();

            final NodeIterator iterator;
            try {

                iterator = ImageGalleryPlugin.this.getModelObject().getNodes();
                while (iterator.hasNext()) {
                    javax.jcr.Node node = iterator.nextNode();
                    if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                        if (node.hasNode(node.getName())) {
                            javax.jcr.Node imageSet = node.getNode(node.getName());
                            try {
                                Item primItem = imageSet.getPrimaryItem();
                                if (primItem.isNode()) {
                                    if (((javax.jcr.Node) primItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                                        nodeModels.add(new JcrNodeModel(node));
                                    } else {
                                        log.warn("primary item of image set must be of type "
                                                + HippoNodeType.NT_RESOURCE);
                                    }
                                }
                            } catch (ItemNotFoundException e) {
                                log.debug("ImageSet must have a primary item. " + node.getPath()
                                        + " probably not of correct image set type");
                            }
                        }
                    } else if (node.isNodeType(IMAGE_FOLDER_TYPE)) {
                        nodeModels.add(new JcrNodeModel(node));
                    } else {
                        log.info("invalid node type, not adding to the list of items");
                    }
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
            return nodeModels.iterator();
        }

        @Override
        protected void populateItem(final org.apache.wicket.markup.repeater.Item<Node> listItem) {

            listItem.add(new AttributeAppender("class", true, new Model<String>("selected"), " ") {
                @Override
                public boolean isEnabled(Component component) {
                    IModel<Node> selected = getSelectedModel();
                    return selected != null && selected.equals(listItem.getDefaultModel());
                }
            });
            listItem.setOutputMarkupId(true);

            final JcrNodeModel imgNodeModel = (JcrNodeModel) listItem.getDefaultModel();
            Node node = imgNodeModel.getNode();

            try {
                if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                    if (node.hasNode(node.getName())) {
                        Node imageSet = node.getNode(node.getName());
                        try {
                            Item primItem = imageSet.getPrimaryItem();
                            if (primItem.isNode()) {
                                if (((Node) primItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                                    AjaxLink itemLink = new AjaxLink("itemLink") {
                                        @Override
                                        public void onClick(AjaxRequestTarget target) {
                                            setSelectedModel(listItem.getModel());
                                            target.addComponent(GalleryItemView.this.getParent());
                                        }
                                    };

                                    Image folderIcon = new Image("folder-icon", "hippo-gallery-folder.png");
                                    folderIcon.setVisible(false);
                                    itemLink.add(folderIcon);
                                    itemLink.add(new ImageContainer("thumbnail", new JcrNodeModel((Node) primItem),
                                            getPluginContext(), getPluginConfig()));

                                    itemLink.add(new LabelWithTitle("title", new NodeTranslator(new JcrNodeModel(node))
                                            .getNodeName()));
                                    listItem.add(itemLink);
                                } else {
                                    log.warn("primary item of image set must be of type " + HippoNodeType.NT_RESOURCE);
                                }
                            }
                        } catch (ItemNotFoundException e) {
                            log.debug("ImageSet must have a primary item. " + node.getPath()
                                    + " probably not of correct image set type");
                        }
                    }

                } else if (node.isNodeType(IMAGE_FOLDER_TYPE)) {
                    AjaxLink itemLink = new AjaxLink("itemLink") {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            setSelectedModel(listItem.getModel());
                            target.addComponent(GalleryItemView.this.getParent());
                        }
                    };

                    Panel thumbnail = new EmptyPanel("thumbnail");
                    Image folderIcon = new Image("folder-icon", "hippo-gallery-folder.png");
                    itemLink.add(folderIcon);
                    itemLink.add(thumbnail);
                    itemLink.add(new Label("title", new NodeTranslator(new JcrNodeModel(node)).getNodeName()));
                    listItem.add(itemLink);
                }

            } catch (RepositoryException e) {
                listItem.add(new EmptyPanel("thumbnail"));

            }
        }
    }
}
