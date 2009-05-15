/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.addon.workflow;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;

import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderService;

public final class WorkflowPlugin extends AbstractWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private final IModelReference modelReference;

    public WorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config.getString(RenderService.MODEL_ID) != null) {
            modelReference = context.getService(config.getString(RenderService.MODEL_ID),
                    IModelReference.class);
            if (modelReference != null) {
                //updateModel(modelReference.getModel());
                context.registerService(new IObserver() {

                    private static final long serialVersionUID = 1L;

                    public IObservable getObservable() {
                        return modelReference;
                    }

                    public void onEvent(Iterator<? extends IEvent> event) {
                        updateModel(modelReference.getModel());
                    }
                }, IObserver.class.getName());
            }
        } else {
            modelReference = null;
            log.warn("No model configured");
        }

        onModelChanged();
    }

    // FIXME, oldModel is necessary for workaround in onModelChanged
    private IModel oldModel = null;

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        Set<Node> nodeSet = new LinkedHashSet<Node>();
        if (getModel() instanceof JcrNodeModel) {
            Node node = ((JcrNodeModel)getModel()).getNode();
            if (node != null) {
                try {
                    // FIXME workaround when editing a document a save on the nodes takes place; this fix makes it impossible
                    // for usages of this workflow container class to update the state of the document.  This could
                    // occur for instance when publishing a document from within an edit screen, but would be applicable
                    // to other usages as well (viewing a request, for instance).  However these cases do not exist at this time.
                    if(oldModel != null && oldModel instanceof JcrNodeModel && node.isSame(((JcrNodeModel)oldModel).getNode())) {
                        return;
                    }
                    nodeSet.add(node);
                    oldModel = getModel();
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage(), ex);
                    oldModel = null;
                }
            }
        }
        MenuHierarchy menu = buildMenu(nodeSet);
        menu.restructure();
        addOrReplace(new MenuBar("menu", menu));

        redraw();
    }
}
