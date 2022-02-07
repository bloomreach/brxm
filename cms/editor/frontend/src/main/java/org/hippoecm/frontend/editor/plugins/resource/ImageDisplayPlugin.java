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
import java.util.UUID;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.resource.loader.ComponentStringResourceLoader;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.editor.compare.StreamComparer;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.util.ByteSizeFormatter;
import org.hippoecm.frontend.resource.JcrResourceStream;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.download.DownloadLink;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.frontend.service.IEditor.Mode;

public class ImageDisplayPlugin extends RenderPlugin<Node> {

    private static final Logger log = LoggerFactory.getLogger(ImageDisplayPlugin.class);

    public static final String MIME_TYPE_HIPPO_BLANK = "application/vnd.hippo.blank";

    ByteSizeFormatter formatter = new ByteSizeFormatter();

    public ImageDisplayPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final IModel<Node> compareModel = getCompareModel();
        if (compareModel != null) {
            final Fragment fragment = new Fragment("fragment", "compare", this);
            final Fragment baseFragment = createResourceFragment("base", compareModel);
            baseFragment.add(ClassAttribute.append("hippo-diff-removed"));
            fragment.add(baseFragment);

            final Fragment currentFragment = createResourceFragment("current", getModel());
            currentFragment.add(ClassAttribute.append("hippo-diff-added"));
            fragment.add(currentFragment);
            add(fragment);
        } else {
            add(createResourceFragment("fragment", getModel()));
        }
    }

    private IModel<Node> getCompareModel() {
        final IPluginConfig config = getPluginConfig();
        final IPluginContext context = getPluginContext();
        final Mode mode = Mode.fromString(config.getString("mode"), Mode.VIEW);

        if (mode != Mode.COMPARE || !config.containsKey("model.compareTo")) {
            return null;
        }

        //noinspection unchecked
        final IModelReference<Node> baseModelRef = context.getService(config.getString("model.compareTo"),
                IModelReference.class);
        if (baseModelRef == null) {
            return null;
        }

        final IModel<Node> baseModel = baseModelRef.getModel();
        final Node baseNode = baseModel.getObject();
        final Node currentNode = getModel().getObject();
        if (baseNode == null || currentNode == null) {
            return null;
        }

        try {
            final Binary baseBinary = baseNode.getProperty(JcrConstants.JCR_DATA).getBinary();
            final Binary currentBinary = currentNode.getProperty(JcrConstants.JCR_DATA).getBinary();
            final StreamComparer comparer = new StreamComparer();
            if (!comparer.areEqual(baseBinary.getStream(), currentBinary.getStream())) {
                return baseModel;
            }
        } catch (final RepositoryException e) {
            log.error("Could not compare streams", e);
        }

        return null;
    }

    private Fragment createResourceFragment(final String id, final IModel<Node> model) {
        try (final JcrResourceStream stream = new JcrResourceStream(model)) {
            if (stream.length().bytes() >= 0) {
                return createResourceFragment(id, stream);
            }
        } catch (final IOException | RepositoryException ex) {
            log.error(ex.getMessage());
        }

        return new Fragment(id, "unknown", this);
    }

    private Fragment createResourceFragment(final String id, final JcrResourceStream stream) throws RepositoryException {
        final Fragment fragment = new Fragment(id, "embed", this);

        fragment.add(new Label("filesize", Model.of(formatter.format(stream.length().bytes()))));
        fragment.add(new Label("mimetype", Model.of(stream.getContentType())));
        fragment.add(createFileLink(stream, stream.getChainedModel().getObject()));
        fragment.setVisible(!stream.getContentType().equals(MIME_TYPE_HIPPO_BLANK));

        return fragment;
    }

    private Link<Void> createFileLink(final JcrResourceStream stream, final Node node) throws RepositoryException {
        final String filename = lookupFilename(node);
        final FileLink fileLink = new FileLink("link", filename, stream);

        final String linkLabel = lookupLinkLabel(filename);
        fileLink.add(new Label("filename", Model.of(linkLabel)));

        return fileLink;
    }

    private String lookupLinkLabel(final String filename) {
        if (!HippoNodeType.NT_RESOURCE.equals(filename)) {
            return filename;
        }

        final ComponentStringResourceLoader componentStringResourceLoader = new ComponentStringResourceLoader();
        return componentStringResourceLoader.loadStringResource(this, "download.link", getLocale(), null, null);
    }

    private static String lookupFilename(final Node node) throws RepositoryException {
        final String filename = JcrUtils.getStringProperty(node, HippoNodeType.HIPPO_FILENAME, null);

        if (StringUtils.isNotEmpty(filename)) {
            return filename;
        }

        if (node.getDefinition().getName().equals("*")) {
            return node.getName();
        }

        return node.getParent().getName();
    }

    @Override
    protected void onModelChanged() {
        replace(createResourceFragment("fragment", getModel()));
        super.onModelChanged();
        redraw();
    }

    private static class FileLink extends DownloadLink<Void> {
        private final String filename;
        private final JcrResourceStream stream;

        public FileLink(final String id, final String filename, final JcrResourceStream stream) {
            super(id);
            this.filename = filename;
            this.stream = stream;
        }

        @Override
        protected CharSequence getURL() {
            // Append a cache-bust request parameter to prevent the client from caching the resource in case the headers
            // set by parent class DownloadLink are ignored or overridden.
            final PageParameters parameters = new PageParameters();
            parameters.set("v", UUID.randomUUID());
            return urlForListener(parameters);
        }

        @Override
        protected String getFilename() {
            return filename;
        }

        @Override
        protected InputStream getContent() {
            try {
                return stream.getInputStream();
            } catch (ResourceStreamNotFoundException e) {
                log.error("Resource not found", e);
            }
            return null;
        }

        @Override
        protected void onDetach() {
            stream.detach();
            super.onDetach();
        }
    }
}
