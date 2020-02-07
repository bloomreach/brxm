/*
 *  Copyright 2011-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery.editor;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.ContentDisposition;

import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.editor.compare.StreamComparer;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.image.JcrImage;
import org.hippoecm.frontend.plugins.standards.util.ByteSizeFormatter;
import org.hippoecm.frontend.resource.JcrResource;
import org.hippoecm.frontend.resource.JcrResourceStream;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageDisplayPlugin extends RenderPlugin<Node> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ImageDisplayPlugin.class);

    public static final String CONFIG_DISPLAY_DIMENSIONS_ONLY = "display.dimensions.only";
    public static final String CONFIG_DISPLAY_MAX_WIDTH = "display.max.width";
    public static final String CONFIG_DISPLAY_MAX_HEIGHT = "display.max.height";

    public static final int DEFAULT_DISPLAY_MAX_WIDTH = 800;
    public static final int DEFAULT_DISPLAY_MAX_HEIGHT = 800;

    ByteSizeFormatter formatter = new ByteSizeFormatter();

    public ImageDisplayPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        IEditor.Mode mode = IEditor.Mode.fromString(config.getString("mode"), IEditor.Mode.VIEW);
        if (mode == IEditor.Mode.COMPARE && config.containsKey("model.compareTo")) {
            IModelReference<Node> baseModelRef = context.getService(config.getString("model.compareTo"),
                    IModelReference.class);
            boolean doCompare = false;
            if (baseModelRef != null) {
                IModel<Node> baseModel = baseModelRef.getModel();
                Node baseNode = baseModel.getObject();
                Node currentNode = getModel().getObject();
                if (baseNode != null && currentNode != null) {
                    try {
                        InputStream baseStream = baseNode.getProperty("jcr:data").getStream();
                        InputStream currentStream = currentNode.getProperty("jcr:data").getStream();
                        StreamComparer comparer = new StreamComparer();
                        if (!comparer.areEqual(baseStream, currentStream)) {
                            doCompare = true;
                        }
                    } catch (RepositoryException e) {
                        log.error("Could not compare streams", e);
                    }
                }
            }
            if (doCompare) {
                Fragment fragment = new Fragment("fragment", "compare", this);
                Fragment baseFragment = createResourceFragment("base", baseModelRef.getModel(), config);
                baseFragment.add(ClassAttribute.append("hippo-diff-removed"));
                fragment.add(baseFragment);

                Fragment currentFragment = createResourceFragment("current", getModel(), config);
                currentFragment.add(ClassAttribute.append("hippo-diff-added"));
                fragment.add(currentFragment);
                add(fragment);
            } else {
                add(createResourceFragment("fragment", getModel(), config));
            }
        } else {
            add(createResourceFragment("fragment", getModel(), config));
        }
    }

    private Fragment createResourceFragment(String id, IModel<Node> model, IPluginConfig config) {
        Fragment fragment = new Fragment(id, "unknown", this);
        try(JcrResourceStream resource = new JcrResourceStream(model)) {
            Node node = getModelObject();
            final String filename;
            if (node.getParent().hasProperty(HippoGalleryNodeType.IMAGE_SET_FILE_NAME)) {
                filename = node.getParent().getProperty(HippoGalleryNodeType.IMAGE_SET_FILE_NAME).getString();
            } else if (node.getDefinition().getName().equals("*")) {
                filename = node.getName();
            } else {
                filename = node.getParent().getName();
            }
            String mimeType = node.getProperty("jcr:mimeType").getString();
            if (mimeType.indexOf('/') > 0) {
                String category = mimeType.substring(0, mimeType.indexOf('/'));
                if ("image".equals(category) && shouldDisplayImage(resource, node, config)) {
                    fragment = createImageFragment(id, resource, filename, node, config);
                } else {
                    fragment = createEmbedFragment(id, resource, filename, node);
                }
            }
        } catch (IOException | RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return fragment;
    }

    protected Fragment createImageFragment(final String id, final JcrResourceStream resource, final String filename,
                                           final Node node, final IPluginConfig config) throws RepositoryException {

        final Fragment fragment = new Fragment(id, "image", this);

        int width = getWidthOrZero(node);
        int height = getHeightOrZero(node);

        fragment.add(new JcrImage("image", resource, width, height));
        addImageMetaData(node, fragment);

        final boolean showExtraMeta  = !config.getBoolean(CONFIG_DISPLAY_DIMENSIONS_ONLY);
        addExtraMetaData(resource, filename, fragment, showExtraMeta);

        return fragment;
    }

    private Fragment createEmbedFragment(final String id, final JcrResourceStream resource, final String filename,final Node node) throws RepositoryException {

        final Fragment fragment = new Fragment(id, "embed", this);
        addImageMetaData(node, fragment);
        addExtraMetaData(resource, filename, fragment, true);

        return fragment;
    }

    protected boolean shouldDisplayImage(final JcrResourceStream resource, Node node, IPluginConfig config)
            throws RepositoryException {

        int width = getWidthOrZero(node);
        int height = getHeightOrZero(node);

        if (width <= 0 || height <= 0) {
            return false;
        }

        final long maxWidth = config.getAsLong(CONFIG_DISPLAY_MAX_WIDTH, DEFAULT_DISPLAY_MAX_WIDTH);
        final long maxHeight = config.getAsLong(CONFIG_DISPLAY_MAX_HEIGHT, DEFAULT_DISPLAY_MAX_HEIGHT);

        return width <= maxWidth && height <= maxHeight;
    }

    protected void addImageMetaData(Node node, Fragment fragment) throws RepositoryException {
        int width = getWidthOrZero(node);
        int height = getHeightOrZero(node);
        fragment.add(new Label("width", Model.of(width)));
        fragment.add(new Label("height", Model.of(height)));
    }

    private int getWidthOrZero(Node imageNode) throws RepositoryException {
        try {
            return (int)imageNode.getProperty(HippoGalleryNodeType.IMAGE_WIDTH).getLong();
        } catch (PathNotFoundException noWidthFound) {
            return 0;
        }
    }

    private int getHeightOrZero(Node imageNode) throws RepositoryException {
        try {
            return (int)imageNode.getProperty(HippoGalleryNodeType.IMAGE_HEIGHT).getLong();
        } catch (PathNotFoundException noHeightFound) {
            return 0;
        }
    }

    private void addExtraMetaData(final JcrResourceStream resource, final String filename, final Fragment fragment, final boolean showExtraMeta) {
        fragment.add(new Label("filesize", Model.of(formatter.format(resource.length().bytes()))));
        fragment.add(new Label("mimetype", Model.of(resource.getContentType())));

        final ResourceLink<Void> link = new ResourceLink<Void>("link", new JcrResource(resource) {
            @Override
            protected ResourceResponse newResourceResponse(final Attributes attributes) {
                ResourceResponse response = super.newResourceResponse(attributes);
                response.setContentDisposition(ContentDisposition.ATTACHMENT);
                response.setFileName(filename);
                return response;
            }

        }) {
            @Override
            protected void onDetach() {
                resource.detach();
                super.onDetach();
            }

        };

        // a <wicket:enclosure> element is used to hide also filesize and mimetype
        link.setVisible(showExtraMeta);

        fragment.add(link);
    }

    @Override
    protected void onModelChanged() {
        replace(createResourceFragment("fragment", getModel(), getPluginConfig()));
        super.onModelChanged();
        redraw();
    }

}
