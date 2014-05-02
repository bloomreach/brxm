/*
 *  Copyright 2011-2014 Hippo B.V. (http://www.onehippo.com)
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

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.ContentDisposition;
import org.hippoecm.frontend.editor.compare.StreamComparer;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.image.JcrImage;
import org.hippoecm.frontend.plugins.standards.util.ByteSizeFormatter;
import org.hippoecm.frontend.plugins.yui.upload.validation.ImageUploadValidationService;
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

    public static final int DEFAULT_DISPLAY_MAX_WIDTH = 800;
    public static final int DEFAULT_DISPLAY_MAX_HEIGHT = 800;

    ByteSizeFormatter formatter = new ByteSizeFormatter();

    public ImageDisplayPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        IEditor.Mode mode = IEditor.Mode.fromString(config.getString("mode", "view"));
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
                baseFragment.add(new AttributeAppender("class", new Model<String>("hippo-diff-removed"), " "));
                fragment.add(baseFragment);

                Fragment currentFragment = createResourceFragment("current", getModel(), config);
                currentFragment.add(new AttributeAppender("class", new Model<String>("hippo-diff-added"), " "));
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
        final JcrResourceStream resource = new JcrResourceStream(model);
        Fragment fragment = new Fragment(id, "unknown", this);
        try {
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
                    fragment = createImageFragment(id, resource, node, config);
                } else {
                    fragment = createEmbedFragment(id, resource, filename);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return fragment;
    }

    protected Fragment createImageFragment(String id, final JcrResourceStream resource, Node node, IPluginConfig config)
            throws RepositoryException {
        Fragment fragment = new Fragment(id, "image", this);

        int width = (int)getWidthOrZero(node);
        int height = (int)getHeightOrZero(node);

        fragment.add(new JcrImage("image", resource, width, height));
        addImageMetaData(node, fragment);

        return fragment;
    }

    protected boolean shouldDisplayImage(final JcrResourceStream resource, Node node, IPluginConfig config)
            throws RepositoryException {

        int width = getWidthOrZero(node);
        int height = getHeightOrZero(node);

        if (width <= 0 || height <= 0) {
            return false;
        }

        final long maxWidth = config.getAsLong("display.max.width", 800);
        final long maxHeight = config.getAsLong("display.max.height", 800);

        return width <= maxWidth && height <= maxHeight;
    }

    protected void addImageMetaData(Node node, Fragment fragment) throws RepositoryException {
        int width = getWidthOrZero(node);
        int height = getHeightOrZero(node);
        fragment.add(new Label("width", new Model<Integer>(width)));
        fragment.add(new Label("height", new Model<Integer>(height)));
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

    private Fragment createEmbedFragment(String id, final JcrResourceStream resource, final String filename) {
        Fragment fragment = new Fragment(id, "embed", this);
        fragment.add(new Label("filesize", new Model<String>(formatter.format(resource.length().bytes()))));
        fragment.add(new Label("mimetype", new Model<String>(resource.getContentType())));
        fragment.add(new ResourceLink<Void>("link", new JcrResource(resource) {
            private static final long serialVersionUID = 1L;

            @Override
            protected ResourceResponse newResourceResponse(final Attributes attributes) {
                ResourceResponse response = super.newResourceResponse(attributes);
                response.setContentDisposition(ContentDisposition.ATTACHMENT);
                response.setFileName(filename);
                return response;
            }

        }) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onDetach() {
                resource.detach();
                super.onDetach();
            }

        });
        return fragment;
    }

    @Override
    protected void onModelChanged() {
        replace(createResourceFragment("fragment", getModel(), getPluginConfig()));
        super.onModelChanged();
        redraw();
    }

}
