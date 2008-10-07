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
package org.hippoecm.frontend.plugins.yui.dragdrop;

import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.util.HeaderContributorHelper.JsConfig;
import org.hippoecm.frontend.service.render.RenderService;

public class DragBehavior extends AbstractDragDropBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public DragBehavior(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void respond(final AjaxRequestTarget target) {
        if (!lookupDropBehavior()) {
            return;
        }
        final IModel draggedModel = getDragModel();
        if (draggedModel == null)
            return;
        
        final String targetId = getComponent().getRequest().getParameter("targetId");
        final Map<String, String[]> parameters = getComponent().getRequest().getParameterMap();
        
        getComponent().getPage().visitChildren(new DropPointVisitor() {
            @Override
            void visit(DropBehavior dropPoint) {
                if (dropPoint.getComponentMarkupId().equals(targetId)) {
                    dropPoint.onDrop(draggedModel, parameters, target);
                }
            }
        });
    }

    protected boolean lookupDropBehavior() {
        return true;
    }

    protected IModel getDragModel() {
        String pluginModelId = config.getString(RenderService.MODEL_ID);
        if (pluginModelId != null) {
            //TODO: generic gedrag uitzoeken
            IModelService pluginModelService = context.getService(pluginModelId, IModelService.class);
            if (pluginModelService != null) {
                return pluginModelService.getModel();
            }
        }
        return null;
    }

    @Override
    protected String getHeaderContributorFilename() {
        return "Drag.js";
    }

    @Override
    protected Class<? extends IBehavior> getHeaderContributorClass() {
        return DragBehavior.class;
    }

    @Override
    protected JsConfig getJavacriptConfig() {
        JsConfig config = super.getJavacriptConfig();
        config.put("centerFrame", true);
        config.put("resizeFrame", false);
        return config;
    }

    private abstract class DropPointVisitor implements IVisitor {
        @SuppressWarnings("unchecked")
        public Object component(Component component) {
            List<IBehavior> behaviors = component.getBehaviors();
            for (int a = 0; a < behaviors.size(); a++) {
                IBehavior behavior = behaviors.get(a);
                if (behavior instanceof DropBehavior) {
                    DropBehavior dropPoint = (DropBehavior) behavior;
                    visit(dropPoint);
                    return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
                }
            }
            return IVisitor.CONTINUE_TRAVERSAL;
        }

        abstract void visit(DropBehavior draggable);
    }

    @Override
    protected String getModelClass() {
        return "YAHOO.hippo.DDModel";
    }

}
