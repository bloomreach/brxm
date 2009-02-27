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

package org.hippoecm.hst.plugins.frontend;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.widgets.TextFieldWidget;

public class SitemapPlugin extends Perspective {
    private static final long serialVersionUID = 1L;

    private TextFieldWidget tfw;

    public SitemapPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        ModelReference ref = new ModelReference(config.getString("wicket.model"), null);
        ref.init(context);

        setModel(ref.getModel());

        add(tfw = new TextFieldWidget("name", new ApartModel()));

    }

    @Override
    protected void onModelChanged() {
        tfw.setModel(getModel());
        redraw();
    }

    class ApartModel implements IModel {
        private static final long serialVersionUID = 1L;

        public Object getObject() {
            try {
                return ((JcrNodeModel) SitemapPlugin.this.getModel()).getNode().getName();
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
            return null;
        }

        public void setObject(Object object) {
            JcrNodeModel nodeModel = (JcrNodeModel) SitemapPlugin.this.getModel();
            try {
                Session session = nodeModel.getNode().getSession();
                session.move(nodeModel.getItemModel().getPath(), nodeModel.getParentModel().getItemModel().getPath()
                        + "/" + (String) object);
            } catch (RepositoryException e) {
                e.printStackTrace();
                info("Nerd!");
            }
        }

        public void detach() {
        }

    }

}
