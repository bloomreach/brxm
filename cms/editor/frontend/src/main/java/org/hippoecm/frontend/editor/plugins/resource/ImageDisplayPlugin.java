/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.resource;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.resource.loader.ComponentStringResourceLoader;
import org.hippoecm.frontend.editor.compare.StreamComparer;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.util.ByteSizeFormatter;
import org.hippoecm.frontend.resource.JcrResource;
import org.hippoecm.frontend.resource.JcrResourceStream;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageDisplayPlugin extends RenderPlugin<Node> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ImageDisplayPlugin.class);

    public static final String MIME_TYPE_HIPPO_UNINITIALIZED = "application/vnd.hippo.uninitialized";

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
                Fragment baseFragment = createResourceFragment("base", baseModelRef.getModel());
                baseFragment.add(new AttributeAppender("class", new Model<String>("hippo-diff-removed"), " "));
                fragment.add(baseFragment);

                Fragment currentFragment = createResourceFragment("current", getModel());
                currentFragment.add(new AttributeAppender("class", new Model<String>("hippo-diff-added"), " "));
                fragment.add(currentFragment);
                add(fragment);
            } else {
                add(createResourceFragment("fragment", getModel()));
            }
        } else {
            add(createResourceFragment("fragment", getModel()));
        }
    }

    private Fragment createResourceFragment(String id, IModel<Node> model) {
        final JcrResourceStream stream = new JcrResourceStream(model);
        Fragment fragment = new Fragment(id, "unknown", this);
        try {
            if (stream.length().bytes() < 0) {
                return fragment;
            }

            fragment = new Fragment(id, "embed", this);
            fragment.add(new Label("filesize", Model.of(formatter.format(stream.length().bytes()))));
            fragment.add(new Label("mimetype", Model.of(stream.getContentType())));
            fragment.add(createFileLink(stream, getModelObject()));

            if (stream.getContentType().equals(MIME_TYPE_HIPPO_UNINITIALIZED)) {
                fragment.setVisible(false);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return fragment;
    }

    private FileLink createFileLink(final JcrResourceStream stream, final Node node) throws RepositoryException {
        String filename = lookupFilename(node);
        String linkLabel = lookupLinkLabel(filename);
        FileResource fileResource = new FileResource(stream, filename);
        FileLink filelink = new FileLink("link", fileResource, stream);
        filelink.add(new Label("filename", new Model<String>(linkLabel)));
        return filelink;
    }

    private String lookupLinkLabel(final String filename) {
        String linkLabel;

        if(HippoNodeType.NT_RESOURCE.equals(filename)) {
            ComponentStringResourceLoader componentStringResourceLoader = new ComponentStringResourceLoader();
            linkLabel = componentStringResourceLoader.loadStringResource(this, "download.link", getLocale(), null, null);
        } else {
            linkLabel = filename;
        }
        return linkLabel;
    }

    private String lookupFilename(final Node node) throws RepositoryException {
        String filename = JcrUtils.getStringProperty(node, HippoNodeType.HIPPO_FILENAME, null);

        if (StringUtils.isEmpty(filename)) {
            if (node.getDefinition().getName().equals("*")) {
                filename = node.getName();
            } else {
                filename = node.getParent().getName();
            }
        }
        return filename;
    }

    @Override
    protected void onModelChanged() {
        replace(createResourceFragment("fragment", getModel()));
        super.onModelChanged();
        redraw();
    }


    private static class FileResource extends JcrResource {

        private static final long serialVersionUID = 1L;

        public FileResource(final JcrResourceStream stream, String filename) {
            super(stream);
            setFileName(filename);
            setContentDisposition(ContentDisposition.ATTACHMENT);
        }

    }


    private static class FileLink extends ResourceLink {

        private static final long serialVersionUID = 1L;

        private final JcrResourceStream stream;

        public FileLink(final String id, final IResource resource, final JcrResourceStream stream) {
            super(id, resource);
            this.stream = stream;
        }

        @Override
        protected void onDetach() {
            stream.detach();
            super.onDetach();
        }
    }

}
