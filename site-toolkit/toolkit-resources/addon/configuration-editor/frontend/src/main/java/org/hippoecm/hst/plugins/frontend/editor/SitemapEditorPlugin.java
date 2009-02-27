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

package org.hippoecm.hst.plugins.frontend.editor;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class SitemapEditorPlugin extends BasicEditorPlugin {
    private static final long serialVersionUID = 1L;

    public SitemapEditorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        FormComponent fc;
        fc = new RequiredTextField("nodename", new ApartModel());
        //fc.add(StringValidator.minimumLength(2));
        //fc.add(new UsernameValidator());

        form.add(fc);
    }

    @Override
    protected void doSave() {
        JcrNodeModel nodeModel = (JcrNodeModel) getModel();
        Session session;
        try {
            session = nodeModel.getNode().getSession();
            session.save();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        //updateModel(nodeModel);
    }

    class ApartModel implements IModel {
        private static final long serialVersionUID = 1L;

        public Object getObject() {
            try {
                return ((JcrNodeModel) SitemapEditorPlugin.this.getModel()).getNode().getName();
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
            return null;
        }

        public void setObject(Object object) {
            JcrNodeModel nodeModel = (JcrNodeModel) SitemapEditorPlugin.this.getModel();
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
