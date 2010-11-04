/*
 *  Copyright 2010 Hippo.
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;

/**
 * Displays a file name of an image set without editing options.
 */
public class FileNameDisplayPlugin extends RenderPlugin<Node> {

    private static final long serialVersionUID = 1L;

    public FileNameDisplayPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(createFileNameFragment("fragment", getModel()));
    }

    private Fragment createFileNameFragment(String id, IModel<Node> model) {
        String fileName = "";
        try {
            fileName = model.getObject().getProperty(HippoGalleryNodeType.IMAGE_SET_FILE_NAME).getString();
        } catch (RepositoryException ignored) {
            // ignore
        }

        Fragment fragment = new Fragment(id, "filename", this);
        fragment.add(new Label("name", new Model<String>(fileName)));
        return fragment;
    }

    @Override
    protected void onModelChanged() {
        replace(createFileNameFragment("fragment", getModel()));
        super.onModelChanged();
        redraw();
    }

}
