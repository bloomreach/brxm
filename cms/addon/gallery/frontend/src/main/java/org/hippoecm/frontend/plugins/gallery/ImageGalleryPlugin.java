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
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.AbstractListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.yui.tables.TableHelperBehavior;
import org.hippoecm.repository.api.HippoNodeType;

public class ImageGalleryPlugin extends AbstractListingPlugin implements IHeaderContributor {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    private static final String IMAGE_GALLERY_CSS = "ImageGalleryPlugin.css";
    private static final String TOGGLE_LIST_IMG = "toggle_list.png";
    private static final String TOGGLE_THUMBNAIL_IMG = "toggle_thumb.png";

    private static final String IMAGE_FOLDER_TYPE = "hippogallery:stdImageGallery";

    private JcrNodeModel model;
    private WebMarkupContainer galleryList;
    private IPluginContext pluginContext;
    private IPluginConfig pluginConfig;

    private String viewMode = "LIST";
    private AjaxLink<String> toggleLink;
    private Image toggleImage;
    private GalleryItemView galleryItemView;


    public ImageGalleryPlugin(final IPluginContext context, final IPluginConfig config) throws RepositoryException {
        super(context, config);
        this.model = (JcrNodeModel) getModel();
        this.pluginContext = context;
        this.pluginConfig = config;
        galleryList = new WebMarkupContainer("gallery-list");
        galleryList.setOutputMarkupId(true);
        galleryList.setVisible(false);
        galleryItemView = new GalleryItemView("gallery-item");
        galleryItemView.setOutputMarkupId(true);
        galleryList.add(galleryItemView);
        add(galleryList);

        toggleImage = new Image("toggleimg", TOGGLE_LIST_IMG);

        toggleImage.setOutputMarkupId(true);

        toggleLink = new AjaxLink<String>("toggle", new Model<String>()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                viewMode = "LIST".equals(viewMode) ? "THUMBNAILS" : "LIST";
                redraw();

            }
        };
        toggleLink.setOutputMarkupId(true);
        toggleLink.add(toggleImage);
        add(toggleLink);
        this.dataTable.setOutputMarkupId(true);
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
    public TableDefinition getTableDefinition() {
        List<ListColumn> columns = new ArrayList<ListColumn>();

        ListColumn column = new ListColumn(new Model<String>(""), null);
        column.setRenderer(new EmptyRenderer());
        column.setAttributeModifier(new GalleryFolderAttributeModifier());
        columns.add(column);


        column = new ListColumn(new StringResourceModel("gallery-name", this, null), "name");
        column.setComparator(new NameComparator());
        columns.add(column);

        return new TableDefinition(columns);
    }

    @Override
    protected ListDataTable getListDataTable(String id, TableDefinition tableDefinition,
                                             ISortableDataProvider dataProvider, TableSelectionListener selectionListener, boolean triState,
                                             ListPagingDefinition pagingDefinition) {
        ListDataTable ldt = super.getListDataTable(id, tableDefinition, dataProvider, selectionListener, triState, pagingDefinition);
        ldt.add(new TableHelperBehavior());
        return ldt;
    }

    public void renderHead(IHeaderResponse response) {
        ResourceReference cssResourceReference = new ResourceReference(ImageGalleryPlugin.class, IMAGE_GALLERY_CSS);
        response.renderCSSReference(cssResourceReference);
    }

    /**
     * Gallery Item View
     */
    private class GalleryItemView extends RefreshingView<Node> {

        public GalleryItemView(String id) {
            super(id);
        }


        @Override
        protected void onModelChanged() {
            super.onModelChanged();
        }

        @Override
        protected Iterator<IModel<Node>> getItemModels() {
            ArrayList<IModel<Node>> nodeModels = new ArrayList<IModel<Node>>();

            final NodeIterator iterator;
            try {

                iterator = model.getNode().getNodes();
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
                                        Gallery.log.warn("primary item of image set must be of type " + HippoNodeType.NT_RESOURCE);
                                    }
                                }
                            } catch (ItemNotFoundException e) {
                                Gallery.log.debug("ImageSet must have a primary item. " + node.getPath()
                                        + " probably not of correct image set type");
                            }
                        }
                    } else if (node.isNodeType(IMAGE_FOLDER_TYPE)) {
                        nodeModels.add(new JcrNodeModel(node));
                    } else {
                        Gallery.log.info("invalid node type, not adding to the list of items");
                    }
                }
            } catch (RepositoryException e) {
                Gallery.log.error(e.getMessage());

            }
            return nodeModels.iterator();
        }

        @Override
        protected void populateItem(final org.apache.wicket.markup.repeater.Item listItem) {

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

                                            IPluginConfig config = getPluginConfig();
                                            if (config.getString("model.document") != null) {
                                                IModelReference<IModel> documentService = getPluginContext().getService(config.getString("model.document"),
                                                        IModelReference.class);
                                                if (documentService != null) {
                                                    documentService.setModel((IModel<IModel>) listItem.getDefaultModel());
                                                }
                                            }

                                        }
                                    };
                                    Image folderIcon = new Image("folder-icon", "hippo-gallery-folder.png");
                                    folderIcon.setVisible(false);
                                    itemLink.add(folderIcon);
                                    itemLink.add(new ImageContainer("thumbnail", new JcrNodeModel((Node) primItem), getPluginContext(), getPluginConfig()));
                                    itemLink.add(new Label("title", new Model<String>(node.getName())));
                                    listItem.add(itemLink);


                                } else {
                                    Gallery.log.warn("primary item of image set must be of type " + HippoNodeType.NT_RESOURCE);
                                }
                            }
                        } catch (ItemNotFoundException e) {
                            Gallery.log.debug("ImageSet must have a primary item. " + node.getPath()
                                    + " probably not of correct image set type");
                        }
                    }

                } else if (node.isNodeType(IMAGE_FOLDER_TYPE)) {
                    AjaxLink itemLink = new AjaxLink("itemLink") {
                        @Override
                        public void onClick(AjaxRequestTarget target) {

                            IPluginConfig config = getPluginConfig();
                            if (config.getString("model.document") != null) {
                                IModelReference<IModel> documentService = getPluginContext().getService(config.getString("model.document"),
                                        IModelReference.class);
                                if (documentService != null) {
                                    documentService.setModel((IModel<IModel>) listItem.getDefaultModel());
                                }
                            }

                        }
                    };

                    Panel thumbnail = new EmptyPanel("thumbnail");
                    Image folderIcon = new Image("folder-icon", "hippo-gallery-folder.png");
                    itemLink.add(folderIcon);
                    itemLink.add(thumbnail);
                    itemLink.add(new Label("title", new Model<String>(node.getName())));
                    listItem.add(itemLink);
                }

            } catch (RepositoryException e) {
                listItem.add(new EmptyPanel("thumbnail"));

            }
        }
    }

    @Override
    public void onModelChanged() {
        this.model = (JcrNodeModel) getModel();
        galleryItemView.onModelChanged();
        super.onModelChanged();

    }
}
