/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.resource.loader.ComponentStringResourceLoader;
import org.hippoecm.frontend.attributes.ClassAttribute;
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

    private static final Logger log = LoggerFactory.getLogger(ImageDisplayPlugin.class);

    public static final String MIME_TYPE_HIPPO_BLANK = "application/vnd.hippo.blank";

    ByteSizeFormatter formatter = new ByteSizeFormatter();

    public ImageDisplayPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final IEditor.Mode mode = IEditor.Mode.fromString(config.getString("mode"), IEditor.Mode.VIEW);
        if (mode == IEditor.Mode.COMPARE && config.containsKey("model.compareTo")) {
            final IModelReference<Node> baseModelRef = context.getService(config.getString("model.compareTo"),
                    IModelReference.class);
            boolean doCompare = false;
            if (baseModelRef != null) {
                final IModel<Node> baseModel = baseModelRef.getModel();
                final Node baseNode = baseModel.getObject();
                final Node currentNode = getModel().getObject();
                if (baseNode != null && currentNode != null) {
                    try {
                        final InputStream baseStream = baseNode.getProperty(JcrConstants.JCR_DATA).getStream();
                        final InputStream currentStream = currentNode.getProperty(JcrConstants.JCR_DATA).getStream();
                        final StreamComparer comparer = new StreamComparer();
                        if (!comparer.areEqual(baseStream, currentStream)) {
                            doCompare = true;
                        }
                    } catch (final RepositoryException e) {
                        log.error("Could not compare streams", e);
                    }
                }
            }
            if (doCompare) {
                final Fragment fragment = new Fragment("fragment", "compare", this);
                final Fragment baseFragment = createResourceFragment("base", baseModelRef.getModel());
                baseFragment.add(ClassAttribute.append("hippo-diff-removed"));
                fragment.add(baseFragment);

                final Fragment currentFragment = createResourceFragment("current", getModel());
                currentFragment.add(ClassAttribute.append("hippo-diff-added"));
                fragment.add(currentFragment);
                add(fragment);
            } else {
                add(createResourceFragment("fragment", getModel()));
            }
        } else {
            add(createResourceFragment("fragment", getModel()));
        }
    }

    private Fragment createResourceFragment(final String id, final IModel<Node> model) {
        Fragment fragment = new Fragment(id, "unknown", this);
        try (final JcrResourceStream stream = new JcrResourceStream(model)) {
            if (stream.length().bytes() < 0) {
                return fragment;
            }

            fragment = new Fragment(id, "embed", this);
            fragment.add(new Label("filesize", Model.of(formatter.format(stream.length().bytes()))));
            fragment.add(new Label("mimetype", Model.of(stream.getContentType())));
            fragment.add(createFileLink(stream, stream.getChainedModel().getObject()));

            if (stream.getContentType().equals(MIME_TYPE_HIPPO_BLANK)) {
                fragment.setVisible(false);
            }
        } catch (final IOException | RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return fragment;
    }

    private FileLink createFileLink(final JcrResourceStream stream, final Node node) throws RepositoryException {
        final String filename = lookupFilename(node);
        final String linkLabel = lookupLinkLabel(filename);
        final FileResource fileResource = new FileResource(stream, filename);
        final FileLink filelink = new FileLink("link", fileResource, stream);
        filelink.add(new Label("filename", new Model<>(linkLabel)));
        return filelink;
    }

    private String lookupLinkLabel(final String filename) {
        final String linkLabel;

        if (HippoNodeType.NT_RESOURCE.equals(filename)) {
            final ComponentStringResourceLoader componentStringResourceLoader = new ComponentStringResourceLoader();
            linkLabel = componentStringResourceLoader.loadStringResource(this, "download.link", getLocale(), null,
                    null);
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

        public FileResource(final JcrResourceStream stream, final String filename) {
            super(stream);
            setFileName(filename);
            setContentDisposition(ContentDisposition.ATTACHMENT);
        }
    }

    private static class FileLink extends ResourceLink<Void> {

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
