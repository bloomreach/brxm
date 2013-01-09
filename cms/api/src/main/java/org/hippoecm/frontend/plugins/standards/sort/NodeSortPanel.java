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
package org.hippoecm.frontend.plugins.standards.sort;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeSortPanel extends Panel {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(NodeSortPanel.class);

    private NodeSortAction sorter;

    private boolean dirty;

    public NodeSortPanel(String id) {
        super(id);

        setOutputMarkupId(true);
        sorter = new NodeSortAction();

        add(new AjaxLink("up") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if(sorter.moveUp()) {
                    target.addComponent(NodeSortPanel.this);
                    onNodeSorted(target);
                }
            }

            @Override
            public boolean isEnabled() {
                return sorter.canMoveUp();
            }
        });

        add(new AjaxLink("down") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if(sorter.moveDown()) {
                    target.addComponent(NodeSortPanel.this);
                    onNodeSorted(target);
                }
            }

            @Override
            public boolean isEnabled() {
                return sorter.canMoveDown();
            }
        });
    }

    @Override
    public void onModelChanged() {
        IModel model = getDefaultModel();
        if (model instanceof JcrNodeModel) {
            sorter.setModel((JcrNodeModel) model);
            dirty = true;
        }
    }

    @Override
    protected void onAfterRender() {
        dirty = false;
        super.onAfterRender();
    }

    public boolean isDirty() {
        return dirty;
    }

    protected void onNodeSorted(AjaxRequestTarget target) {
    }

}
