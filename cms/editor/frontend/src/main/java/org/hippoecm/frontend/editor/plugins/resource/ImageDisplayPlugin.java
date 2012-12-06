/*
 *  Copyright 2008-2011 Hippo.
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

import org.apache.wicket.Response;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebResponse;
import org.hippoecm.frontend.editor.compare.StreamComparer;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.util.ByteSizeFormatter;
import org.hippoecm.frontend.resource.JcrResource;
import org.hippoecm.frontend.resource.JcrResourceStream;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageDisplayPlugin extends RenderPlugin<Node> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ImageDisplayPlugin.class);

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
        final JcrResourceStream resource = new JcrResourceStream(model);
        Fragment fragment = new Fragment(id, "unknown", this);
        try {
            if (resource.length() <= 0) {
                return fragment;
            }

            Node node = getModelObject();
            final String filename;
            if (node.getDefinition().getName().equals("*")) {
                filename = node.getName();
            } else {
                filename = node.getParent().getName();
            }
            fragment = new Fragment(id, "embed", this);
            fragment.add(new Label("filesize", new Model<String>(formatter.format(resource.length()))));
            fragment.add(new Label("mimetype", new Model<String>(resource.getContentType())));
            fragment.add(new ResourceLink<Void>("link", new JcrResource(resource) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void configureResponse(Response response) {
                    if (response instanceof WebResponse) {
                        ((WebResponse) response).setHeader("Content-Disposition", "attachment; filename="
                                + filename);
                    }
                }
            }) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onDetach() {
                    resource.detach();
                    super.onDetach();
                }

            });
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return fragment;
    }

    @Override
    protected void onModelChanged() {
        replace(createResourceFragment("fragment", getModel()));
        super.onModelChanged();
        redraw();
    }

}
