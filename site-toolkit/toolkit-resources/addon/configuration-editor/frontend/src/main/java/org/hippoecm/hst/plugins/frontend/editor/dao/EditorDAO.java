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

package org.hippoecm.hst.plugins.frontend.editor.dao;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.hst.plugins.frontend.editor.context.HstContext;
import org.hippoecm.hst.plugins.frontend.editor.domain.IEditorBean;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EditorDAO<K extends IEditorBean> implements IClusterable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(EditorDAO.class);

    String prototypePath;
    private IPluginContext context;

    public EditorDAO(IPluginContext context, String namespace) {
        this.context = context;
        this.prototypePath = namespace + "/hippo:prototype/hippo:prototype";
    }

    public K create(JcrNodeModel model) {
        Node node = model.getNode();
        try {
            String name = "new";
            String suffix = "";
            int count = 1;
            while (node.hasNode(name + suffix)) {
                suffix = String.valueOf(count++);
            }
            String newPath = model.getItemModel().getPath() + "/" + name + suffix;
            Node prototype = model.getNode().getSession().getRootNode().getNode(prototypePath);
            node = ((HippoSession) model.getNode().getSession()).copy(prototype, newPath);
            return load(new JcrNodeModel(node));
        } catch (RepositoryException e) {
            log.error("Error creating new node", e);
        }
        return null;
    }

    public abstract K load(JcrNodeModel model);

    public boolean save(K k) {
        if (k.getModel() != null) {
            persist(k, k.getModel());
            try {
                Session session = k.getModel().getNode().getSession();
                if (session.hasPendingChanges()) {
                    session.save();
                    return true;
                }
            } catch (RepositoryException e) {
                log.error("An error occured during save of node[" + k.getModel().getItemModel().getPath() + "]", e);
            }
        }
        return false;
    }

    abstract protected void persist(K k, JcrNodeModel model);

    public boolean delete(K k) {
        JcrNodeModel model = k.getModel();
        if (model != null && getHstContext().isDeleteAllowed(model)) {
            Node node = model.getNode();
            try {
                node.remove();
                Session session = k.getModel().getNode().getSession();
                if (session.hasPendingChanges()) {
                    session.save();
                }
                return true;
            } catch (RepositoryException e) {
                log.error("Failed to remove new node", e);
            }
        }
        return false;
    }

    public HstContext getHstContext() {
        return context.getService(HstContext.class.getName(), HstContext.class);
    }

}
