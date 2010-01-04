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
package org.hippoecm.frontend.editor.plugins.resource;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.util.ByteSizeFormatter;
import org.hippoecm.frontend.resource.JcrResource;
import org.hippoecm.frontend.resource.JcrResourceStream;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageDisplayPlugin extends RenderPlugin<Node> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ImageDisplayPlugin.class);

    private JcrResourceStream resource;
    ByteSizeFormatter formatter = new ByteSizeFormatter();

    public ImageDisplayPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        addResourceFragment();
    }
    
    private void addResourceFragment() {
        resource = new JcrResourceStream(getModel());
        Fragment fragment = new Fragment("fragment", "unknown", this);
        try {
            Node node = getModelObject();
            String mimeType = node.getProperty("jcr:mimeType").getString();
            if (mimeType.indexOf('/') > 0) {
                String category = mimeType.substring(0, mimeType.indexOf('/'));
                if ("image".equals(category)) {
                    fragment = new Fragment("fragment", "image", this);
                    fragment.add(new NonCachingImage("image", new JcrResource(resource)));
                } else {
                    fragment = new Fragment("fragment", "embed", this);
                    fragment.add(new Label("filesize", new Model<String>(formatter.format(resource.length()))));
                    fragment.add(new Label("mimetype", new Model<String>(resource.getContentType())));
                    fragment.add(new ResourceLink("link", new JcrResource(resource)));
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        addOrReplace(fragment);
    }

    @Override
    protected void onModelChanged() {
        addResourceFragment();
        super.onModelChanged();
    }

    @Override
    public void onDetach() {
        if (resource != null) {
            resource.detach();
        }
        super.onDetach();
    }

}
