/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

public abstract class DragBehavior extends AbstractDragDropBehavior {

    private static final long serialVersionUID = 1L;

    protected DragSettings dragSettings;

    public DragBehavior(DragSettings settings) {
        super(settings);
        dragSettings = settings;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void respond(final AjaxRequestTarget target) {
        if (!lookupDropBehavior()) {
            return;
        }
        final IModel draggedModel = getDragModel();
        if (draggedModel == null) {
            return;
        }

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

    /**
     * Lookup and return the IModel that will be used to drop on the dropBehavior
     * @return
     */
    abstract protected IModel getDragModel();

    protected boolean lookupDropBehavior() {
        return true;
    }

    @Override
    protected String getHeaderContributorFilename() {
        return "Drag.js";
    }

    @Override
    protected String getClientSideClassname() {
        return "YAHOO.hippo.DDFallbackModel";
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
}
