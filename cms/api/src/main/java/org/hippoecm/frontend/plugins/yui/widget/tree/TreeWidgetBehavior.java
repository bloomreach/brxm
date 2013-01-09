/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.yui.widget.tree;

import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.tree.AbstractTree;
import org.apache.wicket.markup.html.tree.ITreeStateListener;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.widget.WidgetBehavior;

public class TreeWidgetBehavior extends WidgetBehavior {
    private static final long serialVersionUID = 1L;


    TreeWidgetSettings settings;

    boolean update = false;

    public TreeWidgetBehavior(TreeWidgetSettings settings) {
        super(settings);
        this.settings = settings;

        getTemplate().setInstance("YAHOO.hippo.TreeWidget");
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        super.addHeaderContribution(context);

        context.addModule(HippoNamespace.NS, "treewidget");
    }

    @Override
    public void bind(Component component) {
        super.bind(component);

        if (component instanceof AbstractTree) {
            AbstractTree tree = (AbstractTree) component;
            tree.getTreeState().addTreeStateListener(new TreeStateListener());
        }
    }

    public void render(AjaxRequestTarget target) {
        if (update) {
            if (target != null) {
                target.appendJavascript(getUpdateScript());
            }
        }
        reset();
    }

    private void reset() {
        update = false;
    }

    class TreeStateListener implements ITreeStateListener, IClusterable {

        @Override
        public void nodeCollapsed(Object node) {
            update = true;
        }

        @Override
        public void nodeExpanded(Object node) {
            update = true;
        }

        @Override
        public void nodeSelected(Object node) {
        }

        @Override
        public void nodeUnselected(Object node) {
        }

        @Override
        public void allNodesCollapsed() {
        }

        @Override
        public void allNodesExpanded() {
        }

    }

}
