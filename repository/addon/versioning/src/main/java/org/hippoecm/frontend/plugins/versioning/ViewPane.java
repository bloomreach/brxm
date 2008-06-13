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
package org.hippoecm.frontend.plugins.versioning;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.HippoNodeType;

public class ViewPane extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    transient Logger log = LoggerFactory.getLogger(ViewPane.class);

    public ViewPane(IPluginContext context, IPluginConfig config) {
        super(context, config);
        addExtensionPoint("content");
        add(new Label("alternate", "No document selected"));
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        log = LoggerFactory.getLogger(ViewPane.class);
    }

    protected void onBeforeRender() {
        Component contentComponent = get("content");
        Component alternateComponent = get("alternate");
        if (contentComponent.isVisible() && alternateComponent.isVisible()) {
            contentComponent.setVisible(false);
            alternateComponent.setVisible(true);
        }
        super.onBeforeRender();
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        JcrNodeModel model = (JcrNodeModel)getModel();
        if (model != null) {
            try {
                Node modelNode = model.getNode();
                if (model.getNode().isNodeType(HippoNodeType.NT_HANDLE)) {
                    for (NodeIterator iter = modelNode.getNodes(); iter.hasNext();) {
                        Node child = iter.nextNode();
                        if (child.getName().equals(modelNode.getName())) {
                            modelNode = child;
                            break;
                        }
                    }
                }
                // FIXME: this has knowledge of hippostd, which is not fundamentally wrong, but could be cleaner
                if (modelNode.isNodeType(HippoNodeType.NT_DOCUMENT) && !modelNode.isNodeType("hippostd:folder")) {
                    get("content").setVisible(true);
                    get("alternate").setVisible(false);
                    redraw();
                    return;
                }
            } catch (RepositoryException ex) {
            }
        }
        get("content").setVisible(false);
        get("alternate").setVisible(true);
        redraw();
    }
}
