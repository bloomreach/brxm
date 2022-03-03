/*
 * Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.addon.frontend.gallerypicker;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.ajax.BrLink;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.addon.frontend.gallerypicker.dialog.GalleryPickerDialog;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The org.onehippo.addon.frontend.gallerypicker.GalleryPickerPlugin provides a Wicket dialog that allows a content
 * editor to select an image from the image gallery.
 *
 * @author Jeroen Reijn
 */
public class GalleryPickerPlugin extends RenderPlugin<Node> {

    private static final Logger log = LoggerFactory.getLogger(GalleryPickerPlugin.class);

    private static final CssResourceReference GALLERY_PICKER_CSS =
            new CssResourceReference(GalleryPickerPlugin.class, GalleryPickerPlugin.class.getSimpleName() + ".css");

    private static final String DEFAULT_THUMBNAIL_WIDTH = "50";
    private static final String HIPPO_GALLERY_EXAMPLE_IMAGESET_NODETYPE_NAME = "hippogallery:exampleImageSet";
    private static final String HIPPO_GALLERY_STD_GALLERYSET_NODETYPE_NAME = "hippogallery:stdgalleryset";
    private static final String SUPPORTED_PATHS_KEY = "supported.paths";

    public static final String GALLERY_ROOT_PATH = "/content/gallery/";

    private final IModel<String> valueModel;
    private final JcrNodeModel currentNodeModel;
    private final ImageItemFactory imageFactory;
    private final InlinePreviewImage inlinePreviewImage;

    private String[] supportedPaths;
    private AjaxLink<Void> remove;
    protected Mode mode;
    protected IPluginConfig config;

    //this object will be used by wicket based on the propertyModel provided to the inlinePreview image
    @SuppressWarnings("unused")
    private ImageItem image;

    @SuppressWarnings("unchecked")
    public GalleryPickerPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        this.config = config;

        imageFactory = new ImageItemFactory();

        currentNodeModel = (JcrNodeModel) getModel();

        valueModel = getValueModel(currentNodeModel);
        // See if the plugin is in 'edit' or in 'view' mode
        mode = Mode.fromString(config.getString(ITemplateEngine.MODE, "view"));

        if (config.containsKey(SUPPORTED_PATHS_KEY)) {
            supportedPaths = config.getStringArray(SUPPORTED_PATHS_KEY);
        }

        final Fragment fragment;
        switch (mode) {
            case COMPARE:
                fragment = new Fragment("fragment", "compare", this);
                String path = null;
                if (config.containsKey("model.compareTo")) {
                    final IModelReference<Node> baseModelRef = context.getService(config.getString("model.compareTo"),
                            IModelReference.class);
                    if (baseModelRef != null) {
                        final IModel<Node> baseModel = baseModelRef.getModel();
                        if (baseModel != null && baseModel.getObject() != null) {
                            final String uuid = getValueModel(baseModel).getObject();
                            path = imageFactory.createImageItem(uuid).getPrimaryUrl();
                        }
                    }
                }
                final InlinePreviewImage baseImagePreview = new InlinePreviewImage("baseImage", Model.of(path),
                        getWidth(), getHeight());
                baseImagePreview.setVisible(!Strings.isEmpty(path));
                fragment.add(baseImagePreview);
                break;

            case EDIT:
                fragment = new Fragment("fragment", "edit", this);
                fragment.add(new BrLink<Void>("select") {
                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        getDialogService().show(createDialog());
                    }
                });
                addOpenButton(fragment);

                remove = new BrLink<>("remove") {
                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        valueModel.setObject(JcrConstants.ROOT_NODE_ID);
                        triggerModelChanged();
                    }
                };
                fragment.add(remove);

                remove.setVisible(false);
                if (isValidDisplaySelection()) {
                    remove.setVisible(true);
                }
                break;

            default:
                fragment = new Fragment("fragment", "view", this);
        }

        final PropertyModel<String> previewImage = new PropertyModel<>(this, "image.primaryUrl");
        inlinePreviewImage = new InlinePreviewImage("previewImage", previewImage, getWidth(), getHeight());
        inlinePreviewImage.setVisible(isValidDisplaySelection());
        fragment.add(inlinePreviewImage);
        add(fragment);

        setOutputMarkupId(true);

        modelChanged();
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(GALLERY_PICKER_CSS));
    }

    protected String getWidth() {
        return config.getString("preview.width", DEFAULT_THUMBNAIL_WIDTH);
    }

    protected String getHeight() {
        return config.getString("preview.height");
    }

    private static IModel<String> getValueModel(final IModel<Node> nodeModel) {
        final Node node = nodeModel.getObject();
        if (node != null) {
            try {
                final Property prop = node.getProperty("hippo:docbase");
                return new JcrPropertyValueModel<>(-1, prop.getValue(), new JcrPropertyModel<String>(prop));
            } catch (final RepositoryException ex) {
                throw new WicketRuntimeException("Property hippo:docbase is not defined.", ex);
            }
        } else {
            return Model.of("");
        }
    }

    private void addOpenButton(final Fragment fragment) {
        fragment.add(new BrLink<Void>("open") {
            @Override
            public boolean isVisible() {
                return isValidDisplaySelection();
            }

            @Override
            public void onClick(final AjaxRequestTarget target) {
                open();
            }
        });
    }

    private void open() {
        final IPluginConfig config = getPluginConfig();
        final IPluginContext context = getPluginContext();
        final IModel<String> displayModel = getPathModel();
        final String browserId = config.getString("browser.id", "service.browse");
        final IBrowseService browseService = context.getService(browserId, IBrowseService.class);
        final String location = config.getString("option.location", displayModel.getObject());
        if (browseService != null) {
            //noinspection unchecked
            browseService.browse(new JcrNodeModel(location));
        } else {
            log.warn("no browse service found with id '{}', cannot browse to '{}'", browserId, location);
        }
    }

    /**
     * Create a gallery picker dialog to select an image.
     */
    private AbstractDialog<String> createDialog() {
        final IModel<String> dialogModel = new IModel<>() {

            @Override
            public String getObject() {
                return valueModel.getObject();
            }

            @Override
            public void setObject(final String object) {
                valueModel.setObject(object);
                GalleryPickerPlugin.this.modelChanged();
            }

            @Override
            public void detach() {
                valueModel.detach();
            }
        };
        return new GalleryPickerDialog(getPluginContext(), getPluginConfig(), dialogModel);
    }

    @Override
    public void onModelChanged() {
        triggerModelChanged();
    }

    /**
     * If the model of this plugin changes, choose what to do with the image preview. If no image is selected, make the
     * image invisible, so it won't show a red cross in IE. If an image is selected, show the selected image.
     */
    public void triggerModelChanged() {
        if (valueModel == null) {
            return;
        }
        final String uuid = getUUIDFromValueModel();
        if (isValidDisplaySelection()) {
            inlinePreviewImage.setVisible(true);
            image = imageFactory.createImageItem(uuid);
            if (remove != null) {
                remove.setVisible(true);
            }
        } else {
            inlinePreviewImage.setVisible(false);
            if (remove != null) {
                remove.setVisible(false);
            }
        }
        redraw();
    }

    /**
     * Get the UUID of the selected image from the valueModel object.
     *
     * @return UUID represented by a String value
     */
    private String getUUIDFromValueModel() {
        return valueModel.getObject();
    }

    /**
     * Check to see if the selected item is indeed a uuid of a imagesetNodeType
     *
     * @return true if the selected node is of either example imageset or std gallery set, false otherwise.
     */
    public boolean isValidDisplaySelection() {
        final String uuid = getUUIDFromValueModel();
        if (uuid == null) {
            return false;
        } else {
            try {
                final Node selectedNode = getJCRSession().getNodeByIdentifier(uuid);
                if (getNodeTypeName(selectedNode).equals(HIPPO_GALLERY_EXAMPLE_IMAGESET_NODETYPE_NAME) ||
                        getNodeTypeName(selectedNode).equals(HIPPO_GALLERY_STD_GALLERYSET_NODETYPE_NAME) ||
                        selectedNode.getPath().startsWith(GALLERY_ROOT_PATH) ||
                        arrayContainsStartWith(supportedPaths, selectedNode.getPath())) {
                    return true;
                }
            } catch (final RepositoryException e) {
                log.debug("Something went wrong while trying to get the selected node by UUID: {}", e.getMessage());
                return false;
            }
        }
        return false;
    }

    private Session getJCRSession() throws RepositoryException {
        return currentNodeModel.getNode().getSession();
    }

    /**
     * Get the current node name based on the primaryNodeType;
     *
     * @param node the JCR Node for which to lookup the node type name
     * @return the String representation of the current primary node type name
     * @throws javax.jcr.RepositoryException if something goes wrong while trying to get the node type name
     */
    private String getNodeTypeName(final Node node) throws RepositoryException {
        return node.getPrimaryNodeType().getName();
    }

    /**
     * This function is similar to list .contains() function, but instead of a exact match it's looking if the string
     * specified is a start in any of the array items
     *
     * @param array
     * @param path
     * @return boolean; is there a record that starts with the given string
     */
    public static boolean arrayContainsStartWith(final String[] array, final String path) {
        if (array != null) {
            for (final String s : array) {
                if (path.startsWith(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getMirrorPath() {
        final Node node = GalleryPickerPlugin.this.getModelObject();
        try {
            if (node != null && node.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                return getPath(node.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());
            }
        } catch (final ValueFormatException e) {
            log.warn("Invalid value format for docbase {}", e.getMessage());
            log.debug("Invalid value format for docbase ", e);
        } catch (final PathNotFoundException e) {
            log.warn("Docbase not found {}", e.getMessage());
            log.debug("Docbase not found ", e);
        } catch (final ItemNotFoundException e) {
            log.info("Docbase {} could not be dereferenced", e.getMessage());
        } catch (final RepositoryException e) {
            log.error("Invalid docbase {}", e.getMessage(), e);
        }
        return StringUtils.EMPTY;
    }

    private String getPath(final String docbaseUUID) {
        String path = StringUtils.EMPTY;
        try {
            if (!(docbaseUUID == null || docbaseUUID.equals("") || docbaseUUID.equals(JcrConstants.ROOT_NODE_ID))) {
                path = getJCRSession().getNodeByIdentifier(docbaseUUID).getPath();
            }
        } catch (final RepositoryException e) {
            log.error("Invalid docbase {}", e.getMessage(), e);
        }
        return path;
    }

    IModel<String> getPathModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                return getMirrorPath();
            }
        };
    }

    @Override
    protected void onDetach() {
        if (valueModel != null) {
            valueModel.detach();
        }
        super.onDetach();
    }

}
